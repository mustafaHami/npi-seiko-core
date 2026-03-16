package my.lokalix.planning.core;

import java.time.*;
import my.lokalix.planning.core.utils.TimeUtils;

public class Test {
  public static void main(String[] args) {

    String timezone = "Asia/Singapore";

    OffsetDateTime nowUtc = TimeUtils.nowOffsetDateTime("UTC");
    System.out.println(nowUtc);
    OffsetDateTime nowSg = TimeUtils.nowOffsetDateTime(timezone);
    System.out.println(nowSg);
    System.out.println(TimeUtils.replaceOffset(nowUtc, timezone));
  }
}
