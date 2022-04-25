# MySQL

## 安装和卸载

主要是卸载MySQL：

可以直接控制面板卸载，如果控制面板找不到MySQL程序，那就命令行卸载：

1. 管理员方式运行cmd

2. 进入安装目录下的bin目录

3. 停止MySQL：

   ```shell
   net stop mysql
   ```

4. 移除MySQL

   ```shell
   mysqld -remove mysql
   ```

5. 找到电脑上面的注册表文件夹，快捷方式是win+r，然后输入regedit，就可以直接的进入到注册表了

6. 打开了注册表，然后可以复制这个“HKEY_LOCAL_MACHINE\SYSTEM\ControlSet001\Services\Eventlog\Application\MySQL”就可以直接进入了

7. 然后看见了MySQL，将MySQL文件删除，有的可能没有，那就跳过这一步

8. 电脑C盘上面还有其余的文件，还会影响到，输入“C:\Documents and Settings\All Users\Application Data\MySQL”就可以直接的进入了，然后可以直接的删除文件

9. 还有在输入将“C:\ProgramData\MySQL”这个文件夹下面的文件删除，

10.  重启电脑，重新安装MySQL

安装问题的话可以看CSDN收藏的博客，也可以看自己收藏的软件安装资料里的文档。

## jdbc.properties
spring boot 的属性配置
```properties
# mysql 5 驱动不同 com.mysql.jdbc.Driver 
# mysql 8 驱动不同com.mysql.cj.jdbc.Driver、需要增加时区的配置 
# serverTimezone=GMT%2B8 

spring.datasource.username=root 
spring.datasource.password=123456 
spring.datasource.url=jdbc:mysql://localhost:3306/test?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8 
# 主要是要加上时区，其它的参数可以加也可以不加
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```
## 前言
数据库相关概念

	1、DB：数据库，保存一组有组织的数据的容器
	2、DBMS：数据库管理系统，又称为数据库软件（产品），用于管理DB中的数据
			数据库管理系统（ Database Management System ）。
			数据库是通过 DBMS 创建和操作的容器
	3、SQL:结构化查询语言，用于和DBMS通信的语言


### 1.启动和停止MySQL服务

- 通过计算机管理方式 右击计算机—管理—服务—启动或停止MySQL服务 
- 通过管理员模式命令行方式 启动：`net start mysql`，停止：`net stop mysql`

### 2.MySQL服务端的登录与退出

```shell
# 1.通过mysql自带的客户端MySQL 5.5 Command Line Client  然后直接输入用户密码
# 只能是root用户登录 其他用户不能进去 不够灵活 不建议
# 退出 exit或者ctrl+c

# 2. 通过管理员模式命令行方式登录： mysql –h 主机名（自己就是localhost） -P 3306（端口号） –u用户名（root） –p
# 也可以直接在-p后加密码
# 退出 exit
# 3.如果是连接本机的，可以简写：
Mysql -u 用户名 –p密码
```

### 3.MySQL的常见命令 

1.查看当前所有的数据库`show databases;`

2.打开指定的库`use 库名`；会进入这个库

3.查看当前库的所有表`show tables;`

4.查看其它库的所有表`show tables from 库名;`

5.查看表结构`desc 表名;`

### 4.MySQL的语法规范
**重要**

>1.不区分大小写,但建议关键字大写，表名、列名小写

>2.每条命令最好用分号结尾

>3.每条命令根据需要，可以进行缩进或换行(回车)
关键字单独一行

>4.注释
    单行注释：#注释文字
    单行注释：-- 注释文字
      多行注释：/* 注释文字  */

>6.字符串和日期型常量值必须用单引号引起来，数值型不需要。

>7.mysql主键从1开始

>8.limit 【offset,】size;
offset要显示条目的起始索引（起始索引从0开始）
size 要显示的条目个数

## 1.DQL数据查询语句

Data Query Language

建议看《MySQL必知必会》或者菜鸟教程。

## 2.DML数据操纵语句
Data Manipulation Language – 数据操纵语言
- 向表中插入数据
- 修改现存数据
- 删除现存数据

### 插入insert

```sql
INSERT INTO 
表名(字段1,...) 
VALUES(值1,...),...;

-- 也支持直接查询其它表中的数据进行插入
INSERT INTO t_user(username,password)
SELECT username,'1234567'
FROM t_u
WHERE id<3;
```

要求：

1、字段类型和值类型一致或兼容，而且一一对应
2、字段可以省略，但默认所有字段，并且顺序和表中的存储顺序一致

### 修改update

修改单表语法：

```sql
UPDATE 表名 SET 字段1=新值,字段2=新值... [WHERE 条件] ;
```

### 删除delete

方式1：delete语句 

```sql
DELETE FROM 表名 [WHERE 条件] [LIMIT n]
```

方式2：truncate语句  清空数据

```sql
TRUNCATE TABLE 表名
```


两种方式的区别?

>1.truncate不能加where条件，而delete可以加where条件

>2.truncate的效率高一丢丢，删除原来的表，重新建一个表，delete是逐行删除表中的数据

>3.truncate 删除带自增长的列的表后，如果再插入数据，数据从1开始；delete 删除带自增长列的表后，如果再插入数据，数据从上一次的断点处开始

>4.truncate删除不能回滚，delete删除可以回滚

## 3.DDL数据定义语句
数据定义语言(Data Definition Language)
### 库和表的管理
库的管理：

	一、创建库
	CREATE DATABASE 【IF NOT EXISTS】库名 【CHARACTER SET gbk】
	二、删除库
	DROP DATABASE 【if exists】 库名
表的管理：

#### 1.创建表 

```sql
create table [if not exists] 表名(
	字段名 字段类型 [约束],
	字段名 字段类型 [约束],
	...
)
```

#### 2.修改表 alter

```sql
#语法：ALTER TABLE 表名 ADD|MODIFY|DROP|CHANGE COLUMN 字段名 【字段类型】;

#①修改字段名
ALTER TABLE studentinfo CHANGE  COLUMN sex gender CHAR;

#②修改表名
ALTER TABLE stuinfo RENAME 【TO】  studentinfo;

#③修改字段类型和列级约束
alter table 表名 modify column 字段名 字段类型 新约束; #不加就是默认约束，所以要注意不能随便用

#④添加字段	
ALTER TABLE studentinfo ADD COLUMN email VARCHAR(20) first;
#⑤删除字段
ALTER TABLE studentinfo DROP COLUMN email;	
```


#### 3.删除表

```sql
DROP TABLE [IF EXISTS] 表名;
```

#### 4.表的复制

```sql
-- (1).仅仅复制表的结构
CREATE TABLE copy LIKE author;
-- (2).复制表的结构+数据
CREATE TABLE copy2 SELECT * FROM author;
-- (3).只复制部分数据
CREATE TABLE copy3 SELECT id,au_name FROM author WHERE nation='中国';
-- (4)仅仅复制某些字段
CREATE TABLE copy4 
SELECT id,au_name
FROM author
WHERE 0; #这样所有数据都不会复制过来了，但是把表的部分结构复制了过来
```

### 索引管理

#### 1.添加或删除索引

```sql
-- 普通索引
ALTER table tableName ADD INDEX indexName(列名...)
-- 唯一索引
ALTER table mytable ADD UNIQUE [indexName] (列名...)
-- 全文索引 并指定解析器为ngram
ALTER TABLE 表名 ADD FULLTEXT INDEX 索引名 (列名) WITH PARSER ngram;

-- 显示索引信息
SHOW INDEX FROM table_name\G

-- 删除索引
DROP INDEX 索引名 ON 表名;
```

