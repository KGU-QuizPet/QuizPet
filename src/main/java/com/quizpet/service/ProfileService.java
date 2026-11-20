package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.Profile;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.UserRepository;

/** 시작 화면(UI-01)의 기존 사용자 목록을 만듭니다. */
public class ProfileService {
  private final UserRepository userRepository;
  private final PetRepository petRepository;

  public ProfileService() {
    this(new UserRepository(), new PetRepository());
  }

  public ProfileService(UserRepository userRepository, PetRepository petRepository) {
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository는 필수입니다.");
    this.petRepository = Objects.requireNonNull(petRepository, "petRepository는 필수입니다.");
  }

  /**
   * user.txt의 사용자마다 캐릭터를 연결합니다.
   * pet.txt에 존재하지 않는 사용자의 캐릭터가 있으면 IOException으로 불러오기를 거부합니다.
   */
  public List<Profile> getProfiles() throws IOException {
    List<User> users = userRepository.findAll();
    Set<String> userIds = new HashSet<>();
    for (User user : users) {
      userIds.add(user.getUserId());
    }
    Map<String, Pet> petByUserId = new HashMap<>();
    for (Pet pet : petRepository.findAllOwnedBy(userIds)) {
      petByUserId.put(pet.getUserId(), pet);
    }
    List<Profile> profiles = new ArrayList<>();
    for (User user : users) {
      profiles.add(new Profile(user, petByUserId.get(user.getUserId())));
    }
    return List.copyOf(profiles);
  }
}
