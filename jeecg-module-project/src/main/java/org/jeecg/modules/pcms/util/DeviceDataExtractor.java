package org.jeecg.modules.pcms.util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.AbstractMap.SimpleEntry;


public class DeviceDataExtractor {
    
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
     * 方法1: 提取为 Map<pskey, Map<timestamp, Map<indicatorId, value>>>
     */
    public Map<String, Map<String, Map<String, String>>> extractDeviceData(
            Map<String, Map<String, List<Map<String, String>>>> resultData) {

        return resultData.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> extractTimestampData(entry.getValue())
                ));
    }

    private Map<String, Map<String, String>> extractTimestampData(
            Map<String, List<Map<String, String>>> indicators) {

        return indicators.entrySet()
                .stream()
                .filter(entry -> FIXED_POINT_IDS.contains(entry.getKey()))
                .flatMap(entry ->
                        entry.getValue()
                                .stream()
                                .map(dataMap ->
                                        new AbstractMap.SimpleEntry<>(
                                                dataMap.get("time_stamp"),
                                                new AbstractMap.SimpleEntry<>(entry.getKey(), dataMap.get("2"))
                                        )
                                )
                )
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        Collectors.mapping(
                                AbstractMap.SimpleEntry::getValue,
                                Collectors.toMap(
                                        AbstractMap.SimpleEntry::getKey,
                                        SimpleEntry::getValue
                                )
                        )
                ));
    }
    /**
     * 方法2: 提取为 List<DataRecord>
     */
    public List<DataRecord> extractDeviceDataAsList(
            Map<String, Map<String, List<Map<String, String>>>> resultData) {

        return resultData.entrySet()
                .stream()
                .flatMap(psKeyEntry -> {
                    String psKey = psKeyEntry.getKey();

                    // 按时间戳分组收集指标数据
                    Map<String, Map<String, String>> timestampData = psKeyEntry.getValue()
                            .entrySet()
                            .stream()
                            .filter(indicatorEntry -> FIXED_POINT_IDS.contains(indicatorEntry.getKey()))
                            .flatMap(indicatorEntry ->
                                    indicatorEntry.getValue()
                                            .stream()
                                            .map(dataMap ->
                                                    new SimpleEntry<>(
                                                            dataMap.get("time_stamp"),
                                                            new SimpleEntry<>(indicatorEntry.getKey(), dataMap.get("2"))
                                                    )
                                            )
                            )
                            .filter(entry -> entry.getKey() != null)
                            .collect(Collectors.groupingBy(
                                    SimpleEntry::getKey,
                                    Collectors.mapping(
                                            SimpleEntry::getValue,
                                            Collectors.toMap(
                                                    SimpleEntry::getKey,
                                                    SimpleEntry::getValue
                                            )
                                    )
                            ));

                    return timestampData.entrySet()
                            .stream()
                            .map(entry -> new DataRecord(psKey, entry.getKey(), entry.getValue()));
                })
                .collect(Collectors.toList());
    }

    /**
     * 方法3: 按指标分组提取，返回 Map<pskey, Map<indicatorId, List<TimeValue>>>
     */
    public Map<String, Map<String, List<TimeValue>>> extractByIndicator(
            Map<String, Map<String, List<Map<String, String>>>> resultData) {

        return resultData.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        psKeyEntry -> psKeyEntry.getValue()
                                .entrySet()
                                .stream()
                                .filter(indicatorEntry -> FIXED_POINT_IDS.contains(indicatorEntry.getKey()))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        indicatorEntry -> indicatorEntry.getValue()
                                                .stream()
                                                .map(dataMap -> new TimeValue(
                                                        dataMap.get("time_stamp"),
                                                        dataMap.get("2")
                                                ))
                                                .filter(timeValue -> timeValue.getTimestamp() != null)
                                                .collect(Collectors.toList())
                                ))
                ));
    }

    /**
     * 方法4: 简化的数据提取 - 直接返回扁平化列表
     */
    public List<FlatData> extractFlatData(
            Map<String, Map<String, List<Map<String, String>>>> resultData) {

        return resultData.entrySet()
                .stream()
                .flatMap(psKeyEntry ->
                        psKeyEntry.getValue()
                                .entrySet()
                                .stream()
                                .filter(indicatorEntry -> FIXED_POINT_IDS.contains(indicatorEntry.getKey()))
                                .flatMap(indicatorEntry ->
                                        indicatorEntry.getValue()
                                                .stream()
                                                .map(dataMap -> new FlatData(
                                                        psKeyEntry.getKey(),
                                                        indicatorEntry.getKey(),
                                                        dataMap.get("time_stamp"),
                                                        dataMap.get("2")
                                                ))
                                )
                )
                .filter(flatData -> flatData.getTimestamp() != null)
                .collect(Collectors.toList());
    }

    // 数据记录类
    public static class DataRecord {
        private final String psKey;
        private final String timestamp;
        private final Map<String, String> indicators;

        public DataRecord(String psKey, String timestamp, Map<String, String> indicators) {
            this.psKey = psKey;
            this.timestamp = timestamp;
            this.indicators = indicators;
        }

        public String getPsKey() { return psKey; }
        public String getTimestamp() { return timestamp; }
        public Map<String, String> getIndicators() { return indicators; }
    }

    // 时间-数值对类
    public static class TimeValue {
        private final String timestamp;
        private final String value;

        public TimeValue(String timestamp, String value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public String getTimestamp() { return timestamp; }
        public String getValue() { return value; }
    }

    // 扁平化数据结构
    public static class FlatData {
        private final String psKey;
        private final String indicatorId;
        private final String timestamp;
        private final String value;

        public FlatData(String psKey, String indicatorId, String timestamp, String value) {
            this.psKey = psKey;
            this.indicatorId = indicatorId;
            this.timestamp = timestamp;
            this.value = value;
        }

        public String getPsKey() { return psKey; }
        public String getIndicatorId() { return indicatorId; }
        public String getTimestamp() { return timestamp; }
        public String getValue() { return value; }
    }
}