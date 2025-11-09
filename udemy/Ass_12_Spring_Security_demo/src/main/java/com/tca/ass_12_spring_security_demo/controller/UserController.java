package com.tca.ass_12_spring_security_demo.controller;


import com.tca.ass_12_spring_security_demo.entities.User;
import com.tca.ass_12_spring_security_demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    UserService service;

    @PostMapping("register")
    public User register(@RequestBody User user){

        return service.saveUser(user);
    }

}
