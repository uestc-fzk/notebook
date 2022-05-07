# 	资料

go实现的cron库：https://github.com/robfig/cron

该cron库的文档：https://godoc.org/github.com/robfig/cron

维基百科：https://en.wikipedia.org/wiki/Cron

# 学习

## 维基百科

先去维基百科了解一下cron：https://en.wikipedia.org/wiki/Cron

cron本身是一个类Unix系统上的作业调度程序，以固定时间、日期或间隔定时运行，通常用于定时系统维护或管理和一些重复性任务。

这里将要学习一个go语言实现的定时任务库https://github.com/robfig/cron的使用

## cron表达式

cron表达式用于配置定时任务的执行调度计划。

标注的unix类型cron表达式为5个字段组成，最小单位为minute。

标注cron表达式如下：

```
# ┌────────────── 分钟 (0 - 59)
# │ ┌───────────── 小时 (0 - 23)
# │ │ ┌───────────── 一个月中的某天 (1 - 31)
# │ │ │ ┌───────────── 月 (1 - 12)
# │ │ │ │ ┌───────────── 星期几（0 - 6）（星期日至星期六；
# │ │ │ │ 7 在某些系统上也是星期日）
# │ │ │ │ │
# │ │ │ │ │
# * * * * * <要执行的命令>
```

各个字段可选值：

|    Field     | Required | Allowed values  | Allowed special characters |                           Remarks                            |
| :----------: | :------: | :-------------: | :------------------------: | :----------------------------------------------------------: |
|   Minutes    |   Yes    |      0–59       |        `*` `,` `-`         |                                                              |
|    Hours     |   Yes    |      0–23       |        `*` `,` `-`         |                                                              |
| Day of month |   Yes    |      1–31       |  `*` `,` `-` `?` `L` `W`   |           `?` `L` `W` only in some implementations           |
|    Month     |   Yes    | 1–12 or JAN–DEC |        `*` `,` `-`         |                                                              |
| Day of week  |   Yes    | 0–6 or SUN–SAT  |  `*` `,` `-` `?` `L` `#`   |           `?` `L` `#` only in some implementations           |
|     Year     |    No    |    1970–2099    |        `*` `,` `-`         | This field is not supported in standard/default implementations. |

**标准字符：**

`*`表示任意

`,`表示多选

`-`表示范围

**非标准字符**：非标准字符仅存在于某些cron实现中，如Quartz Java 调度程序。

`/`：指定步长，如分钟字段`*/5`表示每5分钟执行

`L`：表示最后1个，如星期字段`5L`表示最后一个周五

`W`：日期字段允许使用，此字符用于指定距给定日期最近的工作日（周一至周五）。例如，如果将“ 15W ”指定为“日期”字段的值，则其含义是：“距该月 15 日最近的工作日”。因此，如果 15 日是星期六，则触发器会在 14 日星期五触发。如果 15 日是星期日，则触发器会在 16 日星期一触发。如果 15 日是星期二，那么它会在 15 日星期二触发。但是，如果将“1W”指定为日期的值，并且第 1 天是星期六，则触发器会在第 3 天的星期一触发，因为它不会“跳过”一个月的日期边界。'W' 字符只能在日期是一天而不是日期范围或列表时指定。

`#`：星期字段运行使用，只能是1-5之间，如`5#3`表示每月第3个周五

`?`：在某些实施方式中，代替`*`用于将日期或星期几留空。

例子：

```properties
# 每分钟执行
cron.spec1=* * * * *

# 每小时的1和15分钟执行
cron.spec2=1,15 * * * *

# 8点到11点的第1和15分钟执行
cron.spec3=1,15 8-11 * * *

# 每隔2天的8点到11点第1和15分钟执行
cron.spec4=1,15 8-11 */2 * *

# 每周一的8点到11点第1和15分钟执行
cron.spec5=1,15 8-11 * * 1

# 每月1号，10号，20号的3点21分执行
cron.spec6=21 3 1,10,20 * *

# 每天23点到7点每小时执行
* 23-7/1 * * *
```

## cron库使用

在cron库的v1版本，默认cron表达式是解析6个字段，即第一个字段为second字段，比标准的cron表达式仅支持到minute级别更加精确。

不过现在的V3版本默认情况下是标准的cron解析，可以选择开启second字段的解析。

1、引入依赖

```shell
go get github.com/robfig/cron/v3
```

2、cron定时任务注册

```go
var Cron *cron.Cron

// InitCronJob 初始化cron定时任务，建议main函数中调用
func InitCronJob() {
	// 默认情况下为标准cron解析：最小单位仅支持到minute
	//Cron = cron.New()

	// 这种配置为6个字段且支持描述符如@monthly
	// 增加对second字段的支持，即第1个字段代表second，第2个字段才代表标准的minute
	// 等同于 cron.New(cron.WithSeconds())
	Cron = cron.New(cron.WithParser(cron.NewParser(
		cron.Second | cron.Minute | cron.Hour | cron.Dom | cron.Month | cron.Dow | cron.Descriptor,
	)))
	// 这个非标准的cron表达式表示每5s执行1次
	_, err := Cron.AddFunc("*/5 * * * * *", func() {
		fmt.Printf("定时任务执行了 当前时间: %v \n", time.Now())
	})
	if err != nil {
		log.Fatal(err)
	}
	Cron.Start()
}

// CloseCronJob 关闭定时任务，建议main函数中调用
func CloseCronJob() {
	Cron.Stop()
}
```

3、main函数

```go
func main() {
	// 初始化cron定时任务
	job.InitCronJob()
	defer job.CloseCronJob()

	time.Sleep(time.Minute * 5) // 主线程等待
}
```

就非常的简单。

建议：

> 在开发中，还是建议标准的cron表达式，因为没有必要精确到second级别。而且标准的cron表达式一看就懂，不用考虑不同库之间的区别。