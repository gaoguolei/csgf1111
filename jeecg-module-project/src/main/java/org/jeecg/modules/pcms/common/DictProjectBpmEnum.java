package org.jeecg.modules.pcms.common;

/**
 * @author jun
 */

public enum DictProjectBpmEnum {
    /**
     * 数据字典：客户项目业务流程
     */
    DATA_ENTRY("10", "资料录入"),
    DATA_APPROVAL("20", "资料审核"),
    POWER_STATION_APPROVAL("30", "供电所审核"),
    ASSIGN_CONSTR_TEAMS("40", "分配施工队"),
    PROJECT_CONSTR("50", "项目施工"),
    CONSTR_INSPECTION("60", "施工巡检"),
    PROJECT_CONNECTED("70", "项目并网"),
    PROJECT_CONCLUSION("80", "项目结项"),
    PROJECT_COMPLETED("90", "项目完成");
    private final String code;
    private final String name;
    private final static DictProjectBpmEnum[] list = values();

    DictProjectBpmEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code值获取枚举值
     *
     * @param code
     * @return
     */
    public static DictProjectBpmEnum get(String code) {
        for (DictProjectBpmEnum element : list) {
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
    public DictProjectBpmEnum first() {
        return list[0];
    }

    /**
     * 获取最后一个枚举值
     *
     * @return
     */
    public DictProjectBpmEnum last() {
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
    public DictProjectBpmEnum previous() {
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
    public DictProjectBpmEnum next() {
        return list[(this.ordinal() + 1) % list.length];
    }

}
