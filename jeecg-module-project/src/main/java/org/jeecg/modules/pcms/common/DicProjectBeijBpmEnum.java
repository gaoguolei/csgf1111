package org.jeecg.modules.pcms.common;

/**
 * @Author gongzhou
 * @Date 2023/9/20 上午7:30
 * @Version 1.5
 */
public enum DicProjectBeijBpmEnum {

    /**
     * 数据字典：客户项目业务流程
     */
//    DATA_ENTRY("10", "资料录入"),
//
//    DATA_WORK_APPROVAL("20", "施工管理员一审"),
//
//    DATA_APPROVAL("30", "资料审核"),
//
//    POWER_STATION_APPROVAL("40", "发改委审核"),
//    ASSIGN_CONSTR_TEAMS("50", "分配施工队"),
//    PROJECT_CONSTR("60", "项目施工"),
//    ADMINWORK_APPROVAL_SECOND("70", "施工管理员二审"),
//
//    CONSTR_INSPECTION("80", "施工巡检"),
//    PROJECT_POWER_CONNECTED("90", "项目发电"),
//    PROJECT_CONNECTED("100", "项目并网"),
//
//    PROJECT_CONCLUSION("110", "项目结项"),
//    PROJECT_COMPLETED("120", "项目完成");


//    DATA_ENTRY("10", "资料录入"),
//
//    DATA_APPROVAL("20", "资料审核"),
//
//    POWER_STATION_APPROVAL("30", "发改委审核"),
//    ASSIGN_CONSTR_TEAMS("40", "分配施工队"),
//    PROJECT_CONSTR("50", "项目施工"),
//
//    CONSTR_INSPECTION("60", "施工巡检"),
//    PROJECT_POWER_CONNECTED("70", "项目发电"),//待并网
//
//    PROJECT_CONNECTED("80", "项目并网"),
//    PROJECT_CONCLUSION("90", "监理巡检"),//新增一流程
//
//    PROJECT_OVER("100", "项目结项"),
//    PROJECT_COMPLETED("110", "项目完成");

    DATA_ENTRY("10", "资料录入"),

    DATA_APPROVAL("20", "资料审核"),

//    POWER_STATION_APPROVAL("30", "发改委审核"), // 去掉
    ASSIGN_CONSTR_TEAMS("40", "分配施工队"),
    PROJECT_CONSTR("50", "项目施工"),

    CONSTR_INSPECTION("60", "施工巡检"),
    PROJECT_CONCLUSION("80", "监理巡检"),//新增一流程
//    PROJECT_POWER_CONNECTED("70", "项目发电"),//待并网, 去掉

    PROJECT_CONNECTED("90", "项目并网"),
//    PROJECT_CONCLUSION("90", "监理巡检"),//新增一流程
    PROJECT_OVER("100", "项目结项"),
    PROJECT_COMPLETED("110", "项目完成");
    private final String code;
    private final String name;
    private final static DicProjectBeijBpmEnum[] list = values();

    DicProjectBeijBpmEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code值获取枚举值
     *
     * @param code
     * @return
     */
    public static DicProjectBeijBpmEnum get(String code) {
        for (DicProjectBeijBpmEnum element : list) {
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
    public DicProjectBeijBpmEnum first() {
        return list[0];
    }

    /**
     * 获取最后一个枚举值
     *
     * @return
     */
    public DicProjectBeijBpmEnum last() {
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
    public DicProjectBeijBpmEnum previous() {
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
    public DicProjectBeijBpmEnum next() {
        return list[(this.ordinal() + 1) % list.length];
    }
}
