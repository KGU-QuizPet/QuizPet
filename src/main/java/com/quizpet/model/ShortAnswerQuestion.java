package main.java.com.quizpet.model;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class ShortAnswerQuestion extends Question {
  private final List<String> alternativeAnswers;

  public ShortAnswerQuestion(String questionId, Category category, String contentId,
      String prompt, String answer, String explanation, List<String> alternativeAnswers) {
    super(questionId, category, contentId, prompt, answer, explanation);
    validateAnswer(answer);
    if (alternativeAnswers == null) throw new IllegalArgumentException("대체 정답 목록은 필수입니다.");
    for (String alternative : alternativeAnswers) validateAnswer(alternative);
    this.alternativeAnswers = List.copyOf(alternativeAnswers);
  }

  public List<String> getAlternativeAnswers() {
    return alternativeAnswers;
  }

  @Override
  public String getType() {
    return "SA";
  }

  @Override
  public boolean checkAnswer(String selectedAnswer) {
    validateAnswer(selectedAnswer);
    String normalized = normalize(selectedAnswer);
    return normalize(getAnswer()).equals(normalized)
        || alternativeAnswers.stream().anyMatch(answer -> normalize(answer).equals(normalized));
  }

  /** 화면의 제출 가능 여부도 이 규칙을 그대로 씁니다. */
  public static boolean isSubmittable(String answer) {
    return answer != null && !normalize(answer).isEmpty() && !answer.contains("|")
        && !answer.matches("(?s).*\\R.*");
  }

  private static void validateAnswer(String answer) {
    if (!isSubmittable(answer)) {
      throw new IllegalArgumentException("답안은 필수이며 구분자(|)나 줄바꿈을 포함할 수 없습니다.");
    }
  }

  // NFKC로 전각 문자·아래첨자(ＣＯ２, N₂)를 맞추고, 끝의 문장부호(낮아진다.)는 무시합니다.
  private static String normalize(String answer) {
    return Normalizer.normalize(answer, Normalizer.Form.NFKC)
        .replaceAll("(?U)\\s+", "").replaceAll("[.!?。]+$", "").toLowerCase(Locale.ROOT);
  }
}
