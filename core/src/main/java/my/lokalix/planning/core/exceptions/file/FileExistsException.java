package my.lokalix.planning.core.exceptions.file;

public class FileExistsException extends RuntimeException implements CustomFileException {
  public FileExistsException(String s) {
    super(s);
  }
}
