package main.java.com.quizpet.model;

import java.util.ArrayList;
import java.util.List;

public class LearningContentTest {
  public static void main(String[] args) {
    List<String> tags = new ArrayList<>(List.of("물리"));
    LearningContent content = new LearningContent(
        "SCIENCE_001", Category.SCIENCE, 0, 1, "중력", tags, "물체는 서로 끌어당깁니다.");
    assert content.getContentId().equals("SCIENCE_001");
    assert content.getCategory() == Category.SCIENCE;
    assert content.getOrder() == 0;
    assert content.getDifficulty() == 1;
    assert content.getTitle().equals("중력");
    assert content.getDescription().equals("물체는 서로 끌어당깁니다.");
    tags.clear();
    assert content.getTags().equals(List.of("물리"));
    try {
      content.getTags().add("화학");
      throw new AssertionError("태그 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }
    expectInvalid(() -> create(" ", Category.SCIENCE, 0, 1, "제목", List.of(), "본문"));
    expectInvalid(() -> create(null, Category.SCIENCE, 0, 1, "제목", List.of(), "본문"));
    expectInvalid(() -> create("C1", null, 0, 1, "제목", List.of(), "본문"));
    expectInvalid(() -> create("C1", Category.SCIENCE, -1, 1, "제목", List.of(), "본문"));
    expectInvalid(() -> create("C1", Category.SCIENCE, 0, 0, "제목", List.of(), "본문"));
    expectInvalid(() -> create("C1", Category.SCIENCE, 0, 1, " ", List.of(), "본문"));
    expectInvalid(() -> create("C1", Category.SCIENCE, 0, 1, "제목", null, "본문"));
    expectInvalid(() -> create("C1", Category.SCIENCE, 0, 1, "제목", List.of(" "), "본문"));
    expectInvalid(() -> create("C1", Category.SCIENCE, 0, 1, "제목", List.of(), " "));
    create("C1", Category.SCIENCE, 0, 1, "제목", List.of(), "본문");
    System.out.println("LearningContent checks passed");
  }

  private static void create(String id, Category category, int order, int difficulty,
      String title, List<String> tags, String description) {
    new LearningContent(id, category, order, difficulty, title, tags, description);
  }

  private static void expectInvalid(Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 학습 콘텐츠가 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }
}
