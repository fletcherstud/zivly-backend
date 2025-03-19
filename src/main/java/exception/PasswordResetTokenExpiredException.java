package exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordResetTokenExpiredException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PasswordResetTokenExpiredException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
    }
}
