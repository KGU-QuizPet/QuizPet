package main.java.com.quizpet.model;

import java.time.LocalDateTime;

public class LearningRecordTest {
  private static final LocalDateTime ANSWERED_AT = LocalDateTime.of(2026, 11, 10, 12, 30);

  public static void main(String[] args) {
    LearningRecord firstCorrect = create("R1", "U1", "Q1", "S1", "O", true, 10, ANSWERED_AT);
    assert firstCorrect.getRecordId().equals("R1");
    assert firstCorrect.getUserId().equals("U1");
    assert firstCorrect.getQuestionId().equals("Q1");
    assert firstCorrect.getSessionId().equals("S1");
    assert firstCorrect.getSelectedAnswer().equals("O");
    assert firstCorrect.isCorrect();
    assert firstCorrect.getEarnedXp() == 10;
    assert firstCorrect.getAnsweredAt().equals(ANSWERED_AT);

    // 재정답은 정답 기록이지만 추가 경험치는 없습니다.
    LearningRecord repeatedCorrect = create("R2", "U1", "Q1", "S2", "O", true, 0, ANSWERED_AT);
    assert repeatedCorrect.isCorrect();
    assert repeatedCorrect.getEarnedXp() == 0;
    assert !repeatedCorrect.getSessionId().equals(firstCorrect.getSessionId());

    LearningRecord incorrect = create("R3", "U1", "Q1", "S3", "X", false, 0, ANSWERED_AT);
    assert !incorrect.isCorrect();
    assert incorrect.getEarnedXp() == 0;
    assert incorrect.getSelectedAnswer().equals("X");

    // 객관식 제출 답안도 문자열 그대로 보존합니다.
    for (String answer : new String[] {"1", "2", "3", "4"}) {
      assert create("R4", "U2", "Q2", "S4", answer, true, 10, ANSWERED_AT)
          .getSelectedAnswer().equals(answer);
    }

    for (String invalid : new String[] {null, "", " ", "\t\n"}) {
      expectInvalid("recordId", () -> create(invalid, "U1", "Q1", "S1", "O", true, 10, ANSWERED_AT));
      expectInvalid("userId", () -> create("R1", invalid, "Q1", "S1", "O", true, 10, ANSWERED_AT));
      expectInvalid("questionId", () -> create("R1", "U1", invalid, "S1", "O", true, 10, ANSWERED_AT));
      expectInvalid("sessionId", () -> create("R1", "U1", "Q1", invalid, "O", true, 10, ANSWERED_AT));
      expectInvalid("selectedAnswer", () -> create("R1", "U1", "Q1", "S1", invalid, true, 10, ANSWERED_AT));
    }
    for (int negativeXp : new int[] {-1, -10, Integer.MIN_VALUE}) {
      expectInvalid("earnedXp", () -> create("R1", "U1", "Q1", "S1", "O", true, negativeXp, ANSWERED_AT));
      expectInvalid("earnedXp", () -> create("R1", "U1", "Q1", "S1", "X", false, negativeXp, ANSWERED_AT));
    }
    for (int positiveXp : new int[] {1, 10, Integer.MAX_VALUE}) {
      expectInvalid("오답", () -> create("R1", "U1", "Q1", "S1", "X", false, positiveXp, ANSWERED_AT));
    }
    expectInvalid("answeredAt", () -> create("R1", "U1", "Q1", "S1", "O", true, 10, null));

    // LocalDateTime의 연산은 새 값을 반환하므로 기록의 풀이 시각은 바뀌지 않습니다.
    LocalDateTime later = firstCorrect.getAnsweredAt().plusDays(1);
    assert !later.equals(ANSWERED_AT);
    assert firstCorrect.getAnsweredAt().equals(ANSWERED_AT);
    assert firstCorrect.getEarnedXp() == 10;
    System.out.println("LearningRecord checks passed");
  }

  private static LearningRecord create(String recordId, String userId, String questionId,
      String sessionId, String answer, boolean correct, int earnedXp, LocalDateTime answeredAt) {
    return new LearningRecord(recordId, userId, questionId, sessionId,
        answer, correct, earnedXp, answeredAt);
  }

  private static void expectInvalid(String field, Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 학습 기록이 허용되었습니다: " + field);
    } catch (IllegalArgumentException expected) {
      assert expected.getMessage().contains(field) : expected.getMessage();
    }
  }
}
