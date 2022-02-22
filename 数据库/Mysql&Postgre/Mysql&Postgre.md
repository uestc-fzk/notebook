# mysql

## 安装和卸载的问题

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
```properties
driver=com.mysql.jdbc.Driver
url=jdbc:mysql://localhost:3306/students
username=root
password=010326
```
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
## 导学
### 0.目录：
>   一、为什么要学习数据库
    二、数据库的相关概念      
        DBMS、DB、SQL
    三、数据库存储数据的特点
    四、初始MySQL
        MySQL产品的介绍        
        MySQL产品的安装          ★        
        MySQL服务的启动和停止     ★
        MySQL服务的登录和退出     ★      
        MySQL的常见命令和语法规范      
    五、DQL语言的学习   ★              
        基础查询        ★             
        条件查询  	   ★			
        排序查询  	   ★				
        常见函数        ★               
        分组函数        ★              
        分组查询		   ★			
        连接查询	 	★			
        子查询       √                  
        分页查询       ★              
        union联合查询	√			     
    六、DML语言的学习    ★             
        插入语句						
        修改语句						
        删除语句						
    七、DDL语言的学习  
        库和表的管理	 √				
        常见数据类型介绍  √          
        常见约束  	  √			
    八、TCL语言的学习
        事务和事务处理                 
    九、视图的讲解           √
    十、变量                      
    十一、存储过程和函数   
    十二、流程控制结构       

数据库的好处

	1.持久化数据到本地
	2.可以实现结构化查询，方便管理



数据库相关概念

	1、DB：数据库，保存一组有组织的数据的容器
	2、DBMS：数据库管理系统，又称为数据库软件（产品），用于管理DB中的数据
			数据库管理系统（ Database Management System ）。
			数据库是通过 DBMS 创建和操作的容器
	3、SQL:结构化查询语言，用于和DBMS通信的语言

数据库存储数据的特点

	1、将数据放到表中，表再放到库中
	2、一个数据库中可以有多个表，每个表都有一个的名字，用来标识自己。表名具有唯一性。
	3、表具有一些特性，这些特性定义了数据在表中如何存储，类似java中 “类”的设计。
	4、表由列组成，我们也称为字段。所有表都是由一个或多个列组成的，每一列类似java 中的”属性”
	5、表中的数据是按行存储的，每一行类似于java中的“对象”。


### 1.启动和停止MySQL服务
    方式一：通过计算机管理方式 右击计算机—管理—服务—启动或停止MySQL服务 
    方式二：通过管理员模式命令行方式 启动：net start mysql服务名 停止：net stop mysql服务名
### 2.MySQL服务端的登录与退出

    1.通过mysql自带的客户端MySQL 5.5 Command Line Client  然后直接输入用户密码
    只能是root用户登录 其他用户不能进去 不够灵活 不建议
    退出 exit或者ctrl+c
    
    2. 通过管理员模式命令行方式登录： mysql –h 主机名（自己就是localhost） -P 3306（端口号） –u用户名（root） –p
    然后输入密码
    也可以直接在-p后加密码
    退出 exit
    3.如果是连接本机的，可以简写：
    Mysql -u 用户名 –p密码


### 3.MySQL的常见命令 

	1.查看当前所有的数据库
	show databases;
	2.打开指定的库
	use 库名
	    会进入这个库
	3.查看当前库的所有表
	show tables;
	4.查看其它库的所有表
	    不会调转到其他库
	show tables from 库名;
	5.创建表
	create table 表名(
	
		列名 列类型,
		列名 列类型，
		...
	);
	6.查看表结构
	desc 表名;
	
	7.查看服务器的版本
	    方式一：登录到mysql服务端
	    select version();
	    方式二：没有登录到mysql服务端
	    mysql --version
	    或
	    mysql --V



### 4.MySQL的语法规范
**重要**

>1.不区分大小写,但建议关键字大写，表名、列名小写

>2.每条命令最好用分号结尾

>3.每条命令根据需要，可以进行缩进或 
换行(回车)
关键字单独一行

>4.注释
    单行注释：#注释文字
    单行注释：-- 注释文字
        必须加空格
    多行注释：/* 注释文字  */

>6.字符串和日期型常量值必须用单引号引起来，数值型不需要。

>7.mysql索引从1开始

>8.limit 【offset,】size;
offset要显示条目的起始索引（起始索引从0开始）
size 要显示的条目个数

### 5.SQLyog使用技巧
快捷键

    1.f12  对代码格式化
着重号 

    `  1左边的那个
    用于区分关键字和字段
    在字段两边加上`

字表名

    忘记怎么写的时候，可以直接去双击表名
    哪里不会点哪里。




