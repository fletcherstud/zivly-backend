package exception;

import com.zivly.edge.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TRACE = "trace";

    @Value("${reflectoring.trace:false}")
    private boolean printStackTrace;

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorResponse response = buildErrorResponse(ex, status, (ServletWebRequest)request);
        if (status.is5xxServerError()) {
            log.error("An exception occurred, which will cause a {} response: {}", status, response, ex);
        } else if (status.is4xxClientError()) {
            log.warn("An exception occurred, which will cause a {} response: {}", status, response, ex);
        } else {
            log.info("An exception occurred, which will cause a {} response: {}", status, response, ex);
        }

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(value = ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException exception, ServletWebRequest webRequest) {
        ErrorResponse response = buildErrorResponse(exception, HttpStatus.FORBIDDEN, webRequest);
        log.info("An exception occurred, which will cause a {} response: {}", HttpStatus.FORBIDDEN, response, exception);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }


    @ExceptionHandler({EntityConflictException.class, HttpClientErrorException.Conflict.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Object> handleConflictEntityException(EntityConflictException exception, ServletWebRequest webRequest) {
        ErrorResponse response = buildErrorResponse(exception, HttpStatus.CONFLICT, webRequest);
        log.info("An exception occurred, which will cause a {} response: {}", HttpStatus.CONFLICT, response, exception);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handleNoSuchElementFoundException(EntityNotFoundException exception,
                                                                    ServletWebRequest webRequest) {
        ErrorResponse response = buildErrorResponse(exception, HttpStatus.NOT_FOUND, webRequest);
        log.info("An exception occurred, which will cause a {} response: {}", HttpStatus.NOT_FOUND, response, exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleBadRequest(HttpClientErrorException.BadRequest exception, ServletWebRequest webRequest) {
        ErrorResponse response = buildErrorResponse(exception, HttpStatus.BAD_REQUEST, webRequest);
        log.info("An exception occurred, which will cause a {} response: {}", HttpStatus.BAD_REQUEST, response, exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ErrorResponse buildErrorResponse(Exception exception, HttpStatusCode httpStatus, ServletWebRequest webRequest) {
        String errorMessage = StringUtils.substringBefore(exception.getMessage(), ';');

        return ErrorResponse.builder()
            .status(httpStatus.value())
            .message(errorMessage)
            .error(httpStatus.toString())
            .timestamp(Instant.now())
            .method(webRequest.getHttpMethod().toString())
            .path(String.format("%s %s", webRequest.getRequest().getMethod(), webRequest.getRequest().getRequestURI()))
            .build();
    }

    private boolean isTraceOn(WebRequest request) {
        String [] value = request.getParameterValues(TRACE);
        return Objects.nonNull(value)
               && value.length > 0
               && value[0].contentEquals("true");
    }
}
