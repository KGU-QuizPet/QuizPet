package main.java.com.quizpet.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuizSession {
  private final String sessionId;
  private final String userId;
  private final Category category;
  private final List<Question> questions;
  private final Map<String, String> answers = new LinkedHashMap<>();
  private int currentIndex;
  private int correctCount;
  private boolean finished;

  public QuizSession(String sessionId, String userId, Category category,
      List<? extends Question> questionPool) {
    if (sessionId == null || sessionId.isBlank() || userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("sessionId와 userId는 필수입니다.");
    }
    if (category == null || questionPool == null) {
      throw new IllegalArgumentException("category와 questionPool은 필수입니다.");
    }
    Map<String, Question> uniqueQuestions = new LinkedHashMap<>();
    for (Question question : questionPool) {
      if (question == null) {
        throw new IllegalArgumentException("문제 목록에 null이 포함될 수 없습니다.");
      }
      if (question.getCategory() == category) {
        uniqueQuestions.putIfAbsent(question.getQuestionId(), question);
      }
    }
    if (uniqueQuestions.isEmpty()) {
      throw new IllegalArgumentException("선택한 분야에 출제 가능한 문제가 없습니다.");
    }
    List<Question> candidates = new ArrayList<>(uniqueQuestions.values());
    Collections.shuffle(candidates);
    this.questions = List.copyOf(candidates.subList(0, Math.min(5, candidates.size())));
    this.sessionId = sessionId;
    this.userId = userId;
    this.category = category;
  }

  public boolean submitAnswer(String selectedAnswer) {
    requireActive();
    if (isCurrentQuestionAnswered()) {
      throw new IllegalStateException("이미 답안을 제출한 문제입니다.");
    }
    Question question = getCurrentQuestion();
    boolean correct = question.checkAnswer(selectedAnswer);
    answers.put(question.getQuestionId(), selectedAnswer);
    if (correct) {
      correctCount++;
    }
    if (answers.size() == questions.size()) {
      finished = true;
    }
    return correct;
  }

  public void nextQuestion() {
    requireActive();
    if (!isCurrentQuestionAnswered()) {
      throw new IllegalStateException("답안을 제출한 뒤 다음 문제로 이동할 수 있습니다.");
    }
    currentIndex++;
  }

  public void end() {
    finished = true;
  }

  private void requireActive() {
    if (finished) {
      throw new IllegalStateException("종료된 퀴즈 세션입니다.");
    }
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getUserId() {
    return userId;
  }

  public Category getCategory() {
    return category;
  }

  public String getCategoryId() {
    return category.getCategoryId();
  }

  public List<Question> getQuestions() {
    return questions;
  }

  public Question getCurrentQuestion() {
    return questions.get(currentIndex);
  }

  public int getCurrentIndex() {
    return currentIndex;
  }

  public int getTotalCount() {
    return questions.size();
  }

  public int getAnsweredCount() {
    return answers.size();
  }

  public int getCorrectCount() {
    return correctCount;
  }

  public Map<String, String> getAnswers() {
    return Map.copyOf(answers);
  }

  public boolean isCurrentQuestionAnswered() {
    return answers.containsKey(getCurrentQuestion().getQuestionId());
  }

  public boolean isFinished() {
    return finished;
  }
}
