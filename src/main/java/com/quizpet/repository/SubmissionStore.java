package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.Pet;

/** 펫과 풀이 기록의 부분 저장을 다음 실행에서 완료하는 redo 기록입니다. */
public final class SubmissionStore {
  private static final String VERSION = "QUIZPET_SUBMISSION_V1";
  private final Path pending;
  private final PetRepository pets;
  private final RecordRepository records;

  public SubmissionStore(Path directory) {
    pending = directory.resolve("submission-pending.txt");
    pets = new PetRepository(directory.resolve("pet.txt"));
    records = new RecordRepository(directory.resolve("record.txt"));
  }

  // shortcut: 같은 JVM의 저장만 직렬화합니다. 여러 프로세스가 저장소를 공유하면 파일 잠금이 필요합니다.
  public static <T> T locked(IOAction<T> action) throws IOException {
    synchronized (SaveFile.LOCK) {
      return action.run();
    }
  }

  public void commit(Pet before, Pet after, LearningRecord record) throws IOException {
    locked(() -> {
      if (Files.exists(pending)) throw new IOException(pending + ": 미복구 제출이 있습니다.");
      try {
        validate(before, after, record);
      } catch (RuntimeException e) {
        throw new SubmissionRejectedException(e);
      }
      SaveFile.writeLines(pending, List.of(VERSION, PetRepository.format(before),
          PetRepository.format(after), RecordRepository.format(record)));
      recover();
      return null;
    });
  }

  public void recover() throws IOException {
    locked(() -> {
      if (Files.notExists(pending)) return null;
      List<String> lines = SaveFile.readLines(pending);
      if (lines.size() != 4 || !VERSION.equals(lines.get(0))) {
        throw new IOException(pending + ": 복구 기록 형식이 올바르지 않습니다.");
      }
      Pet before;
      Pet after;
      LearningRecord record;
      try {
        before = PetRepository.parse(lines.get(1));
        after = PetRepository.parse(lines.get(2));
        record = RecordRepository.parse(lines.get(3));
        validate(before, after, record);
      } catch (RuntimeException e) {
        throw new IOException(pending + ": 복구 기록이 손상되었습니다.", e);
      }
      Pet current = pets.findById(before.getPetId())
          .orElseThrow(() -> new IOException(pending + ": 복구할 캐릭터가 없습니다."));
      String saved = PetRepository.format(current);
      if (!saved.equals(lines.get(1)) && !saved.equals(lines.get(2))) {
        throw new IOException(pending + ": 저장된 캐릭터가 복구 전후 상태와 다릅니다.");
      }
      // 두 파일을 모두 검증한 뒤 쓰므로 손상된 기록 파일을 덮어쓰지 않습니다.
      List<LearningRecord> existing = records.findAll();
      for (LearningRecord item : existing) {
        if (item.getRecordId().equals(record.getRecordId())
            || (item.getUserId().equals(record.getUserId())
            && item.getSessionId().equals(record.getSessionId())
            && item.getQuestionId().equals(record.getQuestionId()))) {
          if (!RecordRepository.format(item).equals(lines.get(3))) {
            throw new IOException(pending + ": 저장된 제출과 복구 기록이 충돌합니다.");
          }
        }
      }
      pets.save(after);
      records.save(record);
      Files.delete(pending);
      return null;
    });
  }

  private static void validate(Pet before, Pet after, LearningRecord record) {
    if (!before.getPetId().equals(after.getPetId())
        || !before.getUserId().equals(after.getUserId())
        || !before.getUserId().equals(record.getUserId())
        || !before.getName().equals(after.getName())
        || before.getAppearance() != after.getAppearance()
        || after.getTotalXp() < 0
        || after.getTotalXp() - (long) before.getTotalXp() != record.getEarnedXp()
        || (record.getEarnedXp() != 0 && record.getEarnedXp() != 10)) {
      throw new IllegalArgumentException("캐릭터와 보상 기록이 일치하지 않습니다.");
    }
    long changed = 0;
    for (String stat : List.of("STR", "INT", "FOC", "WIS")) {
      int afterValue = after.getStats().get(stat);
      long delta = (long) afterValue - before.getStats().get(stat);
      if (afterValue < 0 || delta < 0 || delta > 1) throw new IllegalArgumentException("능력치 증가량이 올바르지 않습니다.");
      changed += delta;
    }
    if (changed != record.getEarnedXp() / 10) {
      throw new IllegalArgumentException("능력치와 경험치 보상이 일치하지 않습니다.");
    }
  }

  @FunctionalInterface
  public interface IOAction<T> {
    T run() throws IOException;
  }
}
