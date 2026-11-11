package org.jeecg.modules.pcms.rent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.OperLogVO;
import org.jeecg.modules.pcms.rent.service.IOperLogQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租金支付管理 - 操作日志。对应需求稿第 11 条。
 *
 * 每条日志都带一句 summary，直接写清楚改了什么，
 * 比如"安装块数 由 10 改为 30"，页面右侧照着渲染即可。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-操作日志")
@RestController
@RequestMapping("/rent/operLog")
public class RentOperLogController {

    @Autowired
    private IOperLogQueryService operLogQueryService;

    @ApiOperation(value = "操作日志分页列表",
            notes = "bizType 取值：农户信息 / 安装信息 / 租金 / 租金台账 / 租金附件 / 导入；都不传则查全部")
    @GetMapping("/list")
    public Result<IPage<OperLogVO>> list(@RequestParam(name = "bizType", required = false) String bizType,
                                         @RequestParam(name = "bizId", required = false) Long bizId,
                                         @RequestParam(name = "operUser", required = false) String operUser,
                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                         @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.OK(operLogQueryService.pageList(bizType, bizId, operUser, pageNo, pageSize));
    }

    @ApiOperation(value = "农户相关的全部操作日志",
            notes = "农户详情页右侧用。含该农户本身、名下安装信息、名下租金台账及其附件的操作")
    @GetMapping("/station")
    public Result<IPage<OperLogVO>> station(@RequestParam(name = "stationId") Long stationId,
                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.OK(operLogQueryService.pageByStation(stationId, pageNo, pageSize));
    }

    @ApiOperation(value = "某条租金台账的操作日志", notes = "租金详情页右侧用，含台账本身和它的附件操作")
    @GetMapping("/ledger")
    public Result<IPage<OperLogVO>> ledger(@RequestParam(name = "ledgerId") Long ledgerId,
                                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                           @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.OK(operLogQueryService.pageByLedger(ledgerId, pageNo, pageSize));
    }
}