## 1.DQL语句
数据查询语言(Data Query Language)
### 进阶1：基础查询
	语法：
	SELECT 要查询的东西
	【FROM 表名】;
	
	类似于Java中 :System.out.println(要打印的东西);
	特点：
	①通过select查询完的结果 ，是一个虚拟的表格，不是真实存在
	② 要查询的东西 可以是常量值、可以是表达式、可以是字段、可以是函数
	
	字符串和日期型常量值必须用单引号引起来，数值型不需要。
	
	6.案例
	题目：显示出表 employees 的全部列，各个列之间用逗号连接，列头显示成 OUT_PUT
	
	SELECT CONCAT(`commission_pct`,',',`first_name`,',',`salary`) AS OUT_PUT
	FROM employees;
	#第一种写法 在`commission_pct`为null是，全都是null。
	SELECT IFNULL(`commission_pct`,0) AS 奖金率,
	`commission_pct`
	FROM employees;
	# ifnull(expr1,expr2)函数  第一个参数为null，则将其赋值为0.
	SELECT CONCAT(IFNULL(`commission_pct`,0),',',`first_name`,',',`salary`) AS OUT_PUT
	FROM employees;
	#这样即使有null，也不会表格中某个值直接变null值。

### 进阶2：条件查询

####语法：
       
        select 
            查询列表#第三步
        from
            表名#第一步
        where	#第二步，过滤，使用WHERE 子句，将不满足条件的行过滤掉。
            筛选条件;

####分类：
       
一、按条件表达式筛选

    简单条件运算符：> < = != <> >= <=
    在mysql中 = 就是等于，不再是== 。  赋值使用 := 符号
    !=和<>都是不等于
    
    #案例1：查询工资>12000的员工信息
    SELECT 
        *
    FROM
        employees
    WHERE
        salary>12000;

二、按逻辑表达式筛选
        
    逻辑运算符：
    作用：用于连接条件表达式
        && || !
        and or not
        
    &&和and：两个条件都为true，结果为true，反之为false
    ||或or： 只要有一个条件为true，结果为true，反之为false
    !或not： 如果连接的条件本身为false，结果为true，反之为false

三、模糊查询
    
    like
    between and
    in
    is null
1.like
    
    特点：
    ①一般和通配符搭配使用
    可以判断字符，也可以数值型
    通配符：
    % 任意多个字符,包含0个字符
    _ 任意单个字符
    
    #案例1：查询员工名中包含字符a的员工信息
    select 
        *
    from
        employees
    where
        last_name like '%a%';#abc
    
    #案例2：查询员工名中第二个字符为_的第五个字符为a员工名和工资
    
    SELECT
        last_name,
        salary
    FROM
        employees
    WHERE
        last_name LIKE '_$___a%' ESCAPE '$';
    escape 关键字指定某个字符为转义字符  当然也可以直接用\

2.between  and            

    ①使用between and 可以提高语句的简洁度
    ②包含临界值
    ③两个临界值不要调换顺序(从小到大)

3.in

    含义：判断某字段的值是否属于in列表中的某一项
    特点：
    ①使用in提高语句简洁度
    ②in列表的值类型必须一致或兼容
    ③in列表中不支持通配符
    
    #案例：查询员工的工种编号是 IT_PROG、AD_VP、AD_PRES中的一个员工名和工种编号
    
    SELECT
        last_name,
        job_id
    FROM
        employees
    WHERE
        job_id IN( 'IT_PROT' ,'AD_VP','AD_PRES');

4.is null 与 安全等于  <=>

    注意：=或<>不能用于判断null值
    is null或is not null 可以判断null值
    #----------以下为错误
    WHERE 
        salary IS 12000;
    
    IS NULL:仅仅可以判断NULL值，可读性较高，建议使用
    <=>    :既可以判断NULL值，又可以判断普通的数值，可读性较低
    #以下都是正确的：
    WHERE 
    salary <=> 12000;
    
    WHERE
    commission_pct <=>NULL;

### 进阶3：排序查询	

	语法：
	select #第三步
		要查询的东西
	from #第一步
		表
	where #第二步
		条件
	order by 排序的字段|表达式|函数|别名 【asc|desc】 #第四步
	
	特点：
	1、asc代表的是升序，可以省略，即默认升序
	desc代表的是降序
	
	2、order by子句可以支持 单个字段、别名、表达式、函数、多个字段
	
	3、order by子句在查询语句的最后面，除了limit子句





### 进阶4：常见函数
    功能：类似于Java中的方法，提高重用性和隐藏实现细节
