package my.lokalix.planning.core.exceptions.file;

public class FileNotFoundException extends RuntimeException implements CustomFileException {
  public FileNotFoundException(String s) {
    super(s);
  }
}
