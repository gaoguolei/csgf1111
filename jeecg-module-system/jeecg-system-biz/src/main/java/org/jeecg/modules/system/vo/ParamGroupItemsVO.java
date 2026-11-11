package org.jeecg.modules.system.vo;

import lombok.Data;
import org.jeecg.modules.system.model.ParamItem;

import java.util.List;

/**
 * @Author gongzhou
 * @Date 2023/3/7 上午8:46
 * @Version 1.5
 */
@Data
public class ParamGroupItemsVO {


    private List<ParamItem> params;

    private String groupName;

    private Integer id;

}
