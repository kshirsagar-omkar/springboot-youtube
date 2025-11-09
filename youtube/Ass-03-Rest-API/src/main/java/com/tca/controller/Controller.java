package com.tca.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Controller {


    @GetMapping("/hi")
    public String home(){
        return "Hello everyone";
    }


    @GetMapping("/iplteams")
    public String[] getIPLTeams(){

        String []str = {"MI","RCB","CSK"};

        return str;
    }



    @GetMapping("/greet/{entredName}")
    public String greet(@PathVariable("entredName") String name){

        String str = "Hello ";

        if(name != null){
            str += name;
        }
        else{
            str += "World";
        }

        return str;
    }



}
