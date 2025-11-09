package com.tca.controller;


import com.tca.entities.Employee;
import com.tca.response.EmployeeResponse;
import com.tca.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public EmployeeResponse save(@RequestBody Employee employee){
        return employeeService.save(employee);
    }

    @GetMapping("/api/employee/{id}")
    public EmployeeResponse findById(@PathVariable Integer id){
        return employeeService.findById(id);
    }

}
