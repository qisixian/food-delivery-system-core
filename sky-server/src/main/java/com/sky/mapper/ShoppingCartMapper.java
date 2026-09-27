package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Select("select * from shopping_cart where user_id = #{userId} and dish_key = #{dishKey} and setmeal_key = #{setmealKey} and dish_flavor = #{dishFlavor}")
    ShoppingCart get(Long userId, Long dishKey, Long setmealKey, String dishFlavor);

    @Update("update shopping_cart set number = #{number} where id = #{id}")
    int updateNumberById(ShoppingCart shoppingCart);

    @Update("update shopping_cart set number = number + 1 where id = #{id} and user_id = #{userId}")
    int addNumberById(Long id, Long userId);

    @Update("update shopping_cart set number = number - 1 where id = #{id} and user_id = #{userId} and number > 0")
    int subNumberById(Long id, Long userId);

    @Delete("delete from shopping_cart where id = #{id} and user_id = #{userId} and number = 0")
    int cleanNumberisZeroById(Long id, Long userId);

    @Insert("insert into shopping_cart (user_id, dish_id, setmeal_id, name, image, dish_flavor, amount, number, create_time) " +
            "values (#{userId}, #{dishId}, #{setmealId}, #{name}, #{image}, #{dishFlavor}, #{amount}, 1, #{createTime})" +
            "ON DUPLICATE KEY UPDATE number = number + 1")
    void insert(ShoppingCart shoppingCart);

    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(ShoppingCart shoppingCart);

    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);
}
