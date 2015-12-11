package com.codefork.refine.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class DebugController {

    @RequestMapping("/debug")
    public String debug() {
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMB = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        return "memory usage=" + usedMB + " max=" + maxMB;
    }

}