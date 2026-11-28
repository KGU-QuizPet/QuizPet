package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;

public final class BossBattleLayoutTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossBattlePanel panel = new BossBattlePanel();
        panel.showBattle(new Pet("P1", "U1", "말랑이", PetAppearance.DUCK));
        for (int[] size : new int[][] {{1280, 736}, {1024, 616}, {1920, 1016}}) {
          panel.setSize(size[0], size[1]);
          layout(panel);
          JPanel arena = (JPanel) field(panel, "arena");
          JLabel boss = (JLabel) field(panel, "bossName");
          JLabel pet = (JLabel) field(panel, "petName");
          assert boss.getX() < arena.getWidth() / 2 : "보스 HP 카드는 왼쪽 위에 표시합니다.";
          assert pet.getX() > arena.getWidth() / 2 : "펫 HP 카드는 오른쪽 아래에 표시합니다.";
          assert pet.getY() > boss.getY() : "시안처럼 HP 카드를 대각선으로 배치합니다.";
          for (Component child : arena.getComponents()) {
            assert child.getX() >= 0 && child.getY() >= 0 : child.getBounds();
            assert child.getX() + child.getWidth() <= arena.getWidth() : child.getBounds();
            assert child.getY() + child.getHeight() <= arena.getHeight() : child.getBounds();
          }
          BufferedImage rendered = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
          java.awt.Graphics2D graphics = rendered.createGraphics();
          panel.printAll(graphics);
          graphics.dispose();
          if (args.length > 0) ImageIO.write(rendered, "png", Path.of(args[0], "battle-" + size[0] + ".png").toFile());
        }
        panel.stopBattle();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("Boss battle layout checks passed");
  }

  private static Object field(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void layout(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }
}
