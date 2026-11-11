package org.jeecg.modules.pcms.util;


import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointDay;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointMonth;
import org.jeecg.modules.pcms.energy.entity.DeviceEquipmentPointYear;

import java.util.*;
import java.util.stream.Collectors;

public class DeviceDataAggregator {
    
    // 固定指标ID集合
    private static final Set<String> FIXED_POINT_IDS;

    static {
        FIXED_POINT_IDS = new HashSet<>();
        FIXED_POINT_IDS.add("p1");
        FIXED_POINT_IDS.add("p87");
        FIXED_POINT_IDS.add("p88");
        FIXED_POINT_IDS.add("p2");
        FIXED_POINT_IDS.add("p67");
        FIXED_POINT_IDS.add("p24");
        FIXED_POINT_IDS.add("p25");
        FIXED_POINT_IDS.add("p14");
        FIXED_POINT_IDS.add("p43");
        FIXED_POINT_IDS.add("p26");
        FIXED_POINT_IDS.add("p18");
        FIXED_POINT_IDS.add("p19");
        FIXED_POINT_IDS.add("p20");
        FIXED_POINT_IDS.add("p21");
        FIXED_POINT_IDS.add("p22");
        FIXED_POINT_IDS.add("p23");
        FIXED_POINT_IDS.add("p94");
        FIXED_POINT_IDS.add("p27");
        FIXED_POINT_IDS.add("p7356");
        FIXED_POINT_IDS.add("p3");
        FIXED_POINT_IDS.add("p15");
        FIXED_POINT_IDS.add("p16");
        FIXED_POINT_IDS.add("p17");
        FIXED_POINT_IDS.add("p95");
        FIXED_POINT_IDS.add("p90");
        FIXED_POINT_IDS.add("p120");
        FIXED_POINT_IDS.add("p4");
        FIXED_POINT_IDS.add("p33");
        FIXED_POINT_IDS.add("p34");
        FIXED_POINT_IDS.add("p35");
        FIXED_POINT_IDS.add("p36");
        FIXED_POINT_IDS.add("p37");
        FIXED_POINT_IDS.add("p38");
        FIXED_POINT_IDS.add("p39");
        FIXED_POINT_IDS.add("p40");
        FIXED_POINT_IDS.add("p41");
        FIXED_POINT_IDS.add("p42");
        FIXED_POINT_IDS.add("p5");
        FIXED_POINT_IDS.add("p7");
        FIXED_POINT_IDS.add("p9");
        FIXED_POINT_IDS.add("p45");
        FIXED_POINT_IDS.add("p47");
        FIXED_POINT_IDS.add("p49");
        FIXED_POINT_IDS.add("p51");
        FIXED_POINT_IDS.add("p53");
        FIXED_POINT_IDS.add("p55");
        FIXED_POINT_IDS.add("p57");
        FIXED_POINT_IDS.add("p6");
        FIXED_POINT_IDS.add("p8");
        FIXED_POINT_IDS.add("p10");
        FIXED_POINT_IDS.add("p46");
        FIXED_POINT_IDS.add("p48");
        FIXED_POINT_IDS.add("p50");
        FIXED_POINT_IDS.add("p52");
        FIXED_POINT_IDS.add("p54");
        FIXED_POINT_IDS.add("p56");
        FIXED_POINT_IDS.add("p58");
        FIXED_POINT_IDS.add("p96");
        FIXED_POINT_IDS.add("p97");
        FIXED_POINT_IDS.add("p98");
        FIXED_POINT_IDS.add("p99");
        FIXED_POINT_IDS.add("p100");
        FIXED_POINT_IDS.add("p101");
        FIXED_POINT_IDS.add("p102");
        FIXED_POINT_IDS.add("p103");
        FIXED_POINT_IDS.add("p104");
        FIXED_POINT_IDS.add("p105");
        FIXED_POINT_IDS.add("p70");
        FIXED_POINT_IDS.add("p71");
        FIXED_POINT_IDS.add("p72");
        FIXED_POINT_IDS.add("p73");
        FIXED_POINT_IDS.add("p74");
        FIXED_POINT_IDS.add("p75");
        FIXED_POINT_IDS.add("p76");
        FIXED_POINT_IDS.add("p77");
        FIXED_POINT_IDS.add("p78");
        FIXED_POINT_IDS.add("p79");
    }
    /**
     * 将原始数据转换为适合数据库插入的格式
     * 返回: List<DeviceDataRecord> - 每个记录对应数据库中的一行
     */
    public List<DeviceEquipmentPointDay> convertToDatabaseDayRecords(Map<String, Map<String, List<Map<String, String>>>> resultData, String source) {

        //日
        return resultData.entrySet()
                .stream()
                .flatMap(psKeyEntry -> {
                    String psKey = psKeyEntry.getKey();

                    // 按时间戳分组，收集所有指标值
                    Map<String, Map<String, String>> timestampData = aggregateByDayTimestamp(psKeyEntry.getValue());

                    // 转换为数据库记录
                    return timestampData.entrySet()
                            .stream()
                            .map(entry -> createDatabaseDayRecord(psKey, entry.getKey(), entry.getValue(), source));
                })
                .collect(Collectors.toList());

    }


