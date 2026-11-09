package main.java.com.quizpet.model;

public final class OXQuestion extends Question {
  public OXQuestion(String questionId, Category category, String contentId,
      String prompt, String answer, String explanation) {
    super(questionId, category, contentId, prompt, answer, explanation);
    validateAnswer(answer);
  }

  @Override
  public String getType() {
    return "OX";
  }

  @Override
  public boolean checkAnswer(String selectedAnswer) {
    validateAnswer(selectedAnswer);
    return getAnswer().equals(selectedAnswer);
  }

  private static void validateAnswer(String answer) {
    if (!"O".equals(answer) && !"X".equals(answer)) {
      throw new IllegalArgumentException("OX 답안은 O 또는 X여야 합니다.");
    }
  }
}
