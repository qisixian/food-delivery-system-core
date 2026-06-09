package com.sky;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.entity.Employee;
import com.sky.mapper.EmployeeMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class PageTests {

    @Autowired
    EmployeeMapper employeeMapper;

    @Disabled("Only run manually")
    @Test
    void pageTest() {
        PageHelper.startPage(2, 5);
        List<Employee> all = employeeMapper.pageQuery();

        for (Employee employee : all) {
            System.out.println(employee);
        }

        PageInfo<Employee> pageInfo = new PageInfo<>(all);

        System.out.println(pageInfo);
        for (Employee employee : pageInfo.getList()) {
            System.out.println(employee);
        }
    }
}
