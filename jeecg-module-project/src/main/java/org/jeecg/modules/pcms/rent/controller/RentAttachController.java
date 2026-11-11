package org.jeecg.modules.pcms.rent.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.RentAttachVO;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.jeecg.modules.pcms.rent.service.IRentAttachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金支付管理 - 租金附件。对应需求稿第 7 条。
 *
 * 附件按用户上传时的文件名显示，可下载可删除，图片支持在线预览；
 * 提交审核前才能上传和删除，提交之后一律只读。
 * 银行回单是对账凭证，任何时候都不允许删。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-租金附件")
@RestController
@RequestMapping("/rent/attach")
public class RentAttachController {

    /** 一次分片读多少字节 */
    private static final int BUFFER_SIZE = 8192;

    @Autowired
    private IRentAttachService attachService;

    @ApiOperation(value = "附件列表", notes = "按上传时间正序，含普通附件和银行回单")
    @GetMapping("/list")
    public Result<List<RentAttachVO>> list(@RequestParam(name = "ledgerId") Long ledgerId) {
        return Result.OK(attachService.listByLedger(ledgerId));
    }

    @ApiOperation(value = "上传附件",
            notes = "单个不超过50MB，支持图片、PDF、Office 文档和压缩包。"
                    + "bizType=ATTACH 业务附件（默认）：待提交、已驳回、纠纷暂停、"
                    + "待支付（传付款凭证）、已支付 都可以传；"
                    + "审核中不可传（提交后不能改），已对账归档后不可传；"
                    + "bizType=AUDIT 审核附件：只在审核中可传，供审核人驳回时带材料")
    @PostMapping("/upload")
    public Result<RentAttachVO> upload(@RequestParam(name = "ledgerId") Long ledgerId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(name = "bizType", required = false) String bizType) {
        return Result.OK("上传成功", attachService.upload(ledgerId, file, bizType));
    }
    @ApiOperation(value = "删除附件",
            notes = "待提交、已驳回、纠纷暂停、待支付 可以删（待支付传错凭证要能撤）；"
                    + "审核中、已支付、已对账 不能删（凭证留痕）；"
                    + "银行回单和审核附件任何时候都不能删")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        attachService.delete(id);
        return Result.OK("删除成功");
    }

    @ApiOperation(value = "下载附件", notes = "浏览器另存，文件名为用户上传时的原始名称")
    @GetMapping("/download")
    public void download(@RequestParam(name = "id") Long id, HttpServletResponse response) {
        write(id, response, true);
    }

    @ApiOperation(value = "在线预览", notes = "图片直接内联返回，非图片按下载处理")
    @GetMapping("/preview")
    public void preview(@RequestParam(name = "id") Long id, HttpServletResponse response) {
        write(id, response, false);
    }

    // ------------------------------------------------------------------

    private void write(Long id, HttpServletResponse response, boolean attachment) {
        TRentAttach attach = attachService.getExisting(id);
        File file = attachService.resolveFile(attach);
        boolean inline = !attachment && attach.getIsImage() != null && attach.getIsImage() == 1;

        try {
            response.setContentType(contentType(file, attach));
            response.setContentLengthLong(file.length());
            // 文件名里有中文，按 RFC 5987 编码，各浏览器都认
            String encoded = URLEncoder.encode(attach.getFileName(), "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition",
                    (inline ? "inline" : "attachment") + "; filename=\"" + encoded
                            + "\"; filename*=UTF-8''" + encoded);

            try (InputStream in = Files.newInputStream(file.toPath());
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } catch (IOException e) {
            // 响应已经开始写了，这时再改成 JSON 错误码也回不去，只能记日志
            log.error("附件输出失败 id={} file={}", id, attach.getFileName(), e);
        }
    }

    private String contentType(File file, TRentAttach attach) {
        try {
            String probed = Files.probeContentType(file.toPath());
            if (probed != null) {
                return probed;
            }
        } catch (IOException e) {
            log.debug("探测文件类型失败，按二进制流输出：{}", attach.getFileName());
        }
        return "application/octet-stream";
    }
}
