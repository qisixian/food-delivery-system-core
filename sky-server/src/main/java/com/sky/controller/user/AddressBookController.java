package com.sky.controller.user;

import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.dto.AddressBookCreateDTO;
import com.sky.dto.AddressBookUpdateDTO;
import com.sky.entity.AddressBook;
import com.sky.exception.ResourceNotFoundException;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/user/addressBook")
@Schema(description = "C端地址簿接口")
public class AddressBookController {

    @Autowired
    private UserContext userContext;

    @Autowired
    private AddressBookService addressBookService;

    @GetMapping("/list")
    @Schema(description = "查询当前登录用户的所有地址信息")
    public Result<List<AddressBook>> list() {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(userContext.get());
        List<AddressBook> list = addressBookService.list(addressBook);
        return Result.success(list);
    }

    @PostMapping
    @Schema(description = "新增地址")
    public Result<Void> save(@RequestBody AddressBook addressBook) {
        addressBookService.save(addressBook);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Schema(description = "根据id查询地址")
    public Result<AddressBook> getById(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    @PutMapping
    @Schema(description = "根据id修改地址")
    public Result<Void> update(@RequestBody AddressBook addressBook) {
        addressBookService.update(addressBook);
        return Result.success();
    }

    @PutMapping("/default")
    @Schema(description = "设置默认地址")
    public Result<Void> setDefault(@RequestParam Long id) {
        addressBookService.setDefault(id);
        return Result.success();
    }

    @DeleteMapping
    @Schema(description = "根据id删除地址")
    public Result<Void> deleteById(@RequestParam Long id) {
        addressBookService.deleteById(id);
        return Result.success();
    }

    @GetMapping("default")
    @Schema(description = "查询默认地址")
    public Result<AddressBook> getDefault() {
        AddressBook addressBook = addressBookService.getDefault();
        return Result.success(addressBook);
    }

}
