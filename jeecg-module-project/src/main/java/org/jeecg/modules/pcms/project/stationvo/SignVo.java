package org.jeecg.modules.pcms.project.stationvo;

import lombok.Data;

/**
 * @Author gongzhou
 * @Date 2023/11/19 下午2:26
 * @Version 1.5
 */
@Data
public class SignVo {


    //项目编号
    private String projectcode;

    private Integer id;

    //甲方姓名（出租方）
    private String artyA;
    //乙方(承租方)
    private String artyB;
    //签订地址
    private String signAddress;
    //签订日期
    private String date;

    //身份证号
    private String idnum;
    //住址
    private String address;
    //联系电话
    private String phone;
    //第二联系人
    private String personTwo;
    //第二联系人电话
    private String personPhone;

    //承租方信息
    // artyB;
    //承租方联系人
    private String personB;
    //承租方联系人电话
    private String phoneB;

    //电表电压
    private String voltage;


    //项目名称格式  村-甲方姓名-光伏电站
    private String projectName;

    //装机容量
    private String peakPower;

    //电站总价
    private String totalPrice;

    //建筑物坐落
    private String houseAddress;

    //产权所有人
    private String owner;

    //产权编号
    private String number;

    //建筑屋顶总面积
    private String area;

    //实际占用面积
    private String realityArea;

    //其他空间使用范围
    private String range;

    //租赁到期年
    private String year;
    //租赁到期月
    private String month;
    //租赁到期日
    private String day;
    //前五年（合计）
    private String amountToFront;
    //后20年（合计）
    private String amountToAfter;
    //开户行
    private String bank;
    //账号
    private String account;
    //长
    private String length;
    //宽
    private String width;
    //屋顶面积
    private String roofArea;

    //房屋结构
    private String structure;

    //组件会数
    private String assembly;

    //电站单价
    private String priceOfPower;

    //电站总价
    private String priceTotal;
    //电站总价（大写）
    private String priceTotalBig;

    //系统容量
    private String capacity;
    //小计
    private String subtotal;
}
