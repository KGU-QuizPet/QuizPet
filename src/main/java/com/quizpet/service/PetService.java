package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.GrowthResult;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.UserRepository;

/** 캐릭터 생성(FR-003)·조회와 경험치·능력치 반영(FR-004, FR-005) */
public class PetService {
  public static final int CORRECT_ANSWER_XP = 10;
  public static final int CORRECT_ANSWER_STAT = 1;

  // 화면마다 PetService를 새로 만들어도 확인-생성·조회-적용-저장이 겹치지 않도록 static 잠금을 씁니다.
  private static final Object LOCK = new Object();

  private final PetRepository petRepository;
  private final UserRepository userRepository;

  public PetService() {
    this(new PetRepository(), new UserRepository());
  }

  public PetService(PetRepository petRepository, UserRepository userRepository) {
    this.petRepository = Objects.requireNonNull(petRepository, "petRepository는 필수입니다.");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository는 필수입니다.");
  }

  /**
   * 캐릭터를 Lv.1, XP 0, 능력치 각 10으로 만듭니다. spriteKey는 PetAppearance의 이름(DUCK 등)입니다.
   * 없는 사용자·잘못된 이름·정의되지 않은 spriteKey는 IllegalArgumentException,
   * 이미 캐릭터가 있는 사용자는 IllegalStateException으로 거부합니다.
   */
  public Pet createPet(String userId, String petName, String spriteKey) throws IOException {
    synchronized (LOCK) {
      if (userId == null || userRepository.findById(userId).isEmpty()) {
        throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
      }
      if (petRepository.findByUserId(userId).isPresent()) {
        throw new IllegalStateException("이미 캐릭터가 있습니다. 사용자당 캐릭터는 1개만 만들 수 있습니다.");
      }
      Pet pet = new Pet(petRepository.nextId(), userId,
          petName == null ? null : petName.strip(), toAppearance(spriteKey));
      petRepository.save(pet);
      return pet;
    }
  }

  /** 프로필 선택 후 분기용입니다. 있으면 메인(UI-03), 비어 있으면 캐릭터 생성(UI-02)으로 이동합니다. */
  public Optional<Pet> findPetByUserId(String userId) throws IOException {
    return petRepository.findByUserId(userId);
  }

  public Pet getPet(String petId) throws IOException {
    return petRepository.findById(petId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 캐릭터입니다: " + petId));
  }

  /**
   * 최초 정답 보상으로 XP +10, categoryId에 연결된 능력치 +1을 적용하고 저장합니다.
   * 최초 정답인지는 호출하는 쪽(성장·기록)에서 판단하고, 최초 정답일 때만 호출합니다.
   * 저장에 실패하면 IOException이 발생하고 파일은 이전 상태로 남습니다.
   */
  public GrowthResult applyCorrectAnswerReward(String petId, String categoryId)
      throws IOException {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId는 필수입니다.");
    }
    Category category = Category.valueOf(categoryId);
    synchronized (LOCK) {
      Pet pet = getPet(petId);
      GrowthResult result = pet.applyReward(CORRECT_ANSWER_XP, category.getStatType(),
          CORRECT_ANSWER_STAT);
      petRepository.save(pet);
      return result;
    }
  }

  private static PetAppearance toAppearance(String spriteKey) {
    if (spriteKey == null) {
      throw new IllegalArgumentException("외형을 선택해 주세요.");
    }
    try {
      return PetAppearance.valueOf(spriteKey);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("정의되지 않은 외형입니다: " + spriteKey, e);
    }
  }
}
