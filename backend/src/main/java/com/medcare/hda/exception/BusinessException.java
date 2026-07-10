package com.medcare.hda.exception;

import com.medcare.hda.common.ResultCode;
import lombok.Getter;

/** 业务异常 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
