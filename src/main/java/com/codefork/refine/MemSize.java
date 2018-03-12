package com.codefork.refine;

import org.ehcache.config.units.MemoryUnit;

public class MemSize {

    private long size;
    private MemoryUnit unit;

    public MemSize(long size, MemoryUnit unit) {
        this.size = size;
        this.unit = unit;
    }

    public long getSize() {
        return size;
    }

    public MemoryUnit getUnit() {
        return unit;
    }

    public static MemSize valueOf(String val) {
        String sizeStr = val.replaceAll("[a-zA-Z]", "");
        String unitStr = val.replaceAll("[0-9]", "");
        return new MemSize(Long.valueOf(sizeStr), MemoryUnit.valueOf(unitStr));
    }
}
