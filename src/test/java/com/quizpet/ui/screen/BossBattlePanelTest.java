package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import main.java.com.quizpet.model.PetStats;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.service.BattleService;

// 보스전 화면의 초기화·배속·재도전·자동 타이머 설정을 검증.
public final class BossBattlePanelTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        BossBattlePanel panel = new BossBattlePanel();
        Method stars = BossBattlePanel.class.getDeclaredMethod("difficultyStars", int.class);
        stars.setAccessible(true);
        for (int i = 0; i < BattleService.BOSSES.size(); i++) {
          assert stars.invoke(null, i).equals("★".repeat(i + 1) + "☆".repeat(3 - i))
              : "네 보스의 난이도는 별 1~4개로 구분해야 합니다.";
        }
        Pet pet = new Pet("P1", "U1", "화면펫", PetAppearance.TURTLE);
        panel.showBattle(pet);

        assert field(panel, "battle") == null : "입장 시에는 보스 선택 화면을 표시합니다.";
        BossSelectPanel selection = (BossSelectPanel) field(panel, "selection");
        assert selection.isVisible();
        Method select = BossBattlePanel.class.getDeclaredMethod("selectBoss", int.class);
        select.setAccessible(true);
        select.invoke(panel, 3);
        assert field(panel, "battle") == null : "레벨이 부족한 보스로 전투를 시작할 수 없습니다.";
        select.invoke(panel, 0);
        assert !selection.isVisible();
        JLabel petImage = (JLabel) field(panel, "petImage");
        assert petImage.getIcon() != null : "선택한 펫 외형 이미지가 로드되어야 합니다.";
        JLabel bossBadge = (JLabel) field(panel, "bossBadge");
        assert bossBadge.getText().contains(BattleService.BOSSES.get(0).name()) : "보스 배지에 보스 이름이 표시되어야 합니다.";

        JLabel petName = (JLabel) field(panel, "petName");
        assert petName.getText().contains("Lv.1") : "펫 레벨이 표시되어야 합니다.";
        assert petName.getText().contains("ATK 40") : "펫 공격력이 표시되어야 합니다.";

        javax.swing.JPanel overlay = (javax.swing.JPanel) field(panel, "resultOverlay");
        assert !overlay.isVisible() : "대기 화면에는 결과 카드가 표시되지 않아야 합니다.";
        Timer timer = (Timer) field(panel, "timer");
        Timer effectTimer = (Timer) field(panel, "effectTimer");
        assert timer.getDelay() == 1000;
        assert timer.getInitialDelay() == 1000;
        assert timer.isRunning();

        JButton start = (JButton) field(panel, "start");
        JButton speed = button(panel, "배속 1x");
        JButton exit = (JButton) field(panel, "exit");
        assert start != null && !start.isEnabled();
        assert speed != null && speed.isEnabled();
        assert exit != null && exit.isEnabled();

        // 배속 토글 검증 (1x <-> 2x).
        speed.doClick();
        assert "배속 2x".equals(speed.getText()) : "배속 버튼 문구가 2x로 바뀌어야 합니다.";
        assert timer.getDelay() == 500 : "2배속 시 타이머 간격은 500ms여야 합니다.";
        speed.doClick();
        assert "배속 1x".equals(speed.getText());
        assert timer.getDelay() == 1000;

        assert timer.isRunning();
        assert !start.isEnabled();
        BattleSession battle = (BattleSession) field(panel, "battle");
        assert battle.getState() == BattleSession.State.PLAYER_TURN;
        assert battle.getTurn() == 0 : "첫 공격은 1초 뒤에 실행되어야 합니다.";
        assert battle.getBoss().equals(BattleService.BOSSES.get(0));

        // 전투 중에도 배속 전환 가능.
        speed.doClick();
        assert timer.getDelay() == 500;

        // 전투 턴별 펫 표정 전환(공격 FOCUS, 피격 SAD, 승리 HAPPY) 및 재도전 시 선택 보스 유지·화면 갱신 검증.
        Method nextTurn = BossBattlePanel.class.getDeclaredMethod("nextTurn");
        nextTurn.setAccessible(true);
        nextTurn.invoke(panel);
        assert field(panel, "currentPetMood") == PetMood.FOCUS : "펫 공격 턴에는 FOCUS 표정이어야 합니다.";
        nextTurn.invoke(panel);
        assert field(panel, "currentPetMood") == PetMood.SAD : "보스 공격 피격 시에는 SAD 표정이어야 합니다.";
        while (battle.isRunning())
          nextTurn.invoke(panel);
        assert !timer.isRunning();
        assert field(panel, "currentPetMood") == PetMood.HAPPY : "전투 승리 시 HAPPY 표정이어야 합니다.";
        assert "재도전".equals(start.getText());
        assert !start.isEnabled() : "오버레이 뒤의 버튼은 비활성화해야 합니다.";
        assert overlay.isVisible() : "승리 시 결과 오버레이를 표시해야 합니다.";
        assert ((JLabel) field(panel, "resultTitle")).getText().equals("승리!");
        assert ((JLabel) field(panel, "resultDamage")).getText().equals("180") : "실제 보스 HP만큼만 준 피해로 집계합니다.";
        JLabel turn = (JLabel) field(panel, "turn");
        assert !turn.getText().contains("WON") && !turn.getText().contains("LOST") : "턴 상태는 한글로 표시되어야 합니다.";

        ((JButton) field(panel, "resultRetry")).doClick();
        assert !overlay.isVisible() : "재도전 시 결과 카드를 닫아야 합니다.";
        assert field(panel, "currentPetMood") == PetMood.IDLE : "재도전 시작 시 펫 표정은 IDLE로 초기화되어야 합니다.";
        assert (int) field(panel, "selectedBossIndex") == 0 : "재도전 시 선택한 보스를 유지해야 합니다.";
        BattleSession retryBattle = (BattleSession) field(panel, "battle");
        assert retryBattle.getBoss().equals(BattleService.BOSSES.get(0));
        assert bossBadge.getText().contains(BattleService.BOSSES.get(0).name()) : "재도전 시 보스 배지가 갱신되어야 합니다.";
        JLabel bossName = (JLabel) field(panel, "bossName");
        Object bossHp = field(panel, "bossHp");
        Method getMaximum = bossHp.getClass().getDeclaredMethod("getMaximum");
        getMaximum.setAccessible(true);
        assert bossName.getText().contains(BattleService.BOSSES.get(0).name()) : "재도전 시 보스 이름이 갱신되어야 합니다.";
        assert (int) getMaximum.invoke(bossHp) == BattleService.BOSSES.get(0).maxHp() : "재도전 시 보스 최대 HP가 갱신되어야 합니다.";

        exit.doClick();
        assert !timer.isRunning();
        assert !effectTimer.isRunning();
        assert retryBattle.getState() == BattleSession.State.ABANDONED;
        assert selection.isVisible();
        assert button(selection, "다시 도전") != null : "클리어한 보스는 다시 도전으로 표시합니다.";
        panel.showBattle(new Pet("P1", "OTHER", "다른펫", PetAppearance.DUCK));
        assert button(selection, "다시 도전") == null : "다른 사용자의 클리어 표시가 섞이면 안 됩니다.";

        Pet vulnerable = new Pet("P2", "U1", "연습펫", PetAppearance.TURTLE, 350,
            new PetStats(1, 1, 1, 1));
        panel.showBattle(vulnerable);
        select.invoke(panel, 3);
        BattleSession lost = (BattleSession) field(panel, "battle");
        while (lost.isRunning()) nextTurn.invoke(panel);
        assert lost.getState() == BattleSession.State.LOST;
        assert overlay.isVisible() : "패배 시에도 결과 카드를 표시해야 합니다.";
        assert ((JLabel) field(panel, "resultTitle")).getText().equals("패배");
        assert ((JLabel) field(panel, "resultHp")).getText().equals("0");
        AtomicInteger resultExits = new AtomicInteger();
        panel.setOnExit(resultExits::incrementAndGet);
        ((JButton) field(panel, "resultExit")).doClick();
        assert selection.isVisible();
        assert resultExits.get() == 0 : "보스 목록 이동은 홈 이동과 구분합니다.";
        assert !effectTimer.isRunning() && !timer.isRunning();

        AtomicInteger exits = new AtomicInteger();
        panel.setOnExit(exits::incrementAndGet);
        panel.showBattle(pet);
        button(selection, "홈으로").doClick();
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
