package main.java.com.quizpet.model;

public class QuestionTest {
  public static void main(String[] args) {
    Question question = create("Q1", Category.SCIENCE, "C1", "지구는 둥글다.", "O", "지구는 구형입니다.");
    assert question.getQuestionId().equals("Q1");
    assert question.getCategory() == Category.SCIENCE;
    assert question.getCategoryId().equals("SCIENCE");
    assert question.getContentId().equals("C1");
    assert question.getPrompt().equals("지구는 둥글다.");
    assert question.getAnswer().equals("O");
    assert question.getExplanation().equals("지구는 구형입니다.");
    assert question.getType().equals("TEST");
    assert question.checkAnswer("O");
    assert !question.checkAnswer("X");

    for (String invalid : new String[] {null, "", " \t\n"}) {
      expectInvalid(() -> create(invalid, Category.SCIENCE, "C1", "문제", "O", "해설"));
      expectInvalid(() -> create("Q1", Category.SCIENCE, invalid, "문제", "O", "해설"));
      expectInvalid(() -> create("Q1", Category.SCIENCE, "C1", invalid, "O", "해설"));
      expectInvalid(() -> create("Q1", Category.SCIENCE, "C1", "문제", invalid, "해설"));
      expectInvalid(() -> create("Q1", Category.SCIENCE, "C1", "문제", "O", invalid));
    }
    expectInvalid(() -> create("Q1", null, "C1", "문제", "O", "해설"));
    System.out.println("Question checks passed");
  }

  private static Question create(String id, Category category, String contentId,
      String prompt, String answer, String explanation) {
    return new Question(id, category, contentId, prompt, answer, explanation) {
      @Override
      public String getType() {
        return "TEST";
      }

      @Override
      public boolean checkAnswer(String selectedAnswer) {
        return getAnswer().equals(selectedAnswer);
      }
    };
  }

  private static void expectInvalid(Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 문제가 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }
}
