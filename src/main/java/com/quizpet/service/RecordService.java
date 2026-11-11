package main.java.com.quizpet.service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.repository.RecordRepository;

/** 사용자별 풀이 기록 조회·생성을 담당합니다. 채점과 성장 보상은 계산하지 않습니다. */
public class RecordService {
  private final RecordRepository recordRepository;
  private final Clock clock;

  public RecordService(RecordRepository recordRepository) {
    this(recordRepository, Clock.systemDefaultZone());
  }

  public RecordService(RecordRepository recordRepository, Clock clock) {
    this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository는 필수입니다.");
    this.clock = Objects.requireNonNull(clock, "clock은 필수입니다.");
  }

  /** 경험치 지급량과 무관하게 해당 사용자의 과거 정답 기록을 확인합니다. */
  public boolean hasCorrectAnswer(String userId, String questionId) throws IOException {
    requireId(questionId, "questionId");
    return getRecords(userId).stream().anyMatch(record -> record.getQuestionId().equals(questionId)
        && record.isCorrect());
  }

  public Optional<LearningRecord> findSubmission(String userId, String sessionId,
      String questionId) throws IOException {
    return recordRepository.findBySubmission(userId, sessionId, questionId);
  }

  /** 파일에 저장된 순서대로 반환합니다. */
  public List<LearningRecord> getRecords(String userId) throws IOException {
    return List.copyOf(recordRepository.findByUserId(userId));
  }

  public List<LearningRecord> getSessionRecords(String userId, String sessionId) throws IOException {
    requireId(sessionId, "sessionId");
    return getRecords(userId).stream()
        .filter(record -> record.getSessionId().equals(sessionId)).toList();
  }

  public List<LearningRecord> getRecentRecords(String userId, int limit) throws IOException {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit은 양수여야 합니다.");
    }
    List<LearningRecord> records = new ArrayList<>(getRecords(userId));
    // 먼저 저장 순서를 뒤집고 안정 정렬하여 동시각에는 나중에 저장된 기록을 우선합니다.
    Collections.reverse(records);
    return records.stream()
        .sorted(Comparator.comparing(LearningRecord::getAnsweredAt).reversed())
        .limit(limit).toList();
  }

  /**
   * 내부 연동용 생성 단계입니다. 저장하지 않으며 earnedXp는 외부 성장 처리의 확정값입니다.
   * 재시도 시에는 새 기록을 만들지 말고 동일 객체 또는 findSubmission() 결과를 사용합니다.
   */
  LearningRecord createRecord(String userId, String questionId, String sessionId,
      String selectedAnswer, boolean correct, int earnedXp) {
    return new LearningRecord(UUID.randomUUID().toString(), userId, questionId, sessionId,
        selectedAnswer, correct, earnedXp, LocalDateTime.now(clock));
  }

  /** 내부 연동용 저장 단계입니다. 중복·형식 검증과 파일 보호는 Repository에 위임합니다. */
  void saveRecord(LearningRecord record) throws IOException {
    recordRepository.save(record);
  }

  private static void requireId(String value, String field) {
    if (value == null || value.isBlank() || value.contains("|")
        || value.matches("(?s).*\\R.*")) {
      throw new IllegalArgumentException(field + "는 필수이며 구분자나 줄바꿈을 포함할 수 없습니다.");
    }
  }
}
