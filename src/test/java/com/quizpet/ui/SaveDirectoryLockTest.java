package main.java.com.quizpet.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveDirectoryLockTest {
  public static void main(String[] args) throws Exception {
    Path directory = Files.createTempDirectory("quizpet-lock-").resolve("save");
    try (SaveDirectoryLock first = new SaveDirectoryLock(directory)) {
      try (SaveDirectoryLock second = new SaveDirectoryLock(directory)) {
        throw new AssertionError("duplicate writer accepted");
      } catch (IOException expected) {
        assert expected.getMessage().contains("실행");
      }
    }
    try (SaveDirectoryLock reopened = new SaveDirectoryLock(directory)) {
      assert Files.exists(directory.resolve(".quizpet.lock"));
    }
    System.out.println("Save directory lock checks passed");
  }
}
