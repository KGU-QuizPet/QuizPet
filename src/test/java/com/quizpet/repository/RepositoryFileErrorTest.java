package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public class RepositoryFileErrorTest {
  public static void main(String[] args) throws IOException {
    Path directory = Files.createTempDirectory("quizpet-file-error-");
    Path invalid = directory.resolve("invalid.txt");
    try {
      Files.write(invalid, new byte[] {(byte) 0xC3, (byte) 0x28});
      for (Loader loader : List.<Loader>of(
          CategoryRepository::new, ContentRepository::new, QuestionRepository::new)) {
        expectReadFailure(loader, directory.resolve("missing.txt"), NoSuchFileException.class);
        expectReadFailure(loader, invalid, MalformedInputException.class);
        expectReadFailure(loader, directory, IOException.class);
      }
      assert !Files.exists(directory.resolve("missing.txt"));
      assert Files.readAllBytes(invalid)[0] == (byte) 0xC3;
    } finally {
      Files.deleteIfExists(invalid);
      Files.deleteIfExists(directory);
    }
    System.out.println("Repository file error checks passed");
  }

  private static void expectReadFailure(Loader loader, Path path,
      Class<? extends IOException> causeType) {
    try {
      loader.load(path);
      throw new AssertionError("읽을 수 없는 파일이 허용되었습니다.");
    } catch (IOException expected) {
      assert expected.getMessage().contains(path.toString());
      assert causeType.isInstance(expected.getCause()) : expected;
    }
  }

  @FunctionalInterface
  private interface Loader {
    void load(Path path) throws IOException;
  }
}
