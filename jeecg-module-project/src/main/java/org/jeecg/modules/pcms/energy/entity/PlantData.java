package org.jeecg.modules.pcms.energy.entity;

import lombok.Data;

import java.util.List;

@Data
public class PlantData {
    // 顶层字段
    public String error_msg;
    public plantDetailData data;
    public int error_code;

    // 内部类：Data
    @Data
    public static class plantDetailData {
        public PeakPowerActual peak_power_actual;
        public int count;
        public List<DataLogger> dataloggers;
    }

    // 内部类：PeakPowerActual
    @Data
    public static class PeakPowerActual {
        public double formulaCo2;
        public String companyName;
        public String etodayCo2Text;
        public Object userBean;
        public double formulaSo2;
        public String gridLfdi;
        public String gridPort;
        public String province;
        public List<Object> children;
        public Object plantFromBean;
        public int id;
        public String state;
        public String EYearMoneyText;
        public int tempType;
        public String etotalCoalText;
        public String etotalSo2Text;
        public String plant_lng;
        public String locationImgName;
        public int deviceCount;
        public int map_countryId;
        public String mapLat;
        public String prMonth;
        public double etotalMoney;
        public int plantType;
        public int windAngle;
        public double formulaMoney;
        public String mapCity;
        public int nominalPower;
        public String logoImgName;
        public String latitudeText;
        public String userAccount;
        public double storage_TodayToUser;
        public List<Object> eventMessBeanList;
        public int map_cityId;
        public String createDateTextA;
        public int status;
        public String formulaMoneyUnitId;
        public double energyMonth;
        public String city;
        public String prToday;
        public String etodayCoalText;
        public String parentID;
        public String plantAddress;
        public double envTemp;
        public double formulaCoal;
        public String treeID;
        public int hasStorage;
        public double storage_TotalToUser;
        public double fixedPowerPrice;
        public String etodaySo2Text;
        public double panelTemp;
        public CreateDate createDate;
        public int map_provinceId;
        public String pairViewUserAccount;
        public String emonthSo2Text;
        public double peakPeriodPrice;
        public int hasDeviceOnLine;
        public double storage_BattoryPercentage;
        public double etodayMoney;
        public int formulaTree;
        public String plantNmi;
        public String protocolId;
        public String gridServerUrl;
        public String moneyUnitText;
        public String longitude_d;
        public String country;
        public String longitude_f;
        public String etodayMoneyText;
        public String longitude_m;
        public String phoneNum;
        public double storage_TodayToGrid;
        public String designCompany;
        public String installMapName;
        public String currentPacStr;
        public String etotalMoneyText;
        public double windSpeed;
        public double valleyPeriodPrice;
        public String latitude_f;
        public String mapLng;
        public String latitude_d;
        public int level;
        public String latitude_m;
        public double energyYear;
        public String longitudeText;
        public double flatPeriodPrice;
        public String emonthCoalText;
        public Object paramBean;
        public String etotalCo2Text;
        public String imgPath;
        public boolean isShare;
        public String plant_lat;
        public String emonthCo2Text;
        public int timezone;
        public String gridCompany;
        public double storage_eChargeToday;
        public List<Object> unitList;
        public String remark;
        public double storage_TotalToGrid;
        public boolean defaultPlant;
        public String createDateText;
        public int gridConnectMode;
        public String currentPacTxt;
        public Object unitMap;
        public double alarmValue;
        public String treeName;
        public String alias;
        public double irradiance;
        public String formulaMoneyStr;
        public int onLineEnvCount;
        public double storage_eDisChargeToday;
        public String timezoneText;
        public List<Object> dataLogList;
        public int map_areaId;
        public String etotalFormulaTreeText;
        public String plantImgName;
        public double eToday;
        public double eTotal;
        public String emonthMoneyText;
        public String timezoneValue;
        public String nominalPowerStr;
        public String plantName;
    }

    // 内部类：CreateDate
    @Data
    public static class CreateDate {
        public int date;
        public int hours;
        public int seconds;
        public int month;
        public int timezoneOffset;
        public int year;
        public int minutes;
        public long time;
        public int day;
    }

    // 内部类：DataLogger
    @Data
    public static class DataLogger {
        public LastUpdateTime last_update_time;
        public boolean lost;
        public String model;
        public String sn;
        public int type;
        public String netmode;
        public String manufacturer;
    }

    // 内部类：LastUpdateTime
    @Data
    public static class LastUpdateTime {
        public int date;
        public int hours;
        public int seconds;
        public int month;
        public int timezoneOffset;
        public int year;
        public int minutes;
        public long time;
        public int day;
    }
}