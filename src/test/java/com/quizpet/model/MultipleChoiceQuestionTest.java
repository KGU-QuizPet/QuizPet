package main.java.com.quizpet.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultipleChoiceQuestionTest {
  public static void main(String[] args) {
    List<String> options = new ArrayList<>(List.of("수성", "금성", "지구", "화성"));
    MultipleChoiceQuestion question = create("3", options);
    assert question.getType().equals("MULTIPLE_CHOICE");
    assert question.getAnswer().equals("3");
    for (String answer : List.of("1", "2", "3", "4")) {
      Question correct = create(answer, options);
      for (String selected : List.of("1", "2", "3", "4")) {
        assert correct.checkAnswer(selected) == answer.equals(selected);
      }
    }
    options.clear();
    assert question.getOptions().equals(List.of("수성", "금성", "지구", "화성"));
    try {
      question.getOptions().set(0, "목성");
      throw new AssertionError("보기 목록은 읽기 전용이어야 합니다.");
    } catch (UnsupportedOperationException expected) {
    }
    for (String invalid : new String[] {null, "", " ", "0", "5", "01", " 1", "1 ", "1.0", "지구"}) {
      expectInvalid(() -> create(invalid, question.getOptions()));
      expectInvalid(() -> question.checkAnswer(invalid));
    }
    expectInvalid(() -> create("1", null));
    expectInvalid(() -> create("1", List.of()));
    expectInvalid(() -> create("1", List.of("A", "B", "C")));
    expectInvalid(() -> create("1", List.of("A", "B", "C", "D", "E")));
    expectInvalid(() -> create("1", Arrays.asList("A", null, "C", "D")));
    expectInvalid(() -> create("1", List.of("A", " ", "C", "D")));
    assert question.checkAnswer("3");
    System.out.println("MultipleChoiceQuestion checks passed");
  }

  private static MultipleChoiceQuestion create(String answer, List<String> options) {
    return new MultipleChoiceQuestion("Q1", Category.SCIENCE, "C1",
        "태양에서 세 번째 행성은?", answer, "지구는 세 번째 행성입니다.", options);
  }

  private static void expectInvalid(Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 객관식 데이터가 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }
}
