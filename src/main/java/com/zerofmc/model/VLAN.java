package com.zerofmc.model;

import java.util.ArrayList;
import java.util.List;

public class VLAN {
    private int vlanId;
    private String department;
    private List<Subnet> subnets;

    public VLAN(int vlanId, String department) {
        this.vlanId = vlanId;
        this.department = department;
        this.subnets = new ArrayList<>();
    }

    public int getVlanId() {
        return vlanId;
    }

    public String getDepartment() {
        return department;
    }

    public List<Subnet> getSubnets() {
        return subnets;
    }

    public void addSubnet(Subnet subnet) {
        this.subnets.add(subnet);
    }
}
