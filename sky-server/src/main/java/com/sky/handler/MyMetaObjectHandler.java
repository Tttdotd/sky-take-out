package com.sky.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始执行 insert 自动填充...");

        LocalDateTime now = LocalDateTime.now();
        Long currentUser = BaseContext.getCurrentId();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createUser", Long.class, currentUser);
        this.strictInsertFill(metaObject, "updateUser", Long.class, currentUser);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始执行 update 自动填充...");

        LocalDateTime now = LocalDateTime.now();
        Long currentUser = BaseContext.getCurrentId();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updateUser", Long.class, currentUser);
    }
}
