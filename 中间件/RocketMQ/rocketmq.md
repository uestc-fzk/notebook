# 资料

书籍《RocketMQ技术内幕》第2版

官方中文文档：https://github.com/apache/rocketmq/tree/master/docs/cn

# RocketMQ学习

## 单机版安装

首先，Linux系统要安装有Java8以上，最好是压缩包安装，不要用yum安装。

官网快速开始：https://rocketmq.apache.org/docs/quick-start/

1、下载安装包

```shell
wget https://dlcdn.apache.org/rocketmq/4.9.3/rocketmq-all-4.9.3-bin-release.zip
```

2、解压并**修改初始内存**

```shell
unzip rocketmq-all-4.9.3-bin-release.zip

# 必须修改配置内存大小，因为它默认设置的是固定4g，哪那么多内存给你用啊，会启动失败的
cd rocketmq-4.9.3/
# 先修改NameServer启动文件
vim bin/runserver.sh 
# 再修改broker启动文件
vim bin/runbroker.sh

# 分别修改的是这个设置内存的地方
JAVA_OPT="${JAVA_OPT} -server -Xms512m -Xmx512m -Xmn256m ....."
JAVA_OPT="${JAVA_OPT} -server -Xms512m -Xmx512m"
```

3、NameServer启动

```shell
# 后台启动NameServer
nohup sh bin/mqnamesrv &
# 查看启动日志，如果成功的话，会有成功信息提示
tail -f ~/logs/rocketmqlogs/namesrv.log
```

4、broker启动

```shell
# 后台启动broker；这个也可以前台启动
nohup sh bin/mqbroker -n localhost:9876 &
# 查看启动日志
tail -f ~/logs/rocketmqlogs/broker.log 
```

5、发消息测试

```shell
# 先暂时设个环境变量
export NAMESRV_ADDR=localhost:9876
# 这个吧，和kafka那个工具不一样，这个是一次性向某个topic发送1000条消息，然后结束
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer
```

6、消费消息测试

```shell
# 这个消费者工具倒是会一直阻塞等新消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

从这里可以看出，这个命令行工具不太好用啊？是不是有其它工具啊？

7、关闭服务

```shell
# 先关闭broker
sh bin/mqshutdown broker
# 再关闭NameServer
sh bin/mqshutdown namesrv
```

## 集群安装

官方中文运维教程：https://github.com/apache/rocketmq/blob/master/docs/cn/operation.md



## 最佳实践

官方文档最佳实践(中文)：https://github.com/apache/rocketmq/blob/master/docs/cn/best_practice.md



# RocketMQ Dashboard

地址：https://github.com/apache/rocketmq-dashboard

可以直接用docker部署，非常简单，缺点是要用8080端口！

```shell
docker pull apacherocketmq/rocketmq-dashboard:latest

docker run -d --name rocketmq-dashboard -e "JAVA_OPTS=-Drocketmq.namesrv.addr=你的RocketMQ地址:9876" -p 8080:8080 -t apacherocketmq/rocketmq-dashboard:latest
```

要想改端口的话，只能按照文档教程，从源码下载然后改`application.properties`，并用maven构建jar包部署，也不是很麻烦。

颜值还可以：

![image-20220512221004325](rocketmq.assets/image-20220512221004325.png)

