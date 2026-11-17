package main.java.com.quizpet.ui;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 같은 저장 폴더로 앱을 두 번 실행하여 보상을 중복 저장하는 것을 막습니다. */
final class SaveDirectoryLock implements AutoCloseable {
  private final FileChannel channel;
  private final FileLock lock;

  SaveDirectoryLock(Path directory) throws IOException {
    Files.createDirectories(directory);
    channel = FileChannel.open(directory.resolve(".quizpet.lock"),
        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    try {
      lock = channel.tryLock();
      if (lock == null) throw new IOException("같은 저장 폴더의 QuizPet이 이미 실행 중입니다.");
    } catch (IOException | OverlappingFileLockException e) {
      channel.close();
      throw new IOException("같은 저장 폴더의 QuizPet 실행 잠금을 얻을 수 없습니다. 다른 실행 창을 확인해 주세요.", e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      lock.release();
    } finally {
      channel.close();
    }
  }
}
