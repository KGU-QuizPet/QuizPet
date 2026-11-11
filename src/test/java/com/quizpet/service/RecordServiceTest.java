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
import java.util.UUID;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.repository.RecordRepository;

public class RecordServiceTest {
  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-11-10T03:30:15.123456789Z"), ZoneId.of("Asia/Seoul"));

  public static void main(String[] args) throws IOException {
    boolean assertionsEnabled = false;
    assert assertionsEnabled = true;
    if (!assertionsEnabled) {
      throw new AssertionError("-ea 옵션으로 실행해 주세요.");
    }
    Path directory = Files.createTempDirectory("quizpet-record-service-");
    try {
      checkHistory(directory.resolve("history.txt"));
      checkRecent(directory.resolve("recent.txt"));
      checkInvalidInput(directory.resolve("input.txt"));
      checkFileErrors(directory.resolve("broken.txt"));
    } finally {
      List<Path> paths;
      try (var stream = Files.walk(directory)) {
        paths = stream.sorted(Comparator.reverseOrder()).toList();
      }
      for (Path path : paths) {
        Files.deleteIfExists(path);
      }
    }
    System.out.println("RecordService checks passed (16 requested scenarios and creation/error checks)");
  }

  private static void checkHistory(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    RecordService service = new RecordService(repository, CLOCK);
    assert service.getRecords("A").isEmpty();
    assert service.getRecentRecords("A", 10).isEmpty();
    assert service.getSessionRecords("A", "S1").isEmpty();
    assert service.findSubmission("A", "S1", "Q1").isEmpty();
    assert !service.hasCorrectAnswer("A", "Q1");
    assert !Files.exists(file);

    LearningRecord wrong = service.createRecord("A", "Q1", "S1", "X", false, 0);
    UUID.fromString(wrong.getRecordId());
    assert wrong.getUserId().equals("A");
    assert wrong.getQuestionId().equals("Q1");
    assert wrong.getSessionId().equals("S1");
    assert wrong.getSelectedAnswer().equals("X");
    assert !wrong.isCorrect() && wrong.getEarnedXp() == 0;
    assert wrong.getAnsweredAt().equals(LocalDateTime.of(2026, 11, 10, 12, 30, 15, 123456789));
    assert !Files.exists(file) : "생성 단계에서 파일을 저장하면 안 됩니다.";
    service.saveRecord(wrong);
    assert !service.hasCorrectAnswer("A", "Q1");

    LearningRecord first = service.createRecord("A", "Q1", "S2", "O", true, 10);
    assert !first.getRecordId().equals(wrong.getRecordId());
    assert !service.hasCorrectAnswer("A", "Q1") : "아직 저장하지 않은 정답은 조회되지 않아야 합니다.";
    service.saveRecord(first);
    assert service.hasCorrectAnswer("A", "Q1");
    LearningRecord repeated = service.createRecord("A", "Q1", "S3", "O", true, 0);
    service.saveRecord(repeated);
    assert service.hasCorrectAnswer("A", "Q1");
    assert !service.hasCorrectAnswer("B", "Q1");
    assert service.getRecords("B").isEmpty();

    // XP 0인 정답 기록 하나만 있어도 과거 정답 이력입니다.
    LearningRecord other = service.createRecord("B", "Q1", "S1", "O", true, 0);
    service.saveRecord(other);
    assert service.hasCorrectAnswer("B", "Q1");
    assert service.getRecords("A").size() == 3;
    assert service.getRecords("B").size() == 1;
    service.saveRecord(service.createRecord("B", "Q2", "S1", "1", true, 0));
    assert service.hasCorrectAnswer("B", "Q2");
    assert !service.hasCorrectAnswer("A", "Q2");
    assert service.findSubmission("A", "S1", "Q1").orElseThrow().getRecordId().equals(wrong.getRecordId());
    assert service.findSubmission("A", "S2", "Q1").orElseThrow().getRecordId().equals(first.getRecordId());
    assert service.findSubmission("B", "S1", "Q1").orElseThrow().getRecordId().equals(other.getRecordId());
    assert service.findSubmission("A", "S4", "Q1").isEmpty();
    assert service.getSessionRecords("A", "S1").size() == 1;
    assert service.getSessionRecords("B", "S1").size() == 2;
    assert service.getSessionRecords("A", "S1").get(0).getUserId().equals("A");

    service.saveRecord(first);
    assert service.getRecords("A").size() == 3;
    expectInvalid(() -> service.saveRecord(service.createRecord("A", "Q1", "S2", "O", true, 10)));
    assert service.getRecords("A").size() == 3;
    RecordService reloaded = new RecordService(new RecordRepository(file));
    assert reloaded.hasCorrectAnswer("A", "Q1");
    assert reloaded.findSubmission("A", "S2", "Q1").orElseThrow().getEarnedXp() == 10;
    assert ids(reloaded.getRecords("A")).equals(List.of(wrong.getRecordId(), first.getRecordId(), repeated.getRecordId()));
    expectReadOnly(service.getRecords("A"));
    expectReadOnly(service.getSessionRecords("A", "S1"));
  }

