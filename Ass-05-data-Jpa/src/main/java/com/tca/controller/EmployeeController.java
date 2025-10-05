package com.tca.controller;


import com.tca.entities.Employee;
import com.tca.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EmployeeController {


    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/api/employees")
    public List<Employee> findAll(){
        return employeeService.findAll();
    }

    @PostMapping("/api/employee")
    public Employee save(@RequestBody Employee employee){
        return employeeService.save(employee);
    }



}
