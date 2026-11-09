package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * data/save 아래 사용자 저장 파일의 공통 읽기·쓰기입니다.
 * 저장은 임시 파일에 모두 쓴 뒤 원본과 교체하므로, 쓰는 도중 실패해도 원본이 깨지지 않습니다.
 */
final class SaveFile {
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

  static void writeLines(Path path, List<String> lines) throws IOException {
    Path temp = path.resolveSibling(path.getFileName() + ".tmp");
    try {
      Path parent = path.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(temp, lines, StandardCharsets.UTF_8);
      try {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      Files.deleteIfExists(temp);
      throw new IOException(path + ": 파일을 저장할 수 없습니다.", e);
    }
  }
}
