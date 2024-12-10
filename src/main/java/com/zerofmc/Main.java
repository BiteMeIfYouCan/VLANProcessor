package com.zerofmc;

import com.zerofmc.service.VLANProcessor;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("使用方法: java -jar VLANProcessor.jar <VLAN输入文件> <排除输入文件> <输出文件>");
            return;
        }

        String vlanInput = args[0];
        String exclusionInput = args[1];
        String outputFile = args[2];

        try {
            VLANProcessor.process(vlanInput, exclusionInput, outputFile);
            System.out.println("处理完成，输出文件: " + outputFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理过程中出错: " + e.getMessage());
        }
    }
}
