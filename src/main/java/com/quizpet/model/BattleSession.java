package main.java.com.quizpet.model;

import java.util.Objects;

// 전투 중 HP와 턴만 관리하는 메모리 객체. 원본 Pet은 변경 X.
public final class BattleSession {
  public enum State { READY, PLAYER_TURN, BOSS_TURN, WON, LOST, ABANDONED }
  private final Pet pet;
  private final Boss boss;
  private final int attack;
  private final int maxPlayerHp;
  private int playerHp;
  private int bossHp;
  private int turn;
  private State state = State.READY;
  private String message = "전투 시작을 누르면 1초마다 자동으로 공격합니다.";

  public BattleSession(Pet pet, Boss boss, int attack, int maxPlayerHp) {
    this.pet = Objects.requireNonNull(pet, "pet은 필수입니다.");
    this.boss = Objects.requireNonNull(boss, "boss는 필수입니다.");
    if (attack <= 0 || maxPlayerHp <= 0) throw new IllegalArgumentException("전투 수치가 올바르지 않습니다.");
    this.attack = attack;
    this.maxPlayerHp = maxPlayerHp;
    playerHp = maxPlayerHp;
    bossHp = boss.maxHp();
  }

  public void start() {
    if (state != State.READY) throw new IllegalStateException("이미 시작한 전투입니다.");
    state = State.PLAYER_TURN;
    message = pet.getName() + "이(가) 먼저 공격합니다.";
  }

  public void advanceTurn() {
    if (!isRunning()) throw new IllegalStateException("진행 중인 전투가 아닙니다.");
    turn++;
    if (state == State.PLAYER_TURN) {
      bossHp = Math.max(0, bossHp - attack);
      message = pet.getName() + " 공격! " + attack + " 피해를 입혔습니다.";
      state = bossHp == 0 ? State.WON : State.BOSS_TURN;
      if (state == State.WON) message = boss.name() + "를 쓰러뜨렸습니다!";
    } else {
      playerHp = Math.max(0, playerHp - boss.attack());
      message = boss.name() + " 공격! " + boss.attack() + " 피해를 받았습니다.";
      state = playerHp == 0 ? State.LOST : State.PLAYER_TURN;
      if (state == State.LOST) message = "패배했습니다. 다시 도전해 보세요.";
    }
  }

  public void abandon() {
    if (state == State.READY || isRunning()) { state = State.ABANDONED; message = "전투를 종료했습니다."; }
  }
  public boolean isRunning() { return state == State.PLAYER_TURN || state == State.BOSS_TURN; }
  public Pet getPet() { return pet; }
  public Boss getBoss() { return boss; }
  public int getAttack() { return attack; }
  public int getMaxPlayerHp() { return maxPlayerHp; }
  public int getPlayerHp() { return playerHp; }
  public int getBossHp() { return bossHp; }
  public int getTurn() { return turn; }
  public State getState() { return state; }
  public String getMessage() { return message; }
}
