## 需要进行学习的Points
1. 地址是什么? 和接口有什么关系?
    + ~~HTTP协议:~~
       + ~~响应报文状态码: 2xx, 成功; 3xx, 重定向; 4xx, 客户端错误; 5xx, 服务端错误~~
    + ~~RESTful Web接口设计风格~~
       + ~~REST(representational state transfer), 表现层状态转移: 一切皆资源, 用URL表示资源, 用HTTP方法表示对资源的操作~~
          + ~~用URL表示是什么~~
          + ~~用HTTP表示干什么~~
       + ~~优点:~~
          + ~~统一, 规范, 可读性强, 易于维护~~
          + ~~前后端分离极易实现~~
          + ~~真实工程中: 可读性 > 教条~~
    + Nginx原理
        + HTTP服务器
        + 反向代理
2. 调试一下登录过程, 熟悉登录业务.
    + 全局异常处理器的构建方法
        + @RestControllerAdvice
        + @ExceptionHandler
    + 在登录成功后, 为什么要向前端返回一个jwt令牌, 原理是什么
    + 后端返回数据同一结构: Result< T>
3. knife4j的使用方法:
    + knife4j是一个spring boot starter(什么是starter)
    + @Configuration的使用方法, 还有@Bean, 他们和starter有什么关系
4. ~~MVC基本思想以及Spring MVC~~
5. ~~后端接口的请求参数是如何设计的~~
6. AOP: 原理 应用场景

## doc文档文件夹说明
1. doc中的interfaces子文件夹中的.json格式的接口文件夹是Yapi格式的, 在分析时需要注意.
2. doc中的database子文件夹由一个数据库设计文档和一个mysql执行脚本组成.