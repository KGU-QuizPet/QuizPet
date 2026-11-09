package main.java.com.quizpet.model;

/** 캐릭터 생성(UI-02)에서 고를 수 있는 외형입니다. pet.txt에는 name()이 spriteKey로 저장됩니다. */
public enum PetAppearance {
  DUCK("고라파카", "images/pets/duck.png"),
  TURTLE("고북이", "images/pets/turtle.png"),
  LIZARD("파이리", "images/pets/lizard.png");

  private final String displayName;
  private final String imagePath;

  PetAppearance(String displayName, String imagePath) {
    this.displayName = displayName;
    this.imagePath = imagePath;
  }

  public String getSpriteKey() {
    return name();
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getImagePath() {
    return imagePath;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
