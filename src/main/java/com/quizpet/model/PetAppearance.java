package main.java.com.quizpet.model;

/** 캐릭터 생성(UI-02)에서 고를 수 있는 외형입니다. pet.txt에는 name()이 spriteKey로 저장됩니다. */
public enum PetAppearance {
  DUCK("고라파카", "duck"),
  TURTLE("고북이", "turtle"),
  LIZARD("파이리", "lizard");

  private static final String IMAGE_DIRECTORY = "images/pets/";

  private final String displayName;
  private final String imageName;

  PetAppearance(String displayName, String imageName) {
    this.displayName = displayName;
    this.imageName = imageName;
  }

  public String getSpriteKey() {
    return name();
  }

  public String getDisplayName() {
    return displayName;
  }

  /** 기본(IDLE) 표정 이미지의 리소스 경로입니다. */
  public String getImagePath() {
    return getImagePath(PetMood.IDLE);
  }

  /** 표정별 이미지의 리소스 경로입니다(예: images/pets/duck_happy.png). */
  public String getImagePath(PetMood mood) {
    if (mood == null) {
      throw new IllegalArgumentException("표정은 필수입니다.");
    }
    return IMAGE_DIRECTORY + imageName + "_" + mood.getFileSuffix() + ".png";
  }

  @Override
  public String toString() {
    return displayName;
  }
}
