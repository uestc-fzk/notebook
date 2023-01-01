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

mysqldump程序将进行**逻辑备份**，生成的是一组SQL语句文件，执行这些SQL来重现数据库原始数据。

更多细节看下面备份与恢复部分描述。


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

mysql服务器日志的存储载体以`log_output`变量指定：(注redolog属于Innodb日志)

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

可以同时将日志写入表和文件中：`log_output=FILE,TABLE`

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

## 错误日志

```sql
mysql> show variables like 'log_error%';
+----------------------------+----------------------------------------+
| Variable_name              | Value                                  |
+----------------------------+----------------------------------------+
| log_error                  | /var/log/mysqld.log                    |
| log_error_services         | log_filter_internal; log_sink_internal |
| log_error_suppression_list |                                        |
| log_error_verbosity        | 2                                      |
+----------------------------+----------------------------------------+
```

`log_error`参数指定错误日志文件名，可配置`stderr`表示标准控制台错误输出

`log_error_services`参数指定加载哪些日志组件，比如可以配置json组件使日志输出格式为json，具体配置看文档。

## 一般查询日志

**general_log仅记录SELECT和SHOW等不修改数据的语句**。

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

一般查询日志默认不开启；默认文件名为`${主机名}.log`。

## 二进制日志

**binlog记录数据库写操作**，如表DDL和表DML语句。可用`mysqlbinlog`程序显示binlog文件内容。

用途：

- 主从复制
- 数据恢复：dump和binlog结合恢复数据

事务是整体写入文件的，从不在文件之间拆分。

```sql
mysql> show variables like 'log_bin%';
+---------------------------------+-----------------------------+
| Variable_name                   | Value                       |
+---------------------------------+-----------------------------+
| log_bin                         | ON                          |
| log_bin_basename                | /var/lib/mysql/binlog       |
| log_bin_index                   | /var/lib/mysql/binlog.index |
| log_bin_trust_function_creators | OFF                         |
| log_bin_use_v1_row_events       | OFF                         |
+---------------------------------+-----------------------------+
```

`log_bin`：配置是否启动binlog，默认启动。

`log_bin_basename`：指定binlog基本名，然后以自增数字填充完整文件名，如`binlog.000001`

`max_binlog_size`：单个binlog文件大小，默认1GB。

`log_bin_index`：为了跟踪使用了哪些二进制日志文件，创建了二进制日志索引文件，基本名字和binlog相同，扩展名为`.index`

### 日志格式

`binlog_format`：指定binlog记录的日志格式。

- `STATEMENT`：记录写操作SQL语句。**对于非确定性SQL如`now()`等时间函数会出现主从数据不一致问题。**

- `ROW`：默认，记录行的修改
- `MIXED`：混合模式，一般记录SQL，非确定性SQL情况下记录行修改。

在少量SQL语句会修改大量行的情况下，记录SQL更有效，而某些SQL的WHERE过滤可能需要大量执行时间，但只会修改几行，此时记录行修改更有效。

> 注意：如果使用innodb引擎且事务隔离级别为`READ_COMMITTED`或`READ_UNCOMMITED`，只能使用基于行的日志格式。

## 慢查询日志

slowlog记录执行时间超过`long_query_time`的SQL，`mysqldumpslow`命令可处理slowlog并汇总其内容。

| 参数                          | 描述                 | 默认值            |
| ----------------------------- | -------------------- | ----------------- |
| slow_query_log                | 开启慢查询日志       | OFF               |
| slow_query_log_file           | slowlog文件名        | {主机名}-slow.log |
| long_query_time               | 慢查询阈值(second)   | 10                |
| log_queries_not_using_indexes | 未使用索引的查询语句 | OFF               |

以下参数将确定最终是否写入slowlog：

`min_examined_row_limit`：默认0，查询扫过的行，少于此参数的查询语句不会写入slowlog

`log-throttle-queries-not-using-indexes`：如果 [`log_queries_not_using_indexes`](https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_log_queries_not_using_indexes) 启用，该 [`log_throttle_queries_not_using_indexes`](https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_log_throttle_queries_not_using_indexes) 变量会限制每分钟可以写入慢速查询日志的此类查询的数量。默认0表示无限制。

# 备份与恢复

## 备份与恢复类型

一般来说：全量备份(快照)采用物理备份，增量备份采用逻辑备份。

1、物理备份：

物理备份包括存储数据库内容的目录和文件的原始副本。这种备份适用于发生问题时需要快速恢复的大型、重要的数据库。

**物理备份比逻辑备份更快。**

物理备份工具都是企业版功能。

2、逻辑备份：

逻辑备份保存为逻辑数据库结构和内容信息，这种类型的备份适用于您可以编辑数据值或表结构，或在不同机器架构上重新创建数据的少量数据。

逻辑备份输出大于物理备份，尤其是在以文本格式保存时。

具有高度可移植性。

逻辑备份工具包括`mysqldump`、`mysqlimport`程序

3、在线备份（热备份）、离线备份（冷备份）

热备份不停机，需 MySQL Enterprise Backup企业版产品

4、本地与远程备份

`mysqldump`命令可以连接本地或远程服务器进行备份。

5、快照备份：如写时复制技术。

6、全量备份(快照)和增量备份

增量恢复是恢复在给定时间跨度内所做的更改。这也称为时间点恢复，因为它使服务器的状态在给定时间保持最新。时间点恢复基于binlog。

## 备份方法

