package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
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

      QuizSession small = service.startSession("U1", "HISTORY");
      assert small.getTotalCount() == 3;
      for (int i = 0; i < 3; i++) {
        assert service.submitAnswer(small, small.getCurrentQuestion().getAnswer());
        if (i < 2) {
          service.nextQuestion(small);
        }
      }
      assert small.isFinished();
      assert small.getCorrectCount() == 3;
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
    QuizSession defaults = new QuizService().startSession("U1", "SCIENCE");
    assert defaults.getTotalCount() >= 1;
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
