package main.java.com.quizpet.repository;

/** 기존 제출 복구가 끝났고, 현재 제출의 저장을 시도하기 전에 거부된 경우입니다. */
public final class SubmissionRejectedException extends IllegalArgumentException {
  public SubmissionRejectedException(RuntimeException cause) {
    super(cause.getMessage(), cause);
  }
}