一、单行函数

	1、字符函数
		concat拼接
		substr截取子串
		upper转换成大写
		lower转换成小写
		trim去前后指定的空格或字符
		ltrim去左边空格
		rtrim去右边空格
		replace全部替换
		lpad用指定的字符实现左填充指定长度
		rpad右填充
		instr返回子串第一次出现的索引
		length 获取字节个数
		
	2、数学函数
		round 四舍五入
		rand 随机数，返回[0-1)之间的小数
		floor向下取整
		ceil向上取整
		mod取余
		truncate截断
	3、日期函数
	        now当前系统日期+时间
	        curdate当前系统日期
	        curtime当前系统时间
	        year(now())获取年
	        str_to_date 将字符解析成日期    STR_TO_DATE('1998-3-2','%Y-%c-%d')
	        解析：
	            1 %Y 四位的年份 
	            2 %y 2位的年份 
	            3 %m 月份（01,02…11,12）
	            4 %c 月份（1,2,…11,12）
	            5 %d 日（01,02,…） 
	            6 %H 小时（24小时制） 
	            7 %h 小时（12小时制） 
	            8 %i 分钟（00,01…59） 
	            9 %s 秒（00,01,…59）
	        date_format将日期转换成字符 DATE_FORMAT(‘2018/6/6’,‘%Y年%m月%d日’) 
	        datediff:返回两个日期相差的天数
	        monthname:以英文形式返回月
	4、流程控制函数
	    if 处理双分支
	
	    case语句 处理多分支
	    情况1：处理等值判断
	    情况2：处理条件判断
	    
	    mysql中
	
	    case 要判断的字段或表达式
	    when 常量1 then 要显示的值1或语句1;
	    when 常量2 then 要显示的值2或语句2;
	    ...
	    else 要显示的值n或语句n;
	    end
		
	5、其他函数
		version版本
		database当前库
		user当前连接用户
	    password('字符串')返回该字符串加密形式


二、分组函数


    sum 求和
    max 最大值
    min 最小值
    avg 平均值
    count 计数
    
    特点：
    1、以上五个分组函数都忽略null值，除了count(*)
    2、sum和avg一般用于处理数值型
    max、min、count可以处理任何数据类型
    3、都可以搭配distinct使用，用于统计去重后的结果，不加distinct，则默认为all
    4、count的参数可以支持：
    字段、*、常量值，一般放1
    
    建议使用 count(*)



### 进阶5：分组查询
	语法：
	select 查询列表
	from 表
	【where 筛选条件】
	group by 分组的字段
	【having 筛选条件】
	【order by 排序的字段】;
	
	特点：
	1、可以按单个字段分组，也可以嵌套分组
	    null也是一种分组
	2、和分组函数一同查询的字段最好是分组后的字段
	3、分组筛选
	            针对的表      位置          关键字	
	分组前筛选：原始表        group by的前面  where
	分组后筛选：分组后的结果集 group by的后面  having
	where 过滤的是行，而having  过滤的是分组；
	事实上，目前所学的所有类型的where子句都可用having 代替；
	一般来讲，能用分组前筛选的，尽量使用分组前筛选，提高效率。
	5.group by 不支持别名
	6、having后可以支持别名



### 进阶6：多表连接查询

	笛卡尔乘积：如果连接条件省略或无效则会出现
	解决办法：添加上连接条件

一、传统模式下的连接 ：等值连接——非等值连接


	1.等值连接的结果 = 多个表的交集
	2.n表连接，至少需要n-1个连接条件
	3.多个表不分主次，没有顺序要求
	4.一般为表起别名，提高阅读性和性能
	    注意：如果为表起了别名，则查询的字段就不能使用原来的表名去限定

二、sql99语法：通过join关键字实现连接


    #sql92 和 sql99
    /*
    sq192 用where联结
    功能：sql99支持的较多
    可读性：sql99实现连接条件和筛选条件的分离，可读性较高
    */
      
    含义：1999年推出的sql语法
    支持：
    内连接
        等值连接、非等值连接、自连接
    外连接
        左外、右外、全外(MySQL不支持)
    交叉连接
    
    语法：
    
    select 字段，...
    from 表1
    【inner|left outer|right outer|cross】join 表2 on  连接条件
    【inner|left outer|right outer|cross】join 表3 on  连接条件
    【where 筛选条件】
    【group by 分组字段】
    【having 分组后的筛选条件】
    【order by 排序的字段或表达式】
    
    好处：语句上，连接条件和筛选条件实现了分离，简洁明了！


​	
三、自连接

案例：查询员工名和直接上级的名称

sql99

	SELECT e.last_name,m.last_name
	FROM employees e
	JOIN employees m ON e.`manager_id`=m.`employee_id`;

sql92


	SELECT e.last_name,m.last_name
	FROM employees e,employees m 
	WHERE e.`manager_id`=m.`employee_id`;
四、外联结
    应用场景：用于查询一个表中有，另一个表没有的记录
    
    特点：
    1、外连接的查询结果为主表中的所有记录
        如果从表中有和它匹配的，则显示匹配的值
        如果从表中没有和它匹配的，则显示null
        外连接查询结果=内连接结果+主表中有而从表没有的记录
    2、左外连接，left join左边的是主表
        右外连接，right join右边的是主表
    3、左外和右外交换两个表的顺序，可以实现同样的效果 
    4、全外连接=内连接的结果+表1中有但表2没有的+表2中有但表1没有的（MySQL不支持会报错）

 五、交叉连接
    其实就是笛卡尔积

    SELECT b.*,bo.*
    FROM beauty b
    CROSS JOIN boys bo;


