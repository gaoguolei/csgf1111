package org.jeecg.modules.pcms.rent.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.pcms.rent.dto.FarmerStationQueryDTO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerQueryDTO;
import org.jeecg.modules.pcms.rent.dto.RentLedgerSummaryVO;
import org.jeecg.modules.pcms.rent.dto.RentPdfRequest;
import org.jeecg.modules.pcms.rent.entity.TFarmerStation;
import org.jeecg.modules.pcms.rent.entity.TRentLedger;
import org.jeecg.modules.pcms.rent.service.IFarmerStationQueryService;
import org.jeecg.modules.pcms.rent.service.IRentLedgerQueryService;
import org.jeecg.modules.pcms.rent.util.RentPdfAssembler;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租金支付管理 - 导出（需求第24条）。农户信息、租金信息支持导出 excel 和 pdf。
 *
 * 【Excel：一个文件装所有数据】
 * 收列表用过的那个 QueryDTO，内部复用列表的筛选逻辑
 * （IFarmerStationQueryService.listForExport / IRentLedgerQueryService.listForExport），
 * 所以"导出的就是页面上筛出来的"。租户隔离（需求第14条）也在这条共用链路里生效。
 *
 * 【PDF：勾选几条就出几个文件】
 * 列表页勾选的 id 逐个生成独立的 PDF（一户一份对账单），打成一个 zip 返回。
 * 打包而不是逐个下载，是因为浏览器会对连续的多个下载做拦截。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-导出")
@RestController
@RequestMapping("/rent/export")
public class RentExportController {

    @Autowired
    private IFarmerStationQueryService farmerQueryService;
    @Autowired
    private IRentLedgerQueryService ledgerQueryService;
    @Autowired
    private RentPdfAssembler pdfAssembler;

    // ==================== Excel：勾选或按筛选，都合成一个文件 ====================

    @ApiOperation(value = "导出农户信息Excel",
            notes = "两种用法："
                    + "① 列表页勾选了记录 → 传 selections（如 selections=1,2,3），只导出勾选的这几条；"
                    + "② 没勾选 → 不传 selections，筛选条件与 /rent/farmerStation/list 一致"
                    + "（姓名、户号、地址、项目、大区、编号、并网日期区间、排序），导出全部命中行。"
                    + "两种都只生成一个 Excel 文件")
    @GetMapping("/farmerXls")
    public ModelAndView exportFarmerXls(FarmerStationQueryDTO query,
                                        @RequestParam(name = "selections", required = false) List<Long> selections) {
        List<TFarmerStation> list = checked(selections)
                ? farmerQueryService.listByIdsForExport(selections)
                : farmerQueryService.listForExport(query);
        return excel("农户信息", TFarmerStation.class, list);
    }

    @ApiOperation(value = "导出租金台账Excel",
            notes = "两个 Sheet：先「汇总」（按项目+期数统计条数、应付合计、实付合计），"
                    + "再「租金台账」明细。"
                    + "传 selections 时只导出勾选的那几条；"
                    + "不传 selections 时导出当前范围内**所有台账** —— "
                    + "不受列表页的年度/状态页签限制（传了 rentYear 才按年度导），"
                    + "姓名、户号、项目、期数、并网日期这些显式筛选照常生效，"
                    + "租户范围按登录用户和顶部的 renterId 参数")
    @GetMapping("/ledgerXls")
    public void exportLedgerXls(RentLedgerQueryDTO query,
                                @RequestParam(name = "selections", required = false) List<Long> selections,
                                HttpServletResponse response) {
        List<TRentLedger> list = checked(selections)
                ? ledgerQueryService.listByIdsForExport(selections)
                : ledgerQueryService.listForExport(query);
        List<RentLedgerSummaryVO> summary = ledgerQueryService.summarize(list);

        // EasyPoi 的多 Sheet 导出：每个 Map 三个 key —— title(ExportParams) / entity(Class) / data(集合)
        List<Map<String, Object>> sheets = new ArrayList<>();
        sheets.add(sheet("汇总", RentLedgerSummaryVO.class, summary));
        sheets.add(sheet("租金台账", TRentLedger.class, list));

        try (org.apache.poi.ss.usermodel.Workbook wb =
                     ExcelExportUtil.exportExcel(sheets, ExcelType.HSSF)) {
            writeExcel(response, "租金台账", wb);
        } catch (IOException e) {
            throw new JeecgBootException("导出租金台账失败：" + e.getMessage());
        }
    }

