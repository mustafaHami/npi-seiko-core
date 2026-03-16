package my.lokalix.planning.core.utils;

import io.micrometer.common.util.StringUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.excel.CellColorEnum;
import my.lokalix.planning.core.models.excel.CellStyleFormatEnum;
import my.lokalix.planning.core.models.excel.ExcelCellStyles;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

@Slf4j
public final class ExcelUtils {
  static final byte[] GREENISH_RGB = new byte[] {(byte) 123, (byte) 204, (byte) 103};
  static final byte[] LIGHT_GREENISH_RGB = new byte[] {(byte) 0, (byte) 176, (byte) 80};
  static final byte[] BEIGE_RGB = new byte[] {(byte) 255, (byte) 235, (byte) 168};
  static final byte[] LIGHT_BLUEISH_RGB = new byte[] {(byte) 51, (byte) 255, (byte) 248};
  static final byte[] BLUEISH_RGB = new byte[] {(byte) 192, (byte) 230, (byte) 245};
  static final byte[] REDDISH_RGB = new byte[] {(byte) 255, (byte) 87, (byte) 51};
  static final byte[] YELLOWISH_RGB = new byte[] {(byte) 255, (byte) 255, (byte) 51};
  static final byte[] ORANGE_RGB = new byte[] {(byte) 255, (byte) 210, (byte) 67};
  private static final DateTimeFormatter localDateFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static String loadStringCell(Cell cell) {
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.BLANK) {
      return null;
    } else if (cell.getCellType() == CellType.NUMERIC) {
      return Double.toString(cell.getNumericCellValue());
    } else if (cell.getCellType() == CellType.STRING) {
      String cellValue = cell.getStringCellValue();
      // The cell is not blank
      if (cellValue.isBlank()) {
        return null;
      }
      return cellValue.strip();
    } else if (cell.getCellType() == CellType.FORMULA) {
      FormulaEvaluator evaluator =
          cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
      CellValue cellValue = evaluator.evaluate(cell);
      return switch (cellValue.getCellType()) {
        case BOOLEAN -> Boolean.toString(cellValue.getBooleanValue());
        case NUMERIC -> Double.toString(cellValue.getNumberValue());
        case STRING -> cellValue.getStringValue();
        default -> null;
      };
    } else if (cell.getCellType() == CellType.ERROR) {
      return null;
    }
    log.warn("Cell type not managed: {}", cell.getCellType());
    return null;
  }

  public static String loadStringCellForceNumericToLong(Cell cell) {
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.BLANK) {
      return null;
    } else if (cell.getCellType() == CellType.NUMERIC) {
      double numericValue = cell.getNumericCellValue();
      if (numericValue == Math.floor(numericValue)) {
        return Long.toString((long) numericValue);
      } else {
        return Double.toString(numericValue);
      }
    } else if (cell.getCellType() == CellType.STRING) {
      String cellValue = cell.getStringCellValue();
      // The cell is not blank
      if (cellValue.isBlank()) {
        return null;
      }
      return cellValue.strip();
    } else if (cell.getCellType() == CellType.FORMULA) {
      FormulaEvaluator evaluator =
          cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
      CellValue cellValue = evaluator.evaluate(cell);
      return switch (cellValue.getCellType()) {
        case BOOLEAN -> Boolean.toString(cellValue.getBooleanValue());
        case NUMERIC -> Integer.toString((int) cellValue.getNumberValue());
        case STRING -> cellValue.getStringValue();
        default -> null;
      };
    }
    log.warn("Cell type not managed: {}", cell.getCellType());
    return null;
  }

  public static LocalDate loadDateCell(Cell cell) {
    // The cell is blank
    if (cell.getNumericCellValue() == 0) {
      return null;
    }
    return cell.getLocalDateTimeCellValue().toLocalDate();
  }

  public static LocalDate loadDateFromStringCell(Cell cell) {
    String cellValue = loadStringCell(cell);
    // The cellValue is blank
    if (StringUtils.isBlank(cellValue)) {
      return null;
    }
    return LocalDate.parse(cellValue, localDateFormatter);
  }

  public static void orangeCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(ORANGE_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void blueishCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(BLUEISH_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void lightBlueCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(LIGHT_BLUEISH_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void beigeCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(BEIGE_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void greenishCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(GREENISH_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void lightGreenishCellStyle(CellStyle cellStyle, Font font) {
    cellStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREENISH_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    font.setColor(IndexedColors.WHITE.index);
    font.setBold(true);
    cellStyle.setFont(font);
    styleAllBorders(cellStyle);
  }

  public static void reddishCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(REDDISH_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void yellowCellStyle(CellStyle cellStyle) {
    cellStyle.setFillForegroundColor(new XSSFColor(YELLOWISH_RGB));
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styleAllBorders(cellStyle);
  }

  public static void whiteCellStyle(CellStyle cellStyle) {
    styleAllBorders(cellStyle);
  }

  public static void createAndStyleCellLeftAlignment(
      Row currentRow,
      int cellNumber,
      Object cellValue,
      CellStyleFormatEnum formatType,
      CellColorEnum color,
      ExcelCellStyles excelCellStyles) {
    createAndStyleCell(
        currentRow,
        cellNumber,
        cellValue,
        formatType,
        color,
        excelCellStyles,
        HorizontalAlignment.LEFT);
  }

  public static void createAndStyleCell(
      Row currentRow,
      int cellNumber,
      Object cellValue,
      CellStyleFormatEnum formatType,
      CellColorEnum color,
      ExcelCellStyles excelCellStyles,
      HorizontalAlignment alignment) {
    Cell cell = currentRow.createCell(cellNumber);
    cell.setCellStyle(excelCellStyles.retrieveCellStyle(formatType, color, alignment));
    setCellValue(cell, cellValue, formatType);
  }

  public static void setCellValue(Cell cell, Object cellValue, CellStyleFormatEnum formatType) {
    if (cellValue != null) {
      switch (formatType) {
        case STRING:
          cell.setCellValue((String) cellValue);
          break;
        case INTEGER:
          cell.setCellValue((Integer) cellValue);
          break;
        case DOUBLE:
          cell.setCellValue((Double) cellValue);
          break;
        case DATE:
          cell.setCellValue((LocalDate) cellValue);
          break;
      }
    }
  }

  private static void styleAllBorders(CellStyle cellStyle) {
    cellStyle.setBorderTop(BorderStyle.THIN);
    cellStyle.setBorderBottom(BorderStyle.THIN);
    cellStyle.setBorderLeft(BorderStyle.THIN);
    cellStyle.setBorderRight(BorderStyle.THIN);
  }
}
