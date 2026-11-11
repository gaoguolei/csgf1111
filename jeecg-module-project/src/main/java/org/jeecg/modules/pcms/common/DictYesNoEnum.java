package org.jeecg.modules.pcms.common;

/**
 * @author jun
 */

public enum DictYesNoEnum {
    /**
     * 数据字典：是否
     */
    YES("Y", "是"),
    NO("N", "否");

    private final String code;
    private final String name;
    private final static DictYesNoEnum[] list = values();

    DictYesNoEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code值获取枚举值
     *
     * @param code
     * @return
     */
    public static DictYesNoEnum get(String code) {
        for (DictYesNoEnum dictYesNo : list) {
            if (dictYesNo.getCode().equals(code)) {
                return dictYesNo;
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
