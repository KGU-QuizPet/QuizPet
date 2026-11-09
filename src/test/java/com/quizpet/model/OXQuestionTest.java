package main.java.com.quizpet.model;

public class OXQuestionTest {
  public static void main(String[] args) {
    Question correctO = create("O");
    Question correctX = create("X");
    assert correctO.getType().equals("OX");
    assert correctO.getAnswer().equals("O");
    assert correctO.checkAnswer("O");
    assert !correctO.checkAnswer("X");
    assert correctX.checkAnswer("X");
    assert !correctX.checkAnswer("O");
    for (String invalid : new String[] {null, "", " ", "o", "x", "0", " O ", "true", "○", "×"}) {
      expectInvalid(() -> create(invalid));
      expectInvalid(() -> correctO.checkAnswer(invalid));
      expectInvalid(() -> correctX.checkAnswer(invalid));
    }
    assert correctO.checkAnswer("O");
    System.out.println("OXQuestion checks passed");
  }

  private static OXQuestion create(String answer) {
    return new OXQuestion("Q1", Category.SCIENCE, "C1", "문제", answer, "해설");
  }

  private static void expectInvalid(Runnable action) {
    try {
      action.run();
      throw new AssertionError("O/X 이외의 값이 허용되었습니다.");
    } catch (IllegalArgumentException expected) {
    }
  }
}
