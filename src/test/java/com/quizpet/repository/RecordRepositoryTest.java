package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import main.java.com.quizpet.model.LearningRecord;

public class RecordRepositoryTest {
  private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 10, 12, 30, 15, 123456789);

  public static void main(String[] args) throws IOException {
    boolean assertionsEnabled = false;
    assert assertionsEnabled = true;
    if (!assertionsEnabled) {
      throw new AssertionError("-ea 옵션으로 실행해 주세요.");
    }
    Path directory = Files.createTempDirectory("quizpet-record-test-");
    try {
      checkRoundTrip(directory.resolve("save/record.txt"));
      checkInvalidFiles(directory.resolve("invalid.txt"));
      checkInvalidFields(directory.resolve("fields.txt"));
      checkWriteFailure(directory.resolve("failure.txt"));
    } finally {
      List<Path> paths;
      try (var stream = Files.walk(directory)) {
        paths = stream.sorted(Comparator.reverseOrder()).toList();
      }
      for (Path path : paths) {
        Files.deleteIfExists(path);
      }
    }
    System.out.println("RecordRepository checks passed");
  }

  private static void checkRoundTrip(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    assert repository.findAll().isEmpty();
    assert repository.findById("missing").isEmpty();
    assert !Files.exists(file);
    assert !Files.exists(file.getParent());
    LearningRecord first = record("R1", "사용자1", "Q1", "S1", "O", true, 10);
    repository.save(first);
    String firstLine = "R1|사용자1|Q1|S1|O|true|10|2026-11-10T12:30:15.123456789\n";
    assert Files.readString(file, StandardCharsets.UTF_8).equals(firstLine);
    assertSame(first, new RecordRepository(file).findById("R1").orElseThrow());
    byte[] original = Files.readAllBytes(file);
    repository.save(record("R1", "사용자1", "Q1", "S1", "O", true, 10));
    assert Arrays.equals(original, Files.readAllBytes(file));
    expectInvalid(() -> repository.save(record("R1", "U2", "Q2", "S2", "1", true, 10)));
    expectInvalid(() -> repository.save(record("R2", "사용자1", "Q1", "S1", "O", true, 10)));
    expectInvalid(() -> repository.save(record("R1", "사용자1", "Q1", "S1", "X", false, 0)));
    assert Arrays.equals(original, Files.readAllBytes(file));

    // 같은 문제의 다른 세션 재정답과 다른 사용자의 풀이 모두 별도 기록입니다.
    repository.save(record("R2", "사용자1", "Q1", "S2", "O", true, 0));
    RecordRepository secondInstance = new RecordRepository(file);
    secondInstance.save(record("R3", "U2", "Q1", "S1", "X", false, 0));
    secondInstance.save(record("R4", "U2", "Q2", "S1", "4", true, 10));
    assert repository.findAll().stream().map(LearningRecord::getRecordId).toList()
        .equals(List.of("R1", "R2", "R3", "R4"));
    assert repository.findByUserId("사용자1").size() == 2;
    assert repository.findByUserId("missing").isEmpty();
    assert repository.findBySubmission("사용자1", "S2", "Q1").orElseThrow().getEarnedXp() == 0;
    assert repository.findBySubmission("U2", "S2", "Q1").isEmpty();
    expectReadOnly(repository.findAll());
    expectReadOnly(repository.findByUserId("U2"));
    expectInvalid(() -> new RecordRepository(null));
    expectInvalid(() -> repository.save(null));
    expectInvalid(() -> repository.findById(""));
    expectInvalid(() -> repository.findByUserId(null));
    expectInvalid(() -> repository.findBySubmission("U2", " ", "Q1"));
  }

  private static void checkInvalidFiles(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    Files.writeString(file, "", StandardCharsets.UTF_8);
    assert repository.findAll().isEmpty();
    Files.writeString(file, "\n \n", StandardCharsets.UTF_8);
    assert repository.findAll().isEmpty();
    String valid = "R1|U1|Q1|S1|O|true|10|2026-11-10T12:30:15\n";
    for (String invalid : List.of(
        "broken\n", valid.replace("|O|", "||"), valid.replace("R1|", "|"),
        valid.replace("|true|", "|TRUE|"), valid.replace("|true|", "|unknown|"),
        valid.replace("|10|", "|-1|"), valid.replace("|10|", "|abc|"),
        valid.replace("|10|", "|2147483648|"), valid.replace("|true|", "|false|"),
        valid.replace("2026-11-10T12:30:15", "2026-02-30T12:30:15"),
        valid.stripTrailing() + "|extra\n", valid + valid,
        valid + valid.replace("R1|", "R2|"), valid.replace("|O|", "|a\u2028b|"))) {
      Files.writeString(file, "\n" + invalid, StandardCharsets.UTF_8);
      byte[] before = Files.readAllBytes(file);
      IOException error = expectIo(() -> repository.findAll(), file);
      assert error.getMessage().contains(":2:") || error.getMessage().contains(":3:");
      expectIo(() -> repository.save(record("NEW", "U2", "Q2", "S2", "1", true, 10)), file);
      assert Arrays.equals(before, Files.readAllBytes(file));
    }
    Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
    IOException error = expectIo(() -> repository.findAll(), file);
    assert error.getCause() instanceof MalformedInputException;
    expectIo(() -> repository.save(record("NEW", "U2", "Q2", "S2", "1", true, 10)), file);
    assert Arrays.equals(Files.readAllBytes(file), new byte[] {(byte) 0xC3, (byte) 0x28});
    expectIo(() -> new RecordRepository(file.getParent()).findAll(), file.getParent());
  }

  private static void checkInvalidFields(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    for (String invalid : List.of("a|b", "a\nb", "a\rb", "a\r\nb", "a\u0085b", "a\u2028b", "a\u2029b")) {
      expectInvalid(() -> repository.save(record(invalid, "U1", "Q1", "S1", "O", true, 10)));
      expectInvalid(() -> repository.save(record("R1", invalid, "Q1", "S1", "O", true, 10)));
      expectInvalid(() -> repository.save(record("R1", "U1", invalid, "S1", "O", true, 10)));
      expectInvalid(() -> repository.save(record("R1", "U1", "Q1", invalid, "O", true, 10)));
      expectInvalid(() -> repository.save(record("R1", "U1", "Q1", "S1", invalid, true, 10)));
    }
    assert !Files.exists(file);
  }

  private static void checkWriteFailure(Path file) throws IOException {
    RecordRepository repository = new RecordRepository(file);
    repository.save(record("R1", "U1", "Q1", "S1", "O", true, 10));
    byte[] before = Files.readAllBytes(file);
    // 기존 파일은 읽을 수 있는 상태에서, 잘못된 서로게이트로 임시 파일 쓰기를 실패시킵니다.
    expectIo(() -> repository.save(record("R2", "U1", "Q2", "S1", "\uD800", true, 10)), file);
    assert Arrays.equals(before, Files.readAllBytes(file));
    assert repository.findAll().size() == 1;
    try (var files = Files.list(file.getParent())) {
      assert files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp"));
    }
    // 부모 경로가 일반 파일이면 디렉터리 생성에 실패합니다.
    Path blocked = file.resolve("record.txt");
    expectIo(() -> new RecordRepository(blocked).save(record("R2", "U1", "Q2", "S1", "1", true, 10)), blocked);
    assert Arrays.equals(before, Files.readAllBytes(file));
    repository.save(record("R2", "U1", "Q2", "S1", "1", true, 10));
    assert new RecordRepository(file).findAll().size() == 2;
  }

  private static LearningRecord record(String id, String user, String question, String session,
      String answer, boolean correct, int xp) {
    return new LearningRecord(id, user, question, session, answer, correct, xp, TIME);
  }

  private static void assertSame(LearningRecord expected, LearningRecord actual) {
    assert actual.getRecordId().equals(expected.getRecordId());
    assert actual.getUserId().equals(expected.getUserId());
    assert actual.getQuestionId().equals(expected.getQuestionId());
    assert actual.getSessionId().equals(expected.getSessionId());
    assert actual.getSelectedAnswer().equals(expected.getSelectedAnswer());
    assert actual.isCorrect() == expected.isCorrect();
    assert actual.getEarnedXp() == expected.getEarnedXp();
    assert actual.getAnsweredAt().equals(expected.getAnsweredAt());
  }

  private static void expectInvalid(Action action) throws IOException {
    try {
      action.run();
      throw new AssertionError("잘못된 기록 또는 조회 입력이 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static IOException expectIo(Action action, Path file) {
    try {
      action.run();
      throw new AssertionError("파일 오류가 전달되지 않았습니다.");
    } catch (IOException expected) {
      assert expected.getMessage().contains(file.toString());
      return expected;
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
