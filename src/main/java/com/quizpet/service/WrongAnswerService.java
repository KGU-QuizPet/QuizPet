package main.java.com.quizpet.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.Question;

/** 읽기 전용 오답 집계. 파일 저장·채점·복습 세션 생성은 하지 않습니다. */
public final class WrongAnswerService {
  public enum ListPolicy { ALL_EVER_WRONG, NEEDS_REVIEW_ONLY }
  public enum ReviewStatus { NEEDS_REVIEW, COMPLETED, AMBIGUOUS }

  /**
   * question이 없으면 원문·분야 미확인입니다. 보존 원문이 있어도 출제 가능하다는 의미는 아닙니다.
   * latestRecords에는 마지막 시각의 모든 제출을 보존합니다. 동시각에는 최신 답안을 임의 선택하지 않습니다.
   * records는 해당 사용자·문제의 정답/오답 원본 전체이며 시간 내림차순, 동시각 recordId 순입니다.
   */
  public record WrongAnswer(String questionId, Optional<Question> question, int wrongCount,
      LocalDateTime lastWrongAt, LocalDateTime lastAnsweredAt, ReviewStatus reviewStatus,
      List<LearningRecord> latestRecords, List<LearningRecord> records) {
    public WrongAnswer {
      Objects.requireNonNull(questionId);
      Objects.requireNonNull(question);
      Objects.requireNonNull(lastWrongAt);
      Objects.requireNonNull(lastAnsweredAt);
      Objects.requireNonNull(reviewStatus);
      latestRecords = List.copyOf(latestRecords);
      records = List.copyOf(records);
    }

    public Optional<Category> category() { return question.map(Question::getCategory); }

    /** 최종 시각의 정오가 상충하면 비어 있습니다. 답안 문자열은 latestRecords에서 원문 그대로 조회합니다. */
    public Optional<Boolean> lastCorrect() {
      return reviewStatus == ReviewStatus.AMBIGUOUS ? Optional.empty()
          : Optional.of(reviewStatus == ReviewStatus.COMPLETED);
    }
  }

  private final RecordService records;
  private final RecordQuestionLookup questions;

  public WrongAnswerService(RecordService records, RecordQuestionLookup questions) {
    this.records = Objects.requireNonNull(records);
    this.questions = Objects.requireNonNull(questions);
  }

  /** 기본은 한 번이라도 틀린 문제 전체 유지. 완료 항목 숨김은 NEEDS_REVIEW_ONLY로 선택합니다. */
  public List<WrongAnswer> getWrongAnswers(String userId) throws IOException {
    return getWrongAnswers(userId, null, ListPolicy.ALL_EVER_WRONG);
  }

  /** 분야가 미확인인 문제는 분야별 조회에 포함하지 않으며 전체/미분류 조회에는 보존합니다. */
  public List<WrongAnswer> getWrongAnswers(String userId, Category category) throws IOException {
    Objects.requireNonNull(category, "분야 필터는 필수입니다. 전체 조회는 단일 인자 메서드를 사용하세요.");
    return getWrongAnswers(userId, category, ListPolicy.ALL_EVER_WRONG);
  }

  /** category=null은 전체입니다. NEEDS_REVIEW_ONLY는 완료만 숨기며 판정 불가는 안전하게 유지합니다. */
  public List<WrongAnswer> getWrongAnswers(String userId, Category category, ListPolicy policy) throws IOException {
    Objects.requireNonNull(policy);
    Map<String, List<LearningRecord>> grouped = new TreeMap<>();
    for (LearningRecord record : records.getRecords(userId)) {
      grouped.computeIfAbsent(record.getQuestionId(), id -> new ArrayList<>()).add(record);
    }
    List<WrongAnswer> result = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      List<LearningRecord> attempts = entry.getValue();
      int wrongCount = (int) attempts.stream().filter(record -> !record.isCorrect()).count();
      if (wrongCount == 0) continue;
      attempts.sort(Comparator.comparing(LearningRecord::getAnsweredAt).reversed()
          .thenComparing(LearningRecord::getRecordId));
      LocalDateTime last = attempts.get(0).getAnsweredAt();
      List<LearningRecord> latest = attempts.stream().filter(record -> record.getAnsweredAt().equals(last)).toList();
      boolean correct = latest.stream().anyMatch(LearningRecord::isCorrect);
      boolean incorrect = latest.stream().anyMatch(record -> !record.isCorrect());
      ReviewStatus status = correct && incorrect ? ReviewStatus.AMBIGUOUS
          : correct ? ReviewStatus.COMPLETED : ReviewStatus.NEEDS_REVIEW;
      Optional<Question> question = questions.findById(entry.getKey());
      if (category != null && question.map(Question::getCategory).filter(category::equals).isEmpty()) continue;
      if (policy == ListPolicy.NEEDS_REVIEW_ONLY && status == ReviewStatus.COMPLETED) continue;
      LocalDateTime lastWrong = attempts.stream().filter(record -> !record.isCorrect())
          .map(LearningRecord::getAnsweredAt).max(Comparator.naturalOrder()).orElseThrow();
      result.add(new WrongAnswer(entry.getKey(), question, wrongCount, lastWrong, last, status, latest, attempts));
    }
    // 문제 ID는 표시 순서만 고정합니다. 실제 제출의 선후관계로 해석하지 않습니다.
    result.sort(Comparator.comparing(WrongAnswer::lastAnsweredAt).reversed().thenComparing(WrongAnswer::questionId));
    return List.copyOf(result);
  }

  public List<WrongAnswer> getUnclassifiedWrongAnswers(String userId) throws IOException {
    return getWrongAnswers(userId).stream().filter(item -> item.question().isEmpty()).toList();
  }
}