企业版可用 [MySQL Enterprise Backup](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_mysql_enterprise_backup)产品对整个实例或选定的数据库、表或两者进行物理备份。该产品包括**增量备份**和**压缩备份**的功能。

### mysqldump

mysqldump程序将进行**逻辑备份**，生成的是一组SQL语句文件，执行这些SQL来重现数据库原始数据。

> 注意：[**mysqldump**](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html)至少需要 [`SELECT`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_select)转储表、[`SHOW VIEW`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_show-view)转储视图、[`TRIGGER`](https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_trigger)转储触发器的权限

mysqldump优点是便利性和灵活性，可以自行插入一些SQL语句进行微改动。

**mysqldump不是用于备份大量数据的快速或可扩展的解决方案**，对于大量数据，即使备份用时短，但是重放SQL恢复数据耗时长，因为重放 SQL 语句涉及用于插入、索引创建等的磁盘 I/O。

**对于大规模的备份和恢复， [物理](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_physical)备份更为合适**，将数据文件以原始格式复制，以便快速恢复，企业版mysqlbackup命令支持物理备份。

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

1、全量逻辑备份例子：

```sql
mysqldump --single-transaction --flush-logs --master-data=2 \
         --all-databases > demo1.sql
```

mysqldump命令进行全量备份时会**先读取此时binlog坐标**：此备份操作会获取所有表的全局读锁（使用[`FLUSH TABLES WITH READ LOCK`](https://dev.mysql.com/doc/refman/8.0/en/flush.html#flush-tables-with-read-lock)），一旦获得此锁，就会读取二进制日志坐标并释放锁。

`-flush-logs`：会将旧binlog日志落盘并创建新binlog文件，这个新文件就是增量文件。

若备份前binlog文件为demo-bin.000001：

周一3点逻辑全量备份为demo1.sql，新binlog为demo-bin.000002，将demo1保存到安全地方。

周二3点逻辑全量备份为demo2.sql，新binlog为demo-bin.000003，将demo2.sql和demo-bin.000002都保存到安全地方(如磁带或光盘)。

可以定期清理不需要的binlog以释放空间。

2、复制数据库例子：

```sql
-- 在服务器1
mysqldump --databases db1 > dump.sql

-- 在服务器2
mysql < dump.sql
```

3、分别转储表定义和数据

```sql
mysqldump --no-data db1 > dump-defs.sql # 数据库表定义
mysqldump --no-create-info db1 > dump-data.sql # 数据
```

### 制作带分隔符的文本文件

```sql
-- 仅保存表数据，不能保存表结构
SELECT * INTO OUTFILE `file_name` FROM tbl_name;
-- 加载数据
-- LOAD DATA 或 mysqlimport
```

### 主从复制

文档：https://dev.mysql.com/doc/refman/8.0/en/replication-solutions-backups.html

# 优化

文档：https://dev.mysql.com/doc/refman/8.0/en/statement-optimization.html



## 索引

### 索引合并

Index merge通过多次扫描索引并将其结果合并，仅能合并单表扫描，不适用于全文索引。

在EXPLAIN输出中，若适用索引合并会在type列显示index merge，在Extra字段显示使用的合并算法：

- Using intersect(...)
- Using union(...)
- Using sort_union(...)

```sql
-- 索引合并 交集
SELECT * FROM tbl_name WHERE k1 =10 AND k2=10;

-- 索引合并 联合
SELECT * FROM tbl_name WHERE k1 = 10 OR k2=20;

-- 索引合并 排序联合
SELECT * FROM tbl_name WHERE k1 <10 OR k2<20;

-- 混合
SELECT * FROM tbl_name WHERE (k1=10 AND k2=20) OR (k3=30 AND k4=40);
```

sort-union 算法和 union 算法之间的区别在于，sort-union 算法必须首先获取所有行的行 ID 并在返回任何行之前对它们进行排序。

## 哈希join优化

MySQL 8.0.18 开始会尽可能对join查询使用哈希进行优化，即hash join，通常比以前[块嵌套循环连接算法](https://dev.mysql.com/doc/refman/8.0/en/nested-loop-joins.html#block-nested-loop-join-algorithm) 更快。从 MySQL 8.0.20 开始，删除了对块嵌套循环的支持。

EXPLIAN时的Extra列：`Using join buffer (hash join)`

```sql
SELECT * FROM tbl1 
INNER JOIN tbl2 ON tbl.c1=tbl2.c1;
```

在 MySQL 8.0.20 之前，如果任何一对连接表不具备至少一个 equi-join 条件，则无法使用哈希连接，并且采用较慢的块嵌套循环算法。MySQL 8.0.80后非等值join也能使用hash join。

```sq;
mysql> EXPLAIN FORMAT=TREE SELECT * FROM t1 JOIN t2 ON t1.c1 < t2.c1\G
*************************** 1. row ***************************
EXPLAIN: -> Filter: (t1.c1 < t2.c1)  (cost=4.70 rows=12)
    -> Inner hash join (no condition)  (cost=4.70 rows=12)
        -> Table scan on t2  (cost=0.08 rows=6)
        -> Hash
            -> Table scan on t1  (cost=0.85 rows=6)
```

参数`join_buffer_size`控制hash join的内存使用，默认256KB。超过限制时将使用文件处理。此参数可设高点。











# 单机运行多MySQL实例

在某些情况下，可能需要在一台机器上跑多个MySQL实例。

文档：https://dev.mysql.com/doc/refman/8.0/en/multiple-servers.html





