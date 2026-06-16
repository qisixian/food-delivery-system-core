package com.sky.controller.user;

import com.sky.context.UserContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @PostMapping("/sub")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        shoppingCartService.removeShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<ShoppingCart>> list() {
        if (userContext.get() == null) {
            log.trace("用户未登录，无法查询购物车");
            return Result.success();
        }
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }

    @DeleteMapping("/clean")
    public Result clean() {
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
}
