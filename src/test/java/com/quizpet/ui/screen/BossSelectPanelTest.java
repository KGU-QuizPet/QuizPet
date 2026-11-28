package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetStats;

public final class BossSelectPanelTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossSelectPanel panel = new BossSelectPanel();
        AtomicInteger selected = new AtomicInteger(-1);
        panel.setOnSelect(selected::set);
        List<JButton> buttons = buttons(panel).stream()
            .filter(b -> !b.getText().equals("홈으로")).toList();
        assert buttons.size() == 4;
        for (int level : new int[] {1, 2, 3, 4, 5, 7, 8}) {
          panel.showPet(new Pet("P1", "U1", "말랑이", PetAppearance.DUCK,
              (level - 1) * 50, new PetStats()), Set.of());
          int[] required = {1, 3, 5, 8};
          for (int i = 0; i < buttons.size(); i++) {
            selected.set(-1);
            assert buttons.get(i).isEnabled() == (level >= required[i]);
            buttons.get(i).doClick();
            assert selected.get() == (level >= required[i] ? i : -1);
          }
          for (int[] size : new int[][] {{1280, 736}, {1024, 616}, {1920, 1016}}) {
            panel.setSize(size[0], size[1]);
            layout(panel);
            for (JButton button : buttons) {
              assert button.getWidth() >= 100 && button.getHeight() >= 40 : button.getBounds();
              assert button.getY() + button.getHeight() <= button.getParent().getHeight();
              java.awt.BorderLayout infoLayout = (java.awt.BorderLayout) button.getParent().getLayout();
              Component text = infoLayout.getLayoutComponent(java.awt.BorderLayout.NORTH);
              assert !button.getBounds().intersects(text.getBounds()) : "카드 설명과 도전 버튼이 겹칩니다.";
            }
            BufferedImage rendered = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = rendered.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            if (args.length > 0 && (level == 1 || level == 4 || level == 8)) {
              ImageIO.write(rendered, "png", Path.of(args[0], "boss-select-lv" + level + "-" + size[0] + ".png").toFile());
            }
          }
        }
        panel.showPet(new Pet("P1", "U1", "말랑이", PetAppearance.DUCK), Set.of(0));
        assert buttons.get(0).getText().equals("다시 도전");
        panel.showPet(new Pet("P2", "U2", "다른펫", PetAppearance.DUCK), Set.of());
        assert buttons.get(0).getText().equals("도전하기");
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("Boss selection level and layout checks passed");
  }

  private static List<JButton> buttons(Container root) {
    List<JButton> found = new ArrayList<>();
    for (Component child : root.getComponents()) {
      if (child instanceof JButton button) found.add(button);
      if (child instanceof Container container) found.addAll(buttons(container));
    }
    return found;
  }

  private static void layout(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }
}
