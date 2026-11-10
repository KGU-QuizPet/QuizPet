package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.OXQuestion;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.repository.QuestionRepository;
import main.java.com.quizpet.repository.RecordRepository;
import main.java.com.quizpet.service.StatisticsService.Statistics;

public class StatisticsServiceTest {
  private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 10, 12, 30);

  public static void main(String[] args) throws IOException {
    boolean enabled = false;
    assert enabled = true;
    if (!enabled) {
      throw new AssertionError("-ea 옵션으로 실행해 주세요.");
    }
    Path directory = Files.createTempDirectory("quizpet-statistics-");
    try {
      checkEmpty(directory.resolve("empty/record.txt"));
      checkOverall(directory.resolve("overall.txt"));
      checkCategories(directory.resolve("categories.txt"));
      checkMissingQuestion(directory.resolve("missing.txt"));
      checkErrors(directory.resolve("errors.txt"));
      checkRepositoryAdapter(directory.resolve("question.txt"), directory.resolve("adapter.txt"));
    } finally {
      List<Path> paths;
      try (var stream = Files.walk(directory)) {
        paths = stream.sorted(Comparator.reverseOrder()).toList();
      }
      for (Path path : paths) {
        Files.deleteIfExists(path);
      }
    }
    System.out.println("StatisticsService checks passed (22 requested scenarios and long XP/adapter checks)");
  }

  private static void checkEmpty(Path file) throws IOException {
    RecordService records = new RecordService(new RecordRepository(file));
    StatisticsService service = new StatisticsService(records, id -> {
      throw new AssertionError("기록이 없으면 문제 조회가 필요하지 않습니다.");
    });
    assertStats(service.getOverallStatistics("A"), 0, 0, 0.0, 0L);
    for (Category category : Category.values()) {
      Statistics result = service.getCategoryStatistics("A", category.getCategoryId());
      assertStats(result, 0, 0, 0.0, 0L);
      assert result.getCategoryId().equals(category.getCategoryId());
    }
    assert !Files.exists(file);
    assert !Files.exists(file.getParent());
  }

  private static void checkOverall(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    RecordService records = new RecordService(repository);
    StatisticsService service = new StatisticsService(records, id -> {
      throw new AssertionError("전체 통계는 문제 데이터를 조회하면 안 됩니다.");
    });
    repository.save(record("R1", "A", "Q1", "S1", true, 10));
    repository.save(record("R2", "A", "Q2", "S1", false, 0));
    assertStats(service.getOverallStatistics("A"), 2, 1, 50.0, 10L);
    repository.save(record("R3", "A", "Q1", "S2", true, 0));
    assertStats(service.getOverallStatistics("A"), 3, 2, 66.7, 10L);
    assert service.getOverallStatistics("A").getCategoryId() == null;
    repository.save(record("R4", "B", "Q1", "S1", true, 7));
    assertStats(service.getOverallStatistics("B"), 1, 1, 100.0, 7L);
    repository.save(record("R5", "C", "Q1", "S1", false, 0));
    assertStats(service.getOverallStatistics("C"), 1, 0, 0.0, 0L);
    assertStats(service.getOverallStatistics("A"), 3, 2, 66.7, 10L);
    assertStats(service.getOverallStatistics("unknown"), 0, 0, 0.0, 0L);
    // 보상 정책을 계산하지 않고 저장된 값 그대로 합산하는지, int 합계 초과도 확인합니다.
    repository.save(record("R6", "D", "Q1", "S1", true, Integer.MAX_VALUE));
    repository.save(record("R7", "D", "Q1", "S2", true, Integer.MAX_VALUE));
    assertStats(service.getOverallStatistics("D"), 2, 2, 100.0, 4294967294L);
  }

  private static void checkCategories(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    RecordService records = new RecordService(repository);
    Map<String, Question> questions = Map.of(
        "Q_HISTORY_001", question("Q_HISTORY_001", Category.SCIENCE),
        "Q_SCIENCE_001", question("Q_SCIENCE_001", Category.SPORTS),
        "Q_LANGUAGE_001", question("Q_LANGUAGE_001", Category.HISTORY));
    StatisticsService service = new StatisticsService(records,
        id -> Optional.ofNullable(questions.get(id)).map(Question::getCategory));
    repository.save(record("R1", "A", "Q_HISTORY_001", "S1", true, 10));
    repository.save(record("R2", "A", "Q_SCIENCE_001", "S1", true, 10));
    repository.save(record("R3", "A", "Q_HISTORY_001", "S2", false, 0));
    repository.save(record("R4", "A", "Q_HISTORY_001", "S3", true, 0));
    repository.save(record("R5", "B", "Q_SCIENCE_001", "S1", false, 0));
    repository.save(record("R6", "A", "Q_LANGUAGE_001", "S1", false, 0));
    byte[] before = Files.readAllBytes(file);
    List<String> filesBefore;
    try (var stream = Files.list(file.getParent())) {
      filesBefore = stream.map(path -> path.getFileName().toString()).sorted().toList();
    }
    Statistics snapshot = service.getCategoryStatistics("A", "SCIENCE");
    assert snapshot.getCategoryId().equals("SCIENCE");
    assertStats(snapshot, 3, 2, 66.7, 10L);
    assertStats(service.getCategoryStatistics("A", "SPORTS"), 1, 1, 100.0, 10L);
    assertStats(service.getCategoryStatistics("A", "HISTORY"), 1, 0, 0.0, 0L);
    assertStats(service.getCategoryStatistics("A", "LANGUAGE"), 0, 0, 0.0, 0L);
    assertStats(service.getCategoryStatistics("B", "SPORTS"), 1, 0, 0.0, 0L);
    assertStats(service.getCategoryStatistics("B", "SCIENCE"), 0, 0, 0.0, 0L);
    StatisticsService reloaded = new StatisticsService(new RecordService(new RecordRepository(file)),
        id -> Optional.ofNullable(questions.get(id)).map(Question::getCategory));
    for (int i = 0; i < 3; i++) {
      assertStats(service.getOverallStatistics("A"), 5, 3, 60.0, 20L);
      assertStats(reloaded.getOverallStatistics("A"), 5, 3, 60.0, 20L);
      assertStats(reloaded.getCategoryStatistics("A", "SCIENCE"), 3, 2, 66.7, 10L);
    }
    assert Arrays.equals(before, Files.readAllBytes(file));
    try (var stream = Files.list(file.getParent())) {
      assert stream.map(path -> path.getFileName().toString()).sorted().toList().equals(filesBefore);
    }
    // 이후 원본에 기록이 추가되어도 이전에 반환한 결과는 불변 스냅샷입니다.
    repository.save(record("R7", "A", "Q_HISTORY_001", "S4", true, 0));
    assertStats(snapshot, 3, 2, 66.7, 10L);
    assertStats(service.getCategoryStatistics("A", "SCIENCE"), 4, 3, 75.0, 10L);
  }

  private static void checkMissingQuestion(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    RecordService records = new RecordService(repository);
    repository.save(record("R1", "A", "deleted", "S1", true, 10));
    StatisticsService service = new StatisticsService(records, id -> Optional.empty());
    for (Category category : Category.values()) {
      try {
        service.getCategoryStatistics("A", category.getCategoryId());
        throw new AssertionError("문제 누락을 조용히 제외했습니다.");
      } catch (IllegalStateException expected) {
        assert expected.getMessage().contains("deleted");
        assert expected.getMessage().contains("R1");
      }
    }
    assertStats(service.getOverallStatistics("A"), 1, 1, 100.0, 10L);
    assertStats(service.getCategoryStatistics("B", "SCIENCE"), 0, 0, 0.0, 0L);
  }

  private static void checkErrors(Path file) throws IOException {
    RecordService records = new RecordService(new RecordRepository(file));
    StatisticsService service = new StatisticsService(records, id -> Optional.of(Category.SCIENCE));
    for (String invalid : new String[] {null, "", " ", "a|b", "a\nb"}) {
      expectInvalid(() -> service.getOverallStatistics(invalid));
      expectInvalid(() -> service.getCategoryStatistics(invalid, "SCIENCE"));
    }
    for (String invalid : new String[] {null, "", " ", "UNKNOWN", "science", " SCIENCE "}) {
      expectInvalid(() -> service.getCategoryStatistics("A", invalid));
    }
    expectNull(() -> new StatisticsService(null, id -> Optional.empty()));
    expectNull(() -> new StatisticsService(records, null));
    Files.writeString(file, "손상된 TXT\n", StandardCharsets.UTF_8);
    byte[] before = Files.readAllBytes(file);
    expectIo(() -> service.getOverallStatistics("A"), file);
    expectIo(() -> service.getCategoryStatistics("A", "SCIENCE"), file);
    assert Arrays.equals(before, Files.readAllBytes(file));
    Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
    expectIo(() -> service.getOverallStatistics("A"), file);
    expectIo(() -> service.getCategoryStatistics("A", "SCIENCE"), file);
    assert Arrays.equals(Files.readAllBytes(file), new byte[] {(byte) 0xC3, (byte) 0x28});
  }

  private static void checkRepositoryAdapter(Path questionFile, Path recordFile) throws IOException {
    Files.writeString(questionFile, "Q_HISTORY_001|SCIENCE|C1|OX|O|\n문제\n해설\n", StandardCharsets.UTF_8);
    QuestionRepository questions = new QuestionRepository(questionFile);
    RecordRepository repository = new RecordRepository(recordFile);
    repository.save(record("R1", "A", "Q_HISTORY_001", "S1", true, 10));
    StatisticsService service = new StatisticsService(new RecordService(repository),
        id -> questions.findById(id).map(Question::getCategory));
    assertStats(service.getCategoryStatistics("A", "SCIENCE"), 1, 1, 100.0, 10L);
    assertStats(service.getCategoryStatistics("A", "HISTORY"), 0, 0, 0.0, 0L);
  }

  private static Question question(String id, Category category) {
    return new OXQuestion(id, category, "C1", "문제", "O", "해설");
  }

  private static LearningRecord record(String id, String user, String question, String session,
      boolean correct, int xp) {
    return new LearningRecord(id, user, question, session, correct ? "O" : "X", correct, xp, TIME);
  }

  private static void assertStats(Statistics result, int attempts, int correct,
      double accuracy, long xp) {
    assert result.getTotalAttempts() == attempts;
    assert result.getCorrectAnswers() == correct;
    assert result.getAccuracy() == accuracy;
    assert result.getAccuracy() >= 0.0 && result.getAccuracy() <= 100.0;
    assert result.getTotalEarnedXp() == xp;
  }

  private static void expectInvalid(Action action) throws IOException {
    try {
      action.run();
      throw new AssertionError("잘못된 입력이 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void expectNull(Action action) throws IOException {
    try {
      action.run();
      throw new AssertionError("필수 의존성 누락이 허용되었습니다.");
    } catch (NullPointerException expected) {
    }
  }

  private static void expectIo(Action action, Path path) {
    try {
      action.run();
      throw new AssertionError("파일 오류를 전달하지 않았습니다.");
    } catch (IOException expected) {
      assert expected.getMessage().contains(path.toString());
    }
  }

  @FunctionalInterface
  private interface Action {
    void run() throws IOException;
  }
}
