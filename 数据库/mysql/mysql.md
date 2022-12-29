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
7. 数据字典

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

# MySQL应用程序

- [mysqld](https://dev.mysql.com/doc/refman/8.0/en/mysqld-server.html)：SQL守护进程（是MySQL服务器，一般以这个启动）
- mysqld_safe：服务器启动脚本
- mysql.server：服务器启动脚本
- mysqlcheck：表维护程序
- mysqldump：数据库备份程序
- mysqlslap：负载仿真客户端，https://dev.mysql.com/doc/refman/8.0/en/mysqlslap.html
- mysqlbinlog：处理二进制日志文件的实用程序
- mysqldumpslow：总结慢查询日志文件

## dump-数据库备份

mysqldump命令将进行**逻辑备份**，生成的是一组SQL语句文件，执行这些SQL来重现数据库原始数据。

> 注意：[**mysqldump**](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html)至少需要 [`SELECT`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_select)转储表、[`SHOW VIEW`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_show-view)转储视图、[`TRIGGER`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_trigger)转储触发器的权限

mysqldump优点是便利性和灵活性，可以自行插入一些SQL语句进行微改动。

**mysqldump不是用于备份大量数据的快速或可扩展的解决方案**，对于大量数据，即使备份用时短，但是重放SQL恢复数据耗时长，因为重放 SQL 语句涉及用于插入、索引创建等的磁盘 I/O。

**对于大规模的备份和恢复， [物理](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_physical)备份更为合适**，将数据文件以原始格式复制，以便快速恢复。

```sql
mysqldump db_name # 备份某个数据库
mysqldump db_name [tbl_name ...] # 备份某个数据库的某些表

# 选项如下：
--database db_name... 	# 备份某些数据库
--all-databases 		# 备份所有数据库
--add-drop-database 	# 在每个 CREATE DATABASE 语句之前添加 DROP DATABASE 语句
--add-drop-table  		# 在每个 CREATE TABLE 语句之前添加 DROP TABLE 语句
--add-drop-trigger		# 在每个 CREATE TRIGGER 语句之前添加 DROP TRIGGER 语句
```

## slap-负载仿真

[**mysqlslap**](https://dev.mysql.com/doc/refman/8.0/en/mysqlslap.html)是一个诊断程序，旨在模拟 MySQL 服务器的客户端负载并报告每个阶段的时间。它的工作方式就好像多个客户端正在访问服务器一样。

文档：https://dev.mysql.com/doc/refman/8.0/en/mysqlslap.html

## binlog-二进制日志

文档：https://dev.mysql.com/doc/refman/8.0/en/mysqlbinlog.html

### 备份binlog

参数选项：

- `--read-from-remote-server`：从远程服务器备份到本地(即本地为远处服务器的副本服务器)。还需结合以下参数：
  - `--host`
  - `--user`
  - `--password`
- `--raw`：写入原始（二进制）输出，而不是文本输出
- `--stop-never`：到达最后一个日志文件的末尾后**保持与服务器的连接**，并继续等待读取新的事件。

- `--result-file`：输出备份文件名的前缀，一般用于指定目录

查看须备份的服务器有哪些binlog：

```sql
mysql> SHOW BINARY LOGS;
+---------------+-----------+-----------+
| Log_name      | File_size | Encrypted |
+---------------+-----------+-----------+
| binlog.000130 |     27459 | No        |
| binlog.000131 |     13719 | No        |
| binlog.000132 |     43268 | No        |
+---------------+-----------+-----------+
```

**1、静态备份**

```sql
-- 指定备份哪些binlog
mysqlbinlog --read-from-remote-server --host=host_name --raw
  binlog.000130 binlog.000131 binlog.000132

-- 备份从某binlog开始到最后
mysqlbinlog --read-from-remote-server --host=host_name --raw
  --to-last-log binlog.000130
```

**2、实时备份**

```sql
-- 从指定的binlog开始复制，并保持连接以复制新事件
mysqlbinlog --read-from-remote-server --host=host_name --raw
  --stop-never binlog.000130
```

使用[`--stop-never`](https://dev.mysql.com/doc/refman/8.0/en/mysqlbinlog.html#option_mysqlbinlog_stop-never)，无需指定 [`--to-last-log`](https://dev.mysql.com/doc/refman/8.0/en/mysqlbinlog.html#option_mysqlbinlog_to-last-log)读取到最后一个日志文件，因为该选项是隐含的。

> 注意：**实时备份的缺点是不会自动重连**。
>
> 实时备份mysqlbinlog的连接终止（如主服务器挂了或该进程被kill），主服务器恢复后，不会进行自动重连，这点不如副本服务器。

**3、输出文件名**

默认在当前目录中写入与原始日志文件同名的文件。`--result-file`可指定文件名前缀(或目录)。

| 选项                   | 输出文件名          |
| ---------------------- | ------------------- |
| `--result-file=x`      | xbinlog.000130      |
| `--result-file=/tmp/`  | /tmp/binlog.000130  |
| `--result-file=/tmp/x` | /tmp/xbinlog.000130 |

### dump+binlog备份和恢复示例

1、定时执行mysqldump命令转储服务器数据快照：

```sql
-- 备份所有数据
-- 选项`--master-data=2`将当前binlog坐标包含在转储文件中
mysqldump --host=host_name --all-databases --events --routines --master-data=2> dump_file
```

2、对binlog进行**实时备份**：

```sql
mysqlbinlog --read-from-remote-server --host=host_name --raw
  --stop-never binlog.000001
```

3、若发生数据丢失，如服务器挂了，**用最新的dump快照先恢复数据，再以binlog修复快照之后的数据**：

```sql
mysql --host=host_name -u root -p < dump_file
```

然后使用二进制日志备份重新执行在转储文件中列出的坐标之后写入的事件。假设文件中的坐标如下所示：

```none
-- CHANGE MASTER TO MASTER_LOG_FILE='binlog.000002', MASTER_LOG_POS=27284;
```

如果最近备份的日志文件名为 `binlog.000004`，则重新执行日志事件，如下所示：

```terminal
mysqlbinlog --start-position=27284 binlog.000002 binlog.000003 binlog.000004
  | mysql --host=host_name -u root -p
```

将备份的dump文件和binlog文件都先复制到服务器主机后再进行恢复操作会更方便。

## dumpslow-慢查询日志

文档：https://dev.mysql.com/doc/refman/8.0/en/mysqldumpslow.html

`mysqldumpslow`程序可解析 MySQL 慢查询日志文件并汇总其内容。

```sql
mysqldumpslow [options] [log_file...]
```



# 服务器日志

| 日志类型               | 日志信息                                                     |
| :--------------------- | :----------------------------------------------------------- |
| Error log              | mysqld程序启动、运行、停止时产生的问题                       |
| General query log      | 已建立的客户端连接和从客户端收到的语句                       |
| Binary log             | 更改数据的语句（也用于复制）                                 |
| Relay log              | Data changes received from a replication source server       |
| Slow query log         | 执行超过参数 [`long_query_time`](https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_long_query_time) 的SQL语句 |
| DDL log (metadata log) | DDL语句执行的元数据操作                                      |

处理错误日志默认启动，其它日志默认不启动。

## 日志存储

```sql
-- log_output: 日志记录目的地
-- 可选：FILE(记录到文件)、TABLE(记录到表)、NONE(不记录)
mysql> SHOW VARIABLES LIKE 'log_output';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| log_output    | FILE  |
+---------------+-------+
```

默认以文件存储日志，用日志表存储有如下优点：

- 格式标准(表结构)
- 可通过SQL查询日志，比如查询某些特殊客户端`user_host`日志

可用如下SQL查询日志表的结构：(建议直接用Navicat等GUI工具看表结构)

```sql
SHOW CREATE TABLE mysql.general_log;
SHOW CREATE TABLE mysql.slow_log;

-- 比如慢查询表DDL如下:
CREATE TABLE `slow_log` (
  `start_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `user_host` mediumtext NOT NULL,
  `query_time` time(6) NOT NULL,
  `lock_time` time(6) NOT NULL,
  `rows_sent` int NOT NULL,
  `rows_examined` int NOT NULL,
  `db` varchar(512) NOT NULL,
  `last_insert_id` int NOT NULL,
  `insert_id` int NOT NULL,
  `server_id` int unsigned NOT NULL,
  `sql_text` mediumblob NOT NULL,
  `thread_id` bigint unsigned NOT NULL
) ENGINE=CSV DEFAULT CHARSET=utf8mb3 COMMENT='Slow log';
```

> 注意：日志表不会写入binlog，因此不会复制到副本。



## 一般查询日志



```sql
-- general_log: 是否开启一般查询日志
mysql> SHOW VARIABLES LIKE '%general%';
+------------------+-------------------------------+
| Variable_name    | Value                         |
+------------------+-------------------------------+
| general_log      | OFF                           |
| general_log_file | /var/lib/mysql/k8s-master.log |
+------------------+-------------------------------+
```

## 慢查询日志


```sql
-- slow_query_log: 是否开启慢查询日志
-- slow_query_log_file: 慢查询日志文件名
-- long_query_time: 慢查询阈值(second)
mysql> SHOW VARIABLES LIKE '%slow_query%';
+---------------------+------------------------------------+
| Variable_name       | Value                              |
+---------------------+------------------------------------+
| slow_query_log      | OFF                                |
| slow_query_log_file | /var/lib/mysql/k8s-master-slow.log |
| long_query_time 	  | 10.000000 						   |
+---------------------+------------------------------------+

```

## 错误日志

