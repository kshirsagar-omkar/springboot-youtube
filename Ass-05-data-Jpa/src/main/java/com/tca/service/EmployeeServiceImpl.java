package com.tca.service;

import com.tca.entities.Employee;
import com.tca.exception.SalaryLessException;
import com.tca.repository.EmployeeRepository;
import com.tca.response.EmployeeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class EmployeeServiceImpl implements EmployeeService{


    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public EmployeeResponse save(Employee employee) {
        if (employee != null && employee.getSalary() > 50000) {
            employeeRepository.save(employee);
            return new EmployeeResponse(employee, "Employee Saved Successfully!!");
        }
        else {
            return new EmployeeResponse(null, "Salary can't be less than 50000");
//            throw new SalaryLessException("Salary can't be less than 50000");
        }
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }


}