#### 2.全文索引

在MySQL 5.6版本以前,只有MyISAM存储引擎支持全文引擎.在5.6版本中,InnoDB加入了对全文索引的支持,但是不支持中文全文索引.在5.7.6版本,MySQL内置了ngram全文解析器,用来支持亚洲语种的分词.

MySQL的全文索引查询有多种模式，经常使用两种.
需要注意的是，MySQL的`倒排索引`对于小数据量可以这么做，但是对于大数据量，还是用ElasticSearch比较好。

MySQL全文索引以**词频**作为唯一标准。

缺点：《高性能MySQL》P304

> 1、全文索引的INSERT、UPDATE、DELETE操作代价很高；
> 2、“双B-Tree"结构会有更多的碎片，需要更多的OPTIMIZE TABLE操作；
> 3、影响MySQL查询优化器的工作；

##### 自然语言搜索

就是普通的包含关键词的搜索.

```sql
-- 一个查询中同时使用两次MATCH并不会有额外的消耗
SELECT id,article_title,MATCH (article_title) AGAINST ('应届生' IN NATURAL LANGUAGE MODE) AS relevance
FROM mk_article 
WHERE MATCH (article_title) AGAINST ('应届生' IN NATURAL LANGUAGE MODE);
-- 省略模式说明也是可以的，即默认情况是自然语言搜索
SELECT * FROM articles WHERE MATCH (title,body) AGAINST ('精神');
```

这类搜索会**自动按照相似度进行排序**

> 注意：如果全文索引是多列索引，MATCH函数中指定的列必须和全文索引指定的列完全相同，因为全文索引并不会记录某个关键词来自于哪个列。
>
> 绕过方法：《高性能MySQL》P301

##### 布尔全文索引

在布尔全文索引中，可以在查询里定义某个关键词的相关性，过滤噪声词。
**搜索结果是未经排序的**。

布尔全文索引通用修饰符:

| 修饰符 | 作用                 |
| ------ | -------------------- |
| 无     | rank值更高           |
| ~      | 使得rank值下降       |
| +      | 必须包含             |
| -      | 不能包含             |
| 阿里*  | 以阿里开头的rank更高 |

案例：

```sql
SELECT id,article_title,MATCH (article_title) AGAINST ('~应届生 +阿里 哈佛*' IN BOOLEAN  MODE) AS relevance
FROM mk_article
WHERE MATCH (article_title) AGAINST ('~应届生 +阿里 哈佛*' IN BOOLEAN MODE);
```

## 4.常见数据类型

看菜鸟教程：https://www.runoob.com/mysql/mysql-data-types.html
## 5.常见约束
### 约束介绍	
含义：一种限制规则，用于限制表中的数据，为了保证表中的数据的完整性和一致性。

- NOT NULL：非空
- DEFAULT：设置字段默认值
- PRIMARY KEY：主键，一般对id字段标注主键，非空唯一
- UNIQUE：唯一，插入时数据库自动检查是否存在重复，存在重复的话会插入失败并报错
- CHECK：检查约束，MySQL不支持，postgresql支持，限制字段只能是某些值，如sex字段限制为男、女、未知
- FOREIGN KEY：外键用于限制两个表的关系，用于保证该字段的值必须来自于主表的关联列的值，在从表添加外键约束，用于引用主表中某列的值

添加约束的时机：

1. 创建表时----列级约束
2. 修改表时----表级约束

约束的添加分类：

	列级约束：
		六大约束语法上都支持，但外键约束没有效果
		
	表级约束：		
		除了非空、默认，其他的都支持
	区别：		
	位置			支持的约束类型			   			是否可以起约束名
	列级约束：	列的后面 语法都支持，但外键没有效果			不可以
	表级约束：	所有列的下面 默认和非空不支持，其他支持	可以（主键没有效果）

外键：（阿里开发手册建议不要用外键）

1、要求在从表设置外键关系
2、从表的外键列的类型和主表的关联列的类型要求一致或兼容，名称无要求
3、主表的关联列必须是一个key（一般是主键或唯一）
4、插入数据时，先插入主表，再插入从表
删除数据时，先删除从表，再删除主表

```sql
#方式一：级联删除
ALTER TABLE stuinfo ADD CONSTRAINT fk_stu_major FOREIGN KEY(majorid) REFERENCES major(id) ON DELETE CASCADE;
#方式二：级联置空
ALTER TABLE stuinfo ADD CONSTRAINT fk_stu_major FOREIGN KEY(majorid) REFERENCES major(id) ON DELETE SET NULL;	
```


### 一、创建表时添加约束

1.添加列级约束

2.添加表级约束

```sql
-- 语法：在各个字段的最下面
-- 【constraint 约束名】 约束类型(字段名) 
-- 如：
CONSTRAINT pk PRIMARY KEY(id),#主键
CONSTRAINT uq UNIQUE(seat),#唯一键
CONSTRAINT ck CHECK(gender ='男' OR gender  = '女'),#检查

constraint 约束名 foreign key(字段名) references 主表（被引用列）
CONSTRAINT fk_stuinfo_major FOREIGN KEY(majorid) REFERENCES major(id)#外键
```

3.通用的写法：★

```sql
CREATE TABLE IF NOT EXISTS stuinfo(
	id INT PRIMARY KEY,
	stuname VARCHAR(20),
	sex CHAR(1),
	age INT DEFAULT 18,
	seat INT UNIQUE,
	majorid INT,
	CONSTRAINT fk_stuinfo_major FOREIGN KEY(majorid) REFERENCES major(id) -- 外键索引名命名最好按以上方式
);
```


### 二、修改表时添加约束

>1、添加列级约束
alter table 表名 modify column 字段名 字段类型 新约束; # 不加就是默认约束，所以要注意不能随便用

>2、添加表级约束
alter table 表名 add 【constraint 约束名】 约束类型(字段名) 【外键的引用】;


例子：
```sql
lDROP TABLE IF EXISTS stuinfo;
CREATE TABLE stuinfo(
	id INT,
	stuname VARCHAR(20),
	gender CHAR(1),
	seat INT,
	age INT,
	majorid INT
)
DESC stuinfo;
#1.添加非空约束
ALTER TABLE stuinfo MODIFY COLUMN stuname VARCHAR(20)  NOT NULL;
#2.添加默认约束
ALTER TABLE stuinfo MODIFY COLUMN age INT DEFAULT 18;
#3.添加主键
#①列级约束
ALTER TABLE stuinfo MODIFY COLUMN id INT PRIMARY KEY;
#②表级约束
ALTER TABLE stuinfo ADD PRIMARY KEY(id);

#4.添加唯一

#①列级约束
ALTER TABLE stuinfo MODIFY COLUMN seat INT UNIQUE;
#②表级约束
ALTER TABLE stuinfo ADD UNIQUE(seat);


#5.添加外键
ALTER TABLE stuinfo ADD CONSTRAINT fk_stuinfo_major FOREIGN KEY(majorid) REFERENCES major(id); 
```

### 三、修改表时删除约束
```sql
#1.删除非空约束
ALTER TABLE stuinfo MODIFY COLUMN stuname VARCHAR(20) NULL;

#2.删除默认约束
ALTER TABLE stuinfo MODIFY COLUMN age INT ;

#3.删除主键
ALTER TABLE stuinfo DROP PRIMARY KEY;

#4.删除唯一
ALTER TABLE stuinfo DROP INDEX seat;

#5.删除外键
ALTER TABLE stuinfo DROP FOREIGN KEY fk_stuinfo_major;

SHOW INDEX FROM stuinfo;
```

