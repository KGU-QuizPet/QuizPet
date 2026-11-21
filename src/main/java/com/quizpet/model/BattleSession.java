package main.java.com.quizpet.model;

import java.util.Objects;

// 전투 시작 시 복사한 능력치와 이번 전투세션의 HP.
public final class BattleSession {
  public enum State { READY, PLAYER_TURN, BOSS_TURN, WON, LOST, ABANDONED }

  private final String petName;
  private final PetAppearance appearance;
  private final int level;
  private final int attack;
  private final int maxHp;
  private final Boss boss;
  private int playerHp;
  private int bossHp;
  private int turnCount;
  private State state = State.READY;
  private String message = "전투 시작을 누르면 1초마다 번갈아 공격합니다.";

  public BattleSession(String petName, PetAppearance appearance, int level,
      int attack, int maxHp, Boss boss) {
    if (petName == null || petName.isBlank() || level < 1 || attack <= 0 || maxHp <= 0) {
      throw new IllegalArgumentException("캐릭터 이름과 양수인 레벨·공격력·HP가 필요합니다.");
    }
    this.petName = petName;
    this.appearance = Objects.requireNonNull(appearance, "appearance는 필수입니다.");
    this.level = level;
    this.attack = attack;
    this.maxHp = maxHp;
    this.boss = Objects.requireNonNull(boss, "boss는 필수입니다.");
    playerHp = maxHp;
    bossHp = boss.maxHp();
  }

  public void start() {
    if (state != State.READY) throw new IllegalStateException("이미 시작하거나 종료한 전투입니다.");
    state = State.PLAYER_TURN;
    message = "전투 시작! 잠시 후 내 캐릭터가 먼저 공격합니다.";
  }

  /** 한 번에 한쪽만 공격하며, HP가 0이 되면 반격 없이 즉시 종료합니다. */
  public void advanceTurn() {
    if (!isRunning()) throw new IllegalStateException("진행 중인 전투가 아닙니다.");
    turnCount++;
    if (state == State.PLAYER_TURN) {
      bossHp = Math.max(0, bossHp - attack);
      message = petName + " 공격! " + attack + " 피해";
      state = bossHp == 0 ? State.WON : State.BOSS_TURN;
      if (state == State.WON) message = "승리! " + boss.name() + "를 쓰러뜨렸습니다.";
    } else {
      playerHp = Math.max(0, playerHp - boss.attack());
      message = boss.name() + " 공격! " + boss.attack() + " 피해";
      state = playerHp == 0 ? State.LOST : State.PLAYER_TURN;
      if (state == State.LOST) message = "패배! 학습으로 성장한 뒤 다시 도전해 보세요.";
    }
  }

  public void abandon() {
    if (state == State.READY || isRunning()) {
      state = State.ABANDONED;
      message = "전투를 종료했습니다.";
    }
  }

  public boolean isRunning() { return state == State.PLAYER_TURN || state == State.BOSS_TURN; }
  public String getPetName() { return petName; }
  public PetAppearance getAppearance() { return appearance; }
  public int getLevel() { return level; }
  public int getAttack() { return attack; }
  public int getMaxHp() { return maxHp; }
  public Boss getBoss() { return boss; }
  public int getPlayerHp() { return playerHp; }
  public int getBossHp() { return bossHp; }
  public int getTurnCount() { return turnCount; }
  public State getState() { return state; }
  public String getMessage() { return message; }
}
