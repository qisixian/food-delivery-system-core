package com.sky.service;

import com.sky.dto.AddressBookCreateDTO;
import com.sky.dto.AddressBookUpdateDTO;
import com.sky.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    List<AddressBook> list(AddressBook addressBook);

    void save(AddressBook addressBook);

    AddressBook getById(Long id);

    void update(AddressBook addressBook);

    void setDefault(Long id);

    void deleteById(Long id);

    AddressBook getDefault();
}
