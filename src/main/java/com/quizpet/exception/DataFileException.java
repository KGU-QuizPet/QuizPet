package main.java.com.quizpet.exception;

/** 데이터 파일을 읽거나 쓰지 못했을 때, 또는 파일 내용이 깨졌을 때 */
public class DataFileException extends RuntimeException {
    public DataFileException(String message) {
        super(message);
    }

    public DataFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
