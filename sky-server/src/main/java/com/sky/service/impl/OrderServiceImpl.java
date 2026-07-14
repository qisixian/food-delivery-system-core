package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.LogFields;
import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.BusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
//import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserContext userContext;

    @Autowired
    private Clock clock;

//    @Autowired
//    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理各种业务异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new BusinessException(MessageConstant.ORDER_DELIVERY_ADDRESS_REQUIRED);
        }
        Long userId = userContext.get();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new BusinessException(MessageConstant.ORDER_SHOPPING_CART_REQUIRED);
        }

        // 插入订单表
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now(clock));
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orderMapper.insert(orders);

        // 插入订单明细表
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        log.atInfo()
            .addKeyValue(LogFields.ORDER_ID, orders.getId())
            .log("Submit order success");

        // 返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = userContext.get();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new BusinessException(MessageConstant.ORDER_ALREADY_PAID);
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .payStatus(Orders.PAID)
                .status(Orders.TO_BE_CONFIRMED)
                .checkoutTime(LocalDateTime.now(clock))
                .build();

        orderMapper.update(orders);

        log.atInfo()
            .addKeyValue(LogFields.ORDER_ID, ordersDB.getId())
            .log("Payment success");

        // 通过WebSocket服务器给管理员端发送来单通知
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); // 1表示来单通知 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        String message = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(message);
    }

    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        // TODO: 这里先mock掉，不查询订单
//        Orders ordersDB = orderMapper.getById(id);
//        if (ordersDB == null) {
//            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
//        }
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); // 1表示来单通知 2表示客户催单
        map.put("orderId", id);
//        map.put("content", "订单号：" + ordersDB.getNumber());
        map.put("content", "订单号：" + id);
        String message = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public List<OrderVO> listByUser(Long userId) {
        List<Orders> ordersList = orderMapper.listByUserId(userId);
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : ordersList) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            List<OrderDetail> details = orderDetailMapper.listByOrderId(orders.getId());
            orderVO.setOrderDetailList(details);
            orderVOList.add(orderVO);
        }
        return orderVOList;
    }

    @Override
    public OrderVO getByOrderNumber(String orderNumber) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(ordersDB, orderVO);
        List<OrderDetail> details = orderDetailMapper.listByOrderId(ordersDB.getId());
        List<String> names = new ArrayList<>();
        for (OrderDetail detail : details) {
            names.add(detail.getName());
        }
        orderVO.setOrderDetailList(details);
        orderVO.setOrderDishes(String.join(", ", names));
        return orderVO;
    }

    @Override
    public OrderVO getById(Long id) {
        // 根据订单id查询订单
        Orders ordersDB = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(ordersDB, orderVO);
        List<OrderDetail> details = orderDetailMapper.listByOrderId(id);
        List<String> names = new ArrayList<>();
        for (OrderDetail detail : details) {
            names.add(detail.getName());
        }
        orderVO.setOrderDetailList(details);
        orderVO.setOrderDishes(String.join(", ", names));
        return orderVO;
    }

    @Override
    public PageResult<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = new Orders();
        order.setId(ordersConfirmDTO.getId());
        order.setStatus(Orders.CONFIRMED);
        orderMapper.update(order);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersRejectionDTO, order);
        order.setStatus(Orders.CANCELLED);
        orderMapper.update(order);
    }

    @Override
    public void delivery(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(order);
    }

    @Override
    public void complete(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(Orders.COMPLETED);
        orderMapper.update(order);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersCancelDTO, order);
        order.setStatus(Orders.CANCELLED);
        orderMapper.update(order);
    }


    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = orderMapper.statistics();
        return orderStatisticsVO;
    }
}
