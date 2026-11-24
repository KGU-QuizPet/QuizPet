package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.service.BattleService;

// 보스전 화면의 초기화·버튼·자동 타이머 설정을 검증.
public final class BossBattlePanelTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossBattlePanel panel = new BossBattlePanel();
        Pet pet = new Pet("P1", "U1", "화면펫", PetAppearance.DUCK);
        panel.showBattle(pet);

        JComboBox<?> selector = find(panel, JComboBox.class);
        assert selector != null;
        assert selector.getItemCount() == 3 : "보스 3종이 표시되어야 합니다.";
        assert !selector.isEnabled() : "보스는 입장 시 랜덤 선택되어야 합니다.";

        Timer timer = (Timer) field(panel, "timer");
        assert timer.getDelay() == 1000;
        assert timer.getInitialDelay() == 1000;
        assert !timer.isRunning();

        JButton start = button(panel, "전투 시작");
        JButton exit = button(panel, "나가기");
        assert start != null && start.isEnabled();
        assert exit != null && exit.isEnabled();

        start.doClick();
        assert timer.isRunning();
        assert !start.isEnabled();
        BattleSession battle = (BattleSession) field(panel, "battle");
        assert battle.getState() == BattleSession.State.PLAYER_TURN;
        assert battle.getTurn() == 0 : "첫 공격은 1초 뒤에 실행되어야 합니다.";

        exit.doClick();
        assert !timer.isRunning();
        assert battle.getState() == BattleSession.State.ABANDONED;

        AtomicInteger exits = new AtomicInteger();
        panel.setOnExit(exits::incrementAndGet);
        panel.showBattle(pet);
        button(panel, "나가기").doClick();
        assert exits.get() == 1;
      } catch (Exception error) {
        throw new AssertionError(error);
      }
    });
    System.out.println("BossBattlePanel checks passed");
  }

  private static Object field(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static <T extends Component> T find(Container root, Class<T> type) {
    for (Component child : root.getComponents()) {
      if (type.isInstance(child)) return type.cast(child);
      if (child instanceof Container container) {
        T found = find(container, type);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static JButton button(Container root, String text) {
    for (Component child : root.getComponents()) {
      if (child instanceof JButton button && text.equals(button.getText())) return button;
      if (child instanceof Container container) {
        JButton found = button(container, text);
        if (found != null) return found;
      }
    }
    return null;
  }
}
