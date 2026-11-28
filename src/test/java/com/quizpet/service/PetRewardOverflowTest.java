package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.UserRepository;

/** 이슈 #74: PetService 보상 경로에서 오버플로가 나면 pet.txt를 바꾸지 않는다. */
public class PetRewardOverflowTest {
  public static void main(String[] args) throws IOException {
    Path directory = Files.createTempDirectory("quizpet-overflow-");
    Path petFile = directory.resolve("pet.txt");
    PetService service = new PetService(new PetRepository(petFile),
        new UserRepository(directory.resolve("user.txt")));

    // XP가 최대값인 캐릭터: 보상을 거부하고 파일은 그대로 둔다.
    Files.writeString(petFile, "P0001|U0001|만렙|DUCK|" + Integer.MAX_VALUE + "|10|10|10|10\n",
        StandardCharsets.UTF_8);
    byte[] before = Files.readAllBytes(petFile);
    expectIllegal(() -> service.applyCorrectAnswerReward("P0001", "SCIENCE"));
    assert Arrays.equals(before, Files.readAllBytes(petFile)) : "실패한 보상이 저장되면 안 됩니다";
    Pet saved = service.getPet("P0001");
    assert saved.getTotalXp() == Integer.MAX_VALUE && saved.getStats().getIntelligence() == 10;

    // 능력치가 최대값인 캐릭터: 경험치도 저장되지 않는다.
    Files.writeString(petFile, "P0001|U0001|만렙|DUCK|100|" + Integer.MAX_VALUE + "|10|10|10\n",
        StandardCharsets.UTF_8);
    before = Files.readAllBytes(petFile);
    expectIllegal(() -> service.applyCorrectAnswerReward("P0001", "SPORTS"));
    assert Arrays.equals(before, Files.readAllBytes(petFile));

    // 같은 캐릭터라도 다른 분야 보상은 정상 저장된다.
    service.applyCorrectAnswerReward("P0001", "HISTORY");
    saved = service.getPet("P0001");
    assert saved.getTotalXp() == 110 && saved.getStats().getWisdom() == 11;
    assert saved.getStats().getStrength() == Integer.MAX_VALUE;
    System.out.println("PetRewardOverflowTest passed");
  }

  private interface IOAction {
    void run() throws IOException;
  }

  private static void expectIllegal(IOAction action) throws IOException {
    try {
      action.run();
      throw new AssertionError("IllegalArgumentException이 발생해야 합니다.");
    } catch (IllegalArgumentException expected) {
      // 정상
    }
  }
}
