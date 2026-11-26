package main.java.com.quizpet.model;

/**
 * 캐릭터 표정입니다. 외형 3종 × 표정 5종 이미지(images/pets/{외형}_{표정}.png)와 1:1로 대응합니다.
 * IDLE은 홈·선택, FOCUS는 학습·풀이 중, HAPPY는 정답, SAD는 오답, LEVEL_UP은 레벨 업 화면에 사용합니다.
 */
public enum PetMood {
  IDLE("평온", "idle"),
  FOCUS("집중", "focus"),
  HAPPY("기쁨", "happy"),
  SAD("시무룩", "sad"),
  LEVEL_UP("레벨 업", "levelup");

  private final String displayName;
  private final String fileSuffix;

  PetMood(String displayName, String fileSuffix) {
    this.displayName = displayName;
    this.fileSuffix = fileSuffix;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** 이미지 파일명의 표정 부분입니다(예: duck_levelup.png의 levelup). */
  public String getFileSuffix() {
    return fileSuffix;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
