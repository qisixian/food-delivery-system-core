package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.BusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private UserContext userContext;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private Clock clock;

    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        if (shoppingCart.getDishFlavor() == null) {
            shoppingCart.setDishFlavor("");
        }
        shoppingCart.setUserId(userContext.get());
        Long dishId = shoppingCart.getDishId();
        if (dishId != null) {
            // 添加的是菜品
            Dish dish = dishMapper.getById(dishId);
            if (dish == null) {
                throw new BusinessException(MessageConstant.DISH_NOT_FOUND);
            }
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        } else {
            // 添加的是套餐
            Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());
            if (setmeal == null) {
                throw new BusinessException(MessageConstant.SETMEAL_NOT_FOUND);
            }
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        }
        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now(clock));
        // ON DUPLICATE KEY UPDATE
        shoppingCartMapper.insert(shoppingCart);

    }

    @Transactional
    public void removeShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        if (shoppingCartDTO.getDishFlavor() == null) {
            shoppingCartDTO.setDishFlavor("");
        }
        ShoppingCart existingCart = shoppingCartMapper.get(userContext.get(),
                shoppingCartDTO.getDishId() == null ? 0 : shoppingCartDTO.getDishId(),
                shoppingCartDTO.getSetmealId() == null ? 0 : shoppingCartDTO.getSetmealId(),
                shoppingCartDTO.getDishFlavor());
        if (existingCart != null) {
            shoppingCartMapper.subNumberById(existingCart.getId(), userContext.get());
            shoppingCartMapper.cleanNumberisZeroById(existingCart.getId(), userContext.get());
        }
    }

    public List<ShoppingCart> showShoppingCart() {
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userContext.get())
                .build();
        return shoppingCartMapper.list(cart);
    }

    public void cleanShoppingCart() {
        Long userId = userContext.get();
        shoppingCartMapper.deleteByUserId(userId);
    }
}