### 四、标识列
又称为自增长列
含义：可以不用手动的插入值，系统提供默认的序列值

特点：
1、标识列必须和主键搭配吗？不一定，但要求是一个key

2、一个表可以有几个标识列？至多一个！

3、标识列的类型只能是数值型

4、标识列可以通过` SET auto_increment_increment=3;`设置步长，可以通过 手动插入值，设置起始值

二、修改表时设置标识列
`ALTER TABLE tab_identity MODIFY COLUMN id INT PRIMARY KEY AUTO_INCREMENT;`

三、修改表时删除标识列
`ALTER TABLE tab_identity MODIFY COLUMN id INT ;`

## 6.TCL事务
Transaction Control Language 事务控制语言

**对于事务的更加详细的说明，最好还是去看书《深入理解分布式事务》。**

>一个或一组sql语句组成一个执行单元，这个执行单元要么全部执行，要么全部不执行。如果单元中某条SQL语句一旦执行失败或产生错误，整个单元将会回滚。

#### 特点ACID

原子性(Atomicity)：要么都执行，要么都回滚。

一致性(Consistency)：保证数据的状态操作前和操作后保持一致。

隔离性(Isolation)：多个事务同时操作相同数据库的同一个数据时，一个事务的执行不受另外一个事务的干扰，MySQL通过锁和MVCC机制保证隔离性。

持久性(Durability)：一个事务一旦提交，则数据将持久化到本地，除非其他事务对其进行修改。

#### 事务的分类

1.隐式事务，没有明显的开启和结束事务的标志

比如`insert、update、delete`语句本身就是一个事务

2.显式事务，具有明显的开启和结束事务的标志

```sql
-- 1、开启事务：取消自动提交事务的功能
set autocommit=0;
-- 2、编写事务的一组逻辑操作单元（多条sql语句）
insert
update
delete
-- 可以设置回滚点：
savepoint 回滚点名;
-- 3、提交事务或回滚事务
-- 提交：commit;
-- 回滚：rollback;
-- 回滚到指定的地方：rollback to 回滚点名;
```

#### 使用到的关键字

```sql
set autocommit=0;
start transaction;
commit;
rollback;

savepoint  断点
commit to 断点
rollback to 断点
-- 设置隔离级别：
set session|global  transaction isolation level 隔离级别名;
-- 查看隔离级别：
SELECT @@transaction_isolation;
```

## 7.视图
含义：理解成一张虚拟的表
mysql5.1版本出现的新特性，是通过表动态生成的数据
视图和表的区别：

|      | 使用方式 | 占用物理空间                |
| ---- | -------- | --------------------------- |
| 视图 | 完全相同 | 不占用，仅仅保存的是sql逻辑 |
| 表   | 完全相同 | 占用                        |

视图的好处：

1、sql语句提高重用性，效率高
2、和表实现了分离，提高了安全性
3、提供一定程度上的逻辑独立性
4、集中展示用户所感兴趣的特定数据

### 视图操作

```sql
-- 创建语法：
CREATE VIEW  <视图名> AS <查询语句>;

-- 1、查看视图的数据
SELECT * FROM my_v4;
SELECT * FROM my_v1 WHERE last_name='Partners';
-- 2、插入视图的数据
INSERT INTO my_v4(last_name,department_id) VALUES('虚竹',90);
-- 3、修改视图的数据
UPDATE my_v4 SET last_name ='梦姑' WHERE last_name='虚竹';
-- 4、删除视图的数据
DELETE FROM my_v4;

-- 5、视图结构删除
DROP DROP VIEW 视图名;
-- 6、视图结构查看
DESC test_v7;
SHOW CREATE VIEW test_v7;
```


### 某些视图不能更新
```sql
包含以下关键字的sql语句：分组函数、distinct、group  by、having、union或者union all
常量视图
Select中包含子查询
join
from一个不能更新的视图
where子句的子查询引用了from子句中的表
```

>注意：这里其实尽量不去更新视图，而且大部分视图都是不能更新的。

### 视图逻辑的更新
```sql
-- 方式一：
CREATE OR REPLACE VIEW test_v7
AS
SELECT last_name FROM employees
WHERE employee_id>100;

-- 方式二:
ALTER VIEW test_v7
AS
SELECT employee_id FROM employees;

SELECT * FROM test_v7;
```
## 8.存储过程

> 注意：阿里开发规范禁用存储过程

含义：一组经过预先编译的sql语句的集合

因开发中一直没有用过这个玩意，所以将原有笔记全部删除，如果需要的话，可以看菜鸟教程：https://www.runoob.com/w3cnote/mysql-stored-procedure.html

## 9. 函数

含义：一组预先编译好的SQL语句的集合，理解成批处理语句


函数和存储过程的区别：
>存储过程：可以有0个返回，也可以有多个返回，适合做批量插入、批量更新
>
>函数：有且仅有1 个返回，适合做处理数据后返回一个结果

因开发中一直没有自定义函数，只用过内置函数，所以删除原有笔记，并附上菜鸟教程中MySQL内置函数：https://www.runoob.com/mysql/mysql-functions.html

## 10.流程控制语句

用在存储过程和函数中。所以就删除原有笔记了。

# 高性能MySQL

## 1.配置

### 慢查询日志

在Linux服务器上的配置文件my.cnf下如下配置：

```shell
# 慢查询日志
slow_query_log=on # 这个参数设置为ON，可以捕获执行时间超过一定数值的SQL语句
slow_query_log_file=/opt/mysql/mysql_slow_query.log  # 记录日志的文件名，必须有写权限
long_query_time=1 # 当SQL语句执行时间超过此数值时，就会被记录到日志中，建议设置为1或者更短
```

重启MySQL服务即可。

## 2.索引

在Innodb存储引擎下，主键是聚族索引，索引结构为B+树，在叶子结点挂载数据。

一篇很好的讲解：https://www.cnblogs.com/nijunyang/p/11406688.html

## 3.锁

一篇讲得比较好的博文：https://blog.csdn.net/cy973071263/article/details/105188519

# postgresql

## 简介

### 客户端程序

![客户端程序](pictures/客户端程序.png)

- psql

![psql](pictures/psql.png)

- pgAdmin

![pgAdmin](pictures/pgAdmin.png)

### 服务器程序

![服务器程序](pictures/服务器程序.png)



### 资料

![psgresql资料1](pictures/psgresql资料1.png)

![psgresql资料2](pictures/psgresql资料2.png)

postgresql14官方中文文档：http://www.postgres.cn/docs/14/index.html

## jdbc.properties

```properties
driver=org.postgresql.Driver
url=jdbc:postgresql://localhost:5432/GradeDB
username=postgres
password=123456
```
## SQL语言

- DDL	数据定义语言
- DML   数据操纵语言
- DQL    数据查询语言
- DCL    数据控制语言
- TPL     事务处理语言
- CCL     游标控制语言

### DDL

数据定义语言。

#### 数据库

不是很建议直接SQL创建，可以先创建之后，在通过GUI方式修改数据库的各个属性。

```sql
CREATE DATABASE <数据库名>;

ALTER DATABASE <数据库名> RENAME TO <新数据库名>;

DROP DATABASE <数据库名>;
```

> 注意：至于，创建和修改数据库的更多配置参数，可以选择看书p59，或者直接GUI方式。

#### 表

