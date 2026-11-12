package main.java.com.quizpet.ui;

import javax.swing.JFrame;
import main.java.com.quizpet.ui.screen.HomePanel;
import main.java.com.quizpet.ui.screen.PetCreatePanel;

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
    PetCreatePanel petCreatePanel = new PetCreatePanel(screens, homePanel);
    screens.addScreen(ScreenManager.HOME, homePanel);
    screens.addScreen(ScreenManager.PET_CREATE, petCreatePanel);

    petCreatePanel.prepare("테스트");
    screens.show(ScreenManager.PET_CREATE);

    add(screens.getContainer());
  }
}
