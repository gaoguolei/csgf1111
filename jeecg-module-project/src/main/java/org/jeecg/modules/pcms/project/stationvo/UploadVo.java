package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/11/19 下午8:12
 * @Version 1.5
 */
@Data
public class UploadVo {

    private Integer code;

    private String message;

    private FileResult data;

}
