package com.tca.service;

import com.tca.entities.Employee;
import com.tca.exception.SalaryLessException;
import com.tca.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class EmployeeServiceImpl implements EmployeeService{


    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee save(Employee employee) {
        if (employee != null && employee.getSalary() > 50000)
            return employeeRepository.save(employee);
        else {
            throw new SalaryLessException("Salary can't be less than 50000");
        }
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }


}
