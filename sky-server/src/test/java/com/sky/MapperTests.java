package com.sky;

import com.sky.mapper.EmployeeMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MapperTests {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Disabled("Only run manually")
    @Test
    void testMapper() {
        System.out.println(employeeMapper.getById(1L));
    }
}
