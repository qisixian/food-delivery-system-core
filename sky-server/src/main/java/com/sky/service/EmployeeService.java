package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    Employee login(EmployeeLoginDTO employeeLoginDTO);

    String createToken(Employee employee);

    PageResult<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    void save(EmployeeDTO employeeDTO);

    void changeStatus(Integer status, Long id);

    Employee getById(Long id);

    void update(EmployeeDTO employeeDTO);
}
