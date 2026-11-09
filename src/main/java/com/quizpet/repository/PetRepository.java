package main.java.com.quizpet.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import main.java.com.quizpet.exception.DataFileException;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetStats;

/**
 * data/save/pet.txt 저장소. 한 줄 = 캐릭터 1마리 (능력치까지 한 줄에 담아서 부분 저장이 생기지 않게 함)
 * 형식: petId|userId|name|spriteKey|totalXp|strength|intelligence|focus|wisdom
 * 예)   p0001|u0001|고라고라니|DUCK|170|15|20|25|40
 */
public class PetRepository {
    private static final String HEADER = "petId|userId|name|spriteKey|totalXp|strength|intelligence|focus|wisdom";

    private final TextFileStore store;

    /** 기본 위치 data/save/pet.txt 사용 */
    public PetRepository() {
        this(TextFileStore.DEFAULT_SAVE_DIR);
    }

    public PetRepository(Path saveDir) {
        this.store = new TextFileStore(saveDir.resolve("pet.txt"), HEADER);
    }

    public List<Pet> findAll() {
        List<Pet> pets = new ArrayList<>();
        for (String[] row : store.readRows())
            pets.add(parse(row));
        return pets;
    }

    public Optional<Pet> findById(String petId) {
        for (Pet p : findAll())
            if (p.getPetId().equals(petId))
                return Optional.of(p);
        return Optional.empty();
    }

    public Optional<Pet> findByUserId(String userId) {
        for (Pet p : findAll())
            if (p.getUserId().equals(userId))
                return Optional.of(p);
        return Optional.empty();
    }

    public void insert(Pet pet) {
        List<String> lines = new ArrayList<>();
        for (Pet p : findAll())
            lines.add(format(p));
        lines.add(format(pet));
        store.writeRows(lines);
    }

    /** 같은 petId 줄을 새 값으로 바꿔서 저장 */
    public void update(Pet pet) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        for (Pet p : findAll()) {
            if (p.getPetId().equals(pet.getPetId())) {
                lines.add(format(pet));
                found = true;
            } else {
                lines.add(format(p));
            }
        }
        if (!found)
            throw new DataFileException("저장된 캐릭터가 없습니다: " + pet.getPetId());
        store.writeRows(lines);
    }

    /** p0001, p0002 ... 순서로 다음 ID */
    public String nextId() {
        int max = 0;
        for (Pet p : findAll())
            max = Math.max(max, Integer.parseInt(p.getPetId().substring(1)));
        return "p%04d".formatted(max + 1);
    }

    private String format(Pet p) {
        PetStats s = p.getStats();
        return String.join(TextFileStore.SEPARATOR,
                p.getPetId(), p.getUserId(), p.getName(), p.getAppearance().name(),
                String.valueOf(p.getTotalXp()),
                String.valueOf(s.getStrength()), String.valueOf(s.getIntelligence()),
                String.valueOf(s.getFocus()), String.valueOf(s.getWisdom()));
    }

    private Pet parse(String[] row) {
        try {
            if (row.length != 9)
                throw new IllegalArgumentException("칸 수가 9가 아님");
            PetStats stats = new PetStats(Integer.parseInt(row[5]), Integer.parseInt(row[6]),
                    Integer.parseInt(row[7]), Integer.parseInt(row[8]));
            return new Pet(row[0], row[1], row[2], PetAppearance.valueOf(row[3]),
                    Integer.parseInt(row[4]), stats);
        } catch (RuntimeException e) {
            // 값이 깨졌을 때 초기값으로 추정하지 않고 오류를 알린다 (기능명세서 FR-004 예외 처리)
            throw new DataFileException("캐릭터 파일이 손상되었습니다: " + String.join("|", row), e);
        }
    }
}
