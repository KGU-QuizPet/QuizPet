package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.repository.RecordRepository;
import main.java.com.quizpet.service.GrowthService.RewardDecision;

public class GrowthServiceTest {
  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-11-11T03:30:15.123456789Z"), ZoneId.of("Asia/Seoul"));
  private static final LocalDateTime TIME = LocalDateTime.now(CLOCK);

  public static void main(String[] args) throws IOException {
    boolean enabled = false;
    assert enabled = true;
    if (!enabled) {
      throw new AssertionError("-ea 옵션으로 실행해 주세요.");
    }
    Path directory = Files.createTempDirectory("quizpet-growth-");
    try {
      checkNewSubmission(directory.resolve("new/record.txt"));
      checkHistory(directory.resolve("history.txt"));
      checkRetries(directory.resolve("retry.txt"));
      checkMapping(directory.resolve("mapping.txt"));
      checkInvalidInput(directory.resolve("invalid.txt"));
      checkErrors(directory.resolve("broken.txt"));
      checkNoWrites(directory.resolve("readonly"));
    } finally {
      List<Path> paths;
      try (var stream = Files.walk(directory)) {
        paths = stream.sorted(Comparator.reverseOrder()).toList();
      }
      for (Path path : paths) {
        Files.deleteIfExists(path);
      }
    }
    System.out.println("GrowthService checks passed (27 requested decision/record scenarios; no pet payment or joint-save verification)");
  }

  private static RecordService records(Path file) {
    return new RecordService(new RecordRepository(file), CLOCK);
  }

  private static void checkNewSubmission(Path file) throws IOException {
    RecordService records = records(file);
    GrowthService service = new GrowthService(records);
    RewardDecision first = service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
    assertDecision(first, true, 10, "INT", 1);
    LearningRecord record = first.getRecord();
    UUID.fromString(record.getRecordId());
    assert record.getUserId().equals("A");
    assert record.getSessionId().equals("S1");
    assert record.getQuestionId().equals("Q1");
    assert record.getSelectedAnswer().equals("O");
    assert record.isCorrect();
    assert record.getAnsweredAt().equals(TIME);
    assert record.getEarnedXp() == first.getPlannedXp();
    assert !Files.exists(file);
    assert !Files.exists(file.getParent());
    assert !records.hasCorrectAnswer("A", "Q1");

    // 판정만으로는 제출·보상이 예약되지 않습니다. 호출자는 미저장 기록을 보관해야 합니다.
    RewardDecision unsavedRetry = service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
    assertDecision(unsavedRetry, true, 10, "INT", 1);
    assert !unsavedRetry.getRecord().getRecordId().equals(record.getRecordId());
    assert !records.hasCorrectAnswer("A", "Q1");
    RewardDecision anotherSession = service.evaluateReward("A", "S2", "Q1", Category.SCIENCE, "O", true);
    assertDecision(anotherSession, true, 10, "INT", 1);
    assert anotherSession.getRecord().getSessionId().equals("S2");
    assert !anotherSession.getRecord().getRecordId().equals(record.getRecordId());
    RewardDecision wrong = service.evaluateReward("A", "S3", "Q1", Category.SCIENCE, "X", false);
    assertDecision(wrong, true, 0, "INT", 0);
    assert !wrong.getRecord().isCorrect();
    assert wrong.getRecord().getEarnedXp() == 0;
  }

  private static void checkHistory(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    GrowthService service = new GrowthService(records(file));
    repository.save(record("R1", "A", "Q1", "S1", "X", false, 0));
    assertDecision(service.evaluateReward("A", "S2", "Q1", Category.SPORTS, "O", true), true, 10, "STR", 1);
    assertDecision(service.evaluateReward("A", "S2", "Q1", Category.SPORTS, "X", false), true, 0, "STR", 0);
    repository.save(record("R2", "A", "Q1", "S2", "O", true, 10));
    RewardDecision repeated = service.evaluateReward("A", "S3", "Q1", Category.SPORTS, "O", true);
    assertDecision(repeated, true, 0, "STR", 0);
    assert repeated.getRecord().getEarnedXp() == 0;
    assertDecision(service.evaluateReward("A", "S3", "Q1", Category.SPORTS, "X", false), true, 0, "STR", 0);
    repository.save(record("R3", "A", "Q2", "S1", "O", true, 0));
    assertDecision(service.evaluateReward("A", "S2", "Q2", Category.HISTORY, "O", true), true, 0, "WIS", 0);
    assertDecision(service.evaluateReward("B", "S1", "Q1", Category.SPORTS, "O", true), true, 10, "STR", 1);
    assertDecision(service.evaluateReward("B", "S1", "Q2", Category.HISTORY, "O", true), true, 10, "WIS", 1);
    assert repository.findAll().size() == 3 : "판정 중 기록을 저장하면 안 됩니다.";
  }

