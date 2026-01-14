package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    @Autowired
    private CommonService commonService;

    /**
     * 上传文件
     * @return
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "通过浏览器上传文件")
    public Result<String> upload(@RequestBody MultipartFile file) {
        log.info("开始上传文件: {}", file.getOriginalFilename());
        String filePath = commonService.upload(file);
        log.info("前端回显路径: {}", filePath);
        return Result.success(filePath);
    }
}
