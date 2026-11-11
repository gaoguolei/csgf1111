package org.jeecg.modules.pcms.energy.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;


/**-
 * 库存
 */
@TableName("device_inventory")
@Data
public class DeviceInventory {
      //主键id
      @TableId(type = IdType.AUTO)
      private Integer id;

      //库存编号
      private String inventoryNo;

      //'资产名称'
      private String inventoryName;

      //'资产型号'
      private String inventoryModel;
      //'供应商'
      private String supplier;
      //'仓库地址'
      private String address;
      //'入库状态'
      private String stockInStatus;
      //'库存数量'
      private Integer num;

      //'入库时间'
      private Date stockInTime;
      //'创建时间'
      private Date createTime;
      //'修改时间'
      private Date updateTime;
      //''备注''
      private String remake;
}
