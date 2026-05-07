package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${erp.upload.image-path}")
    private String imagePath;

    @PostMapping("/image")
    public Result<String> uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传的图片不能为空");
        }

        // 1. 获取原始文件名并提取后缀 (例如: .png, .jpg)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 2. 生成全局唯一的UUID文件名，防止同名文件被覆盖
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 3. 确保物理存储目录存在
        File directory = new File(imagePath);
        if (!directory.exists()) {
            directory.mkdirs(); // 级联创建目录
        }

        // 4. 将文件保存到硬盘
        try {
            File destFile = new File(directory, newFileName);
            file.transferTo(destFile);
            log.info("图片落盘成功: {}", destFile.getAbsolutePath());
            
            // 5. 返回供前端访问的相对 URL (对应 WebConfig 里的映射)
            String imageUrl = "/images/" + newFileName;
            return Result.success(imageUrl);

        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new RuntimeException("图片上传失败，请重试");
        }
    }
}
