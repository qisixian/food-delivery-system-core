package com.sky;

import com.github.pagehelper.Page;
import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.BusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.impl.OrderServiceImpl;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long ADDRESS_BOOK_ID = 20L;
    private static final Long ORDER_ID = 30L;
    private static final LocalDateTime ORDER_TIME = LocalDateTime.of(2026, Month.JULY, 5, 12, 30, 0);

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private AddressBookMapper addressBookMapper;

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private UserContext userContext;

    @Mock
    private Clock clock;

    @Mock
    private WebSocketServer webSocketServer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void submitOrder_whenValidRequest_thenCreateOrderSuccessfully() {
        OrdersSubmitDTO request = buildSubmitDTO();
        AddressBook addressBook = buildAddressBook();
        ShoppingCart dishCartItem = buildDishCartItem(1L, "宫保鸡丁", BigDecimal.valueOf(18.50), 2);
        ShoppingCart setmealCartItem = buildSetmealCartItem(2L, "双人套餐", BigDecimal.valueOf(58.00), 1);


        when(addressBookMapper.getById(ADDRESS_BOOK_ID)).thenReturn(addressBook);
        when(userContext.get()).thenReturn(USER_ID);
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(List.of(dishCartItem, setmealCartItem));

        stubFixedClock();
        stubOrderInsertGeneratedId();

        OrderSubmitVO result = orderService.submitOrder(request);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.getId());
        assertEquals(request.getAmount(), result.getOrderAmount());
        assertEquals(ORDER_TIME, result.getOrderTime());
        assertNotNull(result.getOrderNumber());
        assertTrue(result.getOrderNumber().matches("\\d+"));

        ArgumentCaptor<ShoppingCart> cartQueryCaptor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).list(cartQueryCaptor.capture());
        assertEquals(USER_ID, cartQueryCaptor.getValue().getUserId());

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Orders insertedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, orderCaptor.getValue().getId());
        assertEquals(Orders.PENDING_PAYMENT, insertedOrder.getStatus());
        assertEquals(Orders.UN_PAID, insertedOrder.getPayStatus());
        assertEquals(USER_ID, insertedOrder.getUserId());
        assertEquals(request.getAddressBookId(), insertedOrder.getAddressBookId());
        assertEquals(request.getAmount(), insertedOrder.getAmount());
        assertEquals(ORDER_TIME, insertedOrder.getOrderTime());

        ArgumentCaptor<List<OrderDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderDetailMapper).insertBatch(detailsCaptor.capture());
        List<OrderDetail> insertedDetails = detailsCaptor.getValue();
        assertEquals(2, insertedDetails.size());
        assertOrderDetailMatchesCart(insertedDetails.get(0), dishCartItem);
        assertOrderDetailMatchesCart(insertedDetails.get(1), setmealCartItem);

        verify(shoppingCartMapper).deleteByUserId(USER_ID);
    }

    @Test
    void submitOrder_whenAddressBookNotFound_thenThrowAddressBookBusinessException() {
        OrdersSubmitDTO request = buildSubmitDTO();
        when(addressBookMapper.getById(ADDRESS_BOOK_ID)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.submitOrder(request)
        );

        assertEquals(MessageConstant.ORDER_DELIVERY_ADDRESS_REQUIRED, exception.getMessage());
        verify(addressBookMapper).getById(ADDRESS_BOOK_ID);
        verifyNoInteractions(userContext, shoppingCartMapper, orderMapper, orderDetailMapper);
    }

    @Test
    void submitOrder_whenShoppingCartListIsNull_thenThrowShoppingCartBusinessException() {
        OrdersSubmitDTO request = buildSubmitDTO();
        when(addressBookMapper.getById(ADDRESS_BOOK_ID)).thenReturn(buildAddressBook());
        when(userContext.get()).thenReturn(USER_ID);
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.submitOrder(request)
        );

        assertEquals(MessageConstant.ORDER_SHOPPING_CART_REQUIRED, exception.getMessage());
        verify(orderMapper, never()).insert(any(Orders.class));
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(shoppingCartMapper, never()).deleteByUserId(any());
    }

    @Test
    void submitOrder_whenShoppingCartListIsEmpty_thenThrowShoppingCartBusinessException() {
        OrdersSubmitDTO request = buildSubmitDTO();
        when(addressBookMapper.getById(ADDRESS_BOOK_ID)).thenReturn(buildAddressBook());
        when(userContext.get()).thenReturn(USER_ID);
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.submitOrder(request)
        );

        assertEquals(MessageConstant.ORDER_SHOPPING_CART_REQUIRED, exception.getMessage());
        verify(orderMapper, never()).insert(any(Orders.class));
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(shoppingCartMapper, never()).deleteByUserId(any());
    }

    @Test
    void paySuccess_whenOrderExists_thenUpdateOrderAndNotifyClients() {
        String orderNumber = "ORD123";
        Orders existingOrder = Orders.builder()
                .id(ORDER_ID)
                .number(orderNumber)
                .build();
        when(orderMapper.getByNumber(orderNumber)).thenReturn(existingOrder);
        stubFixedClock();

        orderService.paySuccess(orderNumber);

        verify(orderMapper).getByNumber(orderNumber);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(orderCaptor.capture());
        Orders updatedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, updatedOrder.getId());
        assertEquals(Orders.PAID, updatedOrder.getPayStatus());
        assertEquals(Orders.TO_BE_CONFIRMED, updatedOrder.getStatus());
        assertEquals(ORDER_TIME, updatedOrder.getCheckoutTime());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketServer).sendToAllClient(messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("\"type\":1"));
        assertTrue(message.contains("\"orderId\":30"));
        assertTrue(message.contains("订单号：ORD123"));
    }

    @Test
    void reminder_whenOrderIdProvided_thenNotifyClients() {
        orderService.reminder(ORDER_ID);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketServer).sendToAllClient(messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("\"type\":2"));
        assertTrue(message.contains("\"orderId\":30"));
        assertTrue(message.contains("订单号：30"));
    }

    @Test
    void listByUser_whenUserHasOrders_thenReturnOrdersWithDetails() {
        Orders firstOrder = buildOrder(100L, "ORD100");
        Orders secondOrder = buildOrder(101L, "ORD101");
        List<OrderDetail> firstOrderDetails = List.of(buildOrderDetail(1L, 100L, "宫保鸡丁"));
        List<OrderDetail> secondOrderDetails = List.of(buildOrderDetail(2L, 101L, "米饭"));

        when(orderMapper.listByUserId(USER_ID)).thenReturn(List.of(firstOrder, secondOrder));
        when(orderDetailMapper.listByOrderId(100L)).thenReturn(firstOrderDetails);
        when(orderDetailMapper.listByOrderId(101L)).thenReturn(secondOrderDetails);

        List<OrderVO> result = orderService.listByUser(USER_ID);

        verify(orderMapper).listByUserId(USER_ID);
        verify(orderDetailMapper).listByOrderId(100L);
        verify(orderDetailMapper).listByOrderId(101L);
        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals(firstOrderDetails, result.get(0).getOrderDetailList());
        assertEquals(101L, result.get(1).getId());
        assertEquals(secondOrderDetails, result.get(1).getOrderDetailList());
    }

    @Test
    void getByOrderNumber_whenOrderExists_thenReturnOrderWithDetailsAndDishNames() {
        String orderNumber = "ORD123";
        Orders order = buildOrder(ORDER_ID, orderNumber);
        List<OrderDetail> details = List.of(
                buildOrderDetail(1L, ORDER_ID, "宫保鸡丁"),
                buildOrderDetail(2L, ORDER_ID, "米饭")
        );
        when(orderMapper.getByNumber(orderNumber)).thenReturn(order);
        when(orderDetailMapper.listByOrderId(ORDER_ID)).thenReturn(details);

        OrderVO result = orderService.getByOrderNumber(orderNumber);

        verify(orderMapper).getByNumber(orderNumber);
        verify(orderDetailMapper).listByOrderId(ORDER_ID);
        assertEquals(ORDER_ID, result.getId());
        assertEquals(orderNumber, result.getNumber());
        assertEquals(details, result.getOrderDetailList());
        assertEquals("宫保鸡丁, 米饭", result.getOrderDishes());
    }

    @Test
    void getById_whenOrderExists_thenReturnOrderWithDetailsAndDishNames() {
        Orders order = buildOrder(ORDER_ID, "ORD123");
        List<OrderDetail> details = List.of(
                buildOrderDetail(1L, ORDER_ID, "牛肉面"),
                buildOrderDetail(2L, ORDER_ID, "豆浆")
        );
        when(orderMapper.getById(ORDER_ID)).thenReturn(order);
        when(orderDetailMapper.listByOrderId(ORDER_ID)).thenReturn(details);

        OrderVO result = orderService.getById(ORDER_ID);

        verify(orderMapper).getById(ORDER_ID);
        verify(orderDetailMapper).listByOrderId(ORDER_ID);
        assertEquals(ORDER_ID, result.getId());
        assertEquals("ORD123", result.getNumber());
        assertEquals(details, result.getOrderDetailList());
        assertEquals("牛肉面, 豆浆", result.getOrderDishes());
    }

    @Test
    void pageQuery_whenOrdersExist_thenReturnPageResult() {
        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);
        Page<Orders> page = new Page<>(1, 10);
        page.setTotal(2);
        page.addAll(List.of(
                buildOrder(1L, "ORD001"),
                buildOrder(2L, "ORD002")
        ));
        when(orderMapper.pageQuery(queryDTO)).thenReturn(page);

        PageResult<Orders> result = orderService.pageQuery(queryDTO);

        verify(orderMapper).pageQuery(queryDTO);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals(1L, result.getRecords().get(0).getId());
        assertEquals(2L, result.getRecords().get(1).getId());
    }

    @Test
    void confirm_whenOrderConfirmDtoProvided_thenUpdateOrderStatusToConfirmed() {
        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(ORDER_ID);

        orderService.confirm(dto);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(orderCaptor.capture());
        Orders updatedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, updatedOrder.getId());
        assertEquals(Orders.CONFIRMED, updatedOrder.getStatus());
    }

    @Test
    void rejection_whenRejectionDtoProvided_thenUpdateOrderStatusToCancelled() {
        OrdersRejectionDTO dto = new OrdersRejectionDTO();
        dto.setId(ORDER_ID);
        dto.setRejectionReason("库存不足");

        orderService.rejection(dto);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(orderCaptor.capture());
        Orders updatedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, updatedOrder.getId());
        assertEquals(Orders.CANCELLED, updatedOrder.getStatus());
        assertEquals("库存不足", updatedOrder.getRejectionReason());
    }

    @Test
    void delivery_whenOrderIdProvided_thenUpdateOrderStatusToDeliveryInProgress() {
        orderService.delivery(ORDER_ID);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(orderCaptor.capture());
        Orders updatedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, updatedOrder.getId());
        assertEquals(Orders.DELIVERY_IN_PROGRESS, updatedOrder.getStatus());
    }

    @Test
    void complete_whenOrderIdProvided_thenUpdateOrderStatusToCompleted() {
        orderService.complete(ORDER_ID);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(orderCaptor.capture());
        Orders updatedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, updatedOrder.getId());
        assertEquals(Orders.COMPLETED, updatedOrder.getStatus());
    }

    @Test
    void cancel_whenCancelDtoProvided_thenUpdateOrderStatusToCancelled() {
        OrdersCancelDTO dto = new OrdersCancelDTO();
        dto.setId(ORDER_ID);
        dto.setCancelReason("用户取消");

        orderService.cancel(dto);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(orderCaptor.capture());
        Orders updatedOrder = orderCaptor.getValue();
        assertEquals(ORDER_ID, updatedOrder.getId());
        assertEquals(Orders.CANCELLED, updatedOrder.getStatus());
        assertEquals("用户取消", updatedOrder.getCancelReason());
    }

    @Test
    void statistics_whenStatisticsExist_thenReturnOrderStatistics() {
        OrderStatisticsVO statistics = new OrderStatisticsVO();
        statistics.setToBeConfirmed(2);
        statistics.setConfirmed(3);
        statistics.setDeliveryInProgress(4);
        when(orderMapper.statistics()).thenReturn(statistics);

        OrderStatisticsVO result = orderService.statistics();

        verify(orderMapper).statistics();
        assertNotNull(result);
        assertEquals(2, result.getToBeConfirmed());
        assertEquals(3, result.getConfirmed());
        assertEquals(4, result.getDeliveryInProgress());
    }

    private void stubFixedClock() {
        ZoneId zoneId = ZoneId.of("UTC");
        when(clock.instant()).thenReturn(ORDER_TIME.atZone(zoneId).toInstant());
        when(clock.getZone()).thenReturn(zoneId);
    }

    private void stubOrderInsertGeneratedId() {
        doAnswer(invocation -> {
            Orders order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return null;
        }).when(orderMapper).insert(any(Orders.class));
    }

    private OrdersSubmitDTO buildSubmitDTO() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(ADDRESS_BOOK_ID);
        dto.setPayMethod(1);
        dto.setRemark("少辣");
        dto.setEstimatedDeliveryTime(ORDER_TIME.plusMinutes(45));
        dto.setDeliveryStatus(1);
        dto.setTablewareNumber(2);
        dto.setTablewareStatus(0);
        dto.setPackAmount(3);
        dto.setAmount(BigDecimal.valueOf(40.00));
        return dto;
    }

    private AddressBook buildAddressBook() {
        return AddressBook.builder()
                .id(ADDRESS_BOOK_ID)
                .userId(USER_ID)
                .consignee("张三")
                .phone("13800000000")
                .provinceName("广东省")
                .cityName("深圳市")
                .districtName("南山区")
                .detail("科技园")
                .build();
    }

    private ShoppingCart buildDishCartItem(Long id, String name, BigDecimal amount, Integer number) {
        return ShoppingCart.builder()
                .id(id)
                .userId(USER_ID)
                .dishId(100L + id)
                .name(name)
                .dishFlavor("微辣")
                .amount(amount)
                .number(number)
                .image("dish-" + id + ".jpg")
                .build();
    }

    private ShoppingCart buildSetmealCartItem(Long id, String name, BigDecimal amount, Integer number) {
        return ShoppingCart.builder()
                .id(id)
                .userId(USER_ID)
                .setmealId(200L + id)
                .name(name)
                .amount(amount)
                .number(number)
                .image("setmeal-" + id + ".jpg")
                .build();
    }

    private Orders buildOrder(Long id, String number) {
        return Orders.builder()
                .id(id)
                .number(number)
                .userId(USER_ID)
                .amount(BigDecimal.valueOf(50.00))
                .build();
    }

    private OrderDetail buildOrderDetail(Long id, Long orderId, String name) {
        return OrderDetail.builder()
                .id(id)
                .orderId(orderId)
                .name(name)
                .amount(BigDecimal.valueOf(20.00))
                .number(1)
                .build();
    }

    private void assertOrderDetailMatchesCart(OrderDetail detail, ShoppingCart cartItem) {
        assertEquals(ORDER_ID, detail.getOrderId());
        assertEquals(cartItem.getDishId(), detail.getDishId());
        assertEquals(cartItem.getSetmealId(), detail.getSetmealId());
        assertEquals(cartItem.getName(), detail.getName());
        assertEquals(cartItem.getDishFlavor(), detail.getDishFlavor());
        assertEquals(cartItem.getAmount(), detail.getAmount());
        assertEquals(cartItem.getNumber(), detail.getNumber());
    }
}
