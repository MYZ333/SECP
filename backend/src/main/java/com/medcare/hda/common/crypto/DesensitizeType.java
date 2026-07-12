package com.medcare.hda.common.crypto;

/** 脱敏类型 */
public enum DesensitizeType {
    /** 手机号：138****8000 */
    PHONE,
    /** 身份证：110***********1234 */
    ID_CARD,
    /** 姓名：张* */
    NAME,
    /** 通用：保留首尾各1位，中间****（首尾各留一，其余打码） */
    GENERIC
}