    /** 拼一个 EasyPoi 多 Sheet 条目 */
    private Map<String, Object> sheet(String name, Class<?> clazz, Collection<?> data) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", new ExportParams(name, "导出人:" + realname(), name));
        m.put("entity", clazz);
        m.put("data", data);
        return m;
    }

    // ==================== PDF：勾选几条出几个文件，打包成 zip ====================

    @ApiOperation(value = "导出农户信息PDF",
            notes = "请求体：{\"selections\":[1,2,3]}，列表页勾选的农户ID。"
                    + "每个农户生成一个独立的 PDF（基本信息 + 安装信息），打包成 zip 下载。"
                    + "一条都没勾时返回提示，不会导出空包")
    @PostMapping("/farmerPdf")
    public void exportFarmerPdf(@RequestBody RentPdfRequest req, HttpServletResponse response) {
        List<Long> ids = req == null ? null : req.getSelections();
        if (ids == null || ids.isEmpty()) {
            throw new JeecgBootException("请先勾选要导出的农户");
        }
        List<RentPdfAssembler.Doc> docs = new ArrayList<>();
        for (Long id : ids) {
            docs.add(pdfAssembler.farmerDoc(id));
        }
        writeZip(response, "农户信息", pdfAssembler.zip(docs));
    }

    @ApiOperation(value = "导出租金台账PDF",
            notes = "请求体：{\"selections\":[10,11]}，列表页勾选的台账ID。"
                    + "每条台账生成一个独立的 PDF（基本信息 + 金额信息 + 月明细 + 安装信息 + 附件图片），"
                    + "打包成 zip 下载。一条都没勾时返回提示")
    @PostMapping("/ledgerPdf")
    public void exportLedgerPdf(@RequestBody RentPdfRequest req, HttpServletResponse response) {
        List<Long> ids = req == null ? null : req.getSelections();
        if (ids == null || ids.isEmpty()) {
            throw new JeecgBootException("请先勾选要导出的租金台账");
        }
        List<RentPdfAssembler.Doc> docs = new ArrayList<>();
        for (Long id : ids) {
            docs.add(pdfAssembler.ledgerDoc(id));
        }
        writeZip(response, "租金台账", pdfAssembler.zip(docs));
    }

    /** 把 EasyPoi 生成的 Workbook 写进响应流 */
    private void writeExcel(HttpServletResponse response, String baseName,
                            org.apache.poi.ss.usermodel.Workbook wb) throws IOException {
        String fileName = baseName + ".xls";
        String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        wb.write(response.getOutputStream());
        response.getOutputStream().flush();
    }

    /** 把 PDF 字节写进响应流 */
    private void writePdf(HttpServletResponse response, String baseName, byte[] content) {
        try {
            String fileName = baseName + ".pdf";
            String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new JeecgBootException("导出PDF失败：" + e.getMessage());
        }
    }

    /** 勾选集合是否有效。空集合和 null 都当"没勾选"，走筛选条件导出 */
    private boolean checked(List<Long> selections) {
        return selections != null && !selections.isEmpty();
    }

    // ==================== 内部 ====================

    /** EasyPoi 导出视图，返回的 ModelAndView 由 JeecgEntityExcelView 渲染成 xls 下载 */
    private ModelAndView excel(String title, Class<?> clazz, List<?> list) {
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.FILE_NAME, title);
        mv.addObject(NormalExcelConstants.CLASS, clazz);
        mv.addObject(NormalExcelConstants.PARAMS,
                new ExportParams(title, "导出人:" + realname(), title));
        mv.addObject(NormalExcelConstants.DATA_LIST, list);
        return mv;
    }

    /**
     * 把 zip 字节写进响应流。
     *
     * 文件名是中文，必须 URL 编码后再放进 Content-Disposition，
     * 否则浏览器下载下来是一串乱码。filename* 是 RFC 5987 的写法，
     * 老浏览器认 filename、新的认 filename*，两个都给上最稳。
     */
    private void writeZip(HttpServletResponse response, String baseName, byte[] zip) {
        String fileName = baseName + ".zip";
        try {
            String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
            response.setContentLength(zip.length);
            response.getOutputStream().write(zip);
            response.getOutputStream().flush();
        } catch (UnsupportedEncodingException e) {
            throw new JeecgBootException("文件名编码失败");
        } catch (IOException e) {
            // 用户中途取消下载会走到这里，不是服务端问题，记一条日志即可
            log.warn("导出下载中断：{}", e.getMessage());
        }
    }

    private String realname() {
        try {
            LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            return user == null || user.getRealname() == null ? "" : user.getRealname();
        } catch (Exception e) {
            return "";
        }
    }
}
