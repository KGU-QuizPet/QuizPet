package main.java.com.quizpet.ui;

import java.awt.CardLayout;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ScreenManager {
  public static final String START = "START";
  public static final String PET_CREATE = "PET_CREATE";
  public static final String HOME = "HOME";
  public static final String CATEGORY = "CATEGORY";
  public static final String LEARNING = "LEARNING";
  public static final String QUIZ = "QUIZ";
  public static final String QUIZ_RESULT = "QUIZ_RESULT";
  public static final String HISTORY = "HISTORY";

  private final CardLayout cardLayout = new CardLayout();
  private final JPanel container = new JPanel(cardLayout);
  private final Set<String> registeredScreens = new HashSet<>();

  public JPanel getContainer() {
    return container;
  }

  public void addScreen(String name, JPanel screen) {
    container.add(screen, name);
    registeredScreens.add(name);
  }

  public void show(String name) {
    if (!registeredScreens.contains(name)) {
      JOptionPane.showMessageDialog(container, "아직 준비 중인 화면입니다: " + name);
      return;
    }
    cardLayout.show(container, name);
  }
}
