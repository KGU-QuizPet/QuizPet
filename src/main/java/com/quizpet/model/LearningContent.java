package main.java.com.quizpet.model;

import java.util.List;

public final class LearningContent {
  private final String contentId;
  private final Category category;
  private final int order;
  private final int difficulty;
  private final String title;
  private final List<String> tags;
  private final String description;

  public LearningContent(String contentId, Category category, int order, int difficulty,
      String title, List<String> tags, String description) {
    requireText(contentId, "contentId");
    requireText(title, "title");
    requireText(description, "description");
    if (category == null) {
      throw new IllegalArgumentException("category는 필수입니다.");
    }
    if (order < 0) {
      throw new IllegalArgumentException("order는 0 이상이어야 합니다.");
    }
    if (difficulty < 1) {
      throw new IllegalArgumentException("difficulty는 양수여야 합니다.");
    }
    if (tags == null) {
      throw new IllegalArgumentException("tags는 필수입니다. 태그가 없으면 빈 목록을 사용하세요.");
    }
    for (String tag : tags) {
      requireText(tag, "tag");
    }
    this.contentId = contentId;
    this.category = category;
    this.order = order;
    this.difficulty = difficulty;
    this.title = title;
    this.tags = List.copyOf(tags);
    this.description = description;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
    }
  }

  public String getContentId() {
    return contentId;
  }

  public Category getCategory() {
    return category;
  }

  public String getCategoryId() {
    return category.getCategoryId();
  }

  public int getOrder() {
    return order;
  }

  public int getDifficulty() {
    return difficulty;
  }

  public String getTitle() {
    return title;
  }

  public List<String> getTags() {
    return tags;
  }

  public String getDescription() {
    return description;
  }
}