### 进阶7：子查询

含义：

	一条查询语句中又嵌套了另一条完整的select语句，其中被嵌套的select语句，称为子查询或内查询
	在外面的查询语句，称为主查询或外查询

特点：

	1、子查询都放在小括号内
	2、子查询可以放在from后面、select后面、where后面、having后面，但一般放在条件的右侧
	3、子查询优先于主查询执行，主查询使用了子查询的执行结果
	4、子查询根据查询结果的行数不同分为以下两类：
	① 单行子查询
		结果集只有一行
		一般搭配单行操作符使用：> < = <> >= <= 
		非法使用子查询的情况：
		a、子查询的结果为一组值，主查询报错
		b、子查询的结果为空，此时主查询也不返回任何行
		
	② 多行子查询
		结果集有多行
		一般搭配多行操作符使用：any|some、all、in、not in
		in： 属于子查询结果中的任意一个就行
		any、some和all往往可以用其他查询代替，any、some指其中某个，all指其中全部
	
	5、exists后面（相关子查询）
	一般能用exists的也能换成in之类的替代
	语法：
	exists(完整的查询语句)
	结果：
	1或0，查询出有结果就是1，否则为0，布尔值
	#案例1：查询有员工的部门名
	
	    #in
	    SELECT department_name
	    FROM departments d
	    WHERE d.`department_id` IN(
	        SELECT department_id
	        FROM employees
	
	    )
	
	    #exists
	
	    SELECT department_name
	    FROM departments d
	    WHERE EXISTS(
	        SELECT *
	        FROM employees e
	        WHERE d.`department_id`=e.`department_id`


        );
    
    6.要习惯使用distinct 去重 子查询中出现的重复的意义不大



### 进阶8：分页查询

应用场景：

	实际的web项目中需要根据用户的需求提交对应的分页查询的sql语句

语法：

	select 查询列表 #7
	from 表  #1
	【join type join 表2 #2
	on 连接条件  #3
	where 筛选条件 #4
	group by 分组字段 #5
	having 分组后的筛选 #6
	order by 排序的字段】 #8
	limit 【起始的条目索引，】条目数; #9
	每一步都会生成虚拟结果表集

特点：1.起始条目索引从0开始

	2.limit子句放在查询语句的最后	
	3.公式：select * from  表 limit （page-1）*sizePerPage,sizePerPage
	假如:
	每页显示条目数sizePerPage
	要显示的页数 page



###进阶9：联合查询

引入：
	union 联合、合并  
应用场景：要查询的结果来自于多个表，且多个表没有直接的连接关系，但查询的信息一致时


语法：

	select 字段|常量|表达式|函数 【from 表】 【where 条件】 union 【all】
	select 字段|常量|表达式|函数 【from 表】 【where 条件】 union 【all】
	select 字段|常量|表达式|函数 【from 表】 【where 条件】 union  【all】
	.....
	select 字段|常量|表达式|函数 【from 表】 【where 条件】

特点：

	1、多条查询语句的查询的列数必须是一致的
	2、多条查询语句的查询的列的类型几乎相同
	3、union默认去重，union all代表不去重



## 2.DML语句
Data Manipulation Language – 数据操纵语言
- 向表中插入数据
- 修改现存数据
- 删除现存数据

### 插入

语法：
- 方式1
	insert into 表名(字段名，...)
	values(值1，...),
	...
	(值1，...);
-  方式2
	insert into 表名
	set 列名=值,列名=值,...
- 区别：
	1、方式一支持插入多行,方式二不支持
	2、方式一支持子查询，方式二不支持

		INSERT INTO beauty(id,NAME,phone)
		SELECT id,boyname,'1234567'
		FROM boys WHERE id<3;
	3、所以一般用方式1

特点：

	1、字段类型和值类型一致或兼容，而且一一对应
	2、可以为空的字段，可以不用插入值，或用null填充
	3、不可以为空的字段，必须插入值
	4、字段个数和值的个数必须一致
	5、字段可以省略，但默认所有字段，并且顺序和表中的存储顺序一致

### 修改

修改单表语法：

	update 表名 set 字段=新值,字段=新值
	【where 条件】
修改多表的记录【补充】

    语法：
    sql92语法：
    update 表1 别名,表2 别名
    set 列=值,...
    where 连接条件 and 筛选条件;
    
    sql99语法：
    update 表1 别名
    inner|left|right join 表2 别名
    on 连接条件
    set 列=值,...
    where 筛选条件;

### 删除

方式1：delete语句 

单表的删除： ★
	delete from 表名 【where 筛选条件】【limit 】

多表的删除：
-    sql99语法：

    delete 表1的别名,表2的别名
    from 表1 别名
    inner|left|right join 表2 别名 on 连接条件
    where 筛选条件;



