package com.sky.controller.admin;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    OrderService orderService;

    @GetMapping("/conditionSearch")
    public Result<PageResult<Orders>> page(OrdersPageQueryDTO ordersPageQueryDTO){
        PageResult<Orders> pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(){
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    @GetMapping("/details/{id}")
    public Result<OrderVO> getById(@PathVariable Long id){
        OrderVO orderVO = orderService.getById(id);
        return Result.success(orderVO);
    }

    @PutMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.success();
    }

    @PutMapping("/reject")
    public Result<Void> reject(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        orderService.reject(ordersRejectionDTO);
        return Result.success();
    }

    @PutMapping("/deliver/{id}")
    public Result<Void> deliver(@PathVariable Long id) {
        orderService.startDelivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id) {
        orderService.completeDelivery(id);
        return Result.success();
    }

    @PutMapping("/cancel")
    public Result<Void> cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }
}
