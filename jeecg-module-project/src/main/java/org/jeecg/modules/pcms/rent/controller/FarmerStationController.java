package org.jeecg.modules.pcms.rent.controller;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.DictOptionVO;
import org.jeecg.modules.pcms.rent.dto.FarmerAuditDTO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationDetailVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationFormDTO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationListVO;
import org.jeecg.modules.pcms.rent.dto.FarmerStationQueryDTO;
import org.jeecg.modules.pcms.rent.dto.InstallFormDTO;
import org.jeecg.modules.pcms.rent.dto.ProjectOptionVO;
import org.jeecg.modules.pcms.rent.entity.TFarmerChangeRequest;
import org.jeecg.modules.pcms.rent.service.IFarmerAuditService;
import org.jeecg.modules.pcms.rent.service.IFarmerStationBizService;
import org.jeecg.modules.pcms.rent.service.IFarmerStationQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租金支付管理 - 农户信息管理。
 * 对应需求稿第 3、4、5 条。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-农户信息管理")
@RestController
@RequestMapping("/rent/farmerStation")
public class FarmerStationController {

    @Autowired
    private IFarmerStationQueryService farmerStationQueryService;
    @Autowired
    private IFarmerStationBizService farmerStationBizService;
    @Autowired
    private IFarmerAuditService farmerAuditService;

    // ==================== 第 3、4 条：列表与搜索 ====================

    @ApiOperation(value = "农户信息分页列表",
            notes = "展示编号、姓名、地址、总容量、发电户号、并网日期、租金截止日期、块数；"
                    + "姓名/发电户号/安装地址为模糊查询，并网日期为区间查询。"
                    + "审核状态筛选可以传 auditStatus 精确到明细状态（支持 2 或 2,4,6,8 这种逗号分隔的多个值），"
                    + "也可以只传 auditStatusGroup=PENDING（所有待审核）或 REJECTED（所有驳回），"
                    + "后端把新增/修改 × 项目/业务/领导合并成一档；两个都传时以 auditStatus 为准。"
                    + "返回的 auditStatusName 仍是明细文案")
    @GetMapping("/list")
    public Result<IPage<FarmerStationListVO>> list(FarmerStationQueryDTO query,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(farmerStationQueryService.pageList(query, pageNo, pageSize));
    }

    @ApiOperation(value = "所属项目下拉框",
            notes = "返回大区分组的项目列表，共 5 个大区 12 个项目；"
                    + "查询传 projectId 精确到期数，传 regionCode 查整个大区")
    @GetMapping("/projectOptions")
    public Result<List<ProjectOptionVO>> projectOptions() {
        return Result.OK(farmerStationQueryService.projectOptions());
    }

    // ==================== 第 4 条：农户增删改 ====================

    @ApiOperation(value = "新增农户",
            notes = "只有项目经理、管理员可以新增。提交后进入项目审核。"
                    + "租金截止日期由并网日期加项目租期自动算出，不用传。"
                    + "安装信息选填：请求体里带上 installs 数组，就能在新建农户的同时把安装信息一起录进来"
                    + "（字段与 /rent/farmerStation/install/save 一致，其中 stationId 不用传）；"
                    + "不传或传空数组表示只建农户。安装信息有一条存不下，整个新增会一起回滚，"
                    + "不会留下「有农户、没安装信息」的中间状态")
    @PostMapping("/add")
    public Result<Long> add(@RequestBody FarmerStationFormDTO form) {
        return Result.OK("新增成功", farmerStationBizService.addFarmer(form));
    }

