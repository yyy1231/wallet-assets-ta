package com.novax.util;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExcelUtil {
    static Logger logger = Logger.getLogger(ExcelUtil.class);

    public static HSSFCell getCell(String filePath, int sheetIndex, int rowIndex, int cellIndex) {
        InputStream xlsInputStream = null;
        HSSFCell cell = null;
        try {
            // Excel工作簿输入流
            xlsInputStream = ExcelUtil.class.getResourceAsStream(filePath);
            // 构造工作簿对象
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook(Objects.requireNonNull(xlsInputStream));
            // 获取工作簿
            HSSFSheet sheetAt = hssfWorkbook.getSheetAt(sheetIndex);
            // 获取行
            HSSFRow hssfRow = sheetAt.getRow(rowIndex);
            // 获取单元格
            cell = hssfRow.getCell(cellIndex);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (xlsInputStream != null) {
                try {
                    xlsInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cell;
    }


    public static Object[][] readObjDatas(String name, int sheetIndex) {
        List<Object[]> records = new ArrayList<>();
        Object[][] results;

        // Excel工作簿输入流
        InputStream xlsInputStream = ExcelUtil.class.getResourceAsStream(name);
        // 构造工作簿对象
        HSSFWorkbook hssfWorkbook;
        try {
            hssfWorkbook = new HSSFWorkbook(Objects.requireNonNull(xlsInputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 获取工作簿
        HSSFSheet sheetAt = hssfWorkbook.getSheetAt(sheetIndex);

        int lastRowNum = sheetAt.getLastRowNum();
        out:
        for (int i = 1; i <= lastRowNum; i++) {
            // 获取行
            HSSFRow hssfRow = sheetAt.getRow(i);
            String[] fields = new String[hssfRow.getLastCellNum()];
            for (int j = 0; j < hssfRow.getLastCellNum(); j++) {
                // 获取单元格
                HSSFCell cell = hssfRow.getCell(j);
                if (cell == null) {
                    break;
                } else {
                    fields[j] = cell.getStringCellValue();
                }
            }
            for (String field : fields) {
                if (field == null || field.equals("")) {
                    break out;
                }
            }
            records.add(fields);
        }
        // list转换为Object[][]
        results = new Object[records.size()][];
        // 设置二维数组每行的值，每行是一个Object对象
        for (int i = 0; i < records.size(); i++) {
            results[i] = records.get(i);
        }
        return results;
    }

    // 创建带表头的空sheet
    public static void createSheet(String sheetName, List<String> heaList, String filePath) {
        try {
            // 创建sheet页
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet(sheetName);
            // 创建表头 即第一行
            HSSFRow row = sheet.createRow(0);
            for (int i = 0; i < heaList.size(); i++) {
                HSSFCell cell = row.createCell(i);
                cell.setCellValue(heaList.get(i));
            }
            // 写入文件
            File file = new File(filePath);
            // 将文件保存到指定位置
            workbook.write(new FileOutputStream(file));
            logger.info("文件创建成功");
        } catch (IOException e) {
            logger.error("文件创建失败" + e.getMessage());
        }
    }

    // 批量追加
    public static void writeToFileBatch(String filePath, String sheetName, List<List<String>> contentList) {
        try (
                FileInputStream fis = new FileInputStream(filePath);
                HSSFWorkbook workbook = new HSSFWorkbook(fis);
                FileOutputStream fos = new FileOutputStream(filePath)
        ) {
            Sheet sheet = workbook.getSheet(sheetName);
            // 最后一行行号
            int lastRowNum = sheet.getLastRowNum();
            // 创建新的行 追加数据
            for (int i = lastRowNum; i < lastRowNum + contentList.size(); i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < contentList.get(i).size(); j++) {
                    row.createCell(j).setCellValue(contentList.get(i).get(j));
                }
            }
            // 保存Excel文件
            workbook.write(fos);
            logger.info("文件写入成功");
        } catch (IOException e) {
            logger.error("对文件写入数据发生错误:" + e.getMessage());
        }
    }

    // 追加一行
    public static void writeToFile(String filePath, String sheetName, List<String> dataList) {
        try (
                FileInputStream fis = new FileInputStream(filePath);
                HSSFWorkbook workbook = new HSSFWorkbook(fis);
                FileOutputStream fos = new FileOutputStream(filePath)
        ) {
            Sheet sheet = workbook.getSheet(sheetName);
            // 最后一行行号
            int lastRowNum = sheet.getLastRowNum();
            // 创建新的行 追加数据
            Row newRow = sheet.createRow(lastRowNum + 1);
            for (int j = 0; j < dataList.size(); j++) {
                newRow.createCell(j).setCellValue(dataList.get(j));
            }
            // 保存Excel文件
            workbook.write(fos);
            logger.info("文件写入成功");
        } catch (IOException e) {
            logger.error("对文件写入数据发生错误:" + e.getMessage());
        }
    }

    public static boolean updateFileData(String filePath, String sheetName, int rowIndex, int columnIndex, String newValue) {
        try (
                FileInputStream fis = new FileInputStream(filePath);
                HSSFWorkbook workbook = new HSSFWorkbook(fis);
                FileOutputStream fos = new FileOutputStream(filePath)
        ) {
            Sheet sheet = workbook.getSheet(sheetName);
            // 要更新的行的索引 索引从0开始
            Row row = sheet.getRow(rowIndex);
            // 更新单元格值，列索引从0开始
            if (row != null) {
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                // 设置新的单元格值
                cell.setCellValue(newValue);
            }
            // 保存Excel文件
            workbook.write(fos);
            logger.info("file update success");
            return true;
        } catch (IOException e) {
            logger.error("file update fail:" + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        readObjDatas("/testcase/WalletIncomeTest.xls", 3);
    }
}
