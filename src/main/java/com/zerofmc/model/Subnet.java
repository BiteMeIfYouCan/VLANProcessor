package com.zerofmc.model;

public class Subnet {
    private String originalInput;
    private String cidr;
    private String gateway;
    private String type; // "主要子网" 或 "扩展子网"

    public Subnet(String originalInput, String cidr, String gateway) {
        this.originalInput = originalInput;
        this.cidr = cidr;
        this.gateway = gateway;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public String getCidr() {
        return cidr;
    }

    public String getGateway() {
        return gateway;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
