package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import main.java.com.quizpet.model.LearningRecord;

/**
 * 사용자 풀이 기록을 UTF-8 TXT로 저장합니다. 조회마다 파일을 다시 읽습니다.
 * 임시 규격: recordId|userId|questionId|sessionId|selectedAnswer|correct|earnedXp|answeredAt
 * correct는 true/false, answeredAt은 ISO_LOCAL_DATE_TIME(소수 초 보존)입니다.
 * 원자적 파일 교체를 지원하지 않는 파일 시스템에서는 원본 보호를 위해 저장을 거부합니다.
 * 이 클래스는 record.txt 하나만 보호하며 pet.txt와의 동시 저장은 담당하지 않습니다.
 */
public class RecordRepository {
  // 같은 JVM에서 서로 다른 Repository 인스턴스의 읽기-검사-저장을 직렬화합니다.
  // 다른 프로세스나 이 클래스를 거치지 않는 파일 변경은 보호하지 않습니다.
  private static final Object SAVE_LOCK = new Object();
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private final Path path;

  public RecordRepository() {
    this(Path.of("data/save/record.txt"));
  }

  public RecordRepository(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("path는 필수입니다.");
    }
    this.path = path;
  }

  public List<LearningRecord> findAll() throws IOException {
    if (Files.notExists(path)) {
      return List.of();
    }
    List<String> lines;
    try {
      lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IOException(path + ": 파일을 읽을 수 없습니다.", e);
    }
    List<LearningRecord> records = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Set<List<String>> submissions = new HashSet<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      try {
        LearningRecord record = parse(line);
        if (!ids.add(record.getRecordId())) {
          throw new IllegalArgumentException("중복된 기록 ID입니다.");
        }
        if (!submissions.add(submissionKey(record))) {
          throw new IllegalArgumentException("중복된 사용자·세션·문제 기록입니다.");
        }
        records.add(record);
      } catch (RuntimeException e) {
        throw new IOException(path + ":" + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    return List.copyOf(records);
  }

  public Optional<LearningRecord> findById(String recordId) throws IOException {
    requireField(recordId, "recordId");
    return findAll().stream().filter(record -> record.getRecordId().equals(recordId)).findFirst();
  }

  public List<LearningRecord> findByUserId(String userId) throws IOException {
    requireField(userId, "userId");
    return findAll().stream().filter(record -> record.getUserId().equals(userId)).toList();
  }

  public Optional<LearningRecord> findBySubmission(String userId, String sessionId,
      String questionId) throws IOException {
    requireField(userId, "userId");
    requireField(sessionId, "sessionId");
    requireField(questionId, "questionId");
    return findAll().stream().filter(record -> record.getUserId().equals(userId)
        && record.getSessionId().equals(sessionId) && record.getQuestionId().equals(questionId))
        .findFirst();
  }

  /** 완전히 같은 기록의 재저장은 무시하고, ID 또는 제출 키가 겹치는 다른 기록은 거부합니다. */
  public void save(LearningRecord record) throws IOException {
    if (record == null) {
      throw new IllegalArgumentException("record는 필수입니다.");
    }
    String newLine = format(record);
    synchronized (SAVE_LOCK) {
      List<LearningRecord> savedRecords = findAll();
      for (LearningRecord saved : savedRecords) {
        if (saved.getRecordId().equals(record.getRecordId())
            || submissionKey(saved).equals(submissionKey(record))) {
          if (format(saved).equals(newLine)) {
            return;
          }
          throw new IllegalArgumentException("기록 ID 또는 사용자·세션·문제가 중복됩니다.");
        }
      }
      List<String> lines = new ArrayList<>();
      for (LearningRecord saved : savedRecords) {
        lines.add(format(saved));
      }
      lines.add(newLine);
      writeSafely(lines);
    }
  }

  private static List<String> submissionKey(LearningRecord record) {
    return List.of(record.getUserId(), record.getSessionId(), record.getQuestionId());
  }

  private static LearningRecord parse(String line) {
    String[] fields = line.split("\\|", -1);
    if (fields.length != 8) {
      throw new IllegalArgumentException("필드는 8개여야 합니다.");
    }
    if (!"true".equals(fields[5]) && !"false".equals(fields[5])) {
      throw new IllegalArgumentException("correct는 true 또는 false여야 합니다.");
    }
    LearningRecord record = new LearningRecord(fields[0], fields[1], fields[2], fields[3],
        fields[4], Boolean.parseBoolean(fields[5]), Integer.parseInt(fields[6]),
        LocalDateTime.parse(fields[7], TIME_FORMAT));
    format(record);
    return record;
  }

  private static String format(LearningRecord record) {
    requireField(record.getRecordId(), "recordId");
    requireField(record.getUserId(), "userId");
    requireField(record.getQuestionId(), "questionId");
    requireField(record.getSessionId(), "sessionId");
    requireField(record.getSelectedAnswer(), "selectedAnswer");
    return String.join("|", record.getRecordId(), record.getUserId(), record.getQuestionId(),
        record.getSessionId(), record.getSelectedAnswer(), Boolean.toString(record.isCorrect()),
        Integer.toString(record.getEarnedXp()), TIME_FORMAT.format(record.getAnsweredAt()));
  }

  private static void requireField(String value, String field) {
    if (value == null || value.isBlank() || value.contains("|")
        || value.matches("(?s).*\\R.*")) {
      throw new IllegalArgumentException(field + "는 필수이며 구분자나 줄바꿈을 포함할 수 없습니다.");
    }
  }

  private void writeSafely(List<String> lines) throws IOException {
    Path temp = null;
    try {
      Path target = path.toAbsolutePath();
      Files.createDirectories(target.getParent());
      temp = Files.createTempFile(target.getParent(), "record-", ".tmp");
      // writeString은 잘못된 유니코드 문자열의 UTF-8 변환 실패도 예외로 전달합니다.
      Files.writeString(temp, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      if (temp != null) {
        try {
          Files.deleteIfExists(temp);
        } catch (IOException cleanupError) {
          e.addSuppressed(cleanupError);
        }
      }
      throw new IOException(path + ": 파일을 저장할 수 없습니다.", e);
    }
  }
}
