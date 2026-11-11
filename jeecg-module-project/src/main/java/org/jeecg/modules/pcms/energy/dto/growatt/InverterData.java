package org.jeecg.modules.pcms.energy.dto.growatt;

import lombok.Data;

@Data
public class InverterData {
    private String error_msg;
    private DataDTO data;
    private String device_sn;
    private String dataloggerSn;
    private String error_code;

    @Data
    public static class DataDTO {
        private String epv6Total;
        private String vPidPvcpe;
        private String fac;
        private String realOPPercent;
        private String epv6Today;
        private String warnCode;
        private String epv5Today;
        private String pacr;
        private String pacs;
        private String rac;
        private String pact;
        private String epvTotal;
        private String vacTr;
        private String id;
        private String pBusVoltage;
        private String deratingMode;
        private String epv5Total;
        private String wStringStatusValue;
        private String vString8;
        private String vString7;
        private String temperature5;
        private String temperature4;
        private String vString9;
        private String iPidPvape;
        private String pidStatus;
        private String eRacToday;
        private String iPidPvepe;
        private String vString2;
        private String dwStringWarningValue1;
        private String vString1;
        private String vString4;
        private String eRacTotal;
        private String vString3;
        private String vacSt;
        private String vString6;
        private String vString5;
        private String vPidPvgpe;
        private String inverterId;
        private String status;
        private String vPidPvbpe;
        private String nBusVoltage;
        private String ppv2;
        private String ppv3;
        private String ppv4;
        private String ppv10;
        private String ppv5;
        private String strUnblance;
        private String compqt;
        private String compqs;
        private String compqr;
        private String ppv1;
        private String ctharis;
        private String ctharit;
        private String ctharir;
        private String temperature3;
        private String epv10Today;
        private String temperature2;
        private String epv10Total;
        private String temperature;
        private String debug1;
        private String debug2;
        private String sDci;
        private String epv7Today;
        private String epv1Today;
        private String epv4Total;
        private String epv7Total;
        private String epv1Total;
        private String epv4Today;
        private String vact;
        private String vacs;
        private String vacr;
        private String iPidPvdpe;
        private String ctit;
        private String ipv10;
        private String ctir;
        private String iPidPvhpe;
        private String ctis;
        private String ppv;
        private String faultValue;
        private String wPIDFaultValue;
        private String ctqt;
        private String vPidPvfpe;
        private String strFault;
        private String ctqr;
        private String ctqs;
        private String vpv3;
        private String vPidPvape;
        private String vpv2;
        private String vpv1;
        private String tDci;
        private String vPidPvepe;
        private String apfStatus;
        private boolean bigDevice;
        private String vpv9;
        private String vpv8;
        private String vpv7;
        private String vpv6;
        private String vpv5;
        private String vpv4;
        private String pac;
        private String currentString1;
        private String powerTotal;
        private String powerToday;
        private String strBreak;
        private String currentString5;
        private String currentString4;
        private String currentString3;
        private String currentString2;
        private String currentString9;
        private String currentString8;
        private String currentString7;
        private String currentString6;
        private String pidFaultCode;
        private String warnBit;
        private String vpv10;
        private String iPidPvcpe;
        private String ppv6;
        private String gfci;
        private String iPidPvgpe;
        private String ppv7;
        private String iacr;
        private String ppv8;
        private String ppv9;
        private String iact;
        private String iacs;
        private String currentString16;
        private String warningValue2;
        private String currentString15;
        private String epv3Total;
        private String strUnmatch;
        private String warningValue1;
        private String currentString14;
        private String currentString13;
        private String warningValue3;
        private String currentString12;
        private String epv3Today;
        private String vPidPvdpe;
        private String currentString11;
        private String currentString10;
        private String epv9Today;
        private String epv9Total;
        private String rDci;
        private String epv8Today;
        private String epv8Total;
        private String epv2Today;
        private String ipv2;
        private String opFullwatt;
        private String ipv1;
        private String ipv4;
        private String epv2Total;
        private String ipv3;
        private String vacRs;
        private String ipv6;
        private String ipv5;
        private String ipv8;
        private String faultType;
        private String ipv7;
        private String timeTotalText;
        private String ipv9;
        private String pidBus;
        private TimeCalendar timeCalendar;
        private String vString11;
        private String timeTotal;
        private String vString10;
        private boolean again;
        private String vString15;
        private String vString14;
        private String ipmTemperature;
        private String vString13;
        private String vString12;
        private String iPidPvbpe;
        private String vString16;
        private String compharis;
        private String faultCode2;
        private String iPidPvfpe;
        private String compharit;
        private String faultCode1;
        private String compharir;
        private String pf;
        private String statusText;
        private String vPidPvhpe;
        private String time;
        private String pvIso;
    }

    @Data
    public static class TimeCalendar {
        private String calendarType;
        private String firstDayOfWeek;
        private String minimalDaysInFirstWeek;
        private GregorianChange gregorianChange;
        private TimeZoneDTO timeZone;
        private String weekYear;
        private TimeDTO time;
        private long timeInMillis;
        private String weeksInWeekYear;
        private boolean lenient;
        private boolean weekDateSupported;
    }

    @Data
    public static class GregorianChange {
        private String date;
        private String hours;
        private String seconds;
        private String month;
        private String timezoneOffset;
        private String year;
        private String minutes;
        private long time;
        private String day;
    }

    @Data
    public static class TimeZoneDTO {
        private boolean dirty;
        private Object lastRuleInstance;
        private String DSTSavings;
        private String displayName;
        private String rawOffset;
        private String ID;
    }

    @Data
    public static class TimeDTO {
        private String date;
        private String hours;
        private String seconds;
        private String month;
        private String timezoneOffset;
        private String year;
        private String minutes;
        private long time;
        private String day;
    }
}