方式2：truncate语句  清空数据

	truncate table 表名


两种方式的区别?

>1.truncate不能加where条件，而delete可以加where条件

>2.truncate的效率高一丢丢，删除原来的表，重新建一个表，delete是逐行删除表中的数据

>3.truncate 删除带自增长的列的表后，如果再插入数据，数据从1开始；delete 删除带自增长列的表后，如果再插入数据，数据从上一次的断点处开始

>4.truncate删除不能回滚，delete删除可以回滚

>5.truncate删除不能回滚，delete删除可以回滚.



## 3.DDL语句
数据定义语言(Data Definition Language)
### 库和表的管理
库的管理：

	一、创建库
	create database 【IF NOT EXISTS】库名 【CHARACTER SET gbk】
	二、删除库
	drop database 【if exists】 库名
表的管理：

#### 1.创建表 ★

	create table 【if not exists】 表名(
		字段名 字段类型 【约束】,
		字段名 字段类型 【约束】,
		。。。
		字段名 字段类型 【约束】 
	
	)

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

	DROP TABLE [IF EXISTS] studentinfo;

#### 4.表的复制

	(1).仅仅复制表的结构
	CREATE TABLE copy LIKE author;
	(2).复制表的结构+数据
	CREATE TABLE copy2 
	SELECT * FROM author;
	(3).只复制部分数据
	CREATE TABLE copy3
	SELECT id,au_name
	FROM author 
	WHERE nation='中国';
	(4)仅仅复制某些字段
	CREATE TABLE copy4 
	SELECT id,au_name
	FROM author
	WHERE 0; #这样所有数据都不会复制过来了，但是把表的部分结构复制了过来

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

-	整型：
	- 分类：
		tinyint、smallint、mediumint、int/integer、bigint
	- 特点：
		① 如果不设置无符号还是有符号，默认是有符号，如果想设置无符号，需要添加unsigned关键字
		② 如果插入的数值超出了整型的范围,会报out of range异常，并且插入临界值
		③ 如果不设置长度，会有默认的长度
		长度代表了显示的最大宽度，如果不够会用0在左边填充，但必须搭配zerofill(会默认无符号)使用！

-	小数：
		浮点型
		定点型
	- 分类：
		1.浮点型
		float(M,D)
		double(M,D)
		2.定点型
		dec(M，D)
		decimal(M,D)

	- 特点：
		①
		M：整数部位+小数部位
		D：小数部位位数
		②
		M和D都可以省略
		如果是decimal，则M默认为10，D默认为0
		如果是float和double，则会根据插入的数值的精度来决定精度

		③定点型的精确度较高，如果要求插入数值的精度较高如货币运算等则考虑使用
		④如果超出范围，则报out or range异常，并且插入临界值

-	字符型：
	char、varchar、binary、varbinary、enum、set、text、blob

	char：固定长度的字符，写法为char(M)，最大长度不能超过M，其中M可以省略，默认为1
	varchar：可变长度的字符，写法为varchar(M)，最大长度不能超过M，其中M不可以省略
-	日期型：
	year年
	date日期
	time时间
	datetime 日期+时间          8      
	timestamp 日期+时间         4   比较容易受时区、语法模式、版本的影响，更能反映当前时区的真实时间
-	Blob类型：s

## 5.常见约束
### 约束介绍	
含义：一种限制规则，用于限制表中的数据，为了保证表中的数据的完整性和一致性。


分类：六大约束

	NOT NULL：非空，用于保证该字段的值不能为空
	比如姓名、学号等
	DEFAULT:默认，用于保证该字段有默认值
	比如性别
	PRIMARY KEY:主键，用于保证该字段的值具有唯一性，并且非空
	比如学号、员工编号等
	UNIQUE:唯一，用于保证该字段的值具有唯一性，可以为空
	比如座位号
	CHECK:检查约束【mysql中不支持，没效果，但是不会报错，以适配其他数据库】
	比如年龄、性别
	FOREIGN KEY:外键，用于限制两个表的关系，用于保证该字段的值必须来自于主表的关联列的值
		在从表添加外键约束，用于引用主表中某列的值
	比如学生表的专业编号，员工表的部门编号，员工表的工种编号

添加约束的时机：

	1.创建表时----列级约束
	2.修改表时----表级约束

约束的添加分类：

	列级约束：
		六大约束语法上都支持，但外键约束没有效果
		
	表级约束：		
		除了非空、默认，其他的都支持
	区别：		
	位置			支持的约束类型			   			是否可以起约束名
	列级约束：	列的后面 语法都支持，但外键没有效果			不可以
	表级约束：	所有列的下面 默认和非空不支持，其他支持	可以（主键没有效果）

主键和唯一的对比：

		保证唯一性  是否允许为空    一个表中可以有多少个   是否允许组合
	主键	√		×		至多有1个           √，但不推荐
	唯一	√		√		可以有多个          √，但不推荐
