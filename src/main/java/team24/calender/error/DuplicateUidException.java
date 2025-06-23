package team24.calender.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "uid 중복") // 400 Bad Request를 기본 상태 코드로 설정
public class DuplicateUidException extends RuntimeException {
    private final int errorCode;

    public DuplicateUidException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}