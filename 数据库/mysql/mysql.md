# 资料

MySQL8.0官方参考手册：https://dev.mysql.com/doc/refman/8.0/en/

# 概述

MySQL 是最流行的开源、**关系型 SQL 数据库管理系统**，由 Oracle Corporation 开发、分发和支持。

使用C和C++开发、内核线程实现的多线程。

## 新功能

**MySQL8新增的部分功能如下：**(我简单的选了一些看得懂的)

1. 默认字符集已从 更改 `latin1`为`utf8mb4`。

2. JSON功能。

3. 优化器新增功能：
   - 支持不可见索引
   - 支持降序索引
4. 窗口函数
5. 多值索引
6. RIGHT JOIN 作为 LEFT JOIN 处理。