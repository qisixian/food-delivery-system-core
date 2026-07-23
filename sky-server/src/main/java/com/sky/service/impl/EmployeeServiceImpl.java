package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.UserContext;
import com.sky.dto.EmployeeCreateDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.EmployeeUpdateDTO;
import com.sky.entity.Employee;
import com.sky.exception.*;
import com.sky.mapper.EmployeeMapper;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private UserContext userContext;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Value("${sky.employee-default-password}")
    private String employeeDefaultPassword;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        Employee employee = employeeMapper.getByUsername(username);

        if (employee == null) {
            throw new AuthenticationFailedException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //对前端传来的明文密码进行BCrypt加密，再对比
        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new AuthenticationFailedException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus().equals(StatusConstant.DISABLE)) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        return employee;
    }

    @Override
    public String createToken(Employee employee) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        return JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
    }

    /**
     * 员工分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    public PageResult<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery();
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    public void save(EmployeeCreateDTO employeeCreateDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeCreateDTO, employee);
        employee.setStatus(StatusConstant.ENABLE);
        //设置初始密码，需要进行BCrypt加密
        String password = passwordEncoder.encode(employeeDefaultPassword);
        employee.setPassword(password);
        employeeMapper.insert(employee);
    }

    /**
     * 启用/禁用员工账号
     * @param status
     * @param id
     */
    public void changeStatus(Integer status, Long id) {
        if (!status.equals(StatusConstant.ENABLE) && !status.equals(StatusConstant.DISABLE)) {
            throw new InputValidationException("Invalid status value");
        }
        Employee employee = Employee.builder()
                .id(id)
                .status(status)
                .build();
        int rows = employeeMapper.update(employee);
        if (rows == 0) {
            throw new ResourceNotFoundException(MessageConstant.EMPLOYEE_NOT_FOUND);
        }
    }

    public Employee getById(Long id){
        Employee employee = employeeMapper.getById(id);
        if (employee == null) {
            throw new ResourceNotFoundException(MessageConstant.EMPLOYEE_NOT_FOUND);
        }
        employee.setPassword("****");
        return employee;
    }

    public void update(EmployeeUpdateDTO employeeUpdateDTO){
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeUpdateDTO, employee);
        int rows = employeeMapper.update(employee);
        if (rows == 0) {
            throw new ResourceNotFoundException(MessageConstant.EMPLOYEE_NOT_FOUND);
        }
    }
}
