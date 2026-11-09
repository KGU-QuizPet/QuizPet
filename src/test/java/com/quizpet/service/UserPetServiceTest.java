package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.java.com.quizpet.model.GrowthResult;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.Profile;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.UserRepository;

public class UserPetServiceTest {
  public static void main(String[] args) throws IOException {
    Path directory = Files.createTempDirectory("quizpet-save-");
    Path userFile = directory.resolve("save/user.txt");
    Path petFile = directory.resolve("save/pet.txt");
    try {
      UserRepository userRepository = new UserRepository(userFile);
      PetRepository petRepository = new PetRepository(petFile);
      UserService userService = new UserService(userRepository);
      PetService petService = new PetService(petRepository, userRepository);
      ProfileService profileService = new ProfileService(userRepository, petRepository);

      // 첫 실행: 파일이 없어도 빈 목록이고, 조회만으로 파일을 만들지 않는다.
      assert userService.getUsers().isEmpty();
      assert profileService.getProfiles().isEmpty();
      assert !Files.exists(userFile);

      // TC-01 사용자 생성 (앞뒤 공백 제거, 폴더 자동 생성)
      User sanghun = userService.createUser("  상훈 ");
      assert sanghun.getUserId().equals("U0001");
      assert sanghun.getNickname().equals("상훈");
      assert Files.exists(userFile);

      // TC-02 닉네임 오류
      expectIllegal(() -> userService.createUser("   "));
      expectIllegal(() -> userService.createUser(null));
      expectIllegal(() -> userService.createUser("상훈"));
      expectIllegal(() -> userService.createUser("가나다라마바사아자차카타파"));
      expectIllegal(() -> userService.createUser("a_b"));
      assert userService.getUsers().size() == 1;

      // TC-04 캐릭터 초기값, 사용자당 1개
      Pet pet = petService.createPet(sanghun.getUserId(), " 고라고라니 ", "DUCK");
      assert pet.getPetId().equals("P0001");
      assert pet.getName().equals("고라고라니");
      assert pet.getLevel() == 1 && pet.getTotalXp() == 0;
      assert pet.getStats().getStrength() == 10 && pet.getStats().getWisdom() == 10;
      expectState(() -> petService.createPet(sanghun.getUserId(), "또만듦", "TURTLE"));
      expectIllegal(() -> petService.createPet("U9999", "누구", "TURTLE"));

      // TC-10 최초 정답 보상: 과학&상식 -> INT만 +1
      GrowthResult result = petService.applyCorrectAnswerReward(pet.getPetId(), "SCIENCE");
      assert result.getEarnedXp() == 10 && result.getStatType().equals("INT");
      Pet saved = petService.getPet(pet.getPetId());
      assert saved.getTotalXp() == 10;
      assert saved.getStats().getIntelligence() == 11;
      assert saved.getStats().getStrength() == 10;
      expectIllegal(() -> petService.applyCorrectAnswerReward(pet.getPetId(), "MATH"));
      expectIllegal(() -> petService.applyCorrectAnswerReward("P9999", "SCIENCE"));

      // TC-12 레벨 경계값: 누적 40 -> 50이면 Lv.2, 0/50
      petService.applyCorrectAnswerReward(pet.getPetId(), "HISTORY");
      petService.applyCorrectAnswerReward(pet.getPetId(), "HISTORY");
      petService.applyCorrectAnswerReward(pet.getPetId(), "SPORTS");
      result = petService.applyCorrectAnswerReward(pet.getPetId(), "LANGUAGE");
      assert result.isLevelUp() && result.getLevelAfter() == 2;
      saved = petService.getPet(pet.getPetId());
      assert saved.getTotalXp() == 50 && saved.getXpInLevel() == 0;

      // TC-03 사용자 구분, FR-002 분기
      User minji = userService.createUser("민지");
      User noPet = userService.createUser("캐릭터없음");
      petService.createPet(minji.getUserId(), "고북이", "TURTLE");
      expectIllegal(() -> petService.createPet(noPet.getUserId(), "냥", "CAT"));
      expectIllegal(() -> petService.createPet(noPet.getUserId(), "냥", null));
      expectIllegal(() -> petService.createPet(noPet.getUserId(), "  ", "DUCK"));
      assert petService.findPetByUserId(sanghun.getUserId()).get().getTotalXp() == 50;
      assert petService.findPetByUserId(minji.getUserId()).get().getTotalXp() == 0;
      assert petService.findPetByUserId(noPet.getUserId()).isEmpty();
      expectIllegal(() -> userService.getUser("U9999"));

      // TC-15 재실행: 새 Repository로 다시 읽어도 그대로 복원된다.
      UserService reloadedUsers = new UserService(new UserRepository(userFile));
      PetService reloadedPets = new PetService(new PetRepository(petFile), new UserRepository(userFile));
      assert reloadedUsers.getUsers().size() == 3;
      assert reloadedUsers.getUser("U0001").getCreatedAt().equals(sanghun.getCreatedAt());
      Pet reloaded = reloadedPets.findPetByUserId("U0001").get();
      assert reloaded.getName().equals("고라고라니") && reloaded.getSpriteKey().equals("DUCK");
      assert reloaded.getTotalXp() == 50;
      assert reloaded.getStats().getStrength() == 11 && reloaded.getStats().getIntelligence() == 11;
      assert reloaded.getStats().getFocus() == 11 && reloaded.getStats().getWisdom() == 12;

      List<Profile> profiles = new ProfileService(new UserRepository(userFile),
          new PetRepository(petFile)).getProfiles();
      assert profiles.get(0).toString().equals("상훈 (고라고라니)  Lv 2");
      assert profiles.get(2).toString().equals("캐릭터없음 (캐릭터 없음)");

      List<String> petLines = Files.readAllLines(petFile, StandardCharsets.UTF_8);
      assert petLines.get(0).equals("P0001|U0001|고라고라니|DUCK|50|11|11|11|12");

      // 손상된 줄은 경로와 줄 번호가 포함된 IOException으로 거부하고, 파일을 덮어쓰지 않는다.
      Files.writeString(petFile, "P0001|U0001|고라고라니|DUCK|abc|10|10|10|10\n",
          StandardCharsets.UTF_8);
      try {
        reloadedPets.getPet("P0001");
        throw new AssertionError("손상된 pet.txt가 허용되었습니다.");
      } catch (IOException expected) {
        assert expected.getMessage().contains(petFile + ":1:") : expected.getMessage();
      }
      assert Files.readString(petFile, StandardCharsets.UTF_8).contains("abc");
    } finally {
      for (Path file : new Path[] {userFile, petFile}) {
        Files.deleteIfExists(file);
      }
      Files.deleteIfExists(directory.resolve("save"));
      Files.deleteIfExists(directory);
    }
    System.out.println("UserService/PetService checks passed");
  }

  private static void expectIllegal(ThrowingRunnable action) throws IOException {
    expect(IllegalArgumentException.class, action);
  }

  private static void expectState(ThrowingRunnable action) throws IOException {
    expect(IllegalStateException.class, action);
  }

  private static void expect(Class<? extends RuntimeException> type, ThrowingRunnable action)
      throws IOException {
    try {
      action.run();
      throw new AssertionError("예외가 발생해야 합니다: " + type.getSimpleName());
    } catch (RuntimeException exception) {
      if (!type.isInstance(exception)) {
        throw exception;
      }
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws IOException;
  }
}
