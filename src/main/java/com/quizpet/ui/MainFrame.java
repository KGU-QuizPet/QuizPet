package main.java.com.quizpet.ui;

import javax.swing.JFrame;
import main.java.com.quizpet.ui.screen.HomePanel;

public class MainFrame extends JFrame {
  public static final int FRAME_WIDTH = 480;
  public static final int FRAME_HEIGHT = 720;

  public MainFrame() {
    super("QuizPet");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(FRAME_WIDTH, FRAME_HEIGHT);
    setLocationRelativeTo(null);
    setResizable(false);

    ScreenManager screens = new ScreenManager();

    HomePanel homePanel = new HomePanel(screens);
    screens.addScreen(ScreenManager.HOME, homePanel);

    homePanel.showPet("테스트", "PET_A", 1, 0, 10, 10, 10, 10);
    screens.show(ScreenManager.HOME);

    add(screens.getContainer());
  }
}
