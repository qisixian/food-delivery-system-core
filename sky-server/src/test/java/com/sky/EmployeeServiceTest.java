package com.sky;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeCreateDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.EmployeeUpdateDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AuthenticationFailedException;
import com.sky.exception.InputValidationException;
import com.sky.exception.ResourceNotFoundException;
import com.sky.mapper.EmployeeMapper;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.service.impl.EmployeeServiceImpl;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final String ADMIN_SECRET_KEY = "12345678901234567890123456789012";
    private static final long ADMIN_TTL = 3_600_000L;
    private static final String DEFAULT_PASSWORD = "123456";

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAdminSecretKey(ADMIN_SECRET_KEY);
        jwtProperties.setAdminTtl(ADMIN_TTL);

        employeeService = new EmployeeServiceImpl();
        ReflectionTestUtils.setField(employeeService, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(employeeService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(employeeService, "jwtProperties", jwtProperties);
        ReflectionTestUtils.setField(employeeService, "employeeDefaultPassword", DEFAULT_PASSWORD);
    }

    @AfterEach
    void tearDown() {
        PageHelper.clearPage();
    }

    @Test
    void login_whenCredentialsAreValidAndAccountEnabled_thenReturnsEmployee() {
        EmployeeLoginDTO request = buildLoginDTO();
        Employee employee = buildEmployee(StatusConstant.ENABLE);

        when(employeeMapper.getByUsername(request.getUsername())).thenReturn(employee);
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(true);

        Employee result = employeeService.login(request);

        assertSame(employee, result);
        verify(employeeMapper).getByUsername(request.getUsername());
        verify(passwordEncoder).matches(request.getPassword(), employee.getPassword());
    }

    @Test
    void login_whenEmployeeDoesNotExist_thenThrowsAuthenticationFailedException() {
        EmployeeLoginDTO request = buildLoginDTO();
        when(employeeMapper.getByUsername(request.getUsername())).thenReturn(null);

        AuthenticationFailedException exception = assertThrows(
                AuthenticationFailedException.class,
                () -> employeeService.login(request)
        );

        assertEquals(MessageConstant.ACCOUNT_NOT_FOUND, exception.getMessage());
        verify(employeeMapper).getByUsername(request.getUsername());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_whenPasswordDoesNotMatch_thenThrowsAuthenticationFailedException() {
        EmployeeLoginDTO request = buildLoginDTO();
        Employee employee = buildEmployee(StatusConstant.ENABLE);

        when(employeeMapper.getByUsername(request.getUsername())).thenReturn(employee);
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(false);

        AuthenticationFailedException exception = assertThrows(
                AuthenticationFailedException.class,
                () -> employeeService.login(request)
        );

        assertEquals(MessageConstant.PASSWORD_ERROR, exception.getMessage());
        verify(employeeMapper).getByUsername(request.getUsername());
        verify(passwordEncoder).matches(request.getPassword(), employee.getPassword());
    }

    @Test
    void login_whenAccountIsDisabled_thenThrowsAccountLockedException() {
        EmployeeLoginDTO request = buildLoginDTO();
        Employee employee = buildEmployee(StatusConstant.DISABLE);

        when(employeeMapper.getByUsername(request.getUsername())).thenReturn(employee);
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(true);

        AccountLockedException exception = assertThrows(
                AccountLockedException.class,
                () -> employeeService.login(request)
        );

        assertEquals(MessageConstant.ACCOUNT_LOCKED, exception.getMessage());
        verify(employeeMapper).getByUsername(request.getUsername());
        verify(passwordEncoder).matches(request.getPassword(), employee.getPassword());
    }

    @Test
    void createToken_whenEmployeeProvided_thenReturnsJwtContainingEmployeeId() {
        Employee employee = Employee.builder()
                .id(100L)
                .build();

        String token = employeeService.createToken(employee);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = JwtUtil.parseJWT(ADMIN_SECRET_KEY, token);
        assertEquals(100L, ((Number) claims.get(JwtClaimsConstant.EMP_ID)).longValue());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void pageQuery_whenEmployeesExist_thenReturnsPageResult() {
        EmployeePageQueryDTO request = new EmployeePageQueryDTO();
        request.setPage(2);
        request.setPageSize(5);

        Employee first = Employee.builder().id(1L).name("Alice").build();
        Employee second = Employee.builder().id(2L).name("Bob").build();
        Page<Employee> page = new Page<>(request.getPage(), request.getPageSize());
        page.addAll(List.of(first, second));
        page.setTotal(12);
        when(employeeMapper.pageQuery()).thenReturn(page);

        PageResult<Employee> result = employeeService.pageQuery(request);

        assertEquals(12, result.getTotal());
        assertEquals(List.of(first, second), result.getRecords());
        verify(employeeMapper).pageQuery();
    }

    @Test
    void save_whenRequestIsValid_thenEncodesDefaultPasswordAndInsertsEmployee() {
        EmployeeCreateDTO request = buildCreateDTO();
        when(passwordEncoder.encode(DEFAULT_PASSWORD)).thenReturn("encoded-password");

        employeeService.save(request);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(employeeCaptor.capture());

        Employee insertedEmployee = employeeCaptor.getValue();
        assertEquals(request.getUsername(), insertedEmployee.getUsername());
        assertEquals(request.getName(), insertedEmployee.getName());
        assertEquals(request.getPhone(), insertedEmployee.getPhone());
        assertEquals(request.getSex(), insertedEmployee.getSex());
        assertEquals(request.getIdNumber(), insertedEmployee.getIdNumber());
        assertEquals(StatusConstant.ENABLE, insertedEmployee.getStatus());
        assertEquals("encoded-password", insertedEmployee.getPassword());
        verify(passwordEncoder).encode(DEFAULT_PASSWORD);
    }

    @Test
    void changeStatus_whenStatusIsEnableAndEmployeeExists_thenUpdatesEmployeeStatus() {
        when(employeeMapper.update(any(Employee.class))).thenReturn(1);

        employeeService.changeStatus(StatusConstant.ENABLE, 10L);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(employeeCaptor.capture());

        Employee updatedEmployee = employeeCaptor.getValue();
        assertEquals(10L, updatedEmployee.getId());
        assertEquals(StatusConstant.ENABLE, updatedEmployee.getStatus());
    }

    @Test
    void changeStatus_whenStatusIsDisableAndEmployeeExists_thenUpdatesEmployeeStatus() {
        when(employeeMapper.update(any(Employee.class))).thenReturn(1);

        employeeService.changeStatus(StatusConstant.DISABLE, 10L);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(employeeCaptor.capture());

        Employee updatedEmployee = employeeCaptor.getValue();
        assertEquals(10L, updatedEmployee.getId());
        assertEquals(StatusConstant.DISABLE, updatedEmployee.getStatus());
    }

    @Test
    void changeStatus_whenStatusIsInvalid_thenThrowsInputValidationException() {
        InputValidationException exception = assertThrows(
                InputValidationException.class,
                () -> employeeService.changeStatus(99, 10L)
        );

        assertEquals("Invalid status value", exception.getMessage());
        verify(employeeMapper, never()).update(any(Employee.class));
    }

    @Test
    void changeStatus_whenEmployeeDoesNotExist_thenThrowsResourceNotFoundException() {
        when(employeeMapper.update(any(Employee.class))).thenReturn(0);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.changeStatus(StatusConstant.ENABLE, 10L)
        );

        assertEquals(MessageConstant.EMPLOYEE_NOT_FOUND, exception.getMessage());
        verify(employeeMapper).update(any(Employee.class));
    }

    @Test
    void getById_whenEmployeeExists_thenMasksPasswordAndReturnsEmployee() {
        Employee employee = buildEmployee(StatusConstant.ENABLE);
        String originalPassword = employee.getPassword();

        when(employeeMapper.getById(employee.getId())).thenReturn(employee);

        Employee result = employeeService.getById(employee.getId());

        assertSame(employee, result);
        assertNotEquals(originalPassword, result.getPassword());
        verify(employeeMapper).getById(employee.getId());
    }

    @Test
    void getById_whenEmployeeDoesNotExist_thenThrowsResourceNotFoundException() {
        when(employeeMapper.getById(10L)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.getById(10L)
        );

        assertEquals(MessageConstant.EMPLOYEE_NOT_FOUND, exception.getMessage());
        verify(employeeMapper).getById(10L);
    }

    @Test
    void update_whenEmployeeExists_thenUpdatesEmployee() {
        EmployeeUpdateDTO request = buildUpdateDTO();
        when(employeeMapper.update(any(Employee.class))).thenReturn(1);

        employeeService.update(request);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(employeeCaptor.capture());

        Employee updatedEmployee = employeeCaptor.getValue();
        assertEquals(request.getId(), updatedEmployee.getId());
        assertEquals(request.getUsername(), updatedEmployee.getUsername());
        assertEquals(request.getName(), updatedEmployee.getName());
        assertEquals(request.getPhone(), updatedEmployee.getPhone());
    }

    @Test
    void update_whenEmployeeDoesNotExist_thenThrowsResourceNotFoundException() {
        EmployeeUpdateDTO request = buildUpdateDTO();
        when(employeeMapper.update(any(Employee.class))).thenReturn(0);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.update(request)
        );

        assertEquals(MessageConstant.EMPLOYEE_NOT_FOUND, exception.getMessage());
        verify(employeeMapper).update(any(Employee.class));
    }

    private EmployeeLoginDTO buildLoginDTO() {
        EmployeeLoginDTO request = new EmployeeLoginDTO();
        request.setUsername("alice");
        request.setPassword("plain-password");
        return request;
    }

    private EmployeeCreateDTO buildCreateDTO() {
        EmployeeCreateDTO request = new EmployeeCreateDTO();
        request.setUsername("alice");
        request.setName("Alice");
        request.setPhone("13800000000");
        request.setSex("F");
        request.setIdNumber("110101199001011234");
        return request;
    }

    private EmployeeUpdateDTO buildUpdateDTO() {
        EmployeeUpdateDTO request = new EmployeeUpdateDTO();
        request.setId(10L);
        request.setUsername("alice-updated");
        request.setName("Alice Updated");
        request.setPhone("13900000000");
        request.setSex("F");
        request.setIdNumber("110101199001019999");
        return request;
    }

    private Employee buildEmployee(Integer status) {
        return Employee.builder()
                .id(10L)
                .username("alice")
                .name("Alice")
                .password("encoded-password")
                .phone("13800000000")
                .sex("F")
                .idNumber("110101199001011234")
                .status(status)
                .build();
    }
}
