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

## MySQL应用程序

- mysqld：SQL守护进程（即MySQL服务器）
- mysqld_safe：服务器启动脚本
- mysql.server：服务器启动脚本
- mysqlcheck：表维护程序
- mysqldump：数据库备份程序
- mysqlslap：负载仿真客户端，https://dev.mysql.com/doc/refman/8.0/en/mysqlslap.html
- mysqlbinlog：处理二进制日志文件的实用程序
- mysqldumpslow：总结慢查询日志文件

### mysqldump-数据库备份程序

mysqldump命令将进行**逻辑备份**，生成的是一组SQL语句文件，执行这些SQL来重现数据库原始数据。

> 注意：[**mysqldump**](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html)至少需要 [`SELECT`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_select)转储表、[`SHOW VIEW`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_show-view)转储视图、[`TRIGGER`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_trigger)转储触发器的权限

mysqldump优点是便利性和灵活性，可以自行插入一些SQL语句进行微改动。

**mysqldump不是用于备份大量数据的快速或可扩展的解决方案**，对于大量数据，即使备份用时短，但是重放SQL恢复数据耗时长，因为重放 SQL 语句涉及用于插入、索引创建等的磁盘 I/O。

**对于大规模的备份和恢复， [物理](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_physical)备份更为合适**，将数据文件以原始格式复制，以便快速恢复。

```sql
mysqldump [options] db_name # 备份某个数据库
mysqldump [options] db_name [tbl_name ...] # 备份某个数据库的某些表
mysqldump [options] --database db_name ... # 备份某些数据库
mysqldump [options] --all-databases # 备份所有数据库

# 选项如下：
--add-drop-database 	# 在每个 CREATE DATABASE 语句之前添加 DROP DATABASE 语句
--add-drop-table  		# 在每个 CREATE TABLE 语句之前添加 DROP TABLE 语句
--add-drop-trigger		# 在每个 CREATE TRIGGER 语句之前添加 DROP TRIGGER 语句
```

### mysqlslap-负载仿真程序

[**mysqlslap**](https://dev.mysql.com/doc/refman/8.0/en/mysqlslap.html)是一个诊断程序，旨在模拟 MySQL 服务器的客户端负载并报告每个阶段的时间。它的工作方式就好像多个客户端正在访问服务器一样。

文档：https://dev.mysql.com/doc/refman/8.0/en/mysqlslap.html



### mysqlbinlog-二进制日志文件程序



## 常用命令

```sql
# mysql 命令
# 登录MySQL服务
mysql -h localhost -u root -p # 然后输入密码
# 从文本文件执行SQL语句(需注意文件首句SQL应是 USE db_name来选择导入哪个数据库)
mysql < text_file
mysql db_name text_file # 这样的话则无需在文件中指定数据库名

# 查询MySQL版本/当前用户
mysql> SELECT VERSION(),USER();
+-----------+----------------+
| VERSION() | USER()         |
+-----------+----------------+
| 8.0.30    | root@localhost |
+-----------+----------------+
1 row in set (0.01 sec)

# 展示所有数据库
SHOW DATABASES;
# 使用某个数据库
USE [database_name];
# 展示当前使用的哪个数据库
SELECT DATABASE();

# 展示数据库内所有表
SHOW TABLES;
# 某个表的详细信息
DESCRIBE [table_name];
```

