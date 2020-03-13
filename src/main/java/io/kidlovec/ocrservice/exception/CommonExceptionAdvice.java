package io.kidlovec.ocrservice.exception;

import io.kidlovec.ocrservice.translate.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.io.IOException;
import java.util.Set;


/**
 * @author kidlovec
 * @date 2020-02-18
 * @since 1.0.0
 */
@ControllerAdvice
@ResponseBody
public class CommonExceptionAdvice {
    private static Logger logger = LoggerFactory.getLogger(CommonExceptionAdvice.class);
    @Autowired
    private MessageSource messageSource;

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String msg = String.format("缺少请求参数 paramName:%s, paramType:%s", e.getParameterName(), e.getParameterType());
        logger.error(msg, e);
        return ResponseEntity.unprocessableEntity().body(msg);
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.error("参数解析失败", e);
        return ResponseEntity.unprocessableEntity().body("could_not_read_json");
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logger.error("参数验证失败:{}", e.getMessage());
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String msg = error.getDefaultMessage();
        String itemMessage = messageSource.getMessage(msg, error.getArguments(), msg, LocaleContextHolder.getLocale());
        return ResponseEntity.unprocessableEntity().body(String.format("%s", itemMessage));
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BindException.class)
    public ResponseEntity handleBindException(BindException e) {
        logger.error("参数绑定失败", e);
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
//        String field = error.getField();
        String msg = error.getDefaultMessage();
//        String message = String.format("%s:%s", field, encode);
        String itemMessage = messageSource.getMessage(msg, null, msg, LocaleContextHolder.getLocale());
        String message = String.format("%s", itemMessage);
        return ResponseEntity.unprocessableEntity().body(message);

    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity handleServiceException(ConstraintViolationException e) {
        logger.error("参数验证失败", e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        ConstraintViolation<?> violation = violations.iterator().next();
        String message = violation.getMessage();
        return ResponseEntity.unprocessableEntity().body("parameter:" + message);
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity handleValidationException(ValidationException e) {
        logger.error("参数验证失败", e);
        String message = e.getCause().getMessage();
        if (StringUtils.isBlank(message)) {
            message = "参数验证失败";
        }
        return ResponseEntity.badRequest().body(message);
    }

    /**
     * 405 - Method Not Allowed
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logger.error("不支持当前请求方法", e);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    /**
     * 415 - Unsupported Media Type
     */
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity handleHttpMediaTypeNotSupportedException(Exception e) {
        logger.error("不支持当前媒体类型", e);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    /**
     * 500 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("通用异常", e);
        return ResponseEntity.unprocessableEntity().body(e.getMessage());
    }

    /**
     * 参数转换失败，
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {

        logger.error("传递参数异常, 类型转换失败 param name = [" + e.getName() + "], value = [" + e.getValue() + "].", e);
        return ResponseEntity.unprocessableEntity().build();
    }

    /**
     * 操作数据库出现异常:名称重复，外键关联
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity handleException(DataIntegrityViolationException e) {
        logger.error("操作数据库出现异常:", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IOException.class)
    public ResponseEntity handleIOException(IOException e) {
        logger.error("内部错误（IOException）");
        String itemMessage =
                messageSource.getMessage(e.getMessage(), null, e.getMessage(), LocaleContextHolder.getLocale());
        String errorCause = String.valueOf(e.getCause());
        if (StringUtils.isNotBlank(errorCause) && !"null".equals(errorCause)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorCause);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(itemMessage);
    }


}
