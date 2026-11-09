package com.quizpet.exception;

/** 사용자 입력이 규칙에 맞지 않을 때. getMessage()를 그대로 화면에 보여주면 된다. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
