package main.java.com.quizpet.service;

import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetStats;

// 보스전 전투 규칙과 능력치 계산을 검증.
public final class BattleServiceTest {
  public static void main(String[] args) {
    BattleService service = new BattleService();
    Pet pet = new Pet("P1", "U1", "전투펫", PetAppearance.DUCK);
    int[] requiredLevels = {1, 3, 5, 8};
    assert BattleService.BOSSES.size() == 4 : "UI-09A에는 보스 네 마리가 있어야 합니다.";
    for (int i = 0; i < requiredLevels.length; i++) {
      Boss boss = BattleService.BOSSES.get(i);
      assert boss.requiredLevel() == requiredLevels[i];
      Pet unlocked = new Pet("OPEN", "U1", "도전펫", PetAppearance.DUCK,
          (requiredLevels[i] - 1) * Pet.XP_PER_LEVEL, new PetStats());
      assert service.createBattle(unlocked, i).getBoss().equals(boss);
      if (i > 0) {
        Pet locked = new Pet("LOCK", "U1", "잠긴펫", PetAppearance.DUCK,
            (requiredLevels[i] - 2) * Pet.XP_PER_LEVEL, new PetStats());
        try {
          service.createBattle(locked, boss);
          throw new AssertionError("레벨 미달 보스는 서비스에서도 거부해야 합니다.");
        } catch (IllegalArgumentException expected) { }
      }
    }
    BattleSession battle = service.createBattle(pet, 0);

    assert battle.getAttack() == 40 : "초기 스탯 합은 40이어야 합니다.";
    assert battle.getMaxPlayerHp() == 100 : "Lv.1 최대 HP는 100이어야 합니다.";
    assert battle.getBoss().name().equals("졸음 슬라임");
    assert battle.getBossHp() == 180 && battle.getPlayerHp() == 100;

    expectIllegalState(battle::advanceTurn);
    battle.start();
    expectIllegalState(battle::start);

    battle.advanceTurn();
    assert battle.getBossHp() == 140 : "플레이어 공격은 공격력만큼 피해를 줘야 합니다.";
    assert battle.getPlayerHp() == 100;
    assert battle.getState() == BattleSession.State.BOSS_TURN;

    battle.advanceTurn();
    assert battle.getPlayerHp() == 80 : "보스 공격력이 HP에 반영되어야 합니다.";
    assert battle.getState() == BattleSession.State.PLAYER_TURN;

    while (battle.isRunning()) battle.advanceTurn();
    assert battle.getState() == BattleSession.State.WON;
    assert battle.getPlayerHp() == 20;
    assert battle.getBossHp() == 0;
    expectIllegalState(battle::advanceTurn);

    for (int i = 0; i < 5; i++) pet.applyReward(10, "INT", 1);
    BattleSession grown = service.createBattle(pet, 0);
    assert grown.getAttack() == 45;
    assert grown.getMaxPlayerHp() == 120;

    BattleSession oneHit = service.createBattle(pet, new Boss("약한 보스", 1, 999, null, "테스트 보스", 0xFFFFFF, 1));
    oneHit.start();
    oneHit.advanceTurn();
    assert oneHit.getState() == BattleSession.State.WON;
    assert oneHit.getPlayerHp() == oneHit.getMaxPlayerHp() : "승리한 턴에 반격하면 안 됩니다.";

    BattleSession abandoned = service.createBattle(pet, 0);
    abandoned.start();
    abandoned.abandon();
    assert abandoned.getState() == BattleSession.State.ABANDONED;
    expectIllegalState(abandoned::advanceTurn);

    try {
      service.createBattle(pet, -1);
      throw new AssertionError("잘못된 보스 인덱스를 거부해야 합니다.");
    } catch (IllegalArgumentException expected) { }

    Pet overflow = new Pet("P2", "U2", "범위초과", PetAppearance.DUCK, 0,
        new PetStats(Integer.MAX_VALUE, 10, 10, 10));
    try {
      service.createBattle(overflow, 0);
      throw new AssertionError("공격력 오버플로를 거부해야 합니다.");
    } catch (IllegalArgumentException expected) { }

    System.out.println("BattleService checks passed");
  }

  private static void expectIllegalState(Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 전투 상태를 거부해야 합니다.");
    } catch (IllegalStateException expected) { }
  }
}
