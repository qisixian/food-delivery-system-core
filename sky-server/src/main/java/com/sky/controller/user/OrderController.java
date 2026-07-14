package com.sky.controller.user;

import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.exception.ResourceNotFoundException;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/submit")
    @Schema(description = "用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @Schema(description = "订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = new OrderPaymentVO();
//        log.info("生成预支付交易单：{}", orderPaymentVO);
        // 模拟支付成功
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }

    @GetMapping("/reminder/{id}")
    @Schema(description = "客户催单")
    public Result<Void> reminder(@PathVariable Long id) {
        orderService.reminder(id);
        return Result.success();
    }

    @GetMapping("/{orderNumber}")
    public Result<OrderVO> get(@PathVariable String orderNumber){
        OrderVO ordersVO = orderService.getByOrderNumber(orderNumber);
        if (!Objects.equals(ordersVO.getUserId(), userContext.get())) {
            throw new ResourceNotFoundException(MessageConstant.ORDER_NOT_FOUND);
        }
        return Result.success(ordersVO);
    }

    @GetMapping("/list")
    public Result<List<OrderVO>> list(){
        Long userId = userContext.get();
        List<OrderVO> ordersVO = orderService.listByUser(userId);
        return Result.success(ordersVO);
    }
}
