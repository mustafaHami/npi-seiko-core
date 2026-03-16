package my.lokalix.planning.core.models.smtp2go;

import java.util.List;
import lombok.Value;

@Value
public class EmailRequest {
  List<String> to;
  String sender;
  String subject;
  String html_body;
}
