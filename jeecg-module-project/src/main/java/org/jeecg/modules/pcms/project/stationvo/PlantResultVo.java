package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;
import org.jeecg.modules.pcms.project.vo.PlantVo;

/**
 * @Author gongzhou
 * @Date 2023/9/18 上午9:17
 * @Version 1.5
 */
@Data
public class PlantResultVo {

   private String errorCode;


   private String errorMsg;

   private PlantVo data;

}
