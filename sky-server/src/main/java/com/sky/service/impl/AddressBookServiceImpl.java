package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.UserContext;
import com.sky.dto.AddressBookCreateDTO;
import com.sky.dto.AddressBookUpdateDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Employee;
import com.sky.exception.BusinessException;
import com.sky.exception.ResourceNotFoundException;
import com.sky.mapper.AddressBookMapper;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private UserContext userContext;

    @Autowired
    private AddressBookMapper addressBookMapper;

    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    public void save(AddressBook addressBook) {
        addressBook.setId(null);
        addressBook.setUserId(userContext.get());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        if (addressBook == null) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        if (!Objects.equals(addressBook.getUserId(), userContext.get())) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        return addressBook;
    }

    public void update(AddressBook addressBook) {
        if (!Objects.equals(addressBook.getUserId(), userContext.get())) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        int rows = addressBookMapper.update(addressBook);
        if (rows == 0) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
    }

    @Transactional
    public void setDefault(Long id) {
        AddressBook dbAddressBook = addressBookMapper.getById(id);
        if (dbAddressBook == null || !Objects.equals(dbAddressBook.getUserId(), userContext.get())) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = 0 where user_id = ?
        addressBookMapper.resetIsDefaultByUserId(userContext.get());
        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        AddressBook address = AddressBook.builder().id(id).isDefault(1).userId(userContext.get()).build();
        int rows = addressBookMapper.update(address);
        if (rows == 0) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
    }

    public void deleteById(Long id) {
        int rows = addressBookMapper.deleteById(id, userContext.get());
        if (rows == 0) {
            throw new ResourceNotFoundException(MessageConstant.ADDRESS_NOT_FOUND);
        }
    }

    @Override
    public AddressBook getDefault() {
        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = AddressBook.builder().userId(userContext.get()).isDefault(1).build();
        List<AddressBook> list = addressBookMapper.list(addressBook);
        if (list == null || list.isEmpty()) {
            throw new BusinessException("No default address");
        }
        return list.getFirst();
        // what if there is a data consistency issue with multiple default addresses?
    }

}
