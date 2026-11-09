package main.java.com.quizpet.model;

public final class QuizResult {
  private final int totalCount;
  private final int answeredCount;
  private final int correctCount;

  QuizResult(int totalCount, int answeredCount, int correctCount) {
    this.totalCount = totalCount;
    this.answeredCount = answeredCount;
    this.correctCount = correctCount;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getAnsweredCount() {
    return answeredCount;
  }

  public int getCorrectCount() {
    return correctCount;
  }

  public int getIncorrectCount() {
    return answeredCount - correctCount;
  }

  public double getAccuracy() {
    return answeredCount == 0 ? 0.0 : Math.round(correctCount * 1000.0 / answeredCount) / 10.0;
  }
}
