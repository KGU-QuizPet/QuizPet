package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;
import main.java.com.quizpet.repository.CategoryRepository;
import main.java.com.quizpet.repository.ContentRepository;

public class LearningServiceTest {
  public static void main(String[] args) throws IOException {
    Path file = Files.createTempFile("quizpet-learning-", ".txt");
    try {
      Files.writeString(file,
          "S2|SCIENCE|5|1|중력|물리\n물체는 서로 끌어당깁니다.\n"
              + "H1|HISTORY|0|1|역사|\n과거의 기록입니다.\n"
              + "S1|SCIENCE|2|3|물의 상태|\n물은 세 가지 상태로 존재합니다.\n",
          StandardCharsets.UTF_8);
      LearningService service = new LearningService(
          new CategoryRepository(), new ContentRepository(file));
      assert service.getCategories().equals(List.of(Category.values()));
      assert service.getContents("SCIENCE").stream().map(LearningContent::getContentId)
          .toList().equals(List.of("S1", "S2"));
      assert service.getContents("HISTORY").size() == 1;
      assert service.getContents("SPORTS").isEmpty();
      assert service.getContent("SCIENCE", 0).getContentId().equals("S1");
      assert service.getContent("SCIENCE", 1).getContentId().equals("S2");
      assert service.getContent("SCIENCE", 0).getOrder() == 2;
      assert service.getContent("SCIENCE", 0).getDifficulty() == 3;
      assert service.getContent("SCIENCE", 0).getTitle().equals("물의 상태");
      assert service.getContent("SCIENCE", 0).getDescription().equals("물은 세 가지 상태로 존재합니다.");
      assert service.getContent("SCIENCE", 0).getTags().isEmpty();
      assert service.getContent("SCIENCE", 1).getOrder() == 5;
      assert service.getContent("SCIENCE", 1).getDifficulty() == 1;
      assert service.getContent("SCIENCE", 1).getTitle().equals("중력");
      assert service.getContent("SCIENCE", 1).getDescription().equals("물체는 서로 끌어당깁니다.");
      assert service.getContent("SCIENCE", 1).getTags().equals(List.of("물리"));
      assert service.getContent("HISTORY", 0).getCategory() == Category.HISTORY;
      for (String invalid : new String[] {null, "", " ", "UNKNOWN", "science", " SCIENCE "}) {
        expectInvalid(() -> service.getContents(invalid));
        expectInvalid(() -> service.getContent(invalid, 0));
      }
      expectInvalid(() -> service.getContent("SCIENCE", -1));
      expectInvalid(() -> service.getContent("SCIENCE", 2));
      expectInvalid(() -> service.getContent("SCIENCE", Integer.MAX_VALUE));
      expectInvalid(() -> service.getContent("SPORTS", 0));
      expectReadOnly(service.getCategories());
      expectReadOnly(service.getContents("SCIENCE"));

      Files.writeString(file, "", StandardCharsets.UTF_8);
      LearningService empty = new LearningService(
          new CategoryRepository(), new ContentRepository(file));
      assert empty.getCategories().size() == 4;
      assert empty.getContents("SCIENCE").isEmpty();
      expectInvalid(() -> empty.getContent("SCIENCE", 0));
    } finally {
      Files.deleteIfExists(file);
    }
    LearningService defaults = new LearningService();
    assert defaults.getCategories().equals(List.of(Category.values()));
    for (Category category : defaults.getCategories()) {
      String categoryId = category.getCategoryId();
      List<LearningContent> cards = defaults.getContents(categoryId);
      assert cards.size() == 10;
      expectReadOnly(cards);
      for (int i = 0; i < cards.size(); i++) {
        LearningContent card = defaults.getContent(categoryId, i);
        assert card == cards.get(i);
        assert card.getCategory() == category;
        assert card.getOrder() == i;
        assert !card.getTitle().isBlank();
        assert !card.getDescription().isBlank();
        assert card.getDifficulty() > 0;
        assert !card.getTags().isEmpty();
      }
      expectInvalid(() -> defaults.getContent(categoryId, -1));
      expectInvalid(() -> defaults.getContent(categoryId, cards.size()));
    }
    System.out.println("LearningService checks passed");
  }

  private static void expectInvalid(Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 학습 조회가 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void expectReadOnly(List<?> values) {
    try {
      values.clear();
      throw new AssertionError("조회 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
