package com.homestay.common;

/**
 * 业务异常：由全局异常处理器转成统一响应，不输出堆栈。
 */
public class BizException extends RuntimeException {

    private final int code;

    /** 指定错误码与提示信息构造业务异常 */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 按预定义错误码构造业务异常 */
    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    /** 按预定义错误码并覆盖提示信息构造业务异常 */
    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    /** 获取业务错误码 */
    public int getCode() {
        return code;
    }
}
