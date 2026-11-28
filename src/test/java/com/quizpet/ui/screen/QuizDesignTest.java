package main.java.com.quizpet.ui.screen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.QuestionRepository;
import main.java.com.quizpet.service.QuizService;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.component.*;

public final class QuizDesignTest {
  public static void main(String[] args) throws Exception {
    var repository = new QuestionRepository();
    var all = repository.findAll();
    SwingUtilities.invokeAndWait(() -> {
      try {
        for (String type : List.of("OX", "MULTIPLE_CHOICE", "SA")) {
          Question question = all.stream().filter(q -> q.getType().equals(type)).findFirst().orElseThrow();
          QuizSession session = new QuizSession("RENDER-" + type, "U", question.getCategory(), List.of(question));
          QuizPanel panel = new QuizPanel(new ScreenManager());
          panel.showSession(session);
          assert !LearningDesignTest.button(panel, "답안 제출").isEnabled();
          select(panel, question.getAnswer());
          assert LearningDesignTest.button(panel, "답안 제출").isEnabled();
          for (int[] size : new int[][] {{1280,736},{960,736},{640,600},{480,680}}) {
            LearningDesignTest.resize(panel, size[0], size[1]);
            assert LearningDesignTest.button(panel, "답안 제출").getWidth() >= 150;
            if (args.length > 0) LearningDesignTest.render(panel,
                Path.of(args[0], "UI-06-" + type + "-" + size[0] + ".png"));
          }
          AtomicInteger submits = new AtomicInteger();
          panel.setOnSubmit(answer -> { submits.incrementAndGet(); session.submitAnswer(answer); });
          LearningDesignTest.button(panel, "답안 제출").doClick();
          LearningDesignTest.button(panel, "답안 제출").doClick();
          assert submits.get() == 1 && session.isFinished();
          assert !LearningDesignTest.button(panel, "답안 제출").isEnabled();
          if (question instanceof MultipleChoiceQuestion choice) {
            MultipleChoicePanel answers = new MultipleChoicePanel(question.getPrompt(), choice.getOptions());
            answers.showResult("2", "1");
            assert LearningDesignTest.find(answers, OptionButton.class).stream().noneMatch(java.awt.Component::isEnabled);
            LearningDesignTest.resize(answers, 380, 284);
            assert LearningDesignTest.find(answers, javax.swing.JScrollPane.class).stream()
                .filter(scroll -> scroll.getViewport().getView() instanceof javax.swing.JPanel)
                .allMatch(scroll -> !scroll.getVerticalScrollBar().isVisible())
                : "Short choices must fit the existing FE3 preview";
            answers.setQuestion(question.getPrompt(), choice.getOptions());
            assert answers.getSelectedAnswer() == null;
          } else if (question instanceof ShortAnswerQuestion) {
            ShortAnswerPanel result = new ShortAnswerPanel(question.getPrompt());
            result.showResult(question.getAnswer());
            LearningDesignTest.resize(result, 380, 284);
            var prompt = LearningDesignTest.find(result, javax.swing.JTextArea.class).stream()
                .filter(area -> area.getText().equals(question.getPrompt())).findFirst().orElseThrow();
            var scroll = (javax.swing.JScrollPane) SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane.class, prompt);
            assert scroll.getViewport().getHeight() >= prompt.getFontMetrics(prompt.getFont()).getHeight()
                : "Feedback question text must have a readable viewport";
          }
        }

        List<Question> reviewQuestions = new ArrayList<>();
        for (Category category : List.of(Category.SCIENCE, Category.HISTORY, Category.LANGUAGE)) {
          all.stream().filter(q -> q.getCategory() == category).limit(3).forEach(reviewQuestions::add);
        }
        QuizSession review = new QuizService(repository).startReviewSession("U",
            reviewQuestions.stream().map(Question::getQuestionId).toList());
        assert review.getTotalCount() == 9;
        QuizPanel panel = new QuizPanel(new ScreenManager());
        panel.setOnSubmit(review::submitAnswer);
        for (int i=0;i<review.getTotalCount();i++) {
          panel.showSession(review);
          Question current = review.getCurrentQuestion();
          assert LearningDesignTest.find(panel, JLabel.class).stream()
              .anyMatch(label -> label.getText().contains("복습 · " + current.getCategory().getName()));
          assert LearningDesignTest.find(panel, QuizProgressBar.class).get(0)
              .getAccessibleContext().getAccessibleDescription().contains(" / 9");
          select(panel, current.getAnswer());
          LearningDesignTest.button(panel, "답안 제출").doClick();
          assert review.isCurrentQuestionAnswered();
          if (!review.isFinished()) review.nextQuestion();
        }
        assert review.isFinished() && review.getAnsweredCount() == 9;

        MultipleChoicePanel longChoices = new MultipleChoicePanel("긴 질문 ".repeat(500),
            List.of("긴 보기 ".repeat(500), "둘", "셋", "넷"));
        LearningDesignTest.resize(longChoices, 480, 600);
        var scrolls = LearningDesignTest.find(longChoices, javax.swing.JScrollPane.class);
        assert scrolls.stream().filter(s -> s.getVerticalScrollBar().getMaximum() > s.getViewport().getHeight()).count() >= 2;
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    System.out.println("QuizDesignTest passed");
  }

  static void select(QuizPanel panel, String answer) {
    var fields = LearningDesignTest.find(panel, JTextField.class);
    if (!fields.isEmpty()) fields.get(0).setText(answer);
    else LearningDesignTest.find(panel, OptionButton.class).stream()
        .filter(button -> button.getActionCommand().equals(answer)).findFirst().orElseThrow().doClick();
  }
}
