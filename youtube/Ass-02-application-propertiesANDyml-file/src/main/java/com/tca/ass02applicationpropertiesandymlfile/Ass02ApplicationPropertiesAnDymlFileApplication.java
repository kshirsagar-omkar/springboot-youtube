package com.tca.ass02applicationpropertiesandymlfile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Ass02ApplicationPropertiesAnDymlFileApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext container = SpringApplication.run(Ass02ApplicationPropertiesAnDymlFileApplication.class, args);


        MyApp myApp = container.getBean(MyApp.class);

        System.out.println(myApp.getName());


    }

}
