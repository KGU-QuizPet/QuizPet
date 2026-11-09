package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import main.java.com.quizpet.exception.DataFileException;

/**
 * '|'로 구분된 텍스트 파일 한 개를 읽고 쓰는 공통 도구 (DB 대신 파일 입출력).
 * - '#'로 시작하는 줄은 머리말(설명)로 보고 건너뛴다.
 * - 저장은 임시 파일에 다 쓴 뒤 원본과 바꿔치기해서, 쓰다가 실패해도 원본이 깨지지 않게 한다.
 */
class TextFileStore {
    static final String SEPARATOR = "|";
    /** 실행 위치(프로젝트 루트) 기준 저장 폴더 */
    static final Path DEFAULT_SAVE_DIR = Path.of("data", "save");

    private final Path file;
    private final String header;

    TextFileStore(Path file, String header) {
        this.file = file;
        this.header = header;
    }

    /** 데이터 줄마다 '|'로 나눈 칸 배열을 돌려준다. 파일이 없으면 빈 목록. */
    List<String[]> readRows() {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(file))
            return rows;
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataFileException("데이터 파일을 읽지 못했습니다: " + file, e);
        }
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#"))
                continue;
            rows.add(line.split("\\|", -1));
        }
        return rows;
    }

    void writeRows(List<String> lines) {
        List<String> out = new ArrayList<>();
        out.add("# " + header);
        out.addAll(lines);
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path temp = dir.resolve(file.getFileName() + ".tmp");
            Files.write(temp, out, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DataFileException("저장에 실패했습니다. 다시 시도해 주세요.", e);
        }
    }

    Path getFile() {
        return file;
    }
}
