package main.java.com.quizpet.model;

public enum Category {
  SCIENCE("과학&상식", "INT", "과학 원리와 일반 상식"),
  SPORTS("스포츠", "STR", "경기 규칙과 스포츠 용어"),
  HISTORY("역사", "WIS", "역사적 사건과 인물"),
  LANGUAGE("언어(영어)", "FOC", "영어 단어 및 기초 언어 지식");

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
