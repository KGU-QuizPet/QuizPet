package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.repository.QuestionRepository;

public class QuizServiceTest {
  public static void main(String[] args) throws IOException {
    Path file = Files.createTempFile("quizpet-quiz-", ".txt");
    try {
      StringBuilder data = new StringBuilder();
      for (int i = 0; i < 7; i++) {
        data.append("S").append(i).append("|SCIENCE|C1|OX|O|\n문제\n해설\n");
      }
      data.append("H1|HISTORY|C2|MULTIPLE_CHOICE|2|하나,둘,셋,넷\n문제\n해설\n");
      data.append("H2|HISTORY|C2|OX|X|\n문제\n해설\n");
      data.append("H3|HISTORY|C2|OX|O|\n문제\n해설\n");
      Files.writeString(file, data, StandardCharsets.UTF_8);
      QuizService service = new QuizService(new QuestionRepository(file));
      QuizSession session = service.startSession("U1", "SCIENCE");
      assert session.getUserId().equals("U1");
      assert session.getCategory() == Category.SCIENCE;
      assert session.getTotalCount() == 5;
      HashSet<String> ids = new HashSet<>();
      for (Question question : session.getQuestions()) {
        assert question.getCategory() == Category.SCIENCE;
        assert ids.add(question.getQuestionId());
      }
      QuizSession other = service.startSession("U2", "SCIENCE");
      assert !session.getSessionId().isBlank();
      assert !session.getSessionId().equals(other.getSessionId());
      expect(IllegalStateException.class, () -> service.nextQuestion(session));
      expect(IllegalArgumentException.class, () -> service.submitAnswer(session, "invalid"));
      assert session.getAnsweredCount() == 0;
      for (int i = 0; i < 5; i++) {
        assert service.submitAnswer(session, i == 0 ? "X" : "O") == (i != 0);
        assert !session.getCurrentQuestion().getExplanation().isBlank();
        expect(IllegalStateException.class, () -> service.submitAnswer(session, "O"));
        if (i < 4) {
          service.nextQuestion(session);
        }
      }
      assert session.isFinished();
      assert session.getCorrectCount() == 4;
      assert session.getAnsweredCount() == 5;
      expect(IllegalStateException.class, () -> service.nextQuestion(session));
      assert other.getAnsweredCount() == 0;
      assert !other.isFinished();

      for (boolean correct : new boolean[] {true, false}) {
        QuizSession small = service.startSession("U1", "HISTORY");
        assert small.getTotalCount() == 3;
        for (int i = 0; i < 3; i++) {
          Question question = small.getCurrentQuestion();
          for (String invalid : new String[] {null, "", " ", "invalid", "0", "5"}) {
            expect(IllegalArgumentException.class, () -> service.submitAnswer(small, invalid));
          }
          assert small.getCurrentIndex() == i;
          assert small.getAnsweredCount() == i;
          assert small.getCorrectCount() == (correct ? i : 0);
          assert !small.getAnswers().containsKey(question.getQuestionId());
          assert !small.isCurrentQuestionAnswered();
          String wrong = question.getType().equals("OX")
              ? (question.getAnswer().equals("O") ? "X" : "O")
              : (question.getAnswer().equals("1") ? "2" : "1");
          String selected = correct ? question.getAnswer() : wrong;
          assert service.submitAnswer(small, selected) == correct;
          expect(IllegalStateException.class,
              () -> service.submitAnswer(small, correct ? wrong : question.getAnswer()));
          assert small.getAnswers().get(question.getQuestionId()).equals(selected);
          assert small.getAnsweredCount() == i + 1;
          assert small.getCorrectCount() == (correct ? i + 1 : 0);
          if (i < 2) {
            service.nextQuestion(small);
          }
        }
        assert small.isFinished();
        assert small.getCorrectCount() == (correct ? 3 : 0);
      }
      service.submitAnswer(other, "O");
      service.endSession(other);
      assert other.isFinished();
      assert other.getAnsweredCount() == 1;
      assert other.getCorrectCount() == 1;
      expect(IllegalStateException.class, () -> service.submitAnswer(other, "O"));
      expect(IllegalStateException.class, () -> service.nextQuestion(other));
      for (String invalid : new String[] {null, "", " ", "UNKNOWN", "science", " SCIENCE "}) {
        expect(IllegalArgumentException.class, () -> service.startSession("U1", invalid));
      }
      for (String invalid : new String[] {null, "", " "}) {
        expect(IllegalArgumentException.class, () -> service.startSession(invalid, "SCIENCE"));
      }
      expect(IllegalArgumentException.class, () -> service.startSession("U1", "SPORTS"));
      expect(NullPointerException.class, () -> new QuizService(null));
      expect(NullPointerException.class, () -> service.submitAnswer(null, "O"));
      expect(NullPointerException.class, () -> service.nextQuestion(null));
      expect(NullPointerException.class, () -> service.endSession(null));
      Files.writeString(file, "", StandardCharsets.UTF_8);
      QuizService empty = new QuizService(new QuestionRepository(file));
      expect(IllegalArgumentException.class, () -> empty.startSession("U1", "SCIENCE"));
    } finally {
      Files.deleteIfExists(file);
    }
    QuizService defaults = new QuizService();
    QuestionRepository repository = new QuestionRepository();
    for (Category category : Category.values()) {
      List<Question> pool = repository.findByCategory(category);
      QuizSession session = defaults.startSession("U1", category.getCategoryId());
      assert session.getTotalCount() == 5;
      HashSet<String> ids = new HashSet<>();
      for (Question question : session.getQuestions()) {
        assert question.getCategory() == category;
        assert ids.add(question.getQuestionId());
        assert pool.stream().anyMatch(candidate ->
            candidate.getQuestionId().equals(question.getQuestionId()));
      }
    }
    System.out.println("QuizService checks passed");
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
