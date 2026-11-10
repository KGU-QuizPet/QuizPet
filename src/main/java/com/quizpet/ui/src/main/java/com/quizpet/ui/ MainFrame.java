package main.java.com.quizpet.ui;

import javax.swing.JFrame;

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

    add(screens.getContainer());
  }
}
