package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;

/** 저장된 제출 기록을 집계하는 읽기 전용 서비스입니다. 최근 기록은 RecordService에서 조회합니다. */
public class StatisticsService {
  private final RecordService recordService;
  private final Function<String, Optional<Category>> categoryLookup;

  /**
   * categoryLookup은 문제 ID에 대응하는 실제 분야를 반환하며, 문제 누락은 Optional.empty()입니다.
   * BE2 연결 예: id -> questionRepository.findById(id).map(Question::getCategory)
   */
  public StatisticsService(RecordService recordService,
      Function<String, Optional<Category>> categoryLookup) {
    this.recordService = Objects.requireNonNull(recordService, "recordService는 필수입니다.");
    this.categoryLookup = Objects.requireNonNull(categoryLookup, "categoryLookup은 필수입니다.");
  }

  /** 전체 집계는 문제 데이터 조회 없이 모든 제출 기록을 포함합니다. */
  public Statistics getOverallStatistics(String userId) throws IOException {
    return summarize(null, recordService.getRecords(userId));
  }

  public Statistics getCategoryStatistics(String userId, String categoryId) throws IOException {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId는 필수입니다.");
    }
    Category category = Category.valueOf(categoryId);
    List<LearningRecord> selected = new ArrayList<>();
    for (LearningRecord record : recordService.getRecords(userId)) {
      Optional<Category> found = Objects.requireNonNull(categoryLookup.apply(record.getQuestionId()),
          "categoryLookup은 null 대신 Optional.empty()를 반환해야 합니다.");
      // 임시 정책: 명세에 없는 누락 문제 처리 방식은 주말 BE2 통합 시 최종 협의합니다.
      // 어느 분야의 기록인지 알 수 없으므로 선택 분야와 관계없이 불완전한 집계를 거부합니다.
      Category actual = found.orElseThrow(() -> new IllegalStateException(
          "문제의 카테고리를 확인할 수 없습니다: questionId=" + record.getQuestionId()
              + ", recordId=" + record.getRecordId()));
      if (actual == category) {
        selected.add(record);
      }
    }
    return summarize(category.getCategoryId(), selected);
  }

  private static Statistics summarize(String categoryId, List<LearningRecord> records) {
    int correctAnswers = 0;
    long totalEarnedXp = 0;
    for (LearningRecord record : records) {
      if (record.isCorrect()) {
        correctAnswers++;
      }
      // int 범위인 개별 XP를 long으로 누적합니다. 정답 수로 보상을 추정하지 않습니다.
      totalEarnedXp += record.getEarnedXp();
    }
    int totalAttempts = records.size();
    double accuracy = totalAttempts == 0 ? 0.0
        : Math.round(correctAnswers * 1000.0 / totalAttempts) / 10.0;
    return new Statistics(categoryId, totalAttempts, correctAnswers, accuracy, totalEarnedXp);
  }

  /** 불변 통계 결과입니다. 전체 통계의 categoryId는 null, 분야 통계는 Category의 ID입니다. */
  public static final class Statistics {
    private final String categoryId;
    private final int totalAttempts;
    private final int correctAnswers;
    private final double accuracy;
    private final long totalEarnedXp;

    private Statistics(String categoryId, int totalAttempts, int correctAnswers,
        double accuracy, long totalEarnedXp) {
      this.categoryId = categoryId;
      this.totalAttempts = totalAttempts;
      this.correctAnswers = correctAnswers;
      this.accuracy = accuracy;
      this.totalEarnedXp = totalEarnedXp;
    }

    public String getCategoryId() {
      return categoryId;
    }

    public int getTotalAttempts() {
      return totalAttempts;
    }

    public int getCorrectAnswers() {
      return correctAnswers;
    }

    public double getAccuracy() {
      return accuracy;
    }

    public long getTotalEarnedXp() {
      return totalEarnedXp;
    }
  }
}
