package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.java.com.quizpet.model.Category;

public class CategoryRepositoryTest {
  public static void main(String[] args) throws IOException {
    CategoryRepository repository = new CategoryRepository();
    assert repository.findAll().equals(List.of(Category.values()));
    for (Category category : Category.values()) {
      assert repository.findById(category.getCategoryId()).orElseThrow() == category;
    }
    assert repository.findById("UNKNOWN").isEmpty();
    try {
      repository.findAll().clear();
      throw new AssertionError("분야 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }

    Path file = Files.createTempFile("quizpet-category-", ".txt");
    try {
      String valid = Files.readString(Path.of("data/content/category.txt"), StandardCharsets.UTF_8);
      Files.writeString(file, "\n" + valid + "\n", StandardCharsets.UTF_8);
      assert new CategoryRepository(file).findAll().equals(repository.findAll());
      for (String invalid : List.of(
          "", valid.replace("SCIENCE", "UNKNOWN"),
          valid.replace("SCIENCE|", "science|"),
          valid.replace("|INT|", "|STR|"),
          valid.replace("과학&상식", "다른 이름"),
          valid.replace("과학 원리와 일반 상식", ""),
          valid + "\n" + valid.lines().findFirst().orElseThrow(),
          valid.substring(valid.indexOf('\n') + 1),
          valid.replace("|INT|", "|INT|extra|"))) {
        Files.writeString(file, invalid, StandardCharsets.UTF_8);
        try {
          new CategoryRepository(file);
          throw new AssertionError("잘못된 분야 파일이 허용되었습니다.");
        } catch (IOException expected) {
          assert expected.getMessage().contains(file.toString());
        }
      }
      assert repository.findAll().size() == 4;
    } finally {
      Files.deleteIfExists(file);
    }
    try {
      new CategoryRepository(file);
      throw new AssertionError("존재하지 않는 파일이 허용되었습니다.");
    } catch (IOException expected) {
    }
    System.out.println("CategoryRepository checks passed");
  }
}
