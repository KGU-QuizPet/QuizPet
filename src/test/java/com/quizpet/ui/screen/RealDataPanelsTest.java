package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public final class RealDataPanelsTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      profilesAndPetCreation();
      categoryAndLearning();
      quizSubmissionAndReset();
    });
    System.out.println("Real-data panel checks passed");
  }

  private static void profilesAndPetCreation() {
    ScreenManager screens = new ScreenManager();
    StartPanel start = new StartPanel(screens);
    AtomicReference<String> selected = new AtomicReference<>();
    AtomicReference<String> created = new AtomicReference<>();
    start.setOnSelect(selected::set);
    start.setOnCreate(created::set);
    User user = new User("U1", "새사용자", LocalDateTime.now());
    start.showProfiles(List.of(new Profile(user, null)));
    JList<?> list = components(start, JList.class).get(0);
    assert list.getModel().getSize() == 1;
    list.setSelectedIndex(0);
    button(start, "시작").doClick();
    assert "U1".equals(selected.get());
    components(start, JTextField.class).get(0).setText("실제사용자");
    button(start, "새 사용자 만들기").doClick();
    assert "실제사용자".equals(created.get());
    start.showProfiles(List.of());
    assert list.getModel().getSize() == 0;
    assert list.getSelectedIndex() == -1;

    PetCreatePanel pet = new PetCreatePanel(screens);
    AtomicReference<String> appearance = new AtomicReference<>();
    pet.setOnCreate((name, key) -> { assert "캐릭터".equals(name); appearance.set(key); });
    components(pet, JTextField.class).get(0).setText("캐릭터");
    List<JToggleButton> looks = components(pet, JToggleButton.class);
    looks.get(2).doClick();
    button(pet, "생성하기").doClick();
    assert PetAppearance.LIZARD.name().equals(appearance.get());
    pet.prepare("다른사용자");
    assert components(pet, JTextField.class).get(0).getText().isEmpty();
    assert looks.get(0).isSelected();
    for (String key : UiStyle.PET_KEYS) { PetAppearance.valueOf(key); }
  }

  private static void categoryAndLearning() {
    ScreenManager screens = new ScreenManager();
    CategoryPanel categories = new CategoryPanel(screens);
    AtomicReference<String> selected = new AtomicReference<>();
    categories.setOnSelect(selected::set);
    categories.showCategories(List.of(Category.SPORTS));
    button(categories, "스포츠").doClick();
    assert Category.SPORTS.name().equals(selected.get());
    categories.showCategories(List.of(Category.HISTORY));
    assert categories.getSelectedCategoryId() == null;

    LearningPanel learning = new LearningPanel(screens);
    List<LearningContent> cards = List.of(
        new LearningContent("C1", Category.HISTORY, 1, 1, "실제카드1", List.of("역사"), "긴 설명 ".repeat(800)),
        new LearningContent("C2", Category.HISTORY, 2, 2, "실제카드2", List.of(), "본문"));
    AtomicInteger quiz = new AtomicInteger();
    AtomicInteger back = new AtomicInteger();
    learning.setOnQuiz(quiz::incrementAndGet);
    learning.setOnBack(back::incrementAndGet);
    learning.showContents(Category.HISTORY, cards, 0);
    assert !button(learning, "이전").isEnabled();
    button(learning, "다음").doClick();
    assert !button(learning, "다음").isEnabled();
    assert button(learning, "이전").isEnabled();
    button(learning, "퀴즈 시작").doClick();
    assert quiz.get() == 1;
    learning.setQuizEnabled(false);
    assert !button(learning, "퀴즈 시작").isEnabled();
    button(learning, "뒤로").doClick();
    assert back.get() == 1;
    learning.showContents(Category.HISTORY, cards, 0);
    assert !button(learning, "이전").isEnabled();
    learning.showContents(Category.HISTORY, List.of(), 0);
    assert !button(learning, "다음").isEnabled();
    assert !button(learning, "퀴즈 시작").isEnabled();
    learning.setSize(480, 680);
    layout(learning);
  }

  private static void quizSubmissionAndReset() {
    QuizPanel quiz = new QuizPanel(new ScreenManager());
    AtomicReference<String> answer = new AtomicReference<>();
    AtomicInteger ended = new AtomicInteger();
    quiz.setOnSubmit(answer::set);
    quiz.setOnEnd(ended::incrementAndGet);
    QuizSession first = new QuizSession("S1", "U1", Category.SCIENCE, List.of(
        new OXQuestion("Q1", Category.SCIENCE, "C1", "실제문제 ".repeat(500), "O", "해설")));
    quiz.showSession(first);
    assert !button(quiz, "답안 제출").isEnabled();
    button(quiz, "답안 제출").doClick();
    assert answer.get() == null;
    components(quiz, JToggleButton.class).get(0).doClick();
    button(quiz, "답안 제출").doClick();
    assert "O".equals(answer.get());
    assert first.getAnsweredCount() == 0 : "UI must not grade or advance the session";
    button(quiz, "종료").doClick();
    assert ended.get() == 1;
    QuizSession second = new QuizSession("S2", "U2", Category.SCIENCE, List.of(
        new MultipleChoiceQuestion("Q2", Category.SCIENCE, "C2", "객관식", "2", "해설",
            List.of("보기".repeat(300), "둘", "셋", "넷"))));
    quiz.showSession(second);
    assert quiz.getSelectedAnswer() == null;
    components(quiz, JToggleButton.class).get(1).doClick();
    button(quiz, "답안 제출").doClick();
    assert "2".equals(answer.get());
    second.submitAnswer("2");
    quiz.showSession(second);
    assert quiz.isFinished();
    assert !button(quiz, "답안 제출").isEnabled();
    quiz.setSize(480, 680);
    layout(quiz);
  }

  private static JButton button(Container root, String text) {
    return components(root, JButton.class).stream().filter(b -> text.equals(b.getText()))
        .findFirst().orElseThrow(() -> new AssertionError("Missing button: " + text));
  }

  private static <T> List<T> components(Container root, Class<T> type) {
    List<T> found = new ArrayList<>();
    for (Component child : root.getComponents()) {
      if (type.isInstance(child)) found.add(type.cast(child));
      if (child instanceof Container container) found.addAll(components(container, type));
    }
    return found;
  }

  private static void layout(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }
}
