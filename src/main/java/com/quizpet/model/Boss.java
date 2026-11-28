package main.java.com.quizpet.model;

// 보스전에서 사용할 보스 정보.
public record Boss(String name, int maxHp, int attack, String imagePath, String backgroundPath, int requiredLevel) {
  public Boss(String name, int maxHp, int attack, String imagePath, String backgroundPath) {
    this(name, maxHp, attack, imagePath, backgroundPath, 1);
  }

  public Boss {
    if (name == null || name.isBlank() || maxHp <= 0 || attack <= 0 || requiredLevel < 1) {
      throw new IllegalArgumentException("보스 정보가 올바르지 않습니다.");
    }
  }
}
