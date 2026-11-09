package main.java.com.quizpet.service;

import java.util.Optional;

import main.java.com.quizpet.exception.ValidationException;
import main.java.com.quizpet.model.GrowthResult;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.StatType;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.UserRepository;

/** 캐릭터 생성(FR-003)·조회, 경험치·능력치 반영(FR-004, FR-005) */
public class PetService {
    public static final int XP_PER_CORRECT = 10;
    public static final int STAT_PER_CORRECT = 1;

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PetService(PetRepository petRepository, UserRepository userRepository) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    /** 캐릭터 생성: Lv.1, XP 0, 능력치 각 10. 한 사용자당 1마리. */
    public synchronized Pet createPet(String userId, String petName, PetAppearance appearance) {
        String name = NameValidator.normalize(petName, "캐릭터 이름");
        if (appearance == null)
            throw new ValidationException("외형을 선택해 주세요.");
        if (userRepository.findById(userId).isEmpty())
            throw new ValidationException("존재하지 않는 사용자입니다.");
        if (petRepository.findByUserId(userId).isPresent())
            throw new ValidationException("이미 캐릭터가 있습니다. 사용자당 캐릭터는 1개만 만들 수 있습니다.");
        Pet pet = new Pet(petRepository.nextId(), userId, name, appearance);
        petRepository.insert(pet);
        return pet;
    }

    /** 프로필 선택 후 분기용: 있으면 메인(UI-03), 없으면 캐릭터 생성(UI-02) */
    public Optional<Pet> findPetByUserId(String userId) {
        return petRepository.findByUserId(userId);
    }

    public Pet getPet(String petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ValidationException("존재하지 않는 캐릭터입니다."));
    }

    /**
     * 최초 정답 보상: XP +10, 해당 능력치 +1 을 적용하고 파일에 저장한다.
     * "최초 정답인지" 판단은 퀴즈/학습기록 쪽에서 하고, 최초 정답일 때만 이 메서드를 호출한다.
     * 저장에 실패하면 DataFileException이 나고 파일 내용은 그대로 남는다.
     */
    public synchronized GrowthResult applyCorrectAnswerReward(String petId, StatType statType) {
        Pet pet = getPet(petId);
        GrowthResult result = pet.applyReward(XP_PER_CORRECT, statType, STAT_PER_CORRECT);
        petRepository.update(pet);
        return result;
    }
}