```sql
--1.创建表
CREATE TABLE <表名> (
	<列名> <数据类型> [列完整性约束],
    ...
    [表完整性约束]
);
-- 列约束关键词：
-- PRIMARY KEY,	NOT NULL,	NULL,	UNIQUE,	CHECK,	DEFAULT等
/*注意：列约束中的primary key只能定义单列主键，
若要定义复合主键，需要使用表约束方式*/
-- 表约束关键词
-- CONSTRAINT <表级约束名> PRIMARY KEY(主键列)
-- CONSTRAINT <表级约束名> FOREIGN KEY(外键列) references 表名2(表名2的主键) on delete CASCADE on update CASCADE
-- CASCADE为级联操作，RESTRICT为限制操作，即有依赖时候，不删除或更新
--2.修改表
--修改表看书p64
-- 在修改表的时候，可以添加外键约束
--3.删除表
drop table <表名>
```

例子如下：课程信息表Course

```sql
CREATE TABLE Course(
--	courseId char(4) PRIMARY KEY,
    courseId char(4) not null,
    teacherId char(4) not null,
    courseName varchar(20) NOT NULL UNIQUE,
    courseType varchar(20) NULL CHECK(courseType IN ('基础课','专业','选修')),
    courseCredit smallint null,
    coursePeriod smallint null,
    testMethod char(10) not null default '闭卷考试',
-- 这里用表级约束定义复合主键
    CONSTRAINT Course_PK primary key(couseId,teacherId)
);
-- 定义外键 级联删除
ALTER table Course 
add CONSTRAINT FK_COURSE_TEACHER foreign key(teacherId) 
references teacher(teacherId) on delelte CASCADE;
```

#### 索引

索引是一种针对表中指定列的值进行排序的数据结构，使用它可以**加快表中数据的查询**。

类似于图书的目录结构，将索引列的值及索引指针数据保存在索引结构中。

此后在数据查询时，先在索引结构中查找符合条件的索引指针值，根据索引指针快速找到对应的数据记录。

- 索引优点
  - 提高关系表中数据查询速度
  - 快速连接关联表
  - 减少分组和排序时间
- 索引开销
  - 创建和维护索引需要较大开销
  - 索引额外占用存储空间
  - 数据操纵因维护索引带来系统性能开销

##### 索引操作

```sql
# 创建索引
create [unique] index birthday_index on Student(birthday);
create [unique] INDEX <索引名> on <表名><(列名[,...])>;
# unique 不允许有重复索引值

# 修改索引名
alter index 索引名 rename to new_name;

# 删除索引
drop index 索引名;
```

### DCL

数据控制SQL语句。

对用户数据访问权进行控制的语句。

控制特定用户和角色对表、视图、存储过程、触发器等数据库对象的访问权限。

- GRANT授权语句
- REVOKE权限回收语句
- DENY拒绝权限语句
  - 用于拒绝给用户或角色赋予权限；
  - 并防止用户或角色通过其组或角色成员继承权限；

```sql
-- 授权
GRANT <权限列表> ON <数据库对象> TO <用户|角色> [with GRANT OPTION];
GRANT SELECT,INSERT,UPDATE,DELETE ON table_A TO ROLE_A;

-- 权限回收
REVOKE <权限列表> ON <数据库对象> FROM <用户|角色>;
REVOKE DELETE ON table_A from ROLE_A;

-- 权限拒绝
DENY <权限列表> ON <数据库对象> TO <用户|角色>;
DENY DELETE On table_A TO ROLE_B;
```

## 数据库应用编程

### 存储过程

存储过程是一种数据库对象，由一组能完成特定功能的SQL语句构成。
把重复使用的SQL语句逻辑块封装起来，经编译后，存储在数据库服务器端，被调用时候，不需要再次编译。

PostgreSQL 10之前版本只能使用CREATE FUNCTION命令创建存储过程，
PostgreSQL 10之后版本也可以使用CREATE PRECEDURE命令创建存储过程。

postgresql内置的过程控制语言为PL/pgSQL.

>**postgresql将存储过程和函数统称为存储过程。**

>存储过程与函数的异同：相同点都是过程程序，区别在于存储过程不返回值，因此没有返回类型声明。



#### 创建语法
![存储过程](pictures/存储过程.png)
- default_expr: 指定参数默认值。
- argtype : 函数返回值的数据类型
- retype：returns返回值的数据类型。
如果存在out或者inout参数，可以省略returns语句。


例子1：
创建一个名为countRecords()的过程函数统计STUDENT表的记录数。
```sql
CREATE OR REPLACE FUNCTION countRecords ()  
RETURNS integer AS $$  
declare  
    count integer :=0;  -- 默认为0
BEGIN  
   SELECT count(*) into count FROM STUDENT;  
   RETURN count;  
END;  
$$ LANGUAGE plpgsql;	-- 告诉编译器该函数使用PL/pgSQL实现
```

例2:创建一个名为add_data（a,b,c）的存储过程实现a+b相加运算，并将结果放入c。
```sql
CREATE OR REPLACE PROCEDURE add_data(a integer, b integer,inout c integer)
AS $$
 Begin
  c=a+b;
 End;
$$ LANGUAGE plpgsql;
```

例子3：返回一个表对象的存储过程
```sql
CREATE OR REPLACE FUNCTION countByCourse()  
RETURNS TABLE(c1 bigint, name character varying) AS $$  
begin
 return query
	select	count(*),course.cname as name from grade inner join course on grade.cid=course.cid 
	where grade.score<60
	group by grade.cid,course.cname;
end;
$$ LANGUAGE plpgsql;
```
此时调用这个存储过程：
`select c1,name from countByCourse(); `


#### 调用函数或存储过程
```sql
-- 函数
select 函数名（参数）；
-- 或者：
select  * from 函数名（参数）

-- 存储过程
CALL  存储过程名（参数）
```


#### 删除存储过程
![删除存储过程](pictures/删除存储过程.png)

#### PL/pgSQL基本语法(*)
##### 声明局部变量
```sql
variable_name [constant] variable_type [not null] [{default |:=}expression];
```
- constant 修饰的变量为常量，不能修改。
- not null 修饰的变量不能为null值，则必须在声明时候赋予非空默认值。

```sql
/*变量声明的语法如下：
 declare 
     变量名  变量类型；
如果声明变量为记录类型，变量声明格式为： variable_name RECORD;
注：RECORD不是真正的数据类型，只是一个占位符。*/
-- 例如：
declare 
  count intger；
  rec RECORD ；
```

##### 基本语句
###### 赋值语句
```sql
salePrice:=20;
tax:=salePrice*0.13;
```
等号两端的变量和表达式的类型要相容，否则会产生运行时错误。

###### select into
```sql
select into target select_expressions from ...;
```

- target 可以为一个记录变量(record)、行变量、一组用逗号分隔的简单变量和记录/行字段的列表。
- 如果将一行或者一个变量列表作为target，那么查询值必须准确匹配目标的结构。
- 如果target为记录变量(record)，它将自动将自己构造成命令结果行和行列型。
	- 如果命令返回0行，目标赋值为空值。
	- 如果命令返回多行，只有第一行赋值给目标，其他行忽略。
- 执行select into 语句之后，可以检查内置变量found判断本次赋值是否成功。

例子：
```sql
create or replace function mytest(in a integer,in b integer)
returns float as $$
declare 
	price float;
	tax float;
	stu record;
begin
	price :=a+b;
-- 	tax:=price*0.1;
	select into tax price*0.1;
	select into stu * from student where student.sid=100;
	if found then 
		return 0;
	end if;
	return tax;
end;
$$ language plpgsql;

select mytest(4,2); -- 返回0.6
```

##### 控制结构语句
###### 条件语句if

在PL/pgSQL中有以下三种形式的条件语句，与其他高级语言的条件语句意义相同。

