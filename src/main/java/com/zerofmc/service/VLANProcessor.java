package com.zerofmc.service;

import com.zerofmc.model.Subnet;
import com.zerofmc.model.VLAN;
import com.zerofmc.util.ExcelReader;
import com.zerofmc.util.ExcelWriter;
import com.zerofmc.util.SubnetUtilsHelper;
import org.apache.commons.net.util.SubnetUtils;

import java.util.*;
import java.util.stream.Collectors;

public class VLANProcessor {

    public static void process(String vlanFilePath, String exclusionFilePath, String outputFilePath) throws Exception {
        // 读取VLAN文件
        List<VLAN> vlans = ExcelReader.readVLANFile(vlanFilePath);

        // 分离排除的网段（VLANID = -1）
        List<Subnet> exclusionSubnets = vlans.stream()
                .filter(vlan -> vlan.getVlanId() == -1)
                .flatMap(vlan -> vlan.getSubnets().stream())
                .collect(Collectors.toList());

        // 读取第二个排除文件
        List<String> additionalExclusions = ExcelReader.readExclusionFile(exclusionFilePath);
        for (String excl : additionalExclusions) {
            exclusionSubnets.add(new Subnet(excl, null, null));
        }

        // 处理包含VLANID != -1的VLAN
        List<VLAN> includedVlans = vlans.stream()
                .filter(vlan -> vlan.getVlanId() != -1)
                .collect(Collectors.toList());

        // 解析所有需要包含的子网
        Map<Integer, VLAN> vlanMap = new HashMap<>();
        for (VLAN vlan : includedVlans) {
            vlanMap.put(vlan.getVlanId(), vlan);
            List<Subnet> parsedSubnets = parseSubnetEntries(vlan.getSubnets());
            vlan.getSubnets().clear();
            vlan.getSubnets().addAll(parsedSubnets);
        }

        // 解析所有排除的子网
        List<Subnet> parsedExclusions = parseSubnetEntries(exclusionSubnets);

        // 从每个VLAN的子网中排除排除的网段
        for (VLAN vlan : vlanMap.values()) {
            List<Subnet> processedSubnets = new ArrayList<>();
            for (Subnet subnet : vlan.getSubnets()) {
                List<Subnet> subnetsAfterExclusion = excludeSubnetsByRange(subnet, parsedExclusions);
                processedSubnets.addAll(subnetsAfterExclusion);
            }
            vlan.getSubnets().clear();
            vlan.getSubnets().addAll(processedSubnets);
        }

        // 进行子网拆分（如有必要）
        for (VLAN vlan : vlanMap.values()) {
            List<Subnet> expandedSubnets = new ArrayList<>();
            for (Subnet subnet : vlan.getSubnets()) {
                // 检查是否需要拆分
                if (subnet.getOriginalInput().contains("-")) {
                    String[] parts = subnet.getOriginalInput().split("-");
                    String startIP = parts[0].trim();
                    String endIP = parts[1].trim();
                    List<String> cidrs = SubnetUtilsHelper.convertRangeToCIDR(startIP, endIP);
                    for (String cidr : cidrs) {
                        String network = SubnetUtilsHelper.getNetworkAddress(cidr);
                        String gateway = SubnetUtilsHelper.getGateway(cidr);
                        expandedSubnets.add(new Subnet(cidr, network, gateway));
                    }
                } else {
                    // 直接使用CIDR
                    String cidr = subnet.getOriginalInput();
                    String network = SubnetUtilsHelper.getNetworkAddress(cidr);
                    String gateway = SubnetUtilsHelper.getGateway(cidr);
                    expandedSubnets.add(new Subnet(cidr, network, gateway));
                }
            }
            vlan.getSubnets().clear();
            vlan.getSubnets().addAll(expandedSubnets);
        }

        // 设置子网类型
        for (VLAN vlan : vlanMap.values()) {
            List<Subnet> subnets = vlan.getSubnets();
            if (subnets.isEmpty()) continue;
            for (int i = 0; i < subnets.size(); i++) {
                Subnet subnet = subnets.get(i);
                if (i == 0) {
                    subnet.setType("主要子网");
                } else {
                    subnet.setType("扩展子网");
                }
            }
        }

        // 写入输出文件
        ExcelWriter.writeVLANOutput(outputFilePath, new ArrayList<>(vlanMap.values()));
    }

