package main.java.com.quizpet.model;

public class CategoryTest {
  public static void main(String[] args) {
    assert Category.values().length == 4;
    check(Category.SCIENCE, "SCIENCE", "과학&상식", "INT");
    check(Category.SPORTS, "SPORTS", "스포츠", "STR");
    check(Category.HISTORY, "HISTORY", "역사", "WIS");
    check(Category.LANGUAGE, "LANGUAGE", "언어(영어)", "FOC");
    for (String invalid : new String[] {"UNKNOWN", "", "science", " SCIENCE "}) {
      try {
        Category.valueOf(invalid);
        throw new AssertionError("정의되지 않은 분야 ID가 허용되었습니다.");
      } catch (IllegalArgumentException expected) {
      }
    }
    System.out.println("Category checks passed");
  }

  private static void check(Category category, String id, String name, String statType) {
    assert category.getCategoryId().equals(id);
    assert Category.valueOf(id) == category;
    assert category.getName().equals(name);
    assert category.getStatType().equals(statType);
    assert category.getDescription() != null && !category.getDescription().isBlank();
  }
}
