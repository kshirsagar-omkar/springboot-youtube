package com.tca.service;

import com.tca.entities.Employee;
import com.tca.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    public abstract EmployeeResponse save(Employee employee);
    public abstract List<Employee> findAll();
    public abstract EmployeeResponse findById(Integer id);

}
