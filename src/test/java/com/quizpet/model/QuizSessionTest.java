package main.java.com.quizpet.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class QuizSessionTest {
  public static void main(String[] args) {
    List<Question> pool = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      pool.add(question("Q" + i, Category.SCIENCE));
    }
    pool.add(question("Q0", Category.SCIENCE));
    pool.add(question("SPORTS", Category.SPORTS));
    QuizSession session = create(pool);
    assert session.getSessionId().equals("S1");
    assert session.getUserId().equals("U1");
    assert session.getCategory() == Category.SCIENCE;
    assert session.getQuestions().size() == 5;
    HashSet<String> ids = new HashSet<>();
    for (Question question : session.getQuestions()) {
      assert question.getCategory() == Category.SCIENCE;
      assert ids.add(question.getQuestionId());
    }
    pool.clear();
    assert session.getTotalCount() == 5;
    expect(UnsupportedOperationException.class, () -> session.getQuestions().clear());
    expect(IllegalStateException.class, session::nextQuestion);
    expect(IllegalArgumentException.class, () -> session.submitAnswer("invalid"));
    assert session.getAnsweredCount() == 0;
    assert !session.isCurrentQuestionAnswered();
    for (int i = 0; i < 5; i++) {
      assert session.getCurrentIndex() == i;
      assert session.submitAnswer(i % 2 == 0 ? "O" : "X") == (i % 2 == 0);
      assert session.isCurrentQuestionAnswered();
      expect(IllegalStateException.class, () -> session.submitAnswer("O"));
      if (i < 4) {
        assert !session.isFinished();
        session.nextQuestion();
      }
    }
    assert session.isFinished();
    assert session.getCorrectCount() == 3;
    assert session.getAnsweredCount() == 5;
    expect(IllegalStateException.class, session::nextQuestion);
    expect(UnsupportedOperationException.class, () -> session.getAnswers().clear());

    QuizSession small = create(List.of(question("A", Category.SCIENCE),
        question("B", Category.SCIENCE), question("C", Category.SCIENCE)));
    assert small.getTotalCount() == 3;
    small.submitAnswer("X");
    small.end();
    assert small.isFinished();
    assert small.getAnsweredCount() == 1;
    assert small.getCorrectCount() == 0;
    expect(IllegalStateException.class, () -> small.submitAnswer("O"));
    expect(IllegalStateException.class, small::nextQuestion);

    QuizSession single = create(List.of(question("A", Category.SCIENCE), question("A", Category.SCIENCE)));
    assert single.getTotalCount() == 1;
    single.submitAnswer("O");
    assert single.isFinished();
    Question first = question("A", Category.SCIENCE);
    QuizSession duplicates = create(List.of(
        question("A", Category.SPORTS), first, first,
        question("A", Category.SCIENCE),
        new MultipleChoiceQuestion("A", Category.SCIENCE, "C2", "별개의 문제", "2", "해설",
            List.of("하나", "둘", "셋", "넷")),
        question("B", Category.SCIENCE), question("B", Category.SCIENCE),
        question("C", Category.SCIENCE), question("C", Category.SCIENCE)));
    assert duplicates.getTotalCount() == 3;
    assert duplicates.getQuestions().stream().map(Question::getQuestionId)
        .collect(java.util.stream.Collectors.toSet()).equals(java.util.Set.of("A", "B", "C"));
    assert duplicates.getQuestions().stream()
        .filter(question -> question.getQuestionId().equals("A")).findFirst().orElseThrow() == first;
    HashSet<String> presented = new HashSet<>();
    for (int i = 0; i < duplicates.getTotalCount(); i++) {
      assert presented.add(duplicates.getCurrentQuestion().getQuestionId());
      assert duplicates.submitAnswer("O");
      if (i < duplicates.getTotalCount() - 1) {
        duplicates.nextQuestion();
      }
    }
    assert duplicates.isFinished();
    assert duplicates.getAnsweredCount() == 3;
    assert duplicates.getCorrectCount() == 3;
    assert duplicates.getAnswers().keySet().equals(presented);
    QuizSession stopped = create(List.of(question("A", Category.SCIENCE)));
    stopped.end();
    assert stopped.getAnswers().isEmpty();
    assert stopped.isFinished();
    expect(IllegalArgumentException.class, () -> create(List.of()));
    expect(IllegalArgumentException.class, () -> create(List.of(question("A", Category.SPORTS))));
    expect(IllegalArgumentException.class, () -> create(null));
    expect(IllegalArgumentException.class, () -> create(Arrays.asList((Question) null)));
    expect(IllegalArgumentException.class, () -> new QuizSession(" ", "U1", Category.SCIENCE, List.of()));
    expect(IllegalArgumentException.class, () -> new QuizSession("S1", null, Category.SCIENCE, List.of()));
    expect(IllegalArgumentException.class, () -> new QuizSession("S1", "U1", null, List.of()));
    System.out.println("QuizSession checks passed");
  }

  private static QuizSession create(List<Question> pool) {
    return new QuizSession("S1", "U1", Category.SCIENCE, pool);
  }

  private static Question question(String id, Category category) {
    return new OXQuestion(id, category, "C1", "문제", "O", "해설");
  }

  private static void expect(Class<? extends RuntimeException> type, Runnable action) {
    try {
      action.run();
      throw new AssertionError("예외가 발생해야 합니다: " + type.getSimpleName());
    } catch (RuntimeException exception) {
      if (!type.isInstance(exception)) {
        throw exception;
      }
    }
  }
}
