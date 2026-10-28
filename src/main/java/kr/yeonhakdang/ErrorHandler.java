package kr.yeonhakdang;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status).body(Map.of("code",e.code)); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,IllegalArgumentException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> other(Exception e) {
        if(e instanceof org.springframework.web.ErrorResponse er)
            return ResponseEntity.status(er.getStatusCode()).body(Map.of("code",er.getStatusCode().value()==404?"NOT_FOUND":"INVALID_REQUEST"));
        return ResponseEntity.internalServerError().body(Map.of("code","INTERNAL_ERROR"));
    }
}
