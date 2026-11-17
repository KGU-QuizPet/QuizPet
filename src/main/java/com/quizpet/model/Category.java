package main.java.com.quizpet.model;

public enum Category {
  SCIENCE("과학", "INT", "자연 현상과 과학 원리를 배웁니다"),
  COMMON("상식", "INT", "생활상식들을 배웁니다"),
  SPORTS("스포츠", "STR", "운동, 건강과 관련된 지식들을 배웁니다"),
  LANGUAGE("언어", "FOC", "영어 문법과 어휘(단어)를 배웁니다"),
  HISTORY("역사", "WIS", "한국사의 주요 사건과 기록들을 배웁니다");

  private final String name;
  private final String statType;
  private final String description;

  Category(String name, String statType, String description) {
    this.name = name;
    this.statType = statType;
    this.description = description;
  }

  public String getCategoryId() {
    return name();
  }

  public String getName() {
    return name;
  }

  public String getStatType() {
    return statType;
  }

  public String getDescription() {
    return description;
  }
}
