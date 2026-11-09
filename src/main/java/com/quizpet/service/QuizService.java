package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.repository.QuestionRepository;

public class QuizService {
  private final QuestionRepository questionRepository;

  public QuizService() throws IOException {
    this(new QuestionRepository());
  }

  public QuizService(QuestionRepository questionRepository) {
    this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository는 필수입니다.");
  }

  public QuizSession startSession(String userId, String categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId는 필수입니다.");
    }
    Category category = Category.valueOf(categoryId);
    return new QuizSession(UUID.randomUUID().toString(), userId, category,
        questionRepository.findByCategory(category));
  }

  public boolean submitAnswer(QuizSession session, String selectedAnswer) {
    return Objects.requireNonNull(session, "session은 필수입니다.").submitAnswer(selectedAnswer);
  }

  public void nextQuestion(QuizSession session) {
    Objects.requireNonNull(session, "session은 필수입니다.").nextQuestion();
  }

  public void endSession(QuizSession session) {
    Objects.requireNonNull(session, "session은 필수입니다.").end();
  }
}