外键：

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
语法：

	直接在字段名和类型后面追加 约束类型即可。
	
	只支持：默认、非空、主键、唯一
	
	但是这样不能添加索引名

查看stuinfo表中的所有索引，包括主键、外键、唯一
SHOW INDEX FROM stuinfo;

2.添加表级约束

	语法：在各个字段的最下面
	【constraint 约束名】 约束类型(字段名) 
	如：
	CONSTRAINT pk PRIMARY KEY(id),#主键
	CONSTRAINT uq UNIQUE(seat),#唯一键
	CONSTRAINT ck CHECK(gender ='男' OR gender  = '女'),#检查
	
	constraint 约束名 foreign key(字段名) references 主表（被引用列）
	CONSTRAINT fk_stuinfo_major FOREIGN KEY(majorid) REFERENCES major(id)#外键
	
	可以添加索引名

3.通用的写法：★

	CREATE TABLE IF NOT EXISTS stuinfo(
		id INT PRIMARY KEY,
		stuname VARCHAR(20),
		sex CHAR(1),
		age INT DEFAULT 18,
		seat INT UNIQUE,
		majorid INT,
		CONSTRAINT fk_stuinfo_major FOREIGN KEY(majorid) REFERENCES major(id)
		#外键索引名命名最好按以上方式
	);


### 二、修改表时添加约束

>1、添加列级约束
alter table 表名 modify column 字段名 字段类型 新约束; # 不加就是默认约束，所以要注意不能随便用

>2、添加表级约束
alter table 表名 add 【constraint 约束名】 约束类型(字段名) 【外键的引用】;


例子：
```sql
DROP TABLE IF EXISTS stuinfo;
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
4、标识列可以通过 SET auto_increment_increment=3;设置步长
可以通过 手动插入值，设置起始值
二、修改表时设置标识列
ALTER TABLE tab_identity MODIFY COLUMN id INT PRIMARY KEY AUTO_INCREMENT;

三、修改表时删除标识列
ALTER TABLE tab_identity MODIFY COLUMN id INT ;



## 6.TCL语句
Transaction Control Language 事务控制语言

### 事务：对于DML语句才有事务
>一个或一组sql语句组成一个执行单元，这个执行单元要么全部执行，要么全部不执行。如果单元中某条SQL语句一旦执行失败或产生错误，整个单元将会回滚。

#### 特点
	（ACID）
	原子性(Atomicity)：要么都执行，要么都回滚
	一致性(Consistency)：保证数据的状态操作前和操作后保持一致
	隔离性(Isolation)：多个事务同时操作相同数据库的同一个数据时，一个事务的执行不受另外一个事务的干扰
	持久性(Durability)：一个事务一旦提交，则数据将持久化到本地，除非其他事务对其进行修改

相关步骤：
>1、开启事务
>2、编写事务的一组逻辑操作单元（多条sql语句）
>3、提交事务或回滚事务

#### 事务的分类：

1.隐式事务，没有明显的开启和结束事务的标志

```sql
比如
insert、update、delete语句本身就是一个事务
```


2.显式事务，具有明显的开启和结束事务的标志

```sql
1、开启事务
取消自动提交事务的功能
set autocommit=0;
2、编写事务的一组逻辑操作单元（多条sql语句）
insert
update
delete
可以设置回滚点：
savepoint 回滚点名;
3、提交事务或回滚事务
提交：commit;
回滚：rollback;
回滚到指定的地方：rollback to 回滚点名;
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
```


#### 事务的隔离级别:

事务并发问题如何发生？

当多个事务同时操作同一个数据库的相同数据时,事务的并发问题有哪些？

>1.脏读：一个事务读取到了另外一个事务未提交的数据

>2.不可重复读：同一个事务中，多次读取到的数据不一致

>3.幻读：一个事务读取数据时，另外一个事务进行更新，导致第一个事务读取到了没有更新的数据

如何避免事务的并发问题？

	通过设置事务的隔离级别
	1、READ UNCOMMITTED
	2、READ COMMITTED 可以避免脏读
	3、REPEATABLE READ 默认，可以避免脏读、不可重复读和一部分幻读
	4、SERIALIZABLE可以避免脏读、不可重复读和幻读
								脏读			不可重复读		  幻读
	read uncommitted:读未提交     ×                ×              ×        
	read committed：读已提交      √                ×              ×
	repeatable read：可重复读     √                √              ×
	serializable：串行化          √                √              √

设置隔离级别：

```sql
set session|global  transaction isolation level 隔离级别名;
```
查看隔离级别：

```sql
select @@tx_isolation;
```



## 7.视图
含义：理解成一张虚拟的表
mysql5.1版本出现的新特性，是通过表动态生成的数据
视图和表的区别：

		使用方式	占用物理空间
	
	视图	完全相同	不占用，仅仅保存的是sql逻辑
	
	表	完全相同	占用

视图的好处：

	1、sql语句提高重用性，效率高
	2、和表实现了分离，提高了安全性
	3.提供一定程度上的逻辑独立性
	4.集中展示用户所感兴趣的特定数据

### 视图的创建
```sql
-- 语法：
CREATE VIEW  <视图名>
AS <查询语句>;
```

### 视图的增删改查
	1、查看视图的数据 ★
	
	SELECT * FROM my_v4;
	SELECT * FROM my_v1 WHERE last_name='Partners';
	
	2、插入视图的数据
	INSERT INTO my_v4(last_name,department_id) VALUES('虚竹',90);
	
	3、修改视图的数据
	
	UPDATE my_v4 SET last_name ='梦姑' WHERE last_name='虚竹';
	
	4、删除视图的数据
	DELETE FROM my_v4;

### 某些视图不能更新
	包含以下关键字的sql语句：分组函数、distinct、group  by、having、union或者union all
	常量视图
	Select中包含子查询
	join
	from一个不能更新的视图
	where子句的子查询引用了from子句中的表

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
### 视图的删除
```sql
DROP VIEW test_v1,test_v2,test_v3;
```
### 视图结构的查看	
```sql
DESC test_v7;
SHOW CREATE VIEW test_v7;
```



## 8.存储过程

含义：一组经过预先编译的sql语句的集合
好处：

	1、提高了sql语句的重用性，减少了开发程序员的压力
	2、提高了效率
	3、减少了传输次数

分类：

	1、无返回无参
	2、仅仅带in类型，无返回有参
	3、仅仅带out类型，有返回无参
	4、既带in又带out，有返回有参
	5、带inout，有返回有参
	注意：in、out、inout都可以在一个存储过程中带多个

#### 1.创建存储过程
语法：

	create procedure 存储过程名(in|out|inout 参数名  参数类型,...)
	begin
		存储过程体
	
	end



注意
```sql
1、需要临时更改新的命令行使用程序的语句分隔符：
delimiter 新的结束标记
示例：
delimiter $  #除了\ 都可以作为语句分隔符

