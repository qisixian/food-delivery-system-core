package com.sky.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

//    @PostMapping("/upload")
//    @Schema(description = "文件上传")
//    public Result<String> upload(MultipartFile file) {
//
//        try {
//            String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
//            String fileName = UUID.randomUUID().toString() + extension;
//            String filePath = aliOssUtil.upload(file.getBytes(), fileName);
//            return Result.success(filePath);
//        } catch (IOException e) {
//            log.error("文件上传失败: {}", e.getMessage());
//            return Result.error(MessageConstant.UPLOAD_FAILED);
//        }
//    }
}
