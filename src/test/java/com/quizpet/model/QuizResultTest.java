package main.java.com.quizpet.model;

import java.util.List;

public class QuizResultTest {
  public static void main(String[] args) {
    QuizSession completed = create();
    expectUnfinished(completed);
    completed.submitAnswer("O");
    expectUnfinished(completed);
    completed.nextQuestion();
    completed.submitAnswer("X");
    completed.nextQuestion();
    completed.submitAnswer("O");
    QuizResult result = completed.getResult();
    assert result.getTotalCount() == 3;
    assert result.getAnsweredCount() == 3;
    assert result.getCorrectCount() == 2;
    assert result.getIncorrectCount() == 1;
    assert result.getAccuracy() == 66.7;

    QuizSession stopped = create();
    stopped.submitAnswer("O");
    stopped.nextQuestion();
    stopped.submitAnswer("X");
    stopped.end();
    QuizResult partial = stopped.getResult();
    assert partial.getTotalCount() == 3;
    assert partial.getAnsweredCount() == 2;
    assert partial.getCorrectCount() == 1;
    assert partial.getIncorrectCount() == 1;
    assert partial.getAccuracy() == 50.0;

    QuizSession empty = create();
    empty.end();
    QuizResult unanswered = empty.getResult();
    assert unanswered.getTotalCount() == 3;
    assert unanswered.getAnsweredCount() == 0;
    assert unanswered.getCorrectCount() == 0;
    assert unanswered.getIncorrectCount() == 0;
    assert unanswered.getAccuracy() == 0.0;

    for (String answer : List.of("O", "X")) {
      QuizSession single = new QuizSession("S2", "U1", Category.SCIENCE,
          List.of(question("Q1")));
      single.submitAnswer(answer);
      assert single.getResult().getAccuracy() == (answer.equals("O") ? 100.0 : 0.0);
    }
    assert result.getAccuracy() == 66.7;
    assert completed.getResult().getCorrectCount() == 2;
    System.out.println("QuizResult checks passed");
  }

  private static QuizSession create() {
    return new QuizSession("S1", "U1", Category.SCIENCE,
        List.of(question("Q1"), question("Q2"), question("Q3")));
  }

  private static OXQuestion question(String id) {
    return new OXQuestion(id, Category.SCIENCE, "C1", "문제", "O", "해설");
  }

  private static void expectUnfinished(QuizSession session) {
    try {
      session.getResult();
      throw new AssertionError("진행 중인 세션의 최종 결과가 반환되었습니다.");
    } catch (IllegalStateException expected) {
    }
  }
}
