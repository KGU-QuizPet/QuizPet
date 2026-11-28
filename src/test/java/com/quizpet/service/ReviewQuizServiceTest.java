package main.java.com.quizpet.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.QuestionRepository;

public final class ReviewQuizServiceTest {
  public static void main(String[] args) throws Exception {
    QuestionRepository repository = new QuestionRepository();
    QuizService service = new QuizService(repository);
    List<String> ids = new ArrayList<>();
    for (Category category : Category.values()) {
      ids.add(repository.findByCategory(category).get(0).getQuestionId());
    }
    ids.add(repository.findAll().get(1).getQuestionId());
    ids.add(repository.findAll().get(2).getQuestionId());
    List<String> expected = List.copyOf(ids);
    ids.add(ids.get(0));
    QuizSession review = service.startReviewSession("U1", ids);
    assert review.isReview();
    assert review.getTotalCount() == expected.size() && review.getTotalCount() > 5;
    assert review.getQuestions().stream().map(Question::getQuestionId).toList().equals(expected);
    ids.clear();
    assert review.getTotalCount() == expected.size();
    expect(IllegalStateException.class, review::getCategory);
    expect(IllegalArgumentException.class, () -> service.startReviewSession("U1", List.of()));
    expect(IllegalArgumentException.class, () -> service.startReviewSession("U1", null));
    for (String id : Arrays.asList(null, "", " ", "bad|id", "bad\nid", "MISSING")) {
      expect(IllegalArgumentException.class, () -> service.startReviewSession("U1", Arrays.asList(expected.get(0), id)));
    }
    for (String user : Arrays.asList(null, "", " ", "bad|user")) {
      expect(IllegalArgumentException.class, () -> service.startReviewSession(user, expected));
    }
    Question old = new QuestionRepository(java.nio.file.Path.of("data/content/question-legacy.txt")).findAll().get(0);
    expect(IllegalArgumentException.class, () -> service.startReviewSession("U1", List.of(old.getQuestionId())));
    QuizSession single = service.startReviewSession("U2", List.of(expected.get(0)));
    assert !single.getSessionId().equals(review.getSessionId());
    assert single.getCategory() == repository.findById(expected.get(0)).orElseThrow().getCategory();
    for (Question question : review.getQuestions()) {
      assert review.getCurrentQuestion() == question;
      assert review.submitAnswer(question.getAnswer());
      if (!review.isFinished()) review.nextQuestion();
    }
    assert review.getResult().getAnsweredCount() == expected.size();
    assert single.getAnsweredCount() == 0;
    QuizSession normal = service.startSession("U1", "SCIENCE");
    assert !normal.isReview() && normal.getTotalCount() == 5;
    Question unsupported = new Question("CUSTOM", Category.SCIENCE, "C", "문제", "O", "해설") {
      public String getType() { return "CUSTOM"; }
      public boolean checkAnswer(String answer) { return true; }
    };
    QuizService invalid = new QuizService(new QuestionRepository() {
      @Override public Optional<Question> findById(String id) { return Optional.of(unsupported); }
    });
    expect(IllegalStateException.class, () -> invalid.startReviewSession("U1", List.of("CUSTOM")));
    expect(IllegalStateException.class, () -> invalid.startReviewSession("U1", List.of("MISMATCH")));
    System.out.println("Review quiz selection, order, validation and compatibility checks passed");
  }

  private static void expect(Class<? extends RuntimeException> type, Runnable action) {
    try { action.run(); throw new AssertionError("Expected " + type.getSimpleName()); }
    catch (RuntimeException e) { if (!type.isInstance(e)) throw e; }
  }
}