CREATE PROCEDURE 存储过程名(IN|OUT|INOUT 参数名  参数类型,...)
BEGIN
	sql语句1;
	sql语句2;
END $

delimiter ;	#改回;作为语句分隔符

2、存储过程体中可以有多条sql语句，存储过程体中的每条sql语句的结尾要求必须加分号。如果仅仅一条sql语句，则可以省略begin end

3、参数前面的符号的意思
in:该参数只能作为输入 （该参数不能做返回值）
out：该参数只能作为输出（该参数只能做返回值）
inout：既能做输入又能做输出
调用in模式的参数：call sp1（‘值’）;
调用out模式的参数：set @name; call sp1(@name);select @name;
调用inout模式的参数：set @name=值; call sp1(@name); select @name;
```

#### 2.调用存储过程
	call 存储过程名(实参列表)

#### 3.查看存储过程的信息
```sql
DESC myp2;	#错误
SHOW CREATE PROCEDURE  myp2;
SHOW PROCEDURE STATUS;	#查看存储过程列表
```
#### 4.删除
	drop procedure 存储过程名;

#### 5.案例

######1、创建存储过程或函数实现传入女神名称，返回：女神 and 男神  格式的字符串
如 传入 ：小昭
返回： 小昭 AND 张无忌
```sql
DROP PROCEDURE test_pro5;
DELIMITER $
CREATE PROCEDURE test_pro5(IN beautyName VARCHAR(20),OUT str VARCHAR(50))
BEGIN
	SELECT CONCAT(beautyName,' and ',IFNULL(boyName,'null')) INTO str
	FROM boys bo
	RIGHT JOIN beauty b ON b.boyfriend_id = bo.id
	WHERE b.name=beautyName;
END $

CALL test_pro5('柳岩',@str);
SELECT @str ;
```
######2、创建存储过程或函数，根据传入的条目数和起始索引，查询beauty表的记录
```sql
DROP PROCEDURE test_pro6;
DELIMITER $
CREATE PROCEDURE test_pro6(IN startIndex INT,IN size INT)
BEGIN
	DECLARE START INT;
	SET START:=startIndex-1;
	SELECT * FROM beauty LIMIT START,size;
END $
DELIMITER ;
CALL test_pro6(1,5);
```

## 9. 函数
含义：一组预先编译好的SQL语句的集合，理解成批处理语句
>1、提高代码的重用性

>2、简化操作

>3、减少了编译次数并且减少了和数据库服务器的连接次数，提高了效率


函数和存储过程的区别：
>存储过程：可以有0个返回，也可以有多个返回，适合做批量插入、批量更新

>函数：有且仅有1 个返回，适合做处理数据后返回一个结果


###1.创建语法
```sql
delimiter $
CREATE FUNCTION 函数名(参数列表) RETURNS 返回类型
BEGIN
	函数体
	return 
