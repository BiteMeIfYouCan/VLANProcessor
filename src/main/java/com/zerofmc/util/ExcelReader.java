package com.zerofmc.util;

import com.zerofmc.model.Subnet;
import com.zerofmc.model.VLAN;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class ExcelReader {

    public static List<VLAN> readVLANFile(String filePath) throws Exception {
        List<VLAN> vlans = new ArrayList<>();
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();

            // 读取表头
            if (!iterator.hasNext()) {
                throw new Exception("VLAN文件为空");
            }
            Row header = iterator.next();
            Map<String, Integer> headerMap = getHeaderMap(header);

            while (iterator.hasNext()) {
                Row row = iterator.next();
                Cell vlanIdCell = row.getCell(headerMap.get("VLANID"));
                if (vlanIdCell == null || vlanIdCell.getCellType() != CellType.NUMERIC) {
                    continue; // 跳过无效行
                }
                int vlanId = (int) vlanIdCell.getNumericCellValue();
                Cell subnetCell = row.getCell(headerMap.get("子网"));
                if (subnetCell == null || subnetCell.getCellType() != CellType.STRING) {
                    continue; // 跳过无效行
                }
                String subnet = subnetCell.getStringCellValue().trim();
                Cell deptCell = row.getCell(headerMap.get("部门"));
                String department = (deptCell != null && deptCell.getCellType() == CellType.STRING) ? deptCell.getStringCellValue().trim() : "";

                VLAN vlan = new VLAN(vlanId, department);
                vlan.addSubnet(new Subnet(subnet, null, null)); // cidr and gateway will be processed later
                vlans.add(vlan);
            }
        }
        return vlans;
    }

    public static List<String> readExclusionFile(String filePath) throws Exception {
        List<String> exclusions = new ArrayList<>();
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();

            // 读取表头
            if (!iterator.hasNext()) {
                throw new Exception("排除文件为空");
            }
            Row header = iterator.next();
            int subnetIdx = getHeaderMap(header).get("子网");

            while (iterator.hasNext()) {
                Row row = iterator.next();
                Cell cell = row.getCell(subnetIdx);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    exclusions.add(cell.getStringCellValue().trim());
                }
            }
        }
        return exclusions;
    }

    private static Map<String, Integer> getHeaderMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                map.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
        }
        return map;
    }
}
