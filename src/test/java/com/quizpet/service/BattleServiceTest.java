package main.java.com.quizpet.service;

import main.java.com.quizpet.model.*;

public final class BattleServiceTest {
  public static void main(String[] args) {
    BattleService service = new BattleService();
    Pet pet = new Pet("P1", "U1", "전투펫", PetAppearance.DUCK);
    BattleSession first = service.createBattle(pet);
    assert first.getAttack() == 40 && first.getMaxHp() == 100;
    expectState(first::advanceTurn);
    first.start();
    expectState(first::start);
    first.advanceTurn();
    assert first.getBossHp() == 160 && first.getPlayerHp() == 100;
    first.advanceTurn();
    assert first.getBossHp() == 160 && first.getPlayerHp() == 75;
    while (first.isRunning()) first.advanceTurn();
    assert first.getState() == BattleSession.State.LOST && first.getTurnCount() == 8;
    assert first.getPlayerHp() == 0 && first.getBossHp() == 40;
    expectState(first::advanceTurn);
    for (int i = 0; i < 5; i++) pet.applyReward(10, "INT", 1);
    BattleSession grown = service.createBattle(pet);
    assert grown.getAttack() == 45 && grown.getMaxHp() == 120;
    grown.start();
    while (grown.isRunning()) grown.advanceTurn();
    assert grown.getState() == BattleSession.State.WON && grown.getTurnCount() == 9;
    assert grown.getBossHp() == 0 && grown.getPlayerHp() == 20;
    expectState(grown::advanceTurn);
    grown.abandon();
    assert grown.getState() == BattleSession.State.WON;
    assert pet.getTotalXp() == 50 && pet.getStats().getIntelligence() == 15;
    BattleSession fresh = service.createBattle(pet);
    pet.applyReward(10, "STR", 1);
    assert fresh.getAttack() == 45 && fresh.getPlayerHp() == 120;
    fresh.start();
    fresh.abandon();
    expectState(fresh::advanceTurn);
    BattleSession oneHit = service.createBattle(pet, new Boss("약한보스", 1, 1000, null));
    oneHit.start();
    oneHit.advanceTurn();
    assert oneHit.getState() == BattleSession.State.WON && oneHit.getPlayerHp() == 120;
    System.out.println("BattleService checks passed");
  }

  private static void expectState(Runnable action) {
    try {
      action.run();
      throw new AssertionError("잘못된 전투 상태를 거부해야 합니다.");
    } catch (IllegalStateException expected) { }
  }
}
