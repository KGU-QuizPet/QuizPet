package main.java.com.quizpet.model;

import java.nio.file.Files;
import java.nio.file.Path;

public class PetAppearanceTest {
  public static void main(String[] args) {
    assert PetAppearance.DUCK.getImagePath().equals("images/pets/duck_idle.png");
    assert PetAppearance.TURTLE.getImagePath(PetMood.LEVEL_UP).equals("images/pets/turtle_levelup.png");
    assert PetAppearance.LIZARD.getImagePath(PetMood.SAD).equals("images/pets/lizard_sad.png");

    // 외형 3종 × 표정 5종 이미지가 모두 리소스 폴더에 있어야 한다.
    Path resources = Path.of("src/main/resources");
    for (PetAppearance appearance : PetAppearance.values()) {
      for (PetMood mood : PetMood.values()) {
        Path image = resources.resolve(appearance.getImagePath(mood));
        assert Files.isRegularFile(image) : "펫 이미지 누락: " + image;
      }
    }

    try {
      PetAppearance.DUCK.getImagePath(null);
      assert false : "표정 없이 경로를 만들 수 없어야 한다";
    } catch (IllegalArgumentException expected) {
      // 정상
    }
    System.out.println("PetAppearanceTest passed");
  }
}