```sql
-- 1) IF-THEN 
IF boolean-expression THEN
     statements
END IF; 
-- 2)IF-THEN-ELSE
IF boolean-expression THEN
 statements
ELSE
END IF;
-- 3) IF-THEN-ELSIF-ELSE
IF boolean-expression THEN
 statements
ELSIF boolean-expression THEN
 statements
ELSIF boolean-expression THEN
     statements
ELSE
  statements
END IF; 
```

###### LOOP
```sql
-- 1) LOOP基本循环语句
LOOP
 statements
END LOOP [ label ];
-- lable是可选标签，由exit和continue语句使用，用于嵌套循环中识别循环层次。
```
loop定义了一个无条件循环语句，直到exit或return语句终止。

###### exit和continue

```sql
-- 2) EXIT循环退出语句
 EXIT [ label ] [ WHEN expression ];
例如： LOOP
                   count=count+1;
                   EXIT WHEN count >100;
              END LOOP;

-- 3) CONTINUE循环继续语句
 CONTINUE [ label ] [ WHEN expression ];
例如： LOOP
          count=count+1;
          EXIT WHEN count > 100;
          CONTINUE WHEN count < 50;
          count=count+1;
    END LOOP; 
```
###### while
```sql
-- 4) WHILE
WHILE expression LOOP
    statements
END LOOP [label];
例如：
 WHILE amount_owed > 0 AND  balance > 0 LOOP
     --do something
 END LOOP;
```
###### for
```sql
-- 5) FOR
FOR name IN [ REVERSE ] expression .. expression LOOP
    statements
END LOOP [label];
例如： FOR i IN 1..10 LOOP
                    RAISE NOTICE 'i IS %', i;
              END LOOP;
 FOR i IN REVERSE 10..1  LOOP
      --do something
 END LOOP;
```
- 变量name自动被定义为integer类型；
- name每次迭代自增1；
- 如果声明了reverse，每次迭代自减1

##### 遍历命令结果
```sql
FOR  record_or_row IN query  LOOP
     statements
END LOOP [label]; 
```
例子：从学生表Student查询结果集中，循环输出数据
```sql
create function Out_Record() 
returns RECORD as $$
declare 
    rec RECORD;
begin
 for  rec in  SELECT * FROM student loop
    raise notice  '学生数据：%,%', rec.sid, rec.sname; 
 end loop;
 return rec;
end;
$$ language plpgsql;

-- 调用函数
select Out_Record();
```
![遍历命令结果](pictures/遍历命令结果.png)


#### 修改存储过程
1. 如果是修改存储过程的**业务逻辑**，可以使用`create or replace function`对其源码重新修改。

2. 如果是修改存储过程的**拥有者、名字、所属模式**等，需要用`alter function` 命令。
```sql
-- 修改名字
alter function name ([[argmode][argname]argtype[,...]])
	rename to new_name;

-- 修改所有者
alter function name ([[argmode][argname]argtype[,...]])
	owner to new_owner;

-- 修改所属模式
alter function name ([[argmode][argname]argtype[,...]])
	set schema new_schema;
```


### 触发器

触发器是特殊类型的存储过程，其过程程序由事件(如INSERT、UPDATE、DELETE操作等) 触发而自动执行。
PostgreSQL 触发器是数据库的回调函数，它会在指定的数据库事件发生时**自动执行/调用**。

**触发器用途**：可以实现比约束**更复杂的数据完整性**，经常用于加强数据的完整性约束和业务规则。
如使用触发器来替代外键的参照完整性约束。(见实验作业3)

**触发器特点**
与数据库对象相关：在表或视图上执行DML、DDL操作，其定义的事件触发过程程序执行。 
DML事件触发：由执行INSERT、DELETE、UPDATE操作时触发。
DDL事件触发：由执行CRETE、ALTER、DROP、SELECT INTO操作时触发。

**触发器与存储过程有何异同？**

- 均为过程程序
- 触发器由事件激发自动执行处理程序，存储过程需要由其他程序调用执行

**在数据库应用开发中，触发器主要应用在哪些场景？**

- 验证输入数据的正确性

- 执行复杂的业务规则

- 审计跟踪在数据库表上的数据插入、修改、删除

- 将数据复制到不同数据库表上以实现数据一致性

#### 触发器分类
1. 按**DML**操作语句分类：  
INSERT触发器、DELETE触发器、UPDATE触发器 
![触发器分类](pictures/触发器分类.png)
2. 按触发器**执行次数**分类： 
（1）**语句级触发器**：由关键字**FOR  EACH  STATEMENT**声明，在触发器作用的表上执行一条SQL语句时，该触发器程序只执行一次，即使是修改了零行数据的SQL，也会导致相应的触发器执行。FOR EACH STATEMENT为**默认值**。
（2）**行级触发器**：由关键字**FOR  EACH  ROW**标记的触发器，当触发器所在表中数据发生变化时，每变化一行就会执行一次触发器程序。
3. 按触发的时间分类： 
（1）**BEFORE**触发器：在触发事件之前执行触发器程序。
（2）**AFTER**触发器：在触发事件之后执行触发器程序。
（3）**INSTEAD OF**触发器：当触发事件发生后，执行触发器中指定的过程程序，而不是执行产生触发事件的SQL语句。
![触发器与事件关系](pictures/触发器与事件关系.png)

#### 触发器程序中的特殊变量
1. **NEW**
   
   > NEW变量数据类型是RECORD。
   > 对于行级触发器，它保存了INSERT或UPDATE操作产生的**新行记录数据**。
   > 对于语句级触发器，它的值是NULL。
   
2. **OLD**

   > OLD变量数据类型是RECORD。
   > 对于行级触发器，它保存了UPDATE或DELETE操作修改或删除的**旧行记录数据**。
   > 对于语句级触发器，它的值是NULL。

3. **TG_OP**

   > TG_OP变量数据类型是text，其值为INSERT、UPDATE、DELETE 字符串之一。使用它来获取触发器是由哪类操作引发。
   > **注意：如果需要通过比较TG_OP来进行判断操作时，必须是大写的INSERT,而不能是小写的insert。**

4. 其他的特殊变量

   > 看书p247，用得不多。


#### 创建触发器
```sql
CREATE [CONSTRAINT] TRIGGER <触发器名>
{BEFORE | AFTER | INSTEAD OF } {event [OR ...]}
ON 表名|视图名
[FROM referenced_table_name]
[FOR [EACH] { ROW | STATEMENT} ]
[ WHEN (condition) ]
EXECUTE PROCUDURE function_name(arguments)
```

- event 	

  > INSERT、UPDATE、DELETE或TRUNCATE之一，声明激发触发器的事件，可以用OR声明多个；

- referenced_table_name  

  > 用于有外键约束的两张表，触发器所依附的表所参照的主表，但一般不使用此参数；

- condition

  > 条件布尔表达式，关键字when根据此表达式决定下一步的触发器函数是否被实际执行，返回true才调用该函数；

- function_name

  > 触发器函数，必须在创建触发器之前创建，没有接受参数并且返回trigger类型
  >
  > 此函数将在触发器被触发时被调用


#### 触发器使用步骤
练习：在如下雇员表emp中被插入或更新一行数据时，触发函数程序将当前用户名和时间标记在该数据行中,并且检查雇员的姓名以及薪水是否为空，若为空，输出警示信息。