  private static void checkRecent(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    RecordService service = new RecordService(repository, CLOCK);
    LocalDateTime time = LocalDateTime.now(CLOCK);
    repository.save(at("old", "A", time.minusDays(1)));
    repository.save(at("tie1", "A", time));
    repository.save(at("new", "A", time.plusDays(1)));
    repository.save(at("other", "B", time.plusDays(2)));
    repository.save(at("tie2", "A", time));
    assert ids(service.getRecentRecords("A", 10)).equals(List.of("new", "tie2", "tie1", "old"));
    assert ids(service.getRecentRecords("A", 2)).equals(List.of("new", "tie2"));
    assert ids(service.getRecentRecords("A", 1)).equals(List.of("new"));
    assert service.getRecentRecords("A", Integer.MAX_VALUE).size() == 4;
    assert service.getRecentRecords("missing", 1).isEmpty();
    assert ids(service.getRecords("A")).equals(List.of("old", "tie1", "new", "tie2"));
    expectReadOnly(service.getRecentRecords("A", 10));
    for (int limit : new int[] {0, -1, Integer.MIN_VALUE}) {
      expectInvalid(() -> service.getRecentRecords("A", limit));
    }
  }

  private static void checkInvalidInput(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    RecordService service = new RecordService(repository, CLOCK);
    expectNull(() -> new RecordService(null));
    expectNull(() -> new RecordService(repository, null));
    for (String invalid : new String[] {null, "", " ", "\t\n", "a|b", "a\nb"}) {
      expectInvalid(() -> service.getRecords(invalid));
      expectInvalid(() -> service.getRecentRecords(invalid, 1));
      expectInvalid(() -> service.hasCorrectAnswer(invalid, "Q1"));
      expectInvalid(() -> service.hasCorrectAnswer("A", invalid));
      expectInvalid(() -> service.findSubmission(invalid, "S1", "Q1"));
      expectInvalid(() -> service.findSubmission("A", invalid, "Q1"));
      expectInvalid(() -> service.findSubmission("A", "S1", invalid));
      expectInvalid(() -> service.getSessionRecords(invalid, "S1"));
      expectInvalid(() -> service.getSessionRecords("A", invalid));
    }
    // 모델의 검증을 그대로 재사용합니다.
    for (String invalid : new String[] {null, "", " "}) {
      expectInvalid(() -> service.createRecord(invalid, "Q1", "S1", "O", true, 10));
      expectInvalid(() -> service.createRecord("A", invalid, "S1", "O", true, 10));
      expectInvalid(() -> service.createRecord("A", "Q1", invalid, "O", true, 10));
      expectInvalid(() -> service.createRecord("A", "Q1", "S1", invalid, true, 10));
    }
    expectInvalid(() -> service.createRecord("A", "Q1", "S1", "O", true, -1));
    expectInvalid(() -> service.createRecord("A", "Q1", "S1", "X", false, 10));
    expectInvalid(() -> service.saveRecord(null));
    assert !Files.exists(file);
  }

  private static void checkFileErrors(Path file) throws IOException {
    RecordService service = new RecordService(new RecordRepository(file), CLOCK);
    Files.writeString(file, "손상된 기록\n", StandardCharsets.UTF_8);
    byte[] before = Files.readAllBytes(file);
    expectIo(() -> service.getRecords("A"), file);
    expectIo(() -> service.hasCorrectAnswer("A", "Q1"), file);
    expectIo(() -> service.findSubmission("A", "S1", "Q1"), file);
    expectIo(() -> service.getRecentRecords("A", 1), file);
    expectIo(() -> service.getSessionRecords("A", "S1"), file);
    LearningRecord record = service.createRecord("A", "Q1", "S1", "O", true, 10);
    expectIo(() -> service.saveRecord(record), file);
    assert Arrays.equals(before, Files.readAllBytes(file));
    Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
    expectIo(() -> service.getRecords("A"), file);
    Path directory = file.getParent();
    expectIo(() -> new RecordService(new RecordRepository(directory)).getRecords("A"), directory);
  }

  private static LearningRecord at(String id, String userId, LocalDateTime time) {
    return new LearningRecord(id, userId, id, "S1", "O", true, 0, time);
  }

  private static List<String> ids(List<LearningRecord> records) {
    return records.stream().map(LearningRecord::getRecordId).toList();
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

  private static void expectReadOnly(List<LearningRecord> records) {
    try {
      records.clear();
      throw new AssertionError("조회 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @FunctionalInterface
  private interface Action {
    void run() throws IOException;
  }
}
