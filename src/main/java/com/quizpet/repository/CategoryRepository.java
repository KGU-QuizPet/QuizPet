package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import main.java.com.quizpet.model.Category;

public class CategoryRepository {
  private final List<Category> categories;

  public CategoryRepository() throws IOException {
    this(Path.of("data/content/category.txt"));
  }

  public CategoryRepository(Path path) throws IOException {
    List<Category> loaded = new ArrayList<>();
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      try {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 4) {
          throw new IllegalArgumentException("필드는 4개여야 합니다.");
        }
        Category category = Category.valueOf(fields[0]);
        if (!category.getName().equals(fields[1])
            || !category.getStatType().equals(fields[2])
            || !category.getDescription().equals(fields[3])) {
          throw new IllegalArgumentException("분야 정보가 Category 정의와 일치하지 않습니다.");
        }
        if (loaded.contains(category)) {
          throw new IllegalArgumentException("중복된 분야 ID입니다.");
        }
        loaded.add(category);
      } catch (IllegalArgumentException e) {
        throw new IOException(path + ":" + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    if (loaded.size() != Category.values().length) {
      throw new IOException(path + ": 모든 학습 분야가 있어야 합니다.");
    }
    categories = List.copyOf(loaded);
  }

  public List<Category> findAll() {
    return categories;
  }

  public Optional<Category> findById(String categoryId) {
    return categories.stream()
        .filter(category -> category.getCategoryId().equals(categoryId))
        .findFirst();
  }
}
