package com.codefork.refine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Controller
@RequestMapping("/version")
public class VersionController {

    @RequestMapping(value = "", produces = "text/plain")
    @ResponseBody
    public String getVersion() {
        String sha = null;
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while(resources.hasMoreElements() && sha == null) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                Attributes attr = manifest.getMainAttributes();
                sha = attr.getValue("Git-SHA-1");
            }
        } catch (IOException ioe) {
            System.out.println("error getting resources from class loader");
        }

        sha = sha != null ? sha : "SHA not found";

        return getClass().getPackage().getImplementationVersion() + "\n" + sha;
    }
}
