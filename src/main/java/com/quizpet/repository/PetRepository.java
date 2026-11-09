package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetStats;

/**
 * data/save/pet.txt를 읽고 씁니다. Pet과 PetStats를 한 줄에 함께 저장합니다.
 * 형식: petId|userId|name|spriteKey|totalXp|strength|intelligence|focus|wisdom
 */
public class PetRepository {
  private final Path path;

  public PetRepository() {
    this(Path.of("data/save/pet.txt"));
  }

  public PetRepository(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("path는 필수입니다.");
    }
    this.path = path;
  }

  public List<Pet> findAll() throws IOException {
    List<Pet> loaded = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Set<String> userIds = new HashSet<>();
    List<String> lines = SaveFile.readLines(path);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      try {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 9) {
          throw new IllegalArgumentException("필드는 9개여야 합니다.");
        }
        PetStats stats = new PetStats(Integer.parseInt(fields[5]), Integer.parseInt(fields[6]),
            Integer.parseInt(fields[7]), Integer.parseInt(fields[8]));
        Pet pet = new Pet(fields[0], fields[1], fields[2], PetAppearance.valueOf(fields[3]),
            Integer.parseInt(fields[4]), stats);
        if (!ids.add(pet.getPetId())) {
          throw new IllegalArgumentException("중복된 캐릭터 ID입니다.");
        }
        if (!userIds.add(pet.getUserId())) {
          throw new IllegalArgumentException("한 사용자에게 캐릭터가 2개 이상 있습니다.");
        }
        loaded.add(pet);
      } catch (RuntimeException e) {
        // 값이 깨졌을 때 초기값으로 추정하지 않고 오류를 알립니다(기능명세서 FR-004).
        throw new IOException(path + ":" + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    return List.copyOf(loaded);
  }

  public Optional<Pet> findById(String petId) throws IOException {
    return findAll().stream()
        .filter(pet -> pet.getPetId().equals(petId))
        .findFirst();
  }

  public Optional<Pet> findByUserId(String userId) throws IOException {
    return findAll().stream()
        .filter(pet -> pet.getUserId().equals(userId))
        .findFirst();
  }

  /** 같은 petId가 있으면 그 줄을 바꾸고, 없으면 새 줄을 추가합니다. */
  public void save(Pet pet) throws IOException {
    if (pet == null) {
      throw new IllegalArgumentException("pet은 필수입니다.");
    }
    List<String> lines = new ArrayList<>();
    boolean replaced = false;
    for (Pet saved : findAll()) {
      if (saved.getPetId().equals(pet.getPetId())) {
        lines.add(format(pet));
        replaced = true;
      } else {
        if (saved.getUserId().equals(pet.getUserId())) {
          throw new IllegalArgumentException("이미 캐릭터가 있는 사용자입니다: " + pet.getUserId());
        }
        lines.add(format(saved));
      }
    }
    if (!replaced) {
      lines.add(format(pet));
    }
    SaveFile.writeLines(path, lines);
  }

  /** P0001, P0002 순서로 다음 캐릭터 ID를 만듭니다. */
  public String nextId() throws IOException {
    int max = 0;
    for (Pet pet : findAll()) {
      String id = pet.getPetId();
      if (id.matches("P\\d+")) {
        max = Math.max(max, Integer.parseInt(id.substring(1)));
      }
    }
    return String.format("P%04d", max + 1);
  }

  private static String format(Pet pet) {
    PetStats stats = pet.getStats();
    return String.join("|", pet.getPetId(), pet.getUserId(), pet.getName(), pet.getSpriteKey(),
        String.valueOf(pet.getTotalXp()),
        String.valueOf(stats.getStrength()), String.valueOf(stats.getIntelligence()),
        String.valueOf(stats.getFocus()), String.valueOf(stats.getWisdom()));
  }
}
