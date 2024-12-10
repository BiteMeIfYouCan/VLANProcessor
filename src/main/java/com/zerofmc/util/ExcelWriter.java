package com.zerofmc.util;

import com.zerofmc.model.Subnet;
import com.zerofmc.model.VLAN;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.List;

public class ExcelWriter {

    public static void writeVLANOutput(String filePath, List<VLAN> vlans) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("VLAN Output");

        // 创建表头
        Row header = sheet.createRow(0);
        String[] headers = {"VLAN名称", "VLANID", "管理方式", "管控模板", "自动化分部门", "指定终端划分到部门", "子网类型", "子网", "网关IP"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }

        int rowIdx = 1;
        for (VLAN vlan : vlans) {
            List<Subnet> subnets = vlan.getSubnets();
            if (subnets.isEmpty()) continue;

            // 处理VLAN名称
            String vlanName = generateVLANName(vlan);

            // 管理方式和管控模板
            String management = "远程管理";
            String template = "DF-远程管理";

            // 自动化分部门和指定终端划分到部门逻辑
            String department = vlan.getDepartment();
            String autoDept = "";
            String assignDept = "";

            if (department == null || department.isEmpty()) {
                // 部门为空
                autoDept = "";
                assignDept = "";
            } else if (department.equals("网关设备")) {
                autoDept = "划分到隶属网关的部门";
                assignDept = "";
            } else {
                autoDept = "指定划分部门";
                assignDept = department;
            }

            for (int i = 0; i < subnets.size(); i++) {
                Subnet subnet = subnets.get(i);
                Row row = sheet.createRow(rowIdx++);

                // VLAN名称、VLANID、管理方式、管控模板、自动化分部门、指定终端划分到部门只在第一行填写
                if (i == 0) {
                    row.createCell(0).setCellValue(vlanName);
                    row.createCell(1).setCellValue(vlan.getVlanId());
                    row.createCell(2).setCellValue(management);
                    row.createCell(3).setCellValue(template);
                    row.createCell(4).setCellValue(autoDept);
                    row.createCell(5).setCellValue(assignDept);
                } else {
                    // 其他行VLAN相关单元格留空
                    row.createCell(0).setCellValue("");
                    row.createCell(1).setCellValue("");
                    row.createCell(2).setCellValue("");
                    row.createCell(3).setCellValue("");
                    row.createCell(4).setCellValue("");
                    row.createCell(5).setCellValue("");
                }

                // 子网类型
                row.createCell(6).setCellValue(subnet.getType());

                // 子网
                row.createCell(7).setCellValue(subnet.getCidr());

                // 网关IP
                row.createCell(8).setCellValue(subnet.getGateway());
            }

            // 合并VLAN相关单元格（如果有多个子网）
            if (subnets.size() > 1) {
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - subnets.size(), rowIdx - 1, 0, 0));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - subnets.size(), rowIdx - 1, 1, 1));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - subnets.size(), rowIdx - 1, 2, 2));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - subnets.size(), rowIdx - 1, 3, 3));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - subnets.size(), rowIdx - 1, 4, 4));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIdx - subnets.size(), rowIdx - 1, 5, 5));
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    private static String generateVLANName(VLAN vlan) {
        String dept = vlan.getDepartment();
        if (dept == null || dept.isEmpty()) {
            return "未指定部门_" + vlan.getVlanId();
        }
        // 假设部门名称最后一个/后的文字
        String[] parts = dept.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart + "_" + vlan.getVlanId();
    }
}
