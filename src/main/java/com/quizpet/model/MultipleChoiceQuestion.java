package main.java.com.quizpet.model;

import java.util.List;

public final class MultipleChoiceQuestion extends Question {
  private final List<String> options;

  public MultipleChoiceQuestion(String questionId, Category category, String contentId,
      String prompt, String answer, String explanation, List<String> options) {
    super(questionId, category, contentId, prompt, answer, explanation);
    validateAnswer(answer);
    if (options == null || options.size() != 4) {
      throw new IllegalArgumentException("객관식 보기는 정확히 4개여야 합니다.");
    }
    for (String option : options) {
      if (option == null || option.isBlank()) {
        throw new IllegalArgumentException("객관식 보기는 비어 있을 수 없습니다.");
      }
    }
    this.options = List.copyOf(options);
  }

  public List<String> getOptions() {
    return options;
  }

  @Override
  public String getType() {
    return "MULTIPLE_CHOICE";
  }

  @Override
  public boolean checkAnswer(String selectedAnswer) {
    validateAnswer(selectedAnswer);
    return getAnswer().equals(selectedAnswer);
  }

  private static void validateAnswer(String answer) {
    if (!"1".equals(answer) && !"2".equals(answer)
        && !"3".equals(answer) && !"4".equals(answer)) {
      throw new IllegalArgumentException("객관식 답안은 1~4 중 하나여야 합니다.");
    }
  }
}
