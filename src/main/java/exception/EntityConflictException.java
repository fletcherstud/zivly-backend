package exception;

public class EntityConflictException extends RuntimeException{

    public EntityConflictException(String message, Throwable e) {super(message, e);}

    public EntityConflictException(String message) {super(message);}

}
