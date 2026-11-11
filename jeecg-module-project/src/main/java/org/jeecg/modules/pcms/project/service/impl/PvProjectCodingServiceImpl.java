package org.jeecg.modules.pcms.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.DateUtils;
import org.jeecg.modules.pcms.common.util.PinyinUtil;
import org.jeecg.modules.pcms.project.entity.PvProjectCoding;
import org.jeecg.modules.pcms.project.mapper.PvProjectCodingMapper;
import org.jeecg.modules.pcms.project.service.IPvProjectCodingService;
import org.jeecg.modules.pcms.common.set.service.ISetRegionsService;
import org.jeecg.modules.pcms.project.service.IRegionsServiceOne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.Date;

/**
 * @Description: pv_project_coding
 * @Author: jun
 * @Date: 2023-03-10
 * @Version: V1.0
 */
@Service
public class PvProjectCodingServiceImpl extends ServiceImpl<PvProjectCodingMapper, PvProjectCoding> implements IPvProjectCodingService {
    @Autowired
    private ISetRegionsService setRegionsService;
    @Autowired
    private IRegionsServiceOne iRegionsServiceOne;

    @Override
    public String generateProjectCode(String areaAddress,String projectName) {
        String name = iRegionsServiceOne.getProjectName(projectName);
        int index1 = StringUtils.indexOf(areaAddress, ",", 2);
        int index2 = StringUtils.indexOf(areaAddress, ",", index1 + 1);
        String provinceId = areaAddress.substring(1, index1);
        String cityId = areaAddress.substring(index1 + 1, index2);
        QueryWrapper<PvProjectCoding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date_format(code_date,'%Y')", DateUtils.getYear());
        queryWrapper.eq("province_id", provinceId);
        queryWrapper.eq("city_id", cityId);
        PvProjectCoding pvProjectCoding = baseMapper.selectOne(queryWrapper);
        if (pvProjectCoding == null) {
            // add
            String prefix = PinyinUtil.getPinYinHeadChar(setRegionsService.getLocalName(Integer.valueOf(provinceId)) + setRegionsService.getLocalName(Integer.valueOf(cityId)));
            pvProjectCoding = new PvProjectCoding();
            pvProjectCoding.setId(0);
            pvProjectCoding.setCodeDate(new Date());
            pvProjectCoding.setProvinceId(Integer.valueOf(provinceId));
            pvProjectCoding.setCityId(Integer.valueOf(cityId));
            pvProjectCoding.setPrefix(prefix);
            pvProjectCoding.setBizcode(0);
            baseMapper.insert(pvProjectCoding);
        }
        pvProjectCoding.setBizcode(pvProjectCoding.getBizcode() + 1);

//        String code = String.valueOf(DateUtils.getYear())
//                + pvProjectCoding.getPrefix();
        String code = String.valueOf(DateUtils.getYear())
                + (StringUtils.isBlank(name) ? "QG" : name);
        Format numberFormat = new DecimalFormat("0000");
        code = code + numberFormat.format(pvProjectCoding.getBizcode());

        baseMapper.updateById(pvProjectCoding);
        return code;
    }
}
