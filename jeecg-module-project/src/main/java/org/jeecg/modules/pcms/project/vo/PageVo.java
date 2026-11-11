package org.jeecg.modules.pcms.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/9/12 上午9:37
 * @Version 1.5
 */
@Data
public class PageVo {

    private Integer page;

    private Integer id;

    private Integer perPage;

    private Integer searchType;

    private String searchKeyword;

    private Integer plantId;

    private String startDate;

    private String endDate;

    private String timeUnit;

    private List<Integer> plantIds;

    private String beginTime;

    private Integer addressId;

    //时间类型。 0:日 1：月；2：年；3：总
    private String dayType;

    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date time;

    private String plantName;

    private Integer pageNo;

    private Integer pageSize;

    private String status;

    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date queryTime;

    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date startTime;

    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date endTime;


    //最小报警时间
    private Integer minHour;
    //最大报警时间
    private Integer maxHour;

    //逆变器sn
    private String sn;

    //报警内容
    private String alarmMessage;

    //最小报警时间
    private String minPower;
    //最大报警时间
    private String maxPower;

    private List<String> snList;

    private String renterId;

}
