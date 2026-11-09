package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.OXQuestion;
import main.java.com.quizpet.model.Question;

public class QuestionRepositoryTest {
  public static void main(String[] args) throws IOException {
    Path file = Files.createTempFile("quizpet-question-", ".txt");
    String ox = "Q1|SCIENCE|SCIENCE_001|OX|O|\n물은 고체가 될 수 있다.|문제\n얼음은 고체 상태의 물입니다.|해설\n";
    String choice = "Q2|SPORTS|SPORTS_001|MULTIPLE_CHOICE|2|10명,11명,12명,13명\n축구 한 팀의 인원은?\n골키퍼를 포함해 11명입니다.\n";
    try {
      Files.writeString(file, "\n" + ox + "\n" + choice, StandardCharsets.UTF_8);
      QuestionRepository repository = new QuestionRepository(file);
      assert repository.findAll().stream().map(Question::getQuestionId).toList()
          .equals(List.of("Q1", "Q2"));
      Question question = repository.findById("Q1").orElseThrow();
      assert question instanceof OXQuestion;
      assert question.getCategory() == Category.SCIENCE;
      assert question.getContentId().equals("SCIENCE_001");
      assert question.getPrompt().equals("물은 고체가 될 수 있다.|문제");
      assert question.getExplanation().equals("얼음은 고체 상태의 물입니다.|해설");
      assert question.checkAnswer("O");
      MultipleChoiceQuestion multiple = (MultipleChoiceQuestion) repository.findById("Q2").orElseThrow();
      assert multiple.getOptions().equals(List.of("10명", "11명", "12명", "13명"));
      assert multiple.checkAnswer("2");
      assert repository.findById("UNKNOWN").isEmpty();
      assert repository.findByCategory(Category.SCIENCE).equals(List.of(question));
      assert repository.findByCategory(Category.HISTORY).isEmpty();
      assert repository.findByContentId("SCIENCE_001").equals(List.of(question));
      assert repository.findByContentId("UNKNOWN").isEmpty();
      expectReadOnly(repository.findAll());
      expectReadOnly(repository.findByCategory(Category.SCIENCE));
      expectReadOnly(repository.findByContentId("SCIENCE_001"));

      for (String invalid : List.of(
          ox.replace("Q1|", "|"), ox.replace("SCIENCE|", "UNKNOWN|"),
          ox.replace("SCIENCE_001", ""), ox.replace("|OX|", "|UNKNOWN|"),
          ox.replace("|O|", "|o|"), ox.replace("|O|", "|X|보기"),
          ox.replace("|O|", "|O"), ox.replace("|O|", "|O||"),
          "Q1|SCIENCE|SCIENCE_001|OX|O|\n\n해설\n",
          "Q1|SCIENCE|SCIENCE_001|OX|O|\n문제\n\n",
          choice.replace("|2|", "|5|"), choice.replace("10명,11명,12명,13명", "10명,11명,12명"),
          choice.replace("10명,11명,12명,13명", "10명,11명,12명,"),
          choice.replace("10명,11명,12명,13명", "10명,11명,12명, "),
          choice.replace("10명,11명,12명,13명", "10명,11명,12명,13명,14명"))) {
        Files.writeString(file, invalid + ox, StandardCharsets.UTF_8);
        List<Question> loaded = new QuestionRepository(file).findAll();
        assert loaded.size() == 1 : invalid;
        assert loaded.get(0).getQuestionId().equals("Q1");
      }
      Files.writeString(file, ox + ox + choice, StandardCharsets.UTF_8);
      assert new QuestionRepository(file).findAll().size() == 2;
      for (String incomplete : List.of("Q3|SCIENCE|SCIENCE_001|OX|O|",
          "Q3|SCIENCE|SCIENCE_001|OX|O|\n문제\n")) {
        Files.writeString(file, ox + incomplete, StandardCharsets.UTF_8);
        assert new QuestionRepository(file).findAll().size() == 1;
      }
      Files.writeString(file, "\n", StandardCharsets.UTF_8);
      assert new QuestionRepository(file).findAll().isEmpty();
      assert repository.findAll().size() == 2;
    } finally {
      Files.deleteIfExists(file);
    }
    QuestionRepository defaults = new QuestionRepository();
    ContentRepository contents = new ContentRepository();
    assert defaults.findAll().size() == 4;
    for (Question question : defaults.findAll()) {
      assert contents.findById(question.getContentId()).orElseThrow().getCategory()
          == question.getCategory();
    }
    System.out.println("QuestionRepository checks passed");
  }

  private static void expectReadOnly(List<Question> questions) {
    try {
      questions.clear();
      throw new AssertionError("문제 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
