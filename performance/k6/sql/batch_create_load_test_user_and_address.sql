START TRANSACTION;

-- 创建 1000 个用户；已存在的测试用户跳过
INSERT INTO sky_take_out_loadtest.`user`
(openid, name, phone, sex, create_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT
    CONCAT('loadtest_', LPAD(n, 6, '0')),
    CONCAT('压测用户', LPAD(n, 4, '0')),
    CONCAT('000', LPAD(n, 8, '0')),
    '1',
    NOW()
FROM seq
WHERE NOT EXISTS (
    SELECT 1
    FROM sky_take_out_loadtest.`user` u
    WHERE u.openid = CONCAT('loadtest_', LPAD(seq.n, 6, '0'))
);

-- 为这些用户创建默认地址；已有地址的用户跳过
INSERT INTO sky_take_out_loadtest.address_book (
    user_id,
    consignee,
    sex,
    phone,
    province_code,
    province_name,
    city_code,
    city_name,
    district_code,
    district_name,
    detail,
    label,
    is_default
)
SELECT
    u.id,
    u.name,
    u.sex,
    u.phone,
    '110000',
    '北京市',
    '110100',
    '北京市',
    '110105',
    '朝阳区',
    CONCAT('压测专用地址', RIGHT(u.openid, 6), '号'),
    '测试',
    1
FROM sky_take_out_loadtest.`user` u
WHERE u.openid REGEXP '^loadtest_[0-9]{6}$'
  AND u.openid BETWEEN 'loadtest_000001' AND 'loadtest_001000'
  AND NOT EXISTS (
    SELECT 1
    FROM sky_take_out_loadtest.address_book a
    WHERE a.user_id = u.id
    );

COMMIT;