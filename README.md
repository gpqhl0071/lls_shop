# 莉莉丝交易行数据查询系统

## 项目简介

这是一个基于Spring Boot的Web应用程序,用于查询和展示莉莉丝交易行的商品数据。该系统能够从莉莉丝交易行API获取商品数据,并提供Web界面展示这些数据,包括商品详情、价格、开售时间等信息。

## 主要功能

1. 从API获取商品数据(支持多页数据获取,最多5页)
2. 展示商品列表,包括商品ID、玩家名称、价格、开售时间等信息
3. 支持按商品状态筛选(全部、未开售、已开卖)
4. 提供商品搜索功能
5. 展示倒计时信息
6. 错误处理和日志记录
7. 邮件通知功能(针对即将开售的便宜商品)

## 技术栈

- Java 8+
- Spring Boot
- Spring MVC
- Jackson (JSON处理)
- SLF4J (日志)
- HTML/CSS/JavaScript (前端展示)
- RestTemplate (HTTP客户端)

## 项目结构

- `src/main/java/org/example/`
  - `config/` : 配置类
    - `ApiConfig.java` : API相关配置
  - `controller/` : Web控制器
    - `ProductController.java` : 处理商品相关的Web请求
  - `model/` : 数据模型
    - `ProductSummary.java` : 商品摘要模型
  - `service/` : 业务逻辑服务
    - `ProductService.java` : 商品相关的业务逻辑
  - `ProductFinder.java` : 商品查找器,用于查找特定条件的商品

## 如何运行

1. 确保您的系统已安装Java 8或更高版本
2. 克隆此仓库到本地
3. 在项目根目录下运行: `./mvnw spring-boot:run`
4. 打开浏览器,访问 `http://localhost:8080/api/products/html`

## API端点

- `GET /api/products` : 获取所有商品的JSON数据
- `GET /api/products/html` : 获取HTML格式的商品列表页面
  - 支持status参数: 0(全部), 2(未开售), 3(已开卖)

## 配置

您可以在 `src/main/resources/application.properties` 文件中修改应用程序的配置,如服务器端口等。

## 特色功能

1. 多页数据获取: 系统会自动获取多页数据(最多5页),每页100条记录。
2. 实时倒计时: 商品列表中显示距离开售时间的倒计时。
3. 状态筛选: 可以根据商品状态(全部、未开售、已开卖)筛选商品。
4. 错误处理: 包含全局错误处理,提供友好的错误页面。
5. 邮件通知: 可以通过邮件通知即将开售的便宜商品。

## 注意事项

- 本项目仅用于学习和研究目的,请勿用于商业用途。
- 使用本系统时请遵守莉莉丝交易行的相关规定和API使用政策。

## 贡献

欢迎提交问题和拉取请求。对于重大更改,请先开issue讨论您想要更改的内容。

## 许可证

[MIT](https://choosealicense.com/licenses/mit/)
