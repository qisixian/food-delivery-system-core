package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    SetmealService setmealService;

    @GetMapping("/page")
    public Result<PageResult<SetmealVO>> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageResult<SetmealVO> pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }


    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        SetmealVO setmeal = setmealService.getByIdWithDish(id);
        return Result.success(setmeal);
    }

    @PostMapping
    public Result<Void> save(@RequestBody SetmealDTO setmealDTO){
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SetmealDTO setmealDTO){
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }


    @PostMapping("/status/{status}")
    @Schema(description = "起售禁售套餐")
    public Result<String> changeStatus(@PathVariable Integer status, @RequestParam Long id) {
        setmealService.changeStatus(status, id);
        return Result.success();
    }


    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Long> ids){
        setmealService.deleteBatch(ids);
        return Result.success();
    }
}
