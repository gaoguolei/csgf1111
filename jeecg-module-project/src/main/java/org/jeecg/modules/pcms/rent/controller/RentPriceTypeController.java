package org.jeecg.modules.pcms.rent.controller;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeFormDTO;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeSaveDTO;
import org.jeecg.modules.pcms.rent.dto.RentPriceTypeVO;
import org.jeecg.modules.pcms.rent.service.IRentPriceTypeBizService;
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
 * 租金支付管理 - 租金档位字典。
 *
 * 维护的是"某年某档位"这类字典（天津的 1-5年 / 6-25年，徐州的 前10年 / 后15年），
 * 农户的单价挂在档位上，所以档位的增删改会影响租金计算，改删前都会先查有没有农户在用。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-租金档位")
@RestController
@RequestMapping("/rent/priceType")
public class RentPriceTypeController {

    @Autowired
    private IRentPriceTypeBizService bizService;

    @ApiOperation(value = "某大区的租金档位",
            notes = "按大区查全部档位。每一项带 usedCount（已被多少条租金单价引用）和 editable"
                    + "（1=没人用，可以改删；0=已被引用，改删会被拦）。"
                    + "editable=0 时 blockReason 里是给用户看的原因")
    @GetMapping("/list")
    public Result<List<RentPriceTypeVO>> list(@RequestParam(name = "regionCode") String regionCode) {
        return Result.OK(bizService.listByRegion(regionCode));
    }

    @ApiOperation(value = "新增租金档位",
            notes = "成组新增，合计必须从第1年连续覆盖到第25年，通常两条。"
                    + "请求体：{\"regionCode\":\"TJLF\",\"types\":["
                    + "{\"typeName\":\"1-3年\",\"startYear\":1,\"endYear\":3},"
                    + "{\"typeName\":\"4-25年\",\"startYear\":4,\"endYear\":25}]}。"
                    + "档位编码按 TJ_Y1_3 这种规则自动生成")
    @PostMapping("/add")
    public Result<String> add(@RequestBody RentPriceTypeSaveDTO form) {
        bizService.addTypes(form);
        return Result.OK("新增成功");
    }

    @ApiOperation(value = "修改租金档位",
            notes = "只能改没被引用的档位，被农户用了会返回失败并说明有多少条在用。"
                    + "请求体：{\"id\":15,\"typeName\":\"1-3年\",\"startYear\":1,\"endYear\":3}")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody RentPriceTypeFormDTO form) {
        bizService.editType(form);
        return Result.OK("修改成功");
    }

    @ApiOperation(value = "删除租金档位",
            notes = "只能删没被引用的档位，被农户用了会返回失败并说明有多少条在用")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        bizService.deleteType(id);
        return Result.OK("删除成功");
    }
}