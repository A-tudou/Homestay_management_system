package com.homestay.common;

/**
 * 统一响应体：code 为 0 表示成功，非 0 为错误码。
 */
public record ApiResponse<T>(int code, String message, T data) {

    /** 成功响应，携带业务数据 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    /** 成功响应，无返回数据 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    /** 失败响应，指定错误码与提示信息 */
    public static <T> ApiResponse<T> error(int code, String message) { return new ApiResponse<>(code, message, null);
    }
}
