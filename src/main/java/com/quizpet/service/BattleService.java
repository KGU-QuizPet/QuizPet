package main.java.com.quizpet.service;

import java.util.List;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;

// 캐릭터 능력치로 자동 보스전을 준비.
public final class BattleService {
  public static final List<Boss> BOSSES = List.of(
      new Boss("슬라임", 180, 20, "images/bosses/slime_idle.png", "images/backgrounds/background1.jpg"),
      new Boss("골렘", 220, 25, "images/bosses/golem_idle.png", "images/backgrounds/background2.png"),
      new Boss("드래곤", 260, 30, "images/bosses/dragon_idle.png", "images/backgrounds/background3.png"));

  public BattleSession createBattle(Pet pet, int bossIndex) {
    if (bossIndex < 0 || bossIndex >= BOSSES.size()) throw new IllegalArgumentException("보스를 선택해 주세요.");
    return createBattle(pet, BOSSES.get(bossIndex));
  }

  // 테스트·확장 보스용 생성 경로.
  public BattleSession createBattle(Pet pet, Boss boss) {
    if (boss == null) throw new IllegalArgumentException("보스를 선택해 주세요.");
    long attack = (long) pet.getStats().getStrength() + pet.getStats().getIntelligence()
        + pet.getStats().getFocus() + pet.getStats().getWisdom();
    long hp = 100L + (pet.getLevel() - 1L) * 20L;
    if (attack <= 0 || attack > Integer.MAX_VALUE || hp > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("전투 능력치가 올바르지 않습니다.");
    }
    return new BattleSession(pet, boss, (int) attack, (int) hp);
  }
}
