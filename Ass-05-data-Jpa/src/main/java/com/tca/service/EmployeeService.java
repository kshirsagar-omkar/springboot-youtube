package com.tca.service;

import com.tca.entities.Employee;

import java.util.List;

public interface EmployeeService {

    public abstract Employee save(Employee employee);
    public abstract List<Employee> findAll();

}
