package com.homestay.common;

/**
 * 通用错误码，沿用 HTTP 语义，方便前端按 code 处理。
 */
public enum ErrorCode {

    PARAM_INVALID(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    /** 绑定错误码与默认提示信息 */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 获取错误码 */
    public int getCode() {
        return code;
    }

    /** 获取默认提示信息 */
    public String getMessage() {
        return message;
    }
}
