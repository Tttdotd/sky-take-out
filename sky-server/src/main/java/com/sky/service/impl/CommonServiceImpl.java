package com.sky.service.impl;

import com.sky.service.CommonService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class CommonServiceImpl implements CommonService {

    @Value("${sky.upload.path}")
    private String uploadPath;


    @Override
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        assert originalFilename != null;
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + extension;

        String datePath = LocalDate.now().toString().replace("-", "/");
        File dir = new File(uploadPath + "/" + datePath);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, newFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }

        return "/upload/" + datePath + "/" + newFileName;
    }
}
