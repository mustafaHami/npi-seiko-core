package my.lokalix.planning.core.models.excel;

import java.util.Collections;
import java.util.Map;
import my.lokalix.planning.core.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelCellStyles {

  final Map<CellStyleFormatEnum, Map<CellColorEnum, CellStyle>> styleMap;

  public ExcelCellStyles(Workbook workbook) {
    Font font = workbook.createFont();
    CellStyle integerWhiteNoBorderStyle = workbook.createCellStyle();
    integerWhiteNoBorderStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));

    CellStyle stringWhiteBackgroundStyle = workbook.createCellStyle();
    stringWhiteBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.whiteCellStyle(stringWhiteBackgroundStyle);

    CellStyle integerWhiteBackgroundStyle = workbook.createCellStyle();
    integerWhiteBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.whiteCellStyle(integerWhiteBackgroundStyle);

    CellStyle doubleWhiteBackgroundStyle = workbook.createCellStyle();
    doubleWhiteBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.whiteCellStyle(doubleWhiteBackgroundStyle);

    CellStyle dateWhiteBackgroundStyle = workbook.createCellStyle();
    dateWhiteBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.whiteCellStyle(dateWhiteBackgroundStyle);

    CellStyle stringYellowBackgroundStyle = workbook.createCellStyle();
    stringYellowBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.yellowCellStyle(stringYellowBackgroundStyle);

    CellStyle integerYellowBackgroundStyle = workbook.createCellStyle();
    integerYellowBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.yellowCellStyle(integerYellowBackgroundStyle);

    CellStyle doubleYellowBackgroundStyle = workbook.createCellStyle();
    doubleYellowBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.yellowCellStyle(doubleYellowBackgroundStyle);

    CellStyle dateYellowBackgroundStyle = workbook.createCellStyle();
    dateYellowBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.yellowCellStyle(dateYellowBackgroundStyle);

    CellStyle stringOrangeBackgroundStyle = workbook.createCellStyle();
    stringOrangeBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.orangeCellStyle(stringOrangeBackgroundStyle);

    CellStyle integerOrangeBackgroundStyle = workbook.createCellStyle();
    integerOrangeBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.orangeCellStyle(integerOrangeBackgroundStyle);

    CellStyle doubleOrangeBackgroundStyle = workbook.createCellStyle();
    doubleOrangeBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.orangeCellStyle(doubleOrangeBackgroundStyle);

    CellStyle dateOrangeBackgroundStyle = workbook.createCellStyle();
    dateOrangeBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.orangeCellStyle(dateOrangeBackgroundStyle);

    CellStyle stringRedBackgroundStyle = workbook.createCellStyle();
    stringRedBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.reddishCellStyle(stringRedBackgroundStyle);

    CellStyle integerRedBackgroundStyle = workbook.createCellStyle();
    integerRedBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.reddishCellStyle(integerRedBackgroundStyle);

    CellStyle doubleRedBackgroundStyle = workbook.createCellStyle();
    doubleRedBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.reddishCellStyle(doubleRedBackgroundStyle);

    CellStyle dateRedBackgroundStyle = workbook.createCellStyle();
    dateRedBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.reddishCellStyle(dateRedBackgroundStyle);

    CellStyle stringBlueBackgroundStyle = workbook.createCellStyle();
    stringBlueBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.blueishCellStyle(stringBlueBackgroundStyle);

    CellStyle integerBlueBackgroundStyle = workbook.createCellStyle();
    integerBlueBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.blueishCellStyle(integerBlueBackgroundStyle);

    CellStyle doubleBlueBackgroundStyle = workbook.createCellStyle();
    doubleBlueBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.blueishCellStyle(doubleBlueBackgroundStyle);

    CellStyle dateBlueBackgroundStyle = workbook.createCellStyle();
    dateBlueBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.blueishCellStyle(dateBlueBackgroundStyle);

    CellStyle stringBeigeBackgroundStyle = workbook.createCellStyle();
    stringBeigeBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.beigeCellStyle(stringBeigeBackgroundStyle);

    CellStyle integerBeigeBackgroundStyle = workbook.createCellStyle();
    integerBeigeBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.beigeCellStyle(integerBeigeBackgroundStyle);

    CellStyle doubleBeigeBackgroundStyle = workbook.createCellStyle();
    doubleBeigeBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.beigeCellStyle(doubleBeigeBackgroundStyle);

    CellStyle dateBeigeBackgroundStyle = workbook.createCellStyle();
    dateBeigeBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.beigeCellStyle(dateBeigeBackgroundStyle);

    CellStyle stringGreenBackgroundStyle = workbook.createCellStyle();
    stringGreenBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.greenishCellStyle(stringGreenBackgroundStyle);

    CellStyle integerGreenBackgroundStyle = workbook.createCellStyle();
    integerGreenBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.greenishCellStyle(integerGreenBackgroundStyle);

    CellStyle doubleGreenBackgroundStyle = workbook.createCellStyle();
    doubleGreenBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.greenishCellStyle(doubleGreenBackgroundStyle);

    CellStyle dateGreenBackgroundStyle = workbook.createCellStyle();
    dateGreenBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.greenishCellStyle(dateGreenBackgroundStyle);
    CellStyle stringLightGreenBackgroundStyle = workbook.createCellStyle();
    stringLightGreenBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.lightGreenishCellStyle(stringLightGreenBackgroundStyle, font);

    CellStyle integerLightGreenBackgroundStyle = workbook.createCellStyle();
    integerLightGreenBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.lightGreenishCellStyle(integerLightGreenBackgroundStyle, font);

    CellStyle doubleLightGreenBackgroundStyle = workbook.createCellStyle();
    doubleLightGreenBackgroundStyle.setDataFormat(
        workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.lightGreenishCellStyle(doubleLightGreenBackgroundStyle, font);

    CellStyle dateLightGreenBackgroundStyle = workbook.createCellStyle();
    dateLightGreenBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.lightGreenishCellStyle(dateLightGreenBackgroundStyle, font);

    CellStyle stringLightBlueBackgroundStyle = workbook.createCellStyle();
    stringLightBlueBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
    ExcelUtils.lightBlueCellStyle(stringLightBlueBackgroundStyle);

    CellStyle integerLightBlueBackgroundStyle = workbook.createCellStyle();
    integerLightBlueBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
    ExcelUtils.lightBlueCellStyle(integerLightBlueBackgroundStyle);

    CellStyle doubleLightBlueBackgroundStyle = workbook.createCellStyle();
    doubleLightBlueBackgroundStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    ExcelUtils.lightBlueCellStyle(doubleLightBlueBackgroundStyle);

    CellStyle dateLightBlueBackgroundStyle = workbook.createCellStyle();
    dateLightBlueBackgroundStyle.setDataFormat(
        workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
    ExcelUtils.lightBlueCellStyle(dateLightBlueBackgroundStyle);

    this.styleMap =
        Map.of(
            CellStyleFormatEnum.STRING,
            Map.of(
                CellColorEnum.BLUE,
                stringBlueBackgroundStyle,
                CellColorEnum.RED,
                stringRedBackgroundStyle,
                CellColorEnum.YELLOW,
                stringYellowBackgroundStyle,
                CellColorEnum.ORANGE,
                stringOrangeBackgroundStyle,
                CellColorEnum.BEIGE,
                stringBeigeBackgroundStyle,
                CellColorEnum.GREEN,
                stringGreenBackgroundStyle,
                CellColorEnum.LIGHT_GREEN,
                stringLightGreenBackgroundStyle,
                CellColorEnum.LIGHT_BLUE,
                stringLightBlueBackgroundStyle,
                CellColorEnum.WHITE,
                stringWhiteBackgroundStyle),
            CellStyleFormatEnum.INTEGER,
            Map.of(
                CellColorEnum.BLUE,
                integerBlueBackgroundStyle,
                CellColorEnum.RED,
                integerRedBackgroundStyle,
                CellColorEnum.YELLOW,
                integerYellowBackgroundStyle,
                CellColorEnum.ORANGE,
                integerOrangeBackgroundStyle,
                CellColorEnum.BEIGE,
                integerBeigeBackgroundStyle,
                CellColorEnum.GREEN,
                integerGreenBackgroundStyle,
                CellColorEnum.LIGHT_GREEN,
                integerLightGreenBackgroundStyle,
                CellColorEnum.LIGHT_BLUE,
                integerLightBlueBackgroundStyle,
                CellColorEnum.WHITE,
                integerWhiteBackgroundStyle,
                CellColorEnum.WHITE_NO_BORDER,
                integerWhiteNoBorderStyle),
            CellStyleFormatEnum.DOUBLE,
            Map.of(
                CellColorEnum.BLUE,
                doubleBlueBackgroundStyle,
                CellColorEnum.RED,
                doubleRedBackgroundStyle,
                CellColorEnum.YELLOW,
                doubleYellowBackgroundStyle,
                CellColorEnum.ORANGE,
                doubleOrangeBackgroundStyle,
                CellColorEnum.BEIGE,
                doubleBeigeBackgroundStyle,
                CellColorEnum.GREEN,
                doubleGreenBackgroundStyle,
                CellColorEnum.LIGHT_GREEN,
                doubleLightGreenBackgroundStyle,
                CellColorEnum.LIGHT_BLUE,
                doubleLightBlueBackgroundStyle,
                CellColorEnum.WHITE,
                doubleWhiteBackgroundStyle),
            CellStyleFormatEnum.DATE,
            Map.of(
                CellColorEnum.BLUE,
                dateBlueBackgroundStyle,
                CellColorEnum.RED,
                dateRedBackgroundStyle,
                CellColorEnum.YELLOW,
                dateYellowBackgroundStyle,
                CellColorEnum.ORANGE,
                dateOrangeBackgroundStyle,
                CellColorEnum.BEIGE,
                dateBeigeBackgroundStyle,
                CellColorEnum.GREEN,
                dateGreenBackgroundStyle,
                CellColorEnum.LIGHT_GREEN,
                dateLightGreenBackgroundStyle,
                CellColorEnum.LIGHT_BLUE,
                dateLightBlueBackgroundStyle,
                CellColorEnum.WHITE,
                dateWhiteBackgroundStyle));
  }

  public CellStyle retrieveCellStyle(
      CellStyleFormatEnum cellStyleFormatEnum,
      CellColorEnum cellColorEnum,
      HorizontalAlignment alignment) {
    CellStyle style =
        styleMap.getOrDefault(cellStyleFormatEnum, Collections.emptyMap()).get(cellColorEnum);
    style.setAlignment(alignment);
    return style;
  }
}
