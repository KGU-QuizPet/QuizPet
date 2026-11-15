package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * data/save 아래 사용자 저장 파일의 공통 읽기·쓰기입니다.
 * 저장은 임시 파일에 모두 쓴 뒤 원본과 원자적으로 교체하므로, 쓰는 도중 실패해도 원본이 깨지지 않습니다.
 * 원자적 파일 교체를 지원하지 않는 파일 시스템에서는 원본 보호를 위해 저장을 거부합니다(RecordRepository와 동일).
 */
final class SaveFile {
  /**
   * 같은 JVM에서 서로 다른 Repository 인스턴스의 읽기-검사-저장을 직렬화합니다.
   * 다른 프로세스나 이 클래스를 거치지 않는 파일 변경은 보호하지 않습니다.
   */
  static final Object LOCK = new Object();

  private SaveFile() {
  }

  /** 파일이 아직 없으면(첫 실행) 빈 목록을 반환합니다. */
  static List<String> readLines(Path path) throws IOException {
    if (Files.notExists(path)) {
      return List.of();
    }
    try {
      return Files.readAllLines(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IOException(path + ": 파일을 읽을 수 없습니다.", e);
    }
  }

  /** 줄바꿈은 운영체제와 관계없이 \n으로 통일합니다(record.txt와 동일). */
  static void writeLines(Path path, List<String> lines) throws IOException {
    Path temp = null;
    try {
      Path target = path.toAbsolutePath();
      Files.createDirectories(target.getParent());
      temp = Files.createTempFile(target.getParent(), target.getFileName() + "-", ".tmp");
      String content = lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
      // writeString은 잘못된 유니코드 문자열의 UTF-8 변환 실패도 예외로 전달합니다.
      Files.writeString(temp, content, StandardCharsets.UTF_8);
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
