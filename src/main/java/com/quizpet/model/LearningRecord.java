package main.java.com.quizpet.model;

import java.time.LocalDateTime;

/** 답안 제출 1건의 학습 기록입니다. 정답 여부와 실제 지급된 경험치를 보존합니다. */
public final class LearningRecord {
  private final String recordId;
  private final String userId;
  private final String questionId;
  private final String sessionId;
  private final String selectedAnswer;
  private final boolean correct;
  private final int earnedXp;
  private final LocalDateTime answeredAt;

  public LearningRecord(String recordId, String userId, String questionId, String sessionId,
      String selectedAnswer, boolean correct, int earnedXp, LocalDateTime answeredAt) {
    requireText(recordId, "recordId");
    requireText(userId, "userId");
    requireText(questionId, "questionId");
    requireText(sessionId, "sessionId");
    requireText(selectedAnswer, "selectedAnswer");
    if (earnedXp < 0) {
      throw new IllegalArgumentException("earnedXp는 음수가 될 수 없습니다.");
    }
    if (!correct && earnedXp > 0) {
      throw new IllegalArgumentException("오답에는 경험치를 지급할 수 없습니다.");
    }
    if (answeredAt == null) {
      throw new IllegalArgumentException("answeredAt은 필수입니다.");
    }
    this.recordId = recordId;
    this.userId = userId;
    this.questionId = questionId;
    this.sessionId = sessionId;
    this.selectedAnswer = selectedAnswer;
    this.correct = correct;
    this.earnedXp = earnedXp;
    this.answeredAt = answeredAt;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
    }
  }

  public String getRecordId() {
    return recordId;
  }

  public String getUserId() {
    return userId;
  }

  public String getQuestionId() {
    return questionId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getSelectedAnswer() {
    return selectedAnswer;
  }

  public boolean isCorrect() {
    return correct;
  }

  public int getEarnedXp() {
    return earnedXp;
  }

  public LocalDateTime getAnsweredAt() {
    return answeredAt;
  }
}
