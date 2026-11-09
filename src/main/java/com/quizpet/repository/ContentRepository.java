package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;

public class ContentRepository {
  private final List<LearningContent> contents;

  public ContentRepository() throws IOException {
    this(Path.of("data/content/content.txt"));
  }

  public ContentRepository(Path path) throws IOException {
    List<LearningContent> loaded = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      int headerLine = i + 1;
      try {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 6) {
          throw new IllegalArgumentException("첫 줄의 필드는 6개여야 합니다.");
        }
        if (i + 1 >= lines.size()) {
          throw new IllegalArgumentException("학습 본문 줄이 누락되었습니다.");
        }
        LearningContent content = new LearningContent(
            fields[0], Category.valueOf(fields[1]), Integer.parseInt(fields[2]),
            Integer.parseInt(fields[3]), fields[4],
            fields[5].isEmpty() ? List.of() : List.of(fields[5].split(",", -1)),
            lines.get(++i));
        if (!ids.add(content.getContentId())) {
          throw new IllegalArgumentException("중복된 학습 콘텐츠 ID입니다.");
        }
        loaded.add(content);
      } catch (IllegalArgumentException e) {
        throw new IOException(path + ":" + headerLine + ": " + e.getMessage(), e);
      }
    }
    contents = List.copyOf(loaded);
  }

  public List<LearningContent> findAll() {
    return contents;
  }

  public Optional<LearningContent> findById(String contentId) {
    return contents.stream()
        .filter(content -> content.getContentId().equals(contentId))
        .findFirst();
  }

  public List<LearningContent> findByCategory(Category category) {
    Objects.requireNonNull(category, "category는 필수입니다.");
    return contents.stream()
        .filter(content -> content.getCategory() == category)
        .sorted(Comparator.comparingInt(LearningContent::getOrder))
        .toList();
  }
}
