package main.java.com.quizpet;

import javax.swing.SwingUtilities;
import main.java.com.quizpet.ui.MainFrame;

public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
  }
}