##### 1.建表
```sql
CREATE TABLE emp (
    empID	char(3) primary key,
    empname varchar(20),
    salary integer,
    last_date timestamp,
    last_user varchar(20)
);
```
##### 2.触发器函数程序
**这个函数必须返回trigger类型**
```sql
CREATE FUNCTION emp_stamp() RETURNS trigger AS $$
    BEGIN
        -- 检查给出了 empname 以及 salary
        IF NEW.empname IS NULL THEN
            RAISE EXCEPTION '雇员名不能为空';
        END IF;
        IF NEW.salary IS NULL THEN
            RAISE EXCEPTION '% 薪水不能为空', NEW.empname;
        END IF;
        -- 记住谁在什么时候改变了工资单
        NEW.last_date := current_timestamp;
        NEW.last_user := current_user; -- 返回当前用户
        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;
```

##### 3.触发器定义程序
```sql
CREATE TRIGGER emp_stamp 
BEFORE INSERT OR UPDATE ON emp
    FOR EACH ROW EXECUTE FUNCTION emp_stamp();
```

##### 4.案例

案例为数据库大作业3. 当然更建议看课本。

```sql
-- 删除触发器
drop trigger if exists student_trigger on student;
-- 触发器函数程序
create or replace function student_trigger()
returns trigger as $student_trigger$
begin
	raise notice '进入到触发器';
	if (TG_OP = 'DELETE') then -- 级联删除
		raise notice '删除sid：%,sname：%',old.sid,old.sname;
		delete from grade where sid=old.sid;
		return old;
	elsif (TG_OP='UPDATE') then  -- 级联更新
		raise notice '学生：% 更新',old.sid;
		-- 当修改了学号的时候级联更新
	 	update grade set sid=new.sid where old.sid!=new.sid and sid=old.sid;
		return new;
	end if;
	return null;
end;
$student_trigger$ language plpgsql;

-- 触发器定义程序
create trigger student_trigger
	after delete or update on student
	for each row
	execute function student_trigger(); 
```



#### 触发器管理

##### 列出触发器

```sql
--可以把从 pg_trigger 表中把当前数据库所有触发器列举出来：
SELECT * FROM pg_trigger;


--列举出特定表的触发器，语法如下：
SELECT tgname FROM pg_trigger, pg_class 
	WHERE tgrelid=pg_class.oid AND relname='表名';
```

##### 删除触发器
```sql
DROP TRIGGER [ IF EXISTS ] trigger_name ON table_name [ CASCADE | RESTRICT ]
/*主要参数说明：
IF EXISTS：如果该触发器不存在，则发出提示而不是抛出错误。
table_name：触发器定义所依附的表名称。
CASCADE：级联删除依赖此触发器的对象。
RESTRICT：如果有依赖对象存在，则拒绝删除。该参数缺省是拒绝删除。*/

--  例 将上述触发器score_audit_trig删除，同时级联删除依赖触发器的对象。
  DROP TRIGGER IF EXISTS score_audit_trig ON grade CASCADE;
```

##### 修改触发器

```sql
ALTER TRIGGER name ON table_name RENAME TO new_name
/*主要参数说明：
name：现有触发器名称
table_name：该触发器作用的表名字
new_name：触发器的新名字*/
```





### 事件触发器

事件触发器是针对一个数据库**DDL操作**的触发器，它可以捕获数据库级别上的对象DDL事件。

事件触发器定义在数据库级，权限相对较大，所以只有**超级用户**才能创建和修改事件触发器。

#### 事件触发器与DDL操作

PostgerSQL支持的事件触发器类型：

- ddl_command_start：在DDL开始前触发。

- ddl_command_end：在DDL结束后触发。

- sql_drop：删除一个数据库对象前被触发。

![事件触发器](pictures/事件触发器.png)

#### 创建事件触发器

```sql
CREATE EVENT TRIGGER name
ON event
[WHEN filter_variable IN (filter_value [, ...]) [ AND ... ] ]
EXECUTE PROCEDURE function_name();
/*
（1）name：触发器名称，在数据库内必须唯一
（2）event：触发器的事件名称
（3）filter_variable：过滤事件的变量名称
（4）filter_value：filter_variable相关值，如('DROP FUNCTION', 'CREATE TABLE')
（5）function_name：过程函数，没有参数，返回值类型为event_trigger。
*/
```

具体细节看书p251。



#### 修改事件触发器

```sql
ALTER EVENT TRIGGER name DISABLE;
ALTER EVENT TRIGGER name ENABLE;
ALTER EVENT TRIGGER name OWNER TO new_owner;
ALTER EVENT TRIGGER name RENAME TO new_name;
/*
（1）DISABLE：禁用已有的触发器
（2）ENABLE：使该事件触发器激活
（3）name：事件触发器的名称
（4）new_owner：事件触发器的新属主名称
（5）new_name：事件触发器的新名称
*/
```

#### 删除事件触发器

```sql
DROP EVENT TRIGGER [ IF EXISTS ] name [ CASCADE | RESTRICT ]
/*
（1）IF EXISTS：如果事件触发器不存在，系统不会抛出错误，只会产生提示信息。 
（2）name：事件触发器名称 
（3）CASCADE：级联删除依赖于事件触发器的对象。 
（4）RESTRICT：如果有依赖于事件触发器的对象，则不允许删除这个事件触发器。这是默认行为。
*/
```



### 游标

存储过程和触发器都是用数据库所支持的过程化语言编写的，处理的对象是函数过程中定义的变量，**除数组和记录型变量(record类型)**外，**一般函数中的每个变量每次只能存储一条记录**。
而查询处理的对象是集合，大多数数据库都提供游标作为新的数据处理方法，用于存储SQL语句查询结果。

游标是一种临时的数据库对象，存放从表中查询返回的数据行副本，提供了从多条数据记录的结果集中，每次提取一条记录的机制。

**游标（Cursor**）是一种指向数据库查询结果集的指针，通过它可以从结果集中**提取每一条记录**进行处理。

#### 游标使用步骤

##### 1.声明游标

```sql
/*
（1）使用refcursor定义的游标变量
（2）游标声明语句
	游标名  CURSOR [ ( arguments ) ] FOR query*/
-- 例子：
Declare
curVars1 	refcursor;	-- 这种方法声明的 游标 尚未绑定查询语句
curStudent CURSOR FOR  SELECT * FROM student;
curStudentOne CURSOR (key integer)  IS 
            SELECT * FROM student WHERE SID = key;
```

注意：此时游标仅为声明，DBMS还没有执行查询语句，游标中没有可访问的数据。

##### 2.打开游标

游标在使用之前必须被打开，打开游标就是执行游标所绑定的查询语句。

```sql
/*（1）OPEN… FOR 
        其语句格式： OPEN unbound_cursor FOR query;
       打开未绑定的游标变量，其query查询语句是返回记录的SELECT语句。*/
-- 例如：
OPEN curVars1 FOR SELECT * FROM student WHERE SID = mykey;
/*（2）OPEN… FOR EXECUTE
     其语句格式为：  OPEN unbound_cursor FOR EXECUTE query-string;   
     打开未绑定的游标变量，EXECUTE将动态执行查询字符串*/
-- 例    
OPEN curVars1 FOR EXECUTE 'SELECT * FROM ' || quote_ident($1);
-- 其中$1是指由存储过程传递的第1个参数。

/*（3）打开绑定游标的语句格式：
         OPEN bound_cursor [ ( argument_values ) ];   
        适用于绑定的游标变量。如果游标变量在声明时包含接收参数，在打开游标时需要传递参数，该参数将传入到游标声明的查询语句中执行。*/
-- 例如：
OPEN curStudent;
OPEN curStudentOne (‘20160230302001’); 
```

##### 3.使用游标提取值

游标的打开和读取必须在同一个事务中，因为事务结束会隐式关闭游标。

