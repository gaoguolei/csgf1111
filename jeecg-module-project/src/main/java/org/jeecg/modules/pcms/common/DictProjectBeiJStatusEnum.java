package org.jeecg.modules.pcms.common;

/**
 * @Author gongzhou
 * @Date 2023/9/20 上午8:49
 * @Version 1.5
 */
public enum DictProjectBeiJStatusEnum {
    /**
     * 数据字典：项目状态
     */
//    DATA_ENTRY("10", "资料录入"),
//    ADMINWORK_APPROVAL("20", "施工管理员一审"),
//    ADMINWORK_RETURN("21", "施工管理员审核驳回"),
//
//    DATA_APPROVAL("30", "资料审核"),
//    DATA_APPROVAL_REJECTED("31", "修改资料"),
//    POWER_STATION_APPROVAL("40", "发改委审核"),
//    ASSIGN_CONSTR_TEAMS("50", "分配施工队"),
//    PROJECT_CONSTR("60", "正在施工"),
//
//    ADMINWORK_APPROVAL_SECOND("70", "施工管理员二审"),
//    CONSTR_INSPECTION_REJECTED("71", "施工修改"),
//
//    CONSTR_INSPECTION("80", "巡检"),
//    CONSTR_NO("81", "巡检不合格"),
//    PROJECT_POWER("90", "项目发电"),
//    PROJECT_CONNECTED("100", "并网"),
//
//    PROJECT_CONCLUSION("110", "项目结项"),
//    PROJECT_COMPLETED("120", "项目完成");

//    DATA_ENTRY("10", "资料录入"),
//
//    DATA_APPROVAL("20", "资料审核"),
//    DATA_APPROVAL_REJECTED("21", "修改资料"),
//
//    POWER_STATION_APPROVAL("30", "发改委审核"),
//    ASSIGN_CONSTR_TEAMS("40", "分配施工队"),
//    PROJECT_CONSTR("50", "正在施工"),
//
//    CONSTR_INSPECTION("60", "施工巡检"),//施工完成
//    CONSTR_NO("61", "巡检不合格"),
//    PROJECT_POWER("70", "项目发电"), //待并网
//    PROJECT_CONNECTED("80", "项目并网"),  //已并网
//
//    PROJECT_INSPECTION("90", "监理巡检"),
//    PROJECT_CONCLUSION("100", "项目结项"),
//    PROJECT_COMPLETED("110", "项目完成");

    DATA_ENTRY("10", "资料录入"),

    DATA_APPROVAL("20", "资料审核"),
    DATA_APPROVAL_REJECTED("21", "修改资料"),

//    POWER_STATION_APPROVAL("30", "发改委审核"), // 去掉
    ASSIGN_CONSTR_TEAMS("40", "分配施工队"),
    PROJECT_CONSTR("50", "正在施工"),

    CONSTR_INSPECTION("60", "施工巡检"),//施工完成
    CONSTR_NO("61", "巡检不合格"),
    PROJECT_INSPECTION("80", "监理巡检"),
//    PROJECT_POWER("70", "项目发电"), //待并网, 去掉
    PROJECT_CONNECTED("90", "项目并网"),  //已并网

    PROJECT_CONCLUSION("100", "项目结项"),
    PROJECT_COMPLETED("110", "项目完成");



    private final String code;
    private final String name;
    private final static DictProjectBeiJStatusEnum[] list = values();

    DictProjectBeiJStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code值获取枚举值
     *
     * @param code
     * @return
     */
    public static DictProjectBeiJStatusEnum get(String code) {
        for (DictProjectBeiJStatusEnum element : list) {
            if (element.getCode().equals(code)) {
                return element;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 获取第一个枚举值
     *
     * @return
     */
    public DictProjectBeiJStatusEnum first() {
        return list[0];
    }

    /**
     * 获取最后一个枚举值
     *
     * @return
     */
    public DictProjectBeiJStatusEnum last() {
        return list[list.length - 1];
    }

    /**
     * 是否有上一个枚举值
     *
     * @return
     */
    public boolean hasPrevious() {
        if (this.ordinal() <= 0) {
            return false;
        }
        return true;
    }

    /**
     * 获取上一个枚举值
     *
     * @return
     */
    public DictProjectBeiJStatusEnum previous() {
        return list[(this.ordinal() - 1) % list.length];
    }

    /**
     * 是否有下一个枚举值
     *
     * @return
     */
    public boolean hasNext() {
        if (this.ordinal() >= list.length - 1) {
            return false;
        }
        return true;
    }

    /**
     * 获取下一个枚举值
     *
     * @return
     */
    public DictProjectBeiJStatusEnum next() {
        return list[(this.ordinal() + 1) % list.length];
    }
}