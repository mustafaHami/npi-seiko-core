package my.lokalix.planning.core.exceptions.file;

public class FileUnsupportedException extends RuntimeException implements CustomFileException {
  public FileUnsupportedException(String s) {
    super(s);
  }
}
