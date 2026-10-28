package kr.yeonhakdang;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public ApiException(HttpStatus status, String code) { super(code); this.status=status; this.code=code; }
    public static ApiException conflict(String code) { return new ApiException(HttpStatus.CONFLICT, code); }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND"); }
}
