package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.UUID;
import main.java.com.quizpet.model.*;
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

  /**
   * 선택 ID의 최초 등장 순서를 유지하고 전체 문제로 복습을 구성합니다(혼합 분야 허용).
   * 잘못된 입력·없는 ID는 IllegalArgumentException, 잘못된 현재 출제 정보는 IllegalStateException입니다.
   * 오답 소유권과 로그인 사용자 일치는 호출 경계에서 ReviewPreparationService로 재검증합니다.
   */
  public QuizSession startReviewSession(String userId, List<String> questionIds) {
    requireId(userId);
    if (questionIds == null || questionIds.isEmpty()) throw new IllegalArgumentException("선택 문제가 없습니다.");
    LinkedHashSet<String> ids = new LinkedHashSet<>();
    for (String id : new ArrayList<>(questionIds)) { requireId(id); ids.add(id); }
    List<Question> selected = new ArrayList<>();
    for (String id : ids) {
      Question question = questionRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("현재 출제할 수 없는 문제입니다: " + id));
      if (!id.equals(question.getQuestionId()) || question.getCategory() == null
          || blank(question.getContentId()) || blank(question.getPrompt()) || blank(question.getAnswer())
          || blank(question.getExplanation())) {
        throw new IllegalStateException("출제 정보가 올바르지 않습니다: " + id);
      }
      if (!(question instanceof OXQuestion || question instanceof MultipleChoiceQuestion
          || question instanceof ShortAnswerQuestion)) {
        throw new IllegalStateException("지원하지 않는 문제 유형입니다: " + id);
      }
      if (!question.checkAnswer(question.getAnswer())) {
        throw new IllegalStateException("문제의 정답 정보가 올바르지 않습니다: " + id);
      }
      selected.add(question);
    }
    return QuizSession.review(UUID.randomUUID().toString(), userId, selected);
  }

  private static boolean blank(String value) { return value == null || value.isBlank(); }

  private static void requireId(String id) {
    if (blank(id) || id.contains("|") || id.matches("(?s).*\\R.*")) {
      throw new IllegalArgumentException("유효한 식별자가 필요합니다.");
    }
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
