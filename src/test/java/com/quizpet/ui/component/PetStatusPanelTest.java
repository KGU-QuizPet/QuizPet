package main.java.com.quizpet.ui.component;

import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.model.PetStats;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.screen.HomePanel;

public class PetStatusPanelTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(PetStatusPanelTest::run);
    System.out.println("PetStatusPanelTest passed");
  }

  private static void run() {
    // 실제 Pet 값(누적 XP 180 → Lv.4, 30/50)이 그대로 표시된다.
    Pet pet = new Pet("P0001", "U0001", "말랑이", PetAppearance.DUCK, 180, new PetStats(12, 18, 14, 13));
    PetStatusPanel panel = new PetStatusPanel();
    panel.showPet(pet.getName(), pet.getSpriteKey(), pet.getLevel(), pet.getXpInLevel(), Pet.XP_PER_LEVEL,
        pet.getStats().getStrength(), pet.getStats().getIntelligence(),
        pet.getStats().getFocus(), pet.getStats().getWisdom());
    assert panel.getLevelText().equals("Lv.4") : panel.getLevelText();
    assert panel.getExpText().equals("EXP 30 / 50") : panel.getExpText();
    assert panel.getRemainText().equals("다음 레벨까지 20") : panel.getRemainText();
    assert panel.getExpBar().getValue() == 30 && panel.getExpBar().getMaximum() == 50;
    assert panel.getStatText("INT").equals("18");
    assert panel.getStatText("STR").equals("12");
    assert panel.getStatText("FOC").equals("14");
    assert panel.getStatText("WIS").equals("13");
    // 최고 능력치 18 → 막대 최대 30이므로 INT 막대는 60% 채워진다.
    assert Math.abs(panel.getStatBar("INT").getRatio() - 0.6) < 1e-9;
    assert panel.getStatBar("STR").getRatio() < panel.getStatBar("INT").getRatio();

    // 막대 최대값은 최고 능력치보다 항상 커서 가득 차지 않는다.
    assert PetStatusPanel.statScale(10) == 30;
    assert PetStatusPanel.statScale(30) == 40;
    assert PetStatusPanel.statScale(34) == 40;

    // 기본 표정은 IDLE이며, 표정을 바꿔도 외형 이미지가 유지된다.
    assert panel.getStage().getMood() == PetMood.IDLE;
    assert panel.getStage().hasImage() : "duck_idle.png를 찾지 못함";
    panel.setMood(PetMood.LEVEL_UP, "레벨 업!");
    assert panel.getStage().getMood() == PetMood.LEVEL_UP;
    assert panel.getStage().hasImage();

    // 모든 외형·표정 이미지를 클래스패스에서 읽을 수 있다.
    for (PetAppearance appearance : PetAppearance.values()) {
      for (PetMood mood : PetMood.values()) {
        assert UiStyle.petImage(appearance.getSpriteKey(), mood) != null : appearance + " " + mood;
      }
    }
    // 정의되지 않은 외형은 기본 외형으로 대체한다.
    assert UiStyle.appearanceOf("UNKNOWN") == PetAppearance.DUCK;

    // 범위를 벗어난 경험치는 막대 범위로 보정한다.
    panel.showPet("펫", "TURTLE", 1, 0, 50, 10, 10, 10, 10);
    assert panel.getExpBar().getRatio() == 0.0;
    assert panel.getRemainText().equals("다음 레벨까지 50");

    assert !panel.isSummaryVisible();
    panel.showSummary(68.4, 39);
    assert panel.isSummaryVisible();

    // 홈 배너는 받침에 맞는 조사와 남은 경험치를 보여 준다.
    HomePanel home = new HomePanel(new ScreenManager());
    home.showPet("말랑이", "DUCK", 4, 30, 18, 12, 14, 13);
    assert home.getHeadlineText().contains("말랑이가 Lv.5까지") : home.getHeadlineText();
    assert home.getHeadlineText().contains("20 EXP");
    home.showPet("고라니", "DUCK", 1, 0, 10, 10, 10, 10);
    assert home.getHeadlineText().contains("고라니가");
    home.showPet("곰돌", "DUCK", 1, 0, 10, 10, 10, 10);
    assert home.getHeadlineText().contains("곰돌이 Lv.2") : home.getHeadlineText();
    home.showPet("<b>", "DUCK", 1, 0, 10, 10, 10, 10);
    assert home.getHeadlineText().contains("&lt;b&gt;") : "이름의 HTML은 이스케이프해야 한다";
  }
}