  private static void checkRetries(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    LearningRecord first = record("ORIGINAL", "A", "Q1", "S1", "O", true, 10);
    repository.save(first);
    repository.save(record("WRONG", "A", "Q2", "S1", "X", false, 0));
    repository.save(record("ZERO", "A", "Q3", "S1", "O", true, 0));
    byte[] before = Files.readAllBytes(file);
    GrowthService service = new GrowthService(records(file));
    RewardDecision retry = service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
    assertDecision(retry, false, 0, "INT", 0);
    assert retry.getRecord().getEarnedXp() == 10 : "과거 기록 XP와 신규 예정 XP를 구분해야 합니다.";
    assert retry.getRecord().getRecordId().equals("ORIGINAL");
    assert retry.getRecord().getAnsweredAt().equals(first.getAnsweredAt());
    assertDecision(service.evaluateReward("A", "S1", "Q2", Category.SCIENCE, "X", false), false, 0, "INT", 0);
    assertDecision(service.evaluateReward("A", "S1", "Q3", Category.SCIENCE, "O", true), false, 0, "INT", 0);
    expectConflict(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "X", true));
    expectConflict(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", false));
    expectConflict(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "X", false));
    assert Arrays.equals(before, Files.readAllBytes(file));

    // 동일 제출에서는 생성·과거 이력 조회를 하지 않고 조회된 객체 자체를 반환해야 합니다.
    LearningRecord[] found = new LearningRecord[1];
    RecordService retryRecords = new RecordService(repository, CLOCK) {
      @Override
      public Optional<LearningRecord> findSubmission(String userId, String sessionId,
          String questionId) throws IOException {
        Optional<LearningRecord> result = super.findSubmission(userId, sessionId, questionId);
        found[0] = result.orElseThrow();
        return result;
      }

      @Override
      public boolean hasCorrectAnswer(String userId, String questionId) {
        throw new AssertionError("기존 제출을 신규 보상으로 재판정하면 안 됩니다.");
      }

      @Override
      LearningRecord createRecord(String userId, String questionId, String sessionId,
          String selectedAnswer, boolean correct, int earnedXp) {
        throw new AssertionError("동일 제출 재시도에서 새 UUID·시각을 만들면 안 됩니다.");
      }
    };
    RewardDecision reused = new GrowthService(retryRecords)
        .evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
    assert reused.getRecord() == found[0];
    assertDecision(reused, false, 0, "INT", 0);
  }

  private static void checkMapping(Path file) throws IOException {
    GrowthService service = new GrowthService(records(file));
    Category[] categories = {Category.SCIENCE, Category.SPORTS, Category.HISTORY, Category.LANGUAGE};
    String[] statTypes = {"INT", "STR", "WIS", "FOC"};
    for (int i = 0; i < categories.length; i++) {
      Category category = categories[i];
      assertDecision(service.evaluateReward("A", "S1", "Q1", category, "1", true), true, 10, statTypes[i], 1);
      assertDecision(service.evaluateReward("A", "S1", "Q1", category, "4", false), true, 0, statTypes[i], 0);
    }
    assert !Files.exists(file);
  }

  private static void checkInvalidInput(Path file) throws IOException {
    GrowthService service = new GrowthService(records(file));
    try {
      new GrowthService(null);
      throw new AssertionError("필수 의존성 누락이 허용되었습니다.");
    } catch (NullPointerException expected) {
    }
    for (String invalid : new String[] {null, "", " ", "\t\n", "a|b", "a\nb", "a\rb", "a\u2028b"}) {
      expectInvalid(() -> service.evaluateReward(invalid, "S1", "Q1", Category.SCIENCE, "O", true));
      expectInvalid(() -> service.evaluateReward("A", invalid, "Q1", Category.SCIENCE, "O", true));
      expectInvalid(() -> service.evaluateReward("A", "S1", invalid, Category.SCIENCE, "O", true));
      expectInvalid(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, invalid, true));
    }
    expectInvalid(() -> service.evaluateReward("A", "S1", "Q1", null, "O", true));
    assert !Files.exists(file);
  }

  private static void checkErrors(Path file) throws IOException {
    GrowthService service = new GrowthService(records(file));
    Files.writeString(file, "손상된 TXT\n", StandardCharsets.UTF_8);
    byte[] before = Files.readAllBytes(file);
    expectIo(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true), file);
    expectIo(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "X", false), file);
    assert Arrays.equals(before, Files.readAllBytes(file));
    Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
    expectIo(() -> service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true), file);
    assert Arrays.equals(Files.readAllBytes(file), new byte[] {(byte) 0xC3, (byte) 0x28});
    expectIo(() -> new GrowthService(records(file.getParent()))
        .evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true), file.getParent());

    // 동일 제출 조회 이후 과거 정답 조회에서 실패하는 경우에도 원인 예외를 그대로 전달합니다.
    IOException failure = new IOException("과거 이력 조회 실패");
    RecordService failedHistory = new RecordService(new RecordRepository(file), CLOCK) {
      @Override
      public Optional<LearningRecord> findSubmission(String userId, String sessionId, String questionId) {
        return Optional.empty();
      }

      @Override
      public boolean hasCorrectAnswer(String userId, String questionId) throws IOException {
        throw failure;
      }

      @Override
      LearningRecord createRecord(String userId, String questionId, String sessionId,
          String selectedAnswer, boolean correct, int earnedXp) {
        throw new AssertionError("조회 실패 후 최초 정답 기록을 만들면 안 됩니다.");
      }
    };
    try {
      new GrowthService(failedHistory).evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
      throw new AssertionError("과거 이력 조회 실패가 무시되었습니다.");
    } catch (IOException expected) {
      assert expected == failure;
    }
  }

  private static void checkNoWrites(Path directory) throws IOException {
    Files.createDirectories(directory);
    Path recordFile = directory.resolve("record.txt");
    Path petFile = directory.resolve("pet.txt");
    // 펫 API 호출 없이, 임시 펫 파일의 바이트도 바뀌지 않는지 확인합니다.
    Files.writeString(petFile, "기존 펫 데이터 보존\n", StandardCharsets.UTF_8);
    byte[] petBefore = Files.readAllBytes(petFile);
    RecordRepository repository = new RecordRepository(recordFile);
    GrowthService service = new GrowthService(records(recordFile));
    service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
    assert !Files.exists(recordFile);
    repository.save(record("R1", "A", "Q1", "S1", "O", true, 10));
    byte[] recordBefore = Files.readAllBytes(recordFile);
    for (int i = 0; i < 3; i++) {
      service.evaluateReward("A", "S1", "Q1", Category.SCIENCE, "O", true);
      service.evaluateReward("A", "S2", "Q1", Category.SCIENCE, "O", true);
      service.evaluateReward("A", "S3", "Q2", Category.SPORTS, "O", true);
    }
    assert Arrays.equals(recordBefore, Files.readAllBytes(recordFile));
    assert Arrays.equals(petBefore, Files.readAllBytes(petFile));
    try (var paths = Files.list(directory)) {
      assert paths.map(path -> path.getFileName().toString()).sorted().toList()
          .equals(List.of("pet.txt", "record.txt"));
    }
  }

  private static LearningRecord record(String id, String userId, String questionId,
      String sessionId, String answer, boolean correct, int xp) {
    return new LearningRecord(id, userId, questionId, sessionId, answer, correct, xp, TIME.minusDays(1));
  }

  private static void assertDecision(RewardDecision result, boolean newSubmission,
      int plannedXp, String statType, int plannedStatGain) {
    assert result.isNewSubmission() == newSubmission;
    assert result.isRewardEligible() == (plannedXp > 0);
    assert result.getPlannedXp() == plannedXp;
    assert result.getStatType().equals(statType);
    assert result.getPlannedStatGain() == plannedStatGain;
    if (newSubmission) {
      assert result.getRecord().getEarnedXp() == plannedXp;
    }
  }

  private static void expectInvalid(Action action) throws IOException {
    try {
      action.run();
      throw new AssertionError("잘못된 입력이 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void expectConflict(Action action) throws IOException {
    try {
      action.run();
      throw new AssertionError("동일 제출 충돌이 허용되었습니다.");
    } catch (IllegalStateException expected) {
      assert expected.getMessage().contains("동일 제출");
      assert expected.getMessage().contains("Q1");
    }
  }

  private static void expectIo(Action action, Path path) {
    try {
      action.run();
      throw new AssertionError("기록 조회 실패가 전달되지 않았습니다.");
    } catch (IOException expected) {
      assert expected.getMessage().contains(path.toString());
    }
  }

  @FunctionalInterface
  private interface Action {
    void run() throws IOException;
  }
}
