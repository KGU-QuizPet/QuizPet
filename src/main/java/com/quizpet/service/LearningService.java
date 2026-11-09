package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;
import main.java.com.quizpet.repository.CategoryRepository;
import main.java.com.quizpet.repository.ContentRepository;

public class LearningService {
  private final CategoryRepository categoryRepository;
  private final ContentRepository contentRepository;

  public LearningService() throws IOException {
    this(new CategoryRepository(), new ContentRepository());
  }

  public LearningService(CategoryRepository categoryRepository, ContentRepository contentRepository) {
    this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository는 필수입니다.");
    this.contentRepository = Objects.requireNonNull(contentRepository, "contentRepository는 필수입니다.");
  }

  public List<Category> getCategories() {
    return categoryRepository.findAll();
  }

  public List<LearningContent> getContents(String categoryId) {
    Category category = categoryRepository.findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("정의되지 않은 분야 ID입니다: " + categoryId));
    return contentRepository.findByCategory(category);
  }

  public LearningContent getContent(String categoryId, int contentIndex) {
    List<LearningContent> contents = getContents(categoryId);
    if (contentIndex < 0 || contentIndex >= contents.size()) {
      throw new IllegalArgumentException("학습 카드 인덱스가 범위를 벗어났습니다: " + contentIndex);
    }
    return contents.get(contentIndex);
  }
}
