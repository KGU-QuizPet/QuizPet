package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetStats;

public final class BossBattleLayoutTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossBattlePanel panel = new BossBattlePanel();
        panel.showBattle(new Pet("P1", "U1", "말랑이", PetAppearance.DUCK));
        Method choose = BossBattlePanel.class.getDeclaredMethod("selectBoss", int.class);
        choose.setAccessible(true);
        choose.invoke(panel, 0);
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
        Pet[] pets = {
            new Pet("P1", "U1", "말랑이", PetAppearance.DUCK),
            new Pet("P1", "U1", "말랑이", PetAppearance.DUCK, 350, new PetStats(1, 1, 1, 1)),
            new Pet("P2", "U1", "튼튼이", PetAppearance.TURTLE, Integer.MAX_VALUE,
                new PetStats(Integer.MAX_VALUE, 0, 0, 0))};
        for (int scenario = 0; scenario < pets.length; scenario++) {
          int bossIndex = scenario == 1 ? 3 : 0;
          panel.showBattle(pets[scenario]);
          Method start = BossBattlePanel.class.getDeclaredMethod("selectBoss", int.class);
          Method next = BossBattlePanel.class.getDeclaredMethod("nextTurn");
          start.setAccessible(true);
          next.setAccessible(true);
          start.invoke(panel, bossIndex);
          BattleSession battle = (BattleSession) field(panel, "battle");
          while (battle.isRunning()) next.invoke(panel);
          JPanel overlay = (JPanel) field(panel, "resultOverlay");
          assert overlay.isVisible();
          for (int[] size : new int[][] {{1280, 736}, {1024, 616}, {1920, 1016}}) {
            panel.setSize(size[0], size[1]);
            layout(panel);
            assert overlay.getSize().equals(panel.getSize()) : "결과 배경은 화면 전체를 덮어야 합니다.";
            assertContained(overlay);
            BufferedImage rendered = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = rendered.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            if (args.length > 0) ImageIO.write(rendered, "png", Path.of(args[0],
                "battle-" + battle.getState() + "-" + scenario + "-" + size[0] + ".png").toFile());
          }
        }
        panel.stopBattle();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("Boss battle layout checks passed");
  }

  private static void assertContained(Container root) {
    for (Component child : root.getComponents()) {
      if (!child.isVisible()) continue;
      assert child.getX() >= 0 && child.getY() >= 0 : child.getBounds();
      assert child.getX() + child.getWidth() <= root.getWidth() : child.getBounds();
      assert child.getY() + child.getHeight() <= root.getHeight() : child.getBounds();
      if (child instanceof JLabel label && !label.getText().isEmpty() && label.getIcon() == null) {
        assert label.getFontMetrics(label.getFont()).stringWidth(label.getText())
            <= label.getWidth() - label.getInsets().left - label.getInsets().right
            : "결과 문구가 잘립니다: " + label.getText();
      }
      if (child instanceof Container container) assertContained(container);
    }
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
