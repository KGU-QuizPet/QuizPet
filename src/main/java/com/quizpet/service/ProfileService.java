package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  public List<Profile> getProfiles() throws IOException {
    List<Profile> profiles = new ArrayList<>();
    for (User user : userRepository.findAll()) {
      profiles.add(new Profile(user, petRepository.findByUserId(user.getUserId()).orElse(null)));
    }
    return List.copyOf(profiles);
  }
}
