package org.jeecg.modules.pcms.common;

/**
 * @author jun
 */

public enum DictApprovalResultEnum {
    /**
     * 数据字典：审核结果
     */
    APPROVED("approved", "通过"),
    REJECTED("rejected", "驳回");

    private final String code;
    private final String name;
    private final static DictApprovalResultEnum[] list = values();

    DictApprovalResultEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code值获取枚举值
     *
     * @param code
     * @return
     */
    public static DictApprovalResultEnum get(String code) {
        for (DictApprovalResultEnum element : list) {
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
}
