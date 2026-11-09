package main.java.com.quizpet.model;

public abstract class Question {
  private final String questionId;
  private final Category category;
  private final String contentId;
  private final String prompt;
  private final String answer;
  private final String explanation;

  protected Question(String questionId, Category category, String contentId,
      String prompt, String answer, String explanation) {
    requireText(questionId, "questionId");
    requireText(contentId, "contentId");
    requireText(prompt, "prompt");
    requireText(answer, "answer");
    requireText(explanation, "explanation");
    if (category == null) {
      throw new IllegalArgumentException("category는 필수입니다.");
    }
    this.questionId = questionId;
    this.category = category;
    this.contentId = contentId;
    this.prompt = prompt;
    this.answer = answer;
    this.explanation = explanation;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
    }
  }

  public String getQuestionId() {
    return questionId;
  }

  public Category getCategory() {
    return category;
  }

  public String getCategoryId() {
    return category.getCategoryId();
  }

  public String getContentId() {
    return contentId;
  }

  public String getPrompt() {
    return prompt;
  }

  public String getAnswer() {
    return answer;
  }

  public String getExplanation() {
    return explanation;
  }

  public abstract String getType();

  /** 유효하지 않은 답안은 하위 클래스에서 IllegalArgumentException으로 거부합니다. */
  public abstract boolean checkAnswer(String selectedAnswer);
}
