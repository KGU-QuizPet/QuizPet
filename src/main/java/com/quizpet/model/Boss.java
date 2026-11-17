package main.java.com.quizpet.model;

// 전투마다 유지되는 보스의 기본 정보.
public record Boss(String name, int maxHp, int attack, String imagePath) {
  public Boss {
    if (name == null || name.isBlank() || maxHp <= 0 || attack <= 0) {
      throw new IllegalArgumentException("보스 이름과 양수인 HP·공격력이 필요합니다.");
    }
  }
}
