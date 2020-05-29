package com.xyoye.dandanplay.bean;

import java.util.List;

/**
 * Created by xyoye on 2020/5/29.
 */

public class PFBScanBean {
    private List<String> ipAddresses;
    private String securityCode;

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(String securityCode) {
        this.securityCode = securityCode;
    }
}
