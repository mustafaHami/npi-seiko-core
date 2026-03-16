package my.lokalix.planning.core.models;

import lombok.Value;

@Value
public class DownloadedFileOutput {
  String fileName;
  byte[] fileContent;
}
