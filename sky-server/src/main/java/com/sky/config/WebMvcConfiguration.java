package com.sky.config;

import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
import com.sky.interceptor.JwtTokenAdminInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${sky.upload.path}")
    private String uploadPath;

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login", "/upload/**");

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login", "/user/shop/status");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 处理Windows路径分隔符问题，统一转换为正斜杠
        String normalizedPath = uploadPath.replace("\\", "/");
        // 确保路径末尾有斜杠
        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }
        String wholePath = "file:" + normalizedPath;
        log.info("文件上传路径配置: {}", wholePath);

        registry.addResourceHandler("/upload/**")
                .addResourceLocations(wholePath);
    }

    /**
     * 扩展Spring MVC框架的消息转换器
     * 使用自定义的JacksonObjectMapper，将Long类型序列化为字符串，解决JavaScript数字精度丢失问题
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器，使用自定义的JacksonObjectMapper");
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器，底层使用Jackson将Java对象转为json
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        //将上面的消息转换器对象追加到Spring MVC框架的转换器集合中
        converters.add(0, messageConverter);
    }
}
