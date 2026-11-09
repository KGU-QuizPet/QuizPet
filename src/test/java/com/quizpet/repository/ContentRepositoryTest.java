package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;

public class ContentRepositoryTest {
  public static void main(String[] args) throws IOException {
    Path file = Files.createTempFile("quizpet-content-", ".txt");
    String first = "S2|SCIENCE|2|1|중력|물리,과학\n물체는 서로 끌어당깁니다.|본문\n";
    String second = "S1|SCIENCE|0|2|물의 상태|\n물은 세 가지 상태로 존재합니다.\n";
    String third = "H1|HISTORY|0|1|역사|인물\n역사는 과거의 기록입니다.\n";
    try {
      Files.writeString(file, "\n" + first + "\n" + second + third, StandardCharsets.UTF_8);
      ContentRepository repository = new ContentRepository(file);
      assert repository.findAll().stream().map(LearningContent::getContentId).toList()
          .equals(List.of("S2", "S1", "H1"));
      LearningContent content = repository.findById("S2").orElseThrow();
      assert content.getCategory() == Category.SCIENCE;
      assert content.getOrder() == 2;
      assert content.getDifficulty() == 1;
      assert content.getTitle().equals("중력");
      assert content.getTags().equals(List.of("물리", "과학"));
      assert content.getDescription().equals("물체는 서로 끌어당깁니다.|본문");
      assert repository.findById("S1").orElseThrow().getTags().isEmpty();
      assert repository.findById("UNKNOWN").isEmpty();
      assert repository.findByCategory(Category.SCIENCE).stream()
          .map(LearningContent::getContentId).toList().equals(List.of("S1", "S2"));
      assert repository.findByCategory(Category.SPORTS).isEmpty();
      expectReadOnly(repository.findAll());
      expectReadOnly(repository.findByCategory(Category.SCIENCE));

      for (String invalid : List.of(
          "S1|SCIENCE|0|1|제목|", "S1|SCIENCE|0|1|제목|\n\n",
          first + first, first.replace("SCIENCE", "UNKNOWN"),
          first.replace("|2|1|", "|no|1|"), first.replace("|2|1|", "|-1|1|"),
          first.replace("|2|1|", "|2147483648|1|"),
          first.replace("|2|1|", "|2|no|"), first.replace("|2|1|", "|2|-1|"),
          first.replace("|2|1|", "|2|2147483648|"),
          first.replace("|2|1|", "|2|0|"), first.replace("|중력|", "||"),
          first.replace("S2|", "|"), first.replace("물리,과학", "물리,"),
          first.replace("물리,과학", "물리, "), first.replace("|물리,과학", ""),
          first.replace("|물리,과학", "|물리|과학"))) {
        Files.writeString(file, invalid, StandardCharsets.UTF_8);
        try {
          new ContentRepository(file);
          throw new AssertionError("잘못된 학습 콘텐츠 파일이 허용되었습니다.");
        } catch (IOException expected) {
          assert expected.getMessage().contains(file.toString());
          assert expected.getMessage().contains(":1:")
              || expected.getMessage().contains(":3:");
        }
      }
      assert repository.findAll().size() == 3;
      Files.writeString(file, "\n", StandardCharsets.UTF_8);
      assert new ContentRepository(file).findAll().isEmpty();
    } finally {
      Files.deleteIfExists(file);
    }
    try {
      new ContentRepository(file);
      throw new AssertionError("존재하지 않는 파일이 허용되었습니다.");
    } catch (IOException expected) {
    }
    assert new ContentRepository().findAll().size() == 4;
    System.out.println("ContentRepository checks passed");
  }

  private static void expectReadOnly(List<LearningContent> contents) {
    try {
      contents.clear();
      throw new AssertionError("학습 콘텐츠 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
