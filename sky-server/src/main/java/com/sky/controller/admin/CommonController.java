package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/common")
@Tag(name = "通用控制器")
@Slf4j
public class CommonController {

    @Autowired
    private CommonService commonService;

    /**
     * 上传文件
     * @param file 上传的文件（multipart/form-data 格式，虽然在 Body 中，但需要使用 @RequestParam 接收）
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "通过浏览器上传文件")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        log.info("开始上传文件: {}", file.getOriginalFilename());
        String filePath = commonService.upload(file);
        return Result.success(filePath);
    }
}
