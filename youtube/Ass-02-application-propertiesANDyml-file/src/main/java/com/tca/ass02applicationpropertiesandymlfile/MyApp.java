package com.tca.ass02applicationpropertiesandymlfile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MyApp {

//    @Value("demo") directly set the value form here or use the properties file to get value form there
    @Value("${myapp.name}")
    private String name;

//    @Value("1.0")
    @Value("${myapp.version}")
    private String version;

    @Value("${myapp.description}")
    private String description;

    public MyApp() {
    }

    public MyApp(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
