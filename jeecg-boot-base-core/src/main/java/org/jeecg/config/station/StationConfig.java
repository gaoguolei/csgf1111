package org.jeecg.config.station;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author gongzhou
 * @Date 2023/11/7 下午4:39
 * @Version 1.5
 */

@Configuration
public class StationConfig {

    @Value("${growatt.inverterNew}")
    private String inverterNew;
    @Value("${growatt.deviceList}")
    private String deviceList;
    @Value("${growatt.stationList}")
    private String stationList;
    @Value("${growatt.historyEnergy}")
    private String historyEnergy;
    @Value("${growatt.inverterAlarm}")
    private String inverterAlarm;
    @Value("${growatt.dayPower}")
    private String dayPower;
    @Value(value = "${growatt.stationDetails}")
    private String stationDetails;
    @Value(value = "${growatt.inverterInfo}")
    private String inverterInfo;
    @Value("${growatt.stationData}")
    private String stationData;

    @Value("${growatt.inverterHistory}")
    private String inverterHistory;
    @Value("${growatt.token}")
    private String token;

    @Value("${growatt.dataLogger}")
    private String dataLogger;

    public String getInverterNew() {
        return inverterNew;
    }

    public String getDeviceList(){
        return deviceList;
    }

    public String getStationList(){
        return stationList;
    }
    public String getHistoryEnergy(){
        return historyEnergy;
    }
    public String getInverterAlarm(){
        return inverterAlarm;
    }
    public String getDayPower(){
        return dayPower;
    }
    public String getStationDetails(){
        return stationDetails;
    }
    public String getInverterInfo(){
        return inverterInfo;
    }
    public String getStationData(){
        return stationData;
    }
    public String getDataLogger(){
        return dataLogger;
    }

    public String getInverterHistory(){
        return historyEnergy;
    }
    public String getToken(){
        return token;
    }


}