    /**
     * 将原始数据转换为适合数据库插入的格式
     * 返回: List<DeviceDataRecord> - 每个记录对应数据库中的一行
     */
    public List<DeviceEquipmentPointMonth> convertToDatabaseMonthRecords(Map<String, Map<String, List<Map<String, String>>>> resultData, String source) {
        return resultData.entrySet()
                .stream()
                .flatMap(psKeyEntry -> {
                    String psKey = psKeyEntry.getKey();

                    // 按时间戳分组，收集所有指标值
                    Map<String, Map<String, String>> timestampData = aggregateByTimestamp(psKeyEntry.getValue());

                    // 转换为数据库记录
                    return timestampData.entrySet()
                            .stream()
                            .map(entry -> createDatabaseMonthRecord(psKey, entry.getKey(), entry.getValue(), source));
                })
                .collect(Collectors.toList());
    }

    public List<DeviceEquipmentPointYear> convertToDatabaseYearRecords(Map<String, Map<String, List<Map<String, String>>>> resultData, String source) {
        return resultData.entrySet()
                .stream()
                .flatMap(psKeyEntry -> {
                    String psKey = psKeyEntry.getKey();

                    // 按时间戳分组，收集所有指标值
                    Map<String, Map<String, String>> timestampData = aggregateByTimestamp(psKeyEntry.getValue());

                    // 转换为数据库记录
                    return timestampData.entrySet()
                            .stream()
                            .map(entry -> createDatabaseYearRecord(psKey, entry.getKey(), entry.getValue(), source));
                })
                .collect(Collectors.toList());
    }

    /**
     * 按时间戳聚合指标数据
     */
    private Map<String, Map<String, String>> aggregateByTimestamp(Map<String, List<Map<String, String>>> indicators) {

        Map<String, Map<String, String>> result = new HashMap<>();

        indicators.forEach((indicatorId, dataList) -> {
            for (Map<String, String> data : dataList) {
                // 获取时间戳（统一处理）
                String timeStamp = data.get("time_stamp");
                if (timeStamp == null || timeStamp.isEmpty()) continue;

                // 根据 indicatorId 决定取哪个字段的值
                String valueKey = getValueKeyForIndicator(indicatorId);
                String value = data.get(valueKey);

                // 初始化时间戳下的 map
                result.computeIfAbsent(timeStamp, k -> new HashMap<>())
                        .put(indicatorId, value); // 存储 p1, p2 等对应的值
            }
        });

        return result;
    }

