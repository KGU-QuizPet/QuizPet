package main.java.com.quizpet.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MainFrame extends JFrame {
  private SaveDirectoryLock saveLock;

  public MainFrame() {
    super("QuizPet");
    ImageIcon logo = UiStyle.logoIcon(256);
    if (logo != null) setIconImage(logo.getImage());
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    setMinimumSize(new Dimension(Math.min(1280, screen.width), Math.min(800, screen.height)));
    setBounds(screen);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent event) {
        if (saveLock != null) {
          try {
            saveLock.close();
          } catch (IOException e) {
            System.getLogger(MainFrame.class.getName()).log(System.Logger.Level.WARNING, e.getMessage());
          }
        }
      }
    });
    loadApplication();
  }

  private void loadApplication() {
    try {
      Path directory = Path.of("data/save");
      if (saveLock == null) saveLock = new SaveDirectoryLock(directory);
      setContentPane(new AppController(directory).getWindowContent());
    } catch (IOException | IllegalArgumentException | IllegalStateException e) {
      JPanel error = new JPanel(new BorderLayout(0, 20));
      error.setBorder(BorderFactory.createEmptyBorder(30, 25, 30, 25));
      error.setBackground(UiStyle.BACKGROUND);
      error.add(UiStyle.label("실행 데이터를 확인해 주세요", UiStyle.SUBTITLE_FONT), BorderLayout.NORTH);
      JTextArea message = new JTextArea(e.getMessage());
      message.setFont(UiStyle.BODY_FONT);
      message.setEditable(false);
      message.setLineWrap(true);
      message.setWrapStyleWord(true);
      error.add(new JScrollPane(message), BorderLayout.CENTER);
      JButton retry = UiStyle.button("다시 불러오기");
      retry.addActionListener(event -> loadApplication());
      error.add(retry, BorderLayout.SOUTH);
      setContentPane(error);
    }
    revalidate();
    repaint();
  }
}
