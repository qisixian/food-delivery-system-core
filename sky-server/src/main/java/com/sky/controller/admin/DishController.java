package com.sky.controller.admin;

import com.sky.context.UserContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

    @Autowired
    DishService dishService;

    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.trace("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.trace("根据id查询菜品：{}", id);
        DishVO dish = dishService.getByIdWithFlavor(id);
        return Result.success(dish);
    }

    @GetMapping("/list")
    public Result<List<Dish>> listByCategoryId(@RequestParam Long categoryId){
        log.trace("根据分类id查询菜品：{}", categoryId);
        List<Dish> dishList = dishService.listByCategoryId(categoryId);
        return Result.success(dishList);
    }

    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO){
        log.trace("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        log.trace("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }


    @PostMapping("/status/{status}")
    @Schema(description = "起售禁售菜品")
    public Result<String> changeStatus(@PathVariable Integer status, @RequestParam Long id) {
        dishService.changeStatus(status, id);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.trace("删除菜品：{}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }
}
