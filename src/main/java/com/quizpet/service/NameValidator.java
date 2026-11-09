package main.java.com.quizpet.service;

import main.java.com.quizpet.exception.ValidationException;

/** 닉네임·캐릭터 이름 공통 규칙: 앞뒤 공백 제거 후 1~12자, '|'·'_'·줄바꿈 금지 */
class NameValidator {
    static final int MAX_LENGTH = 12;

    private NameValidator() {
    }

    /** 규칙에 맞으면 정리된(trim) 이름을 돌려주고, 아니면 ValidationException */
    static String normalize(String input, String label) {
        if (input == null || input.trim().isEmpty())
            throw new ValidationException(label + "을(를) 입력해 주세요.");
        String name = input.trim();
        if (name.codePointCount(0, name.length()) > MAX_LENGTH)
            throw new ValidationException(label + "은(는) " + MAX_LENGTH + "자 이하로 입력해 주세요.");
        if (name.contains("|") || name.contains("_") || name.contains("\n") || name.contains("\r"))
            throw new ValidationException(label + "에 '|', '_' 문자나 줄바꿈은 사용할 수 없습니다.");
        return name;
    }
}
