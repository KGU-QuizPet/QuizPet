package main.java.com.quizpet.service;

import java.util.Objects;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetStats;

// 학습으로 성장한 능력치를 복사. 경험치와 저장 데이터는 변경 X.
public final class BattleService {
  public static final Boss TRAINING_BOSS = new Boss("연습 보스", 200, 25, "images/bosses/boss1.png");

  public BattleSession createBattle(Pet pet) {
    return createBattle(pet, TRAINING_BOSS);
  }

  public BattleSession createBattle(Pet pet, Boss boss) {
    Objects.requireNonNull(pet, "pet은 필수입니다.");
    PetStats stats = pet.getStats();
    long attack = (long) stats.getStrength() + stats.getIntelligence()
        + stats.getFocus() + stats.getWisdom();
    long maxHp = 100L + (pet.getLevel() - 1L) * 20L;
    if (attack <= 0 || attack > Integer.MAX_VALUE || maxHp > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("전투 능력치가 허용 범위를 벗어났습니다.");
    }
    return new BattleSession(pet.getName(), pet.getAppearance(), pet.getLevel(),
        (int) attack, (int) maxHp, boss);
  }
}