    @ApiOperation(value = "编辑农户",
            notes = "只有项目经理、管理员可以修改。提交后进入项目审核。"
                    + "改了姓名或户号会同步刷新安装信息、租金表里的冗余字段；改了并网日期会重算租金截止日期")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody FarmerStationFormDTO form) {
        farmerStationBizService.editFarmer(form);
        return Result.OK("编辑成功");
    }

    @ApiOperation(value = "删除农户", notes = "逻辑删除，同时级联删除其安装信息和租金记录")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        farmerStationBizService.deleteFarmer(id);
        return Result.OK("删除成功");
    }

    @ApiOperation(value = "批量删除农户", notes = "ids 用英文逗号分隔")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<Long> idList = new java.util.ArrayList<>();
        for (String s : Arrays.asList(ids.split(","))) {
            if (!s.trim().isEmpty()) {
                idList.add(Long.valueOf(s.trim()));
            }
        }
        farmerStationBizService.deleteFarmerBatch(idList);
        return Result.OK("删除成功");
    }

    // ==================== 第 5 条：详情与安装信息、租金的增删改 ====================

    @ApiOperation(value = "农户详情",
            notes = "返回三层结构：农户主信息 -> 多条安装信息 -> 每条安装信息下的各档位租金单价。"
                    + "农户姓名只读，其余可改")
    @GetMapping("/detail")
    public Result<FarmerStationDetailVO> detail(@RequestParam(name = "id") Long id) {
        return Result.OK(farmerStationBizService.detail(id));
    }

    @ApiOperation(value = "新增或编辑安装信息",
            notes = "不传 id 为新增，传 id 为编辑。租金单价随安装信息一起提交，"
                    + "保存时该条安装信息下的旧租金记录整体替换；块数变了金额自动重算")
    @PostMapping("/install/save")
    public Result<Long> saveInstall(@RequestBody InstallFormDTO form) {
        return Result.OK("保存成功", farmerStationBizService.saveInstall(form));
    }

    @ApiOperation(value = "删除安装信息", notes = "同时删除该安装信息下的全部租金记录，并重算农户总容量总块数")
    @DeleteMapping("/install/delete")
    public Result<String> deleteInstall(@RequestParam(name = "installId") Long installId) {
        farmerStationBizService.deleteInstall(installId);
        return Result.OK("删除成功");
    }

    @ApiOperation(value = "安装房型下拉框",
            notes = "按所属大区过滤，北京4种、天津3种、徐州海南河南各3种。"
                    + "stationId 和 projectId 传一个即可：农户详情页传 stationId；"
                    + "新建农户时农户还没建出来，传 projectId")
    @GetMapping("/installTypeOptions")
    public Result<List<DictOptionVO>> installTypeOptions(
            @RequestParam(name = "stationId", required = false) Long stationId,
            @RequestParam(name = "projectId", required = false) Long projectId) {
        return Result.OK(farmerStationBizService.installTypeOptions(stationId, projectId));
    }

    @ApiOperation(value = "租金档位下拉框",
            notes = "按所属大区过滤，返回档位名称和适用区间。stationId 和 projectId 传一个即可，说明同上")
    @GetMapping("/priceTypeOptions")
    public Result<List<DictOptionVO>> priceTypeOptions(
            @RequestParam(name = "stationId", required = false) Long stationId,
            @RequestParam(name = "projectId", required = false) Long projectId) {
        return Result.OK(farmerStationBizService.priceTypeOptions(stationId, projectId));
    }

    // ==================== 农户变更审核（需求 28~33） ====================

    @ApiOperation(value = "待审核的农户变更详情",
            notes = "审核弹窗用。返回本次变更的改前/改后快照、提交人、提交时间；"
                    + "没有待审核变更时 data 为 null")
    @GetMapping("/change/pending")
    public Result<TFarmerChangeRequest> pending(@RequestParam(name = "stationId") Long stationId) {
        return Result.OK(farmerAuditService.pendingOf(stationId));
    }

    @ApiOperation(value = "审核通过",
            notes = "三级审核：项目审核（项目经理/管理员）通过后转业务审核，"
                    + "业务审核（业务专员）通过后转领导审核，"
                    + "领导审核（公司领导）通过后状态恢复正常，该农户的租金解锁。"
                    + "每级只能由本级审核角色操作，接口按申请上的 auditStage 自动判断")
    @PostMapping("/change/approve")
    public Result<String> approve(@RequestBody FarmerAuditDTO form) {
        farmerAuditService.approve(form.getRequestId(), form.getRemark());
        return Result.OK("审核通过");
    }

    @ApiOperation(value = "审核驳回",
            notes = "必须填驳回原因；任何一级驳回都会终止本次申请。"
                    + "修改类申请会把数据回滚到修改前，新增类申请保留农户但标记为不可用（不能生成租金）")
    @PostMapping("/change/reject")
    public Result<String> reject(@RequestBody FarmerAuditDTO form) {
        farmerAuditService.reject(form.getRequestId(), form.getRemark());
        return Result.OK("已驳回");
    }

    @ApiOperation(value = "取消修改",
            notes = "项目经理/管理员在审核前主动取消；修改类申请会回滚数据")
    @PostMapping("/change/cancel")
    public Result<String> cancel(@RequestBody FarmerAuditDTO form) {
        farmerAuditService.cancel(form.getRequestId());
        return Result.OK("已取消");
    }
}
