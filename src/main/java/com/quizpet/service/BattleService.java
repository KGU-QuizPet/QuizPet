package main.java.com.quizpet.service;

import java.util.List;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;

// 캐릭터 능력치로 자동 보스전을 준비.
public final class BattleService {
  public static final List<Boss> BOSSES = List.of(
      new Boss("졸음 슬라임", 180, 20, "images/bosses/slime_idle.png", "공부만 하면 졸음이<br>쏟아지게 만든다", 0xB8EBDC, 1),
      new Boss("까먹귀신", 200, 22, "images/bosses/ghost_idle.png", "외운 내용을<br>슬쩍 지워 버린다", 0xD3C6F7, 3),
      new Boss("헷갈골렘", 220, 25, "images/bosses/golem_idle.png", "비슷한 보기로<br>머리를 헷갈리게 한다", 0xCDD2DA, 5),
      new Boss("망각의 용", 260, 30, "images/bosses/dragon_idle.png", "모든 기억을 불태우는<br>최종 보스", 0xD7C4F8, 8));

  public BattleSession createBattle(Pet pet, int bossIndex) {
    if (bossIndex < 0 || bossIndex >= BOSSES.size()) throw new IllegalArgumentException("보스를 선택해 주세요.");
    return createBattle(pet, BOSSES.get(bossIndex));
  }

  public int calculateAttack(Pet pet) {
    long attack = (long) pet.getStats().getStrength() + pet.getStats().getIntelligence()
        + pet.getStats().getFocus() + pet.getStats().getWisdom();
    if (attack <= 0 || attack > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("전투 능력치가 올바르지 않습니다.");
    }
    return (int) attack;
  }

  public int calculateMaxHp(Pet pet) {
    long hp = 100L + (pet.getLevel() - 1L) * 20L;
    if (hp <= 0 || hp > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("전투 능력치가 올바르지 않습니다.");
    }
    return (int) hp;
  }

  // 테스트·확장 보스용 생성 경로.
  public BattleSession createBattle(Pet pet, Boss boss) {
    if (boss == null) throw new IllegalArgumentException("보스를 선택해 주세요.");
    if (pet.getLevel() < boss.requiredLevel()) {
      throw new IllegalArgumentException("Lv." + boss.requiredLevel() + " 이상부터 도전할 수 있습니다.");
    }
    return new BattleSession(pet, boss, calculateAttack(pet), calculateMaxHp(pet));
  }
}
