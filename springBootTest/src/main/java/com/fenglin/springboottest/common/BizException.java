package com.fenglin.springboottest.common;

/**
 * 业务异常。由 {@link GlobalExceptionHandler} 统一转换为 {@link Result}，
 * 业务代码里直接 throw，不用层层 return 错误码。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        this(Result.CODE_BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
