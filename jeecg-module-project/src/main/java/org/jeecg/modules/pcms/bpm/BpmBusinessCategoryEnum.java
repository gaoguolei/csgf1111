package org.jeecg.modules.pcms.bpm;

/**
 * @author jun
 */

public enum BpmBusinessCategoryEnum {
    /**
     * 平台流程分类
     */

    PROJECT("project","客户项目");

    private final String code;
    private final String name;
        BpmBusinessCategoryEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }



    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
