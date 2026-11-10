package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;

/**
 * 채점된 제출의 보상 예정값과 기록을 준비합니다. 펫 변경·파일 저장은 수행하지 않습니다.
 * 판정은 지급 예약이나 트랜잭션이 아니므로 저장 전 같은 문제의 다른 요청도 보상 대상일 수 있습니다.
 * 통합 시 과거 정답 재확인부터 펫·기록 공동 저장까지 공통 잠금으로 묶어야 합니다.
 * 부분 저장·복구 상태가 불확실하면 재지급하지 말고 복구를 먼저 완료해야 합니다.
 */
public class GrowthService {
  private static final int FIRST_CORRECT_XP = 10;
  private static final int FIRST_CORRECT_STAT = 1;
  private final RecordService recordService;

  public GrowthService(RecordService recordService) {
    this.recordService = Objects.requireNonNull(recordService, "recordService는 필수입니다.");
  }

  /**
   * category와 correct는 BE2가 확인한 실제 분야·채점 결과를 전달해야 합니다.
   * 신규 기록의 earnedXp는 저장할 예정값이며, 이 메서드 호출만으로 지급·저장되지 않습니다.
   * 기존 제출은 답안·정오가 같을 때 그대로 반환하며 신규 보상 예정값은 항상 0입니다.
   * 기존 기록 존재는 pet.txt의 지급 완료를 보장하지 않습니다.
   *
   * @throws IllegalArgumentException 필수 입력 누락 또는 구분자·줄바꿈 포함
   * @throws IllegalStateException 동일 제출의 답안 또는 정오 충돌
   * @throws IOException 기록 조회 실패. 빈 이력으로 대체하지 않습니다.
   */
  public RewardDecision evaluateReward(String userId, String sessionId, String questionId,
      Category category, String selectedAnswer, boolean correct) throws IOException {
    requireField(userId, "userId");
    requireField(sessionId, "sessionId");
    requireField(questionId, "questionId");
    requireField(selectedAnswer, "selectedAnswer");
    if (category == null) {
      throw new IllegalArgumentException("category는 필수입니다.");
    }

    Optional<LearningRecord> existing = recordService.findSubmission(userId, sessionId, questionId);
    if (existing.isPresent()) {
      LearningRecord record = existing.get();
      if (!record.getSelectedAnswer().equals(selectedAnswer) || record.isCorrect() != correct) {
        throw new IllegalStateException("동일 제출의 답안 또는 정오가 저장된 기록과 다릅니다: "
            + "userId=" + userId + ", sessionId=" + sessionId + ", questionId=" + questionId);
      }
      return new RewardDecision(false, 0, category.getStatType(), 0, record);
    }

    boolean alreadyCorrect = recordService.hasCorrectAnswer(userId, questionId);
    boolean eligible = correct && !alreadyCorrect;
    int plannedXp = eligible ? FIRST_CORRECT_XP : 0;
    int plannedStatGain = eligible ? FIRST_CORRECT_STAT : 0;
    LearningRecord record = recordService.createRecord(userId, questionId, sessionId,
        selectedAnswer, correct, plannedXp);
    return new RewardDecision(true, plannedXp, category.getStatType(), plannedStatGain, record);
  }

  private static void requireField(String value, String field) {
    if (value == null || value.isBlank() || value.contains("|")
        || value.matches("(?s).*\\R.*")) {
      throw new IllegalArgumentException(field + "는 필수이며 구분자나 줄바꿈을 포함할 수 없습니다.");
    }
  }

  /**
   * 불변 판정 결과입니다. planned 값은 이번 요청의 예정 증가량이며 실제 적용 결과가 아닙니다.
   * statType은 무보상일 때도 전달된 분야의 능력치이며, 증가 여부는 plannedStatGain으로 판단합니다.
   * 기존 제출의 record.earnedXp는 과거 기록값으로 plannedXp와 다를 수 있습니다.
   */
  public static final class RewardDecision {
    private final boolean newSubmission;
    private final int plannedXp;
    private final String statType;
    private final int plannedStatGain;
    private final LearningRecord record;

    private RewardDecision(boolean newSubmission, int plannedXp, String statType,
        int plannedStatGain, LearningRecord record) {
      this.newSubmission = newSubmission;
      this.plannedXp = plannedXp;
      this.statType = statType;
      this.plannedStatGain = plannedStatGain;
      this.record = record;
    }

    public boolean isNewSubmission() {
      return newSubmission;
    }

    public boolean isRewardEligible() {
      return plannedXp > 0;
    }

    public int getPlannedXp() {
      return plannedXp;
    }

    public String getStatType() {
      return statType;
    }

    public int getPlannedStatGain() {
      return plannedStatGain;
    }

    public LearningRecord getRecord() {
      return record;
    }
  }
}