    private static List<Subnet> parseSubnetEntries(List<Subnet> subnetEntries) {
        List<Subnet> parsed = new ArrayList<>();
        for (Subnet entry : subnetEntries) {
            String subnetStr = entry.getOriginalInput();
            if (subnetStr.contains("/")) {
                parsed.add(new Subnet(subnetStr, null, null));
            } else if (subnetStr.contains("-")) {
                parsed.add(new Subnet(subnetStr, null, null));
            }
        }
        return parsed;
    }

    /**
     * 使用范围计算来进行子网排除操作：
     * 1. 将base子网转换为地址范围 [start, end]
     * 2. 将所有排除子网转换为其地址范围 [es, ee]
     * 3. 从 [start, end] 中依次减去排除范围
     * 4. 剩余地址范围再转换回CIDR表示
     */
    private static List<Subnet> excludeSubnetsByRange(Subnet base, List<Subnet> exclusions) {
        // 将base子网转换为范围
        Range baseRange = cidrOrRangeToRange(base.getOriginalInput());
        if (baseRange == null) {
            // 无法解析则直接返回
            return Collections.singletonList(base);
        }

        List<Range> remainRanges = new ArrayList<>();
        remainRanges.add(baseRange);

        // 对每个排除子网进行排除
        for (Subnet excl : exclusions) {
            Range exclRange = cidrOrRangeToRange(excl.getOriginalInput());
            if (exclRange == null) continue;

            remainRanges = subtractRanges(remainRanges, exclRange);
            if (remainRanges.isEmpty()) {
                break;
            }
        }

        // 将剩余范围转换回CIDR列表
        List<Subnet> result = new ArrayList<>();
        for (Range r : remainRanges) {
            List<String> cidrs = SubnetUtilsHelper.convertRangeToCIDR(r.startIP, r.endIP);
            for (String cidr : cidrs) {
                String network = SubnetUtilsHelper.getNetworkAddress(cidr);
                String gateway = SubnetUtilsHelper.getGateway(cidr);
                result.add(new Subnet(cidr, network, gateway));
            }
        }

        return result;
    }

    // 将CIDR或"IP-IP"范围转换为Range
    private static Range cidrOrRangeToRange(String input) {
        try {
            if (input.contains("/")) {
                // CIDR
                SubnetUtils utils = new SubnetUtils(input);
                utils.setInclusiveHostCount(true);
                String low = utils.getInfo().getLowAddress();
                String high = utils.getInfo().getHighAddress();
                return new Range(low, high);
            } else if (input.contains("-")) {
                String[] parts = input.split("-");
                String startIP = parts[0].trim();
                String endIP = parts[1].trim();
                return new Range(startIP, endIP);
            } else {
                // 非法格式
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // 从ranges列表中减去excl范围
    private static List<Range> subtractRanges(List<Range> ranges, Range excl) {
        List<Range> result = new ArrayList<>();
        for (Range r : ranges) {
            // 如果没有重叠，保留原范围
            if (r.end < excl.start || r.start > excl.end) {
                result.add(r);
            } else {
                // 存在重叠，需要裁剪
                // r: [r.start, r.end]
                // excl: [excl.start, excl.end]

                // 剩余部分可能在左边 [r.start, excl.start-1]
                if (r.start < excl.start) {
                    long leftEnd = excl.start - 1;
                    if (leftEnd >= r.start) {
                        result.add(new Range(r.start, leftEnd));
                    }
                }

                // 剩余部分可能在右边 [excl.end+1, r.end]
                if (excl.end < r.end) {
                    long rightStart = excl.end + 1;
                    if (rightStart <= r.end) {
                        result.add(new Range(rightStart, r.end));
                    }
                }
            }
        }
        return result;
    }

    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long res = 0;
        for (String part : parts) {
            res = (res << 8) | Integer.parseInt(part);
        }
        return res;
    }

    private static String longToIP(long val) {
        return ((val >> 24) & 0xFF) + "." +
                ((val >> 16) & 0xFF) + "." +
                ((val >> 8) & 0xFF) + "." +
                (val & 0xFF);
    }

    // 用于表示一个IP范围
    static class Range {
        long start;
        long end;

        Range(String startIP, String endIP) {
            this.start = ipToLong(startIP);
            this.end = ipToLong(endIP);
        }

        Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public Range(long start, long end, boolean dummy) {
            this.start = start;
            this.end = end;
        }
    }
}
