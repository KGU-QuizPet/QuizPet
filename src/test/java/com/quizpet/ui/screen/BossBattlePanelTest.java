package main.java.com.quizpet.ui.screen;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.service.BattleService;

public final class BossBattlePanelTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossBattlePanel panel = new BossBattlePanel();
        BattleSession session = session();
        panel.showBattle(session);
        Object fieldView = field(panel, "battlefield");
        assertImage(fieldView, "playerImage", 48, 48, true);
        assertImage(fieldView, "bossImage", 4000, 4000, true);
        assertImage(fieldView, "backgroundImage", 1280, 720, false);
        Timer timer = timer(panel);
        assert timer.getDelay() == 1000 && timer.getInitialDelay() == 1000;
        JButton start = button(panel, "전투 시작");
        start.doClick();
        assert !start.isEnabled() && timer.isRunning() && session.getTurnCount() == 0;
        start.doClick();
        for (int i = 0; i < 8; i++) tick(timer);
        assert session.getState() == BattleSession.State.LOST && !timer.isRunning();
        tick(timer);
        assert session.getTurnCount() == 8;
        start.doClick();
        tick(timer);
        BattleSession retry = battle(panel);
        assert retry.getBossHp() == 160 && retry.getPlayerHp() == 100;
        button(panel, "나가기").doClick();
        assert !timer.isRunning() && retry.getState() == BattleSession.State.ABANDONED;
        tick(timer);
        assert retry.getTurnCount() == 1;
        start.doClick();
        panel.removeNotify();
        assert !timer.isRunning();
        panel.showBattle(session());
        panel.setSize(480, 680);
        layout(panel);
        BufferedImage image = new BufferedImage(480, 680, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.printAll(g);
        g.dispose();
        if (args.length > 0) ImageIO.write(image, "png", Path.of(args[0]).toFile());
      } catch (Exception error) { throw new AssertionError(error); }
    });
    realTimer();
    System.out.println("BossBattlePanel checks passed (actual timer, retry, exit, rendering)");
  }

  private static void realTimer() throws Exception {
    CountDownLatch twoTurns = new CountDownLatch(1);
    AtomicReference<BossBattlePanel> panelRef = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossBattlePanel panel = new BossBattlePanel();
        panelRef.set(panel);
        BattleSession session = session();
        panel.showBattle(session);
        timer(panel).addActionListener(event -> SwingUtilities.invokeLater(() -> {
          if (session.getTurnCount() == 2) {
            try {
              assert session.getBossHp() == 160 && session.getPlayerHp() == 75;
            } catch (Throwable error) { failure.set(error); }
            finally { panel.stopBattle(); twoTurns.countDown(); }
          }
        }));
        button(panel, "전투 시작").doClick();
      } catch (Exception error) { failure.set(error); twoTurns.countDown(); }
    });
    try {
      if (!twoTurns.await(6, TimeUnit.SECONDS)) throw new AssertionError("자동 공격이 실행되지 않았습니다.");
      if (failure.get() != null) throw new AssertionError(failure.get());
    } finally {
      SwingUtilities.invokeAndWait(() -> { if (panelRef.get() != null) panelRef.get().stopBattle(); });
    }
  }

  private static BattleSession session() {
    return new BattleService().createBattle(new Pet("P1", "U1", "뭉치", PetAppearance.DUCK));
  }

  private static void assertImage(Object view, String name, int width, int height, boolean transparent)
      throws Exception {
    BufferedImage image = (BufferedImage) field(view, name);
    assert image != null : name + " 리소스를 읽지 못했습니다.";
    assert image.getWidth() == width && image.getHeight() == height;
    assert image.getColorModel().hasAlpha() == transparent;
  }

  private static Object field(Object panel, String name) throws Exception {
    Field field = panel.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(panel);
  }

  private static Timer timer(BossBattlePanel panel) throws Exception { return (Timer) field(panel, "timer"); }
  private static BattleSession battle(BossBattlePanel panel) throws Exception { return (BattleSession) field(panel, "battle"); }

  private static void tick(Timer timer) {
    for (var listener : timer.getActionListeners()) {
      listener.actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "tick"));
    }
  }

  private static JButton button(Container parent, String text) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JButton button && button.getText().equals(text)) return button;
      if (child instanceof Container container) {
        JButton found = button(container, text);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static void layout(Container parent) {
    parent.doLayout();
    for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
  }
}
