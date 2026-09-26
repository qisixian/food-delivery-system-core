package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where user_id = #{userId} order by order_time desc")
    List<Orders> listByUserId(Long userId);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

//    @Update("update orders set status = 2 where id = #{id} and status = 1")
//    int completePayment(Long id);

    @Update("""
        UPDATE orders
        SET status = 2,
            pay_status = 1,
            checkout_time = #{checkoutTime}
        WHERE id = #{id}
          AND status = 1
          AND pay_status = 0
    """)
    int payOrder(Long id, LocalDateTime checkoutTime);

    @Update("update orders set status = 3 where id = #{id} and status = 2")
    int acceptOrder(Long id);

    @Update("update orders set status = 4 where id = #{id} and status = 3")
    int startDelivery(Long id);

    @Update("update orders set status = 5 where id = #{id} and status = 4")
    int completeDelivery(Long id);

    @Update("update orders set status = 6, rejection_reason = #{rejectReason} where id = #{id} and status = #{fromStatus} and status in (1, 2, 3, 4)")
    int rejectOrder(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, String rejectReason);

    @Update("update orders set status = 6, cancel_reason = #{cancelReason} where id = #{id} and status = #{fromStatus} and status in (1, 2, 3, 4)")
    int cancelOrder(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, String cancelReason);

    Double sumByMap(Map<String, Object> map);

    Integer countByMap(Map<String, Object> map);

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    OrderStatisticsVO statistics();
}