END $
deliter ;
```
注意：

	1.参数列表 包含两部分：
	参数名 参数类型
	2.函数体：肯定会有return语句，如果没有会报错
	如果return语句没有放在函数体的最后也不报错，但不建议
	return 值;
	3.函数体中仅有一句话，则可以省略begin end
	4.使用 delimiter语句设置结束标记

###2.调用语法
	SELECT 函数名(参数列表)

###3.查看函数
	SHOW CREATE FUNCTION myf3;

###4.删除函数
	DROP FUNCTION myf3;


## 10.流程控制语句：
	顺序、分支、循环

### 1. 分支结构

1.if函数

	语法：if(条件,值1，值2)
	功能：实现双分支
	特点：
		可以作为表达式放在任何位置

2.case结构

	情况1：类似于switch
	case 变量或表达式
	when 值1 then 语句1;
	when 值2 then 语句2;
	...
	else 语句n;
	end 
	
	情况2：
	case 
	when 条件1 then 语句1;
	when 条件2 then 语句2;
	...
	else 语句n;
	end 
注意：
>可以放在任何位置，
如果放在begin end 外面，作为表达式结合着其他语句使用
如果放在begin end 里面，一般作为独立的语句使用
作为表达式的时候，每个语句后面没有分号，作为独立的语句时，每个语句后面都有分号

3.if结构

	语法：
	if 条件1 then 语句1;
	elseif 条件2 then 语句2;
	....
	else 语句n;
	end if;
	功能：类似于多重if

注意：只能应用在begin end 中

### 2.循环结构
分类：

	while、loop、repeat

循环控制关键字：
>iterate类似于 continue，继续，结束本次循环，继续下一次

>leave 类似于  break，跳出，结束当前所在的循环

>使用：leave 标签(如label);

1.while

	语法：
	【标签:】while 循环条件 do
		循环体;
	end while【 标签】;

2.loop

	语法：
	【标签:】loop
		循环体;
	end loop 【标签】;

注意：loop没有结束循环的条件，可以用来模拟简单的死循环；
要跳出循环，需要使用leave；

3.repeat

	语法：
	【标签：】repeat
		循环体;
	until 结束循环的条件
	end repeat 【标签】;


4.案例1：批量插入，根据次数插入到admin表中多条记录，只插入偶数次
```sql
TRUNCATE TABLE admin;
DROP PROCEDURE if exists test_while1;
DELIMITER $
CREATE PROCEDURE test_while1(IN insertCount INT)
BEGIN
	DECLARE i INT DEFAULT 0;
	a:WHILE i<=insertCount DO
		SET i=i+1;
		IF MOD(i,2)!=0 THEN ITERATE a;
		END IF;
		INSERT INTO admin(username,`password`) 
		VALUES(CONCAT('xiaohua',i),'0000');
				
	END WHILE a;
END $
delimiter ;

CALL test_while1(100);
SELECT * FROM admin;
```

案例2
已知表stringcontent
其中字段：
id 自增长
content varchar(20)
向该表插入指定个数的，随机的字符串
```sql
DROP TABLE IF EXISTS stringcontent;
CREATE TABLE stringcontent(
	id INT PRIMARY KEY AUTO_INCREMENT,
	content VARCHAR(20)
);
DROP PROCEDURE IF EXISTS test_insert;
DELIMITER $
CREATE PROCEDURE test_insert(IN insert_count INT)
BEGIN 
	DECLARE i INT DEFAULT 1;
	DECLARE str VARCHAR(26) DEFAULT 'abcdefghijklmnopqrstuvwxyz';
	DECLARE len INT DEFAULT 0;
	DECLARE startindex INT DEFAULT 1;
	WHILE i<=insert_count DO
		SET startindex=FLOOR(RAND()*26+1);
		SET len=FLOOR((26-startindex)*RAND()+1);
		INSERT INTO stringcontent(content)
		VALUES(SUBSTR(str,startindex,len));
		SET i:=i+1;
		END WHILE;
END $

TRUNCATE TABLE stringcontent;
CALL test_insert(20);
SELECT * FROM stringcontent;
```

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









## jdbc.properties

```properties
driver=org.postgresql.Driver
url=jdbc:postgresql://localhost:5432/GradeDB
username=postgres
password=010326
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

### DML

数据操纵SQL语句。

对数据库表中数据进行插入、更新和删除处理。

```sql
insert into <表名|视图>[(列名...)] values(列值...);
-- 注意：插入时，给出部分列名，即为插入部分列值，其余列填充NULL
-- 如果不给出列名，默认是插入所有列，其中代理键不需要出现，由DBMS自动填充

update <表名|视图> set <列名1>=<表达式1>,...
[where <条件表达式>];

delete from <表名|视图> [where <条件表达式>];
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