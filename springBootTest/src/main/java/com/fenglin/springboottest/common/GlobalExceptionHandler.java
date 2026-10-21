package com.fenglin.springboottest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：保证任何异常都以统一 JSON 结构返回，前端不会拿到 500 白页。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：可预期的错误，按 warn 记录即可 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @Valid 校验失败：取第一条字段错误信息返回，前端可直接弹窗 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数不合法";
        log.warn("参数校验失败: {}", message);
        return Result.fail(Result.CODE_BAD_REQUEST, message);
    }

    /** 缺少必填的 query 参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(Result.CODE_BAD_REQUEST, "缺少必要参数：" + e.getParameterName());
    }

    /**
     * 数据库唯一约束冲突。
     * <p>典型场景：并发注册同一邮箱。{@code existsByEmail} 检查与 {@code save} 之间存在时间窗，
     * 两个请求可能同时通过检查，最终靠数据库唯一索引兜底。这里把它翻译成友好提示，
     * 而不是让前端收到笼统的 500。
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("数据完整性冲突: {}", e.getMostSpecificCause().getMessage());
        return Result.fail(Result.CODE_BAD_REQUEST, "该邮箱已被注册，请直接登录");
    }

    /** 兜底：非预期异常，打印堆栈方便排查，但不把细节暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(Result.CODE_ERROR, "服务器开小差了，请稍后再试");
    }
}