```sql
/*其语句格式：    FETCH cursor INTO target;
FETCH命令从游标中读取当前指针所指向记录的数据到目标中。可通过PL/pgSQL内置的系统变量FOUND来判断读取是否成功。*/
--例
FETCH curVars1 INTO rowvar; 
FETCH curStudent INTO SID, Sname, sex;
```

##### 4.关闭游标

```sql
CLOSE cursorName;
-- 当处理结束游标数据后，需要关闭游标，以释放其占有的系统资源，主要是释放占用的内存资源。

-- 游标被关闭后，如果需要再次读取游标中的数据，需要再次open打开游标。
```

#### 例1 

在函数中使用游标查询student表的学号、学生姓名和性别。

```sql
CREATE OR REPLACE FUNCTION cursorDemo() 
returns boolean as $$ 
Declare                       		--定义变量及游标
 unbound_refcursor refcursor;  		--声明游标变量
 vsid varchar;               		--学号变量
 vsname varchar;            			--姓名变量
 vsex varchar;      		 	--性别变量 
begin  
 open unbound_refcursor for execute 'select sid,sname,sex from student';  		--打开未绑定的游标变量执行
loop  						--开始循环
  fetch unbound_refcursor into vsid,vsname,vsex; --从游标中提取值赋予变量 
	 if found then  --如果从游标中取到数据，则输出这些数据
         raise notice '%,%,%',vsid,vsname,vsex; 
     else 
         exit; 
     end if; 
end loop;                    		--结束循环
close unbound_refcursor;        		--关闭游标
raise notice '取数据循环结束...';		--打印消息
return true;                    		--为函数返回布尔值
exception when others then  			--处理异常
  raise exception 'error-(%)', sqlerrm;	-- sqlerrm错误代码变量
end;  						--结束
$$   LANGUAGE plpgsql;  			--规定语言
select cursorDemo();	-- 执行存储过程
drop function cursorDemo();	-- 删除存储过程
```

#### 例2

编写带参数的游标函数，从成绩表中查询分数大于某给定值的学号和课程号。

```sql
create or replace function cusorGrade(myscore int) returns void as $$ 
   declare
     vstuscore Grade%ROWTYPE;  			--声明与表Grade结构相同的行变量
     vstucursor cursor( invalue int) 		
       for select courseid,studentid,grade from Grade where grade>=invalue order by studentid; 						--声明带有输入参数的游标
   begin
     open vstucursor(myscore);  			--打开带有参数的游标
     loop 
	fetch vstucursor into vstuscore;
       exit when not found;  			-- 假如没有检索到记录，结束循环处理
       raise notice '%,%,%',vstuscore.studentid,vstuscore.courseid,vstuscore.grade; 
      end loop;
      close vstucursor;  				--关闭游标
   end;
$$ language plpgsql;
```

## 数据库管理

### 事务管理

Transaction Control Language 事务控制语言.

**事务的特征**

> （ACID）
> 原子性(Atomicity)：要么都执行，要么都回滚
> 一致性(Consistency)：保证数据的状态操作前和操作后保持一致
> 隔离性(Isolation)：多个事务同时操作相同数据库的同一个数据时，一个事务的执行不受另外一个事务的干扰
> 持久性(Durability)：一个事务一旦提交，则数据将持久化到本地，除非其他事务对其进行修改

**TCL语句：**

> - BEGIN或START TRANSACTION	为事务开始语句
> - ROLLBACK  为事务回滚语句
> - COMMIT      为事务提交语句
> - SAVEPOINT 为事务保存点语句
>
> ```sql
> START TRANSACTION;
> insert into table_A values(123);
> update table_A set id=1234;
> select * from table_A;
> COMMIT;	-- 或者是 ROLLBACK;
> 
> BEGIN;
> insert into table_A values(123);
> SAVEPOINT point1;
> update table_A set id=1234;
> select * from table_A;
> ROLLBACK to point1;
> commit;
> ```

**注意**：

在事务处理语句块中，仅能使用DML和DQL语句，不能使用DDL语句，

因为DDL语句会在数据库中自动提交，导致事务中断。

默认情况下，每条SQL语句构成一个单独的事务。

### 并发控制

并发控制指的是DBMS运行多个并发事务程序时，为确保各个事务独立正常运行，并防止相互干扰，保持数据一致性，所采取的控制和管理。

目的是确保一个事务的执行不会对另一个事务的执行产生不合理的影响，解决可能产生的数据不一致、事务程序死锁问题。

保证事务执行的隔离性。

#### 并发问题

##### 1.脏读

Dirty Read。一个事务读取了另一个事务修改后的数据，但修改数据的事务因某种原因失败，数据未提交，读取到了一个脏数据。

脏数据是对未提交数据的统称。

![脏读](pictures/脏读.png)

##### 2.不可重复读

![不可重复读](pictures/不可重复读.png)

注意：如果不是更新操作，而是删除操作，即第二次读取时，发现某些记录消失了，也是不可重复读。

##### 3.幻读

在上面的不可重复读基础上，如果不是更新操作，而是添加操作，即第二次读取发现多了某些记录，称为幻读。

幻读其实是不可重复读的一种，在事务并发控制中，一般将其归于不可重复读。

##### 4.丢失更新

事务T1对共享数据进行更新，再次查询该数据时，发现与自己的更新值不一样。即为丢失更新。

![丢失更新](pictures/丢失更新.png)

#### 并发事务调度

对于调度机制，数据库锁机制，并发控制协议，事务隔离级别，看书p168.







### 安全管理

#### 风险

![数据库面临的安全风险](pictures/数据库面临的安全风险.png)



#### 数据库系统安全模型

![数据库系统安全模型](pictures/数据库系统安全模型.png)

数据库安全一般采用多层安全控制体系进行安全控制和管理。

- 用户管理，每次连接数据库都要在DBMS进行身份验证，合法用户才能进入；

- 权限管理，只有具有一定的数据库对象操作权限，才能操作访问数据库对象；

- 角色管理，为了方便对总多用户及其权限进行管理，DBMS通常将一组具有相同权限的用户定义为角色。不同的角色代表不同的权限集合的用户集合。

数据库系统安全模型中，最基本的安全管理手段是DBMS提供的**用户授权**和**访问权限控制功能**。

一个用户可以对应多个角色，每个角色可以对应多个用户。用户、角色都可以被赋予数据库对象访问权限。



![访问权限表](pictures/访问权限表.png)



#### 角色管理

> 在DBMS中，为了方便对众多用户及其权限进行管理，通常将一组具有相同权限的用户定义为角色。
> 不同角色代表**不同权限集合**的用户集合。

**角色分类：**

- 系统角色
  - 数据库内建的角色，已经被定义好了相应的操作权限，如postgres就是一个系统角色，具有系统管理员的所有权限。
- 用户定义角色
  - DBA(数据库系统管理员)根据业务应用需求，设计了不同权限范围的用户类别。

角色管理是对用户自定义角色进行操作管理。

**角色管理内容**

- 角色创建
- 角色修改
- 角色删除

**角色管理方式**

- 执行SQL语句管理角色
- GUI操作管理角色

##### SQL语句

![角色管理SQL语句](pictures/角色管理SQL语句.png)

option属性如下：

![角色option属性](pictures/角色option属性.jpg)

##### GUI方式

看书。

#### 权限管理

##### 权限类别

- 数据库对象**访问**操作权限
  - 指用户被赋予的特定数据库对象的**数据访问操作**权限
  - 如对数据库表的crud
- 数据库对象**定义**操作权限
  - 指用户在数据库中被赋予的**数据库对象**创建、删除、和修改权限。
  - 如对库、表、视图、存储过程、自定义函数、索引等对象的cud。

