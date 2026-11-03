package main.java.com.quizpet;

import javax.swing.*;

public class Main {
  public static void main(String[] args) {
    JFrame frame = new JFrame("Swing Example"); // 윈도우 창 생성
    frame.setSize(400, 300);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel panel = new JPanel(); // 패널 생성
    JButton button = new JButton("클릭하세요"); // 버튼 생성

    panel.add(button); // 패널에 버튼 추가
    frame.add(panel); // 프레임에 패널 추가

    frame.setVisible(true); // 창을 화면에 표시
  }
}
