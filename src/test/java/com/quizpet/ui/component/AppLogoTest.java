package main.java.com.quizpet.ui.component;

import java.awt.Component;
import java.awt.Container;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.screen.StartPanel;

public final class AppLogoTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      assert hasLogo(new TopBar(), 52) : "Top bar must display the new logo";
      assert hasLogo(new StartPanel(new ScreenManager()), 200)
          : "Start screen must display the new logo";
    });
    System.out.println("AppLogoTest passed");
  }

  private static boolean hasLogo(Component component, int size) {
    if (component instanceof JLabel label && label.getIcon() instanceof ImageIcon icon
        && "QuizPet".equals(icon.getDescription())) {
      assert icon.getIconWidth() == size && icon.getIconHeight() == size;
      assert icon.getImageLoadStatus() == java.awt.MediaTracker.COMPLETE;
      return true;
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        if (hasLogo(child, size)) return true;
      }
    }
    return false;
  }
}
