package com.sky.constant;

/**
 * 信息提示常量类
 */
public final class MessageConstant {

    private MessageConstant() {} // 这个类不能被实例化

    public static final String PASSWORD_ERROR = "PASSWORD_ERROR"; // "密码错误"
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND"; // "账号不存在"
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED"; // "账号被锁定"
    public static final String ALREADY_EXISTS = "ALREADY_EXISTS"; // "用户名已存在"
    public static final String UNEXPECTED_SYSTEM_ERROR = "UNEXPECTED_SYSTEM_ERROR"; // "未知错误"
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED"; // "用户未登录"
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "CATEGORY_BE_RELATED_BY_SETMEAL"; // "当前分类关联了套餐,不能删除"
    public static final String CATEGORY_BE_RELATED_BY_DISH = "CATEGORY_BE_RELATED_BY_DISH"; // "当前分类关联了菜品,不能删除"
    public static final String ORDER_SHOPPING_CART_REQUIRED = "ORDER_SHOPPING_CART_REQUIRED"; // "购物车数据为空，不能下单"
    public static final String ORDER_DELIVERY_ADDRESS_REQUIRED = "ORDER_DELIVERY_ADDRESS_REQUIRED"; // "用户地址为空，不能下单"
    public static final String LOGIN_FAILED = "LOGIN_FAILED"; // "登录失败"
    public static final String UPLOAD_FAILED = "UPLOAD_FAILED"; // "文件上传失败"
    public static final String SETMEAL_CONTAINS_DISABLED_DISH = "SETMEAL_CONTAINS_DISABLED_DISH"; // "套餐内包含未启售菜品，无法启售"
    public static final String PASSWORD_EDIT_FAILED = "PASSWORD_EDIT_FAILED"; // "密码修改失败"
    public static final String DISH_ON_SALE = "DISH_ON_SALE"; // "起售中的菜品不能删除"
    public static final String SETMEAL_ON_SALE = "SETMEAL_ON_SALE"; // "起售中的套餐不能删除"
    public static final String DISH_BE_RELATED_BY_SETMEAL = "DISH_BE_RELATED_BY_SETMEAL"; // "当前菜品关联了套餐,不能删除"
    public static final String ORDER_STATUS_ERROR = "ORDER_STATUS_ERROR"; // "订单状态错误"
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND"; // "订单不存在"
    public static final String ORDER_ALREADY_PAID = "ORDER_ALREADY_PAID"; // "订单已支付"

}
