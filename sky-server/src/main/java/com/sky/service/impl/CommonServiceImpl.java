package com.sky.service.impl;

import com.sky.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Value("${sky.upload.path}")
    private String uploadPath;


    @Override
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + extension;

        String datePath = LocalDate.now().toString().replace("-", "/");
        
        // 处理路径分隔符，确保使用系统默认分隔符
        String fullPath = uploadPath + File.separator + datePath;
        File dir = new File(fullPath);

        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new RuntimeException("创建目录失败: " + fullPath);
            }
        }

        File dest = new File(dir, newFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }

        // 返回的访问路径统一使用正斜杠
        String accessPath = "/upload/" + datePath + "/" + newFileName;
        log.info("文件访问路径: {}", accessPath);
        return accessPath;
    }
}