系统管理员(超级用户)拥有最高权限，可以对其他角色或用户进行权限管理。

数据库对象拥有者(dbo)对其所拥有的对象具有全部权限。

普通用户(user)只具有被赋予的数据库访问操作权限。

##### 权限管理SQL

基本操作：授予权限(GRANT)、收回权限(REVOKE)、拒绝权限(DENY)。

```sql
GRANT SELECT,INSERT,UPDATE,DELETE ON table_name TO "role_A";
REVOKE delete on table_name from "role_A";
DENY delete on table_name to "user_A";
```

> 权限授予之后，如何通过GUI方式简单查看权限情况呢？
> 答：书上p183.
> 即在pgadmin4中，右键点击某个表对象，打开属性对话框，在点击"安全"属性页，即可看到这个表的各个用户/角色的权限情况。

##### 权限继承

```sql
GRANT "role_A" to "user_A";
```

这样即可将用户纳入角色的成员，称为角色分组的一员，并获取角色相同的权限。



##### 权限列表界面

![权限列表查看](pictures/权限列表查看.png)



#### 用户管理

![用户管理](pictures/用户管理.png)

> 以下将记录SQL操作。
> GUI方式管理去看书。

##### 用户创建

```sql
CREATE USER user_name [[with] option[...]];
-- 例子
create user "user_A" with
 login
 NOSUPERUSER
 NOCREATEDB
 NOCREATEROLE
 INHERIT
 NOREPLICATION
 CONNECTION LIMIT -1 -- -1 表示连接数不受限
 PASSWORD '123456';
COMMENT on ROLE "user_A" IS "用户A";

-- GRANT "role_A" to "user_A"; 	-- 将角色A分派给用户A，用户A成为角色A的一个分组成员，获得同role_A的权限
```

其option属性如下：

![用户option属性](pictures/用户option属性.jpg)

##### 用户修改

```sql
-- 修改用户属性
alter USER user_A 
	connection limit 10
	password '123456';
-- 修改用户名称
alter USER user_A
	rename to user_B;
-- 修改用户的参数值
alter USER user_B
	set <参数项> {TO | =} {value | DEFAULT};
-- 重置用户参数值
alter USER user_B
	RESET <参数项>;	
```

##### 用户删除

```sql
DROP USER user_A;
```



#### 最佳实践

```tex
0.设计好库、表、索引、视图、存储过程、触发器
1.先设计存取权限控制模型
2.创建角色并给角色授权
3.创建用户
4.用户继承角色的权限(GRANT "ROLE_A" TO "USER_A";)
5.验证权限模型正确性
如有不理解的地方，可以去看数据库的实验作业报告。
```

### 备份和恢复

待补充

看书

## java嵌入式SQL

### 获取连接

#### 1.注册驱动

```java
Class.forName("org.postgresql.Driver");
//加载PostgreSQL驱动程序。
```

#### 2.获取连接

```java
String url="jdbc:postgresql://localhost:5432/testdb";
String username="postgre";
String password="123456";
DriverManager.getConnection(String url, String username, String password)
//建立与数据库的连接。
```

### sql操作

#### 1.创建SQL语句对象

```java
// 使用Connection对象方法创建SQL语句对象有如下几种方式： 
① Connection.createStatement()	//创建Statement对象，实现静态SQL语句查询； 
② Connection.prepareStatement(String sql)	//创建PreparedStatement对象，实现动态SQL语句查询； 
③ Connection.prepareCall(String sql)		//创建CallableStatement对象，实现数据库存储过程调用。
```

#### 2.执行sql

```java
//1）执行各种SQL语句，返回一个boolean类型值，true表示执行的SQL语句具备查询结果，可通过Statement.getResultSet()方法获取；
Statement.execute(String sql) 
//2)执行SQL中的insert/update/delete语句，返回一个int值，反馈受影响的记录数；
Statement.executeUpdate(String sql)
//3）执行SQL中的select语句，返回一个表示查询结果的ResultSet对象。
Statement.executeQuery(String sql)
```

### 结果集处理

```java
//Java程序中使用ResultSet对象用于存储查询结果集，并通过游标访问结果集数据。
//1）将游标由当前位置移动到下一行；
    ResultSet.next()
//2）获取当前行指定字段的String类型值；
        ResultSet.getString(String columnName) 
//3） 获取当前行指定列的String类型值；
        ResultSet.getString(int columnIndex)
//4）将游标由当前行移动到上一行。
        ResuleSet.previous（）
```

### PreparedStatement

向SQL语句传递参数

如果Java语言向SQL语句传递参数，则需使用动态查询PreparedStatement对象执行操作。该对象有如下三种执行方式：
1）prepareStatement.executeUpdate()执行更新；
2）prepareStatement对象使用addBatch()方法向批处理中加入更新语句，
3）executeBatch()方法用于成批地执行SQL语句。

### 例子

```java
public class SQLinJava {
public static void main(String[] args) {
    Connection conn = null;
    String URL = "jdbc:postgresql://localhost:5432/testDB";
    String userName = "myuser";
    String passWord = "sa";
    String sid[] = {"14102","14103","14202","14301","14101","14201","14503"};
    String cid[] = {"1205","1208","1205","1208","1201","1201","1201"};
    int score[] = {90,78,89,68,86,96,83};           
    try {
         Class.forName("org.postgresql.Driver");
         conn = DriverManager.getConnection(URL , userName, passWord );
         System.out.println("成功连接数据库！");
         
         String insertSql = "INSERT INTO stu_score(sid, cid, score) VALUES (?,?,?)";
         String querySql = "select sid, cid, score from stu_score where score>=?";
         PreparedStatement psInsert = conn.prepareStatement(insertSql); //定义动态执行SQL语句对象
         PreparedStatement psQuery = conn.prepareStatement(querySql）; //定义动态执行SQL语句对象
       	for (int i=0; i<sid.length; i++)
         {
             psInsert.setString(1, sid[i]);
             psInsert.setString(2, cid[i]);
             psInsert.setInt(3, score[i]);
             psInsert.addBatch();   //添加批处理的记录        
         }
         psInsert.executeBatch();//批处理执行多条数据记录
         
         psQuery.setInt(1, 80);
         ResultSet rs = psQuery.executeQuery();
         while (rs.next()) { // 判断是否还有下一个数据    
             System.out.println(rs.getString("sid") + "  " + 
                                rs.getString("cid") + " " + rs.getInt("score"));  
         }
         // 释放资源
         psQuery.close();
         psInsert.close();
         conn.close();
       } catch ( Exception e ) {
         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
         System.exit(0);
       }
 }
}
```



## 一些问题

1.**sql语句中单引号和双引号有什么区别？**
比如一般字符串使用单引号，但创建角色时使用双引号。

答：数据库对象名称若使用双引号，则在系统中支持大小写区别。一般字符串数据则使用单引号。

2.**如何在数据库中查看一个角色对表的访问权限**？

- SQL

通过SQL语句打印指定角色的所有表权限 `SELECT * FROM information_schema.table_privileges WHERE grantee='xxx';`

- GUI

或者GUI方式：通过SQL语句打印指定角色的所有表权限 SELECT * FROM information_schema.table_privileges WHERE grantee='xxx';

![查看角色权限](pictures/查看角色权限.jpg)

**3.在Pgadmin中创建了一个新用户我该如何用这个用户身份登录，然后来进行访问操作呢？**

GUI：先断开与数据库的连接然后在 propriety 里设置 connection中用户名再点击连接输入 密码。

Shell：在SQL Shell中进行用户名和密码的登录

