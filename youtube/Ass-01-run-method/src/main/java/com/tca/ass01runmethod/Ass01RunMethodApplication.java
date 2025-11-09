package com.tca.ass01runmethod;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Ass01RunMethodApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext container = SpringApplication.run(Ass01RunMethodApplication.class, args);

        Student student = container.getBean(Student.class);

        System.out.println(student);
        System.out.println("Student Name : " + student.getName());

    }

    @Bean
    public Student createObj(){
        return new Student();
    }

}
