package com.zerofmc.util;

import org.apache.commons.net.util.SubnetUtils;

import java.util.ArrayList;
import java.util.List;

public class SubnetUtilsHelper {

    // 将CIDR表示转换为网络地址和网关
    public static String getNetworkAddress(String cidr) {
        SubnetUtils utils = new SubnetUtils(cidr);
        utils.setInclusiveHostCount(true);
        return utils.getInfo().getNetworkAddress() + "/" + utils.getInfo().getCidrSignature().split("/")[1];
    }

    // 将CIDR表示转换为网关地址（通常为网络地址加1）
    public static String getGateway(String cidr) {
        SubnetUtils utils = new SubnetUtils(cidr);
        utils.setInclusiveHostCount(true);
        long gatewayLong = ipToLong(utils.getInfo().getNetworkAddress()) + 1;
        return longToIP(gatewayLong);
    }

    // 将IP范围转换为多个CIDR
    public static List<String> convertRangeToCIDR(String startIP, String endIP) {
        List<String> cidrs = new ArrayList<>();
        long start = ipToLong(startIP);
        long end = ipToLong(endIP);

        while (start <= end) {
            // 找到从start开始的最大块
            byte maxSize = 32;
            while (maxSize > 0) {
                long mask = CIDR_MASK(maxSize - 1);
                if ((start & mask) != 0) {
                    break;
                }
                maxSize--;
            }

            // 计算从start到end的最大块大小
            long maxDiff = (long) Math.floor(Math.log(end - start + 1) / Math.log(2));
            byte maxBlock = (byte) Math.min(maxSize, 32 - (int) maxDiff);

            String cidr = longToIP(start) + "/" + maxBlock;
            cidrs.add(cidr);
            start += 1 << (32 - maxBlock);
        }

        return cidrs;
    }

    private static long ipToLong(String ipAddress) {
        String[] ip = ipAddress.split("\\.");
        long result = 0;
        for (String part : ip) {
            result = (result << 8) | Integer.parseInt(part);
        }
        return result;
    }

    private static String longToIP(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }

    private static long CIDR_MASK(int bits) {
        return ~((1L << (32 - bits)) - 1);
    }
}