    private Map<String, Map<String, String>> aggregateByDayTimestamp(Map<String, List<Map<String, String>>> indicators) {

        Map<String, Map<String, String>> result = new HashMap<>();

        indicators.forEach((indicatorId, dataList) -> {
            for (Map<String, String> data : dataList) {
                // 获取时间戳（统一处理）
                String timeStamp = data.get("time_stamp");
                if (timeStamp == null || timeStamp.isEmpty()) continue;

                // 根据 indicatorId 决定取哪个字段的值
                String valueKey = getValueKeyDayForIndicator(indicatorId);
                String value = data.get(valueKey);

                // 初始化时间戳下的 map
                result.computeIfAbsent(timeStamp, k -> new HashMap<>())
                        .put(indicatorId, value); // 存储 p1, p2 等对应的值
            }
        });

        return result;
    }

    private String getValueKeyDayForIndicator(String indicatorId) {
        return "2";
    }
    private String getValueKeyForIndicator(String indicatorId) {
        // 可以扩展为 map 或配置表
        if ("p1".equals(indicatorId)) {
            return "4";
        }
        // 默认取 "2"
        return "2";
    }

    /**
     * 创建数据库记录对象
     */
    private DeviceEquipmentPointDay createDatabaseDayRecord(String psKey, String timestamp,
                                                            Map<String, String> indicators, String source) {

        DeviceEquipmentPointDay record = new DeviceEquipmentPointDay();
        record.setPsKey(psKey);
        record.setTimeStamp(timestamp);
        record.setSource(source);
        // type字段根据您的业务需求设置
        record.setType("4"); // 假设是60分钟类型

        // 设置所有指标值
        indicators.forEach((indicatorId, value) -> {
            setIndicatorDayValue(record, indicatorId, value);
        });

        return record;
    }
    private DeviceEquipmentPointMonth createDatabaseMonthRecord(String psKey, String timestamp,
                                                                Map<String, String> indicators, String source) {

        DeviceEquipmentPointMonth record = new DeviceEquipmentPointMonth();
        record.setPsKey(psKey);
        record.setTimeStamp(timestamp);
        record.setSource(source);
        // type字段根据您的业务需求设置
        record.setType("4"); // 假设是60分钟类型

        // 设置所有指标值
        indicators.forEach((indicatorId, value) -> {
            setIndicatorMonthValue(record, indicatorId, value);
        });

        return record;
    }

    private DeviceEquipmentPointYear createDatabaseYearRecord(String psKey, String timestamp,
                                                              Map<String, String> indicators, String source) {

        DeviceEquipmentPointYear record = new DeviceEquipmentPointYear();
        record.setPsKey(psKey);
        record.setTimeStamp(timestamp);
        record.setSource(source);
        // type字段根据您的业务需求设置
        record.setType("4"); // 假设是60分钟类型

        // 设置所有指标值
        indicators.forEach((indicatorId, value) -> {
            setIndicatorYearValue(record, indicatorId, value);
        });

        return record;
    }


    /**
     * 使用反射设置指标值
     */
    private void setIndicatorDayValue(DeviceEquipmentPointDay record, String indicatorId, String value) {
        try {
            String setterName = "set" + indicatorId.substring(0, 1).toUpperCase() + indicatorId.substring(1);
            java.lang.reflect.Method setter = record.getClass().getMethod(setterName, String.class);
            setter.invoke(record, value);
        } catch (Exception e) {
            // 忽略找不到setter方法的异常
        }
    }
    private void setIndicatorMonthValue(DeviceEquipmentPointMonth record, String indicatorId, String value) {
        try {
            String setterName = "set" + indicatorId.substring(0, 1).toUpperCase() + indicatorId.substring(1);
            java.lang.reflect.Method setter = record.getClass().getMethod(setterName, String.class);
            setter.invoke(record, value);
        } catch (Exception e) {
            // 忽略找不到setter方法的异常
        }
    }
    private void setIndicatorYearValue(DeviceEquipmentPointYear record, String indicatorId, String value) {
        try {
            String setterName = "set" + indicatorId.substring(0, 1).toUpperCase() + indicatorId.substring(1);
            java.lang.reflect.Method setter = record.getClass().getMethod(setterName, String.class);
            setter.invoke(record, value);
        } catch (Exception e) {
            // 忽略找不到setter方法的异常
        }
    }
}