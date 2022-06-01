# 资料

书籍《RocketMQ技术内幕》第2版

官方中文文档：https://github.com/apache/rocketmq/tree/master/docs/cn

# RocketMQ运维

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

## 运维管理

地址：https://github.com/apache/rocketmq/blob/master/docs/cn/operation.md

### 集群理论

#### 复制策略

复制策略是Broker的Master与Slave间的数据同步方式。分为同步复制与异步复制：

- 同步复制：消息写入master后，master会等待slave同步数据成功后才向producer返回成功ACK

- 异步复制：消息写入master后，master立即向producer返回成功ACK，无需等待slave同步数据成功

异步复制策略会降低系统的写入延迟，*RT*变小，提高了系统的吞吐量，但是如果发送者发送的消息到master后，master返回ACK后就挂掉了，则此条消息就丢失了。

#### 刷盘策略

刷盘策略指的是broker中消息的落盘方式，即消息发送到broker内存后消息持久化到磁盘的方式。分为同步刷盘与异步刷盘：

- 同步刷盘：当消息持久化到broker的磁盘后才算是消息写入成功。

- 异步刷盘：当消息写入到broker的内存后即表示消息写入成功，无需等待消息持久化到磁盘。

*1*）异步刷盘策略会降低系统的写入延迟，*RT*变小，提高了系统的吞吐量 

*2*）消息写入到*Broker*的内存，一般是写入到了*PageCache* 

*3*）对于异步 刷盘策略，消息会写入到*PageCache*后立即返回成功*ACK*。但并不会立即做落盘操作，而是当*PageCache*到达一定量时会自动进行落盘

#### 集群模式

##### 1.单master

这种方式风险较大，一旦Broker重启或者宕机时，会导致整个服务不可用。不建议线上环境使用,可以用于本地测试。

单master的安装方式就很简单。

##### 2.多master

一个集群无Slave，全是Master，例如2个Master或者3个Master，这种模式的优缺点如下：

- 优点：配置简单，单个Master宕机或重启维护对应用无影响，在磁盘配置为RAID10时，即使机器宕机不可恢复情况下，由于RAID10磁盘非常可靠，消息也不会丢（异步刷盘丢失少量消息，同步刷盘一条不丢），性能最高；
- 缺点：单台机器宕机期间，这台机器上未被消费的消息在机器恢复之前不可订阅，消息实时性会受到影响。

##### 3.多master多slave-异步复制

每个Master配置一个Slave，有多对Master-Slave，HA采用异步复制方式，即消息写入master成功后，master立即向producer返回成功ACK，无需等待slave同步数据成功。主备有短暂消息延迟（毫秒级），这种模式的优缺点如下：

- 优点：即使磁盘损坏，消息丢失的非常少，且消息实时性不会受影响，同时Master宕机后，消费者仍然可以从Slave消费，而且此过程对应用透明，不需要人工干预，性能同多Master模式几乎一样；
- 缺点：Master宕机，磁盘损坏情况下会丢失少量消息。

##### 4.多master多slave-同步双写

每个Master配置一个Slave，有多对Master-Slave，HA采用同步双写方式，指的是消息写入master成功后，master会等待slave同步数据成功后才向producer返回成功ACK，即master与slave都要写入成功后才会返回成功ACK，也即双写。这种模式的优缺点如下：

- 优点：数据与服务都无单点故障，Master宕机情况下，消息无延迟，服务可用性与数据可用性都非常高；
- 缺点：性能比异步复制模式略低（大约低10%左右），发送单个消息的RT会略高，**且目前版本在主节点宕机后，备机不能自动切换为主机**

### 多master集群安装

官方中文运维教程：https://github.com/apache/rocketmq/blob/master/docs/cn/operation.md

这里用两台轻量云服务器124.233.192.8和101.34.5.36以多master集群为例子进行记录。(多master多slave-异步复制集群安装可以看PDF文档)。

首先可以看下rocketmq的conf目录下有哪些配置文件：其中2m-2s-async需要关注，这个目录中放着多master多slave-异步复制集群模式的配置文件。

```shell
[root@k8s-master conf]# ls
2m-2s-async  2m-2s-sync  2m-noslave  acl  broker.conf  dledger  logback_broker.xml  logback_namesrv.xml  logback_tools.xml  tools.yml
[root@k8s-master conf]# cd 2m-2s-async/
[root@k8s-master 2m-2s-async]# ls
broker-a.properties  broker-a-s.properties  broker-b.properties  broker-b-s.properties
```

再看一下此时需要的多master目录：

```shell
[root@k8s-master ~]# cd /opt/rocketmqDemo/rocketmq-4.9.3/
[root@k8s-master rocketmq-4.9.3]# cd conf/2m-noslave/
[root@k8s-master 2m-noslave]# ls
broker-a.properties  broker-b.properties  broker-trace.properties
```

0、两台服务器都需要修改启动文件，详细参看单机版安装步骤。

1、首先是一个基本的详细配置如下：待会所有的配置文件修改都基于此，所以就得确保两台服务器的rocketMQ是安装在同样的文件目录`/opt/rocketmqDemo/rocketmq-4.9.3`中的。

```properties
listenPort=10911 # 接受客户端连接的监听端口
# nameServer 地址，分号分隔
namesrvAddr=124.223.192.8:9876;101.34.5.36:9876
# 强制指定本机IP，需要根据每台机器进行修改。官方介绍可为空，系统默认自动识别，但多网卡时IP地址可能读取错误
brokerIP1=124.223.192.8
# 指定master-slave集群的名称。一个RocketMQ集群可以包含多个master-slave集群。
# master和slave的名字要求一样，毕竟slave是根据brokerName找到它的master的
brokerName=broker-a
# 指定整个broker集群的名称，或者说是RocketMQ集群的名称
brokerClusterName=DefaultCluster
brokerId=0 # broker id, 0 表示 master, 其他的正整数表示 slave
# 主从复制策略：指定当前broker为异步复制master
brokerRole=ASYNC_MASTER
# 落盘策略：异步刷盘
flushDiskType=ASYNC_FLUSH

# 指定消息存储相关的路径。默认路径为$HOME/store目录
# 存储根路径
storePathRootDir=/opt/rocketmqDemo/rocketmq-4.9.3/store/
storePathCommitLog=/opt/rocketmqDemo/rocketmq-4.9.3/store/commitlog/
storePathConsumeQueue=/opt/rocketmqDemo/rocketmq-4.9.3/store/consumequeue
storePathIndex=/opt/rocketmqDemo/rocketmq-4.9.3/store/index
storeCheckpoint=/opt/rocketmqDemo/rocketmq-4.9.3/store/checkpoint
abortFile=/opt/rocketmqDemo/rocketmq-4.9.3/store/abort

# mappedFileSizeCommitLog=1024*1024*1024 # (1G)	commit log 的映射文件大小
deleteWhen=04 # 在每天的凌晨4点删除已经超过文件保留时间的 commit log
fileReservedTime=72 # 指定未发生更新的消息存储文件的保留时长
```

2、服务器1修改broker-a.properties，在上面的基础修改如下几条：

```properties
namesrvAddr=124.223.192.8:9876;101.34.5.36:9876
brokerIP1=124.223.192.8
brokerName=broker-a
brokerId=0 # broker id, 0 表示 master, 其他的正整数表示 slave
```

> 注意：这个NameServer的地址是分号`;`作为分隔！不是传统的逗号`,`

3、服务器2修改broker-b.properties

```properties
namesrvAddr=124.223.192.8:9876;101.34.5.36:9876
brokerIP1=101.34.5.36
brokerName=broker-b
brokerId=0 # broker id, 0 表示 master, 其他的正整数表示 slave
```

4、两台服务器都先启动NameServer

上面只修改了broker的数据存储地址，namesrv的日志地址没有修改，还在默认位置。

```shell
# 后台启动NameServer
nohup sh bin/mqnamesrv &
# 查看启动日志，如果成功的话，会有成功信息提示
tail -f ~/logs/rocketmqlogs/namesrv.log
```

5、服务器1启动broker-a集群的master

> 注意：启动集群前先删除原有的测试单节点是mq所创建的存储，默认是~/store，否则集群会启动失败。

```shell
nohup sh bin/mqbroker -c conf/2m-noslave/broker-a.properties & 

tail -f ~/logs/rocketmqlogs/broker.log
```

6、服务器2启动broker-b集群的master

```shell
nohup sh bin/mqbroker -c conf/2m-noslave/broker-b.properties & 

tail -f ~/logs/rocketmqlogs/broker.log
```

> 注意：这两台服务器启动指定的配置文件是不同的噶，分别代表了两个集群的master节点配置。

如果启动失败，则改为前台启动`sh bin/mqbroker -c conf/2m-noslave/broker-b.properties`查看错误原因，如果是下面的样子：

```shell
[root@k8s-node1 rocketmq-4.9.3]# sh bin/mqbroker -c conf/2m-noslave/broker-b.properties
Java HotSpot(TM) 64-Bit Server VM warning: Option UseBiasedLocking was deprecated in version 15.0 and will likely be removed in a future release.
The Name Server Address[124.223.192.8:9876;101.34.5.36:9876 # nameServer å°åï¼éå·åé] illegal, please set it as follows, "127.0.0.1:9876;192.168.0.1:9876"
```

经过自己多次尝试，应该是程序读配置namesrvAddr时的一些读取方式有问题，导致了其它配置这么写都没问题，就这一行有问题。

避免措施：用vim命令行手动输入`namesrvAddr=xxx`，并且后面不能跟注释，不能出现中文，**末尾不能有任何空格**！

7、顺利的话，可以在dashboard监控界面中看到如下结果：

![image-20220515211921364](rocketmq.assets/image-20220515211921364.png)

然后再去看一下配置的store目录是否创建成功：

下面这种就是成功了的，如果这里目录乱码了，说明配置文件那里又有空格或中文，vim操作，删掉所有空格和中文注释！

```shell
[root@k8s-master store]# cd /opt/rocketmqDemo/rocketmq-4.9.3/store/
[root@k8s-master store]# ls
abort  checkpoint  commitlog  config  consumequeue  lock
```

8、集群关闭

```shell
# 先关闭broker
sh bin/mqshutdown broker
# 再关闭NameServer
sh bin/mqshutdown namesrv
```

### mqadmin管理工具

在mq解压目录的bin目录下有一个mqadmin命令，该命令是一个运维指令，用于对mq的主题，集群，broker 等信息进行管理。

官方手册：https://github.com/apache/rocketmq/blob/master/docs/cn/operation.md#2-mqadmin%E7%AE%A1%E7%90%86%E5%B7%A5%E5%85%B7

```shell
./bin/mqadmin # 可以查看能执行的全部命令参数

[root@k8s-master rocketmq-4.9.3]# ./bin/mqadmin topicList
RocketMQLog:WARN No appenders could be found for logger (io.netty.util.internal.InternalThreadLocalMap).
RocketMQLog:WARN Please initialize the logger system properly.
org.apache.rocketmq.tools.command.SubCommandException: TopicListSubCommand command faile
......
```

如果出现了上面这个错误，那没法了，PDF中的解决措施是修改tool.sh指定jre下的ext目录，可是Java17早已经没了jre目录了。

> 虽然这个mqadmin管理工具用不了，但是dashboard可以用啊。

### dashboard管理

上面已经知道mqadmin工具好像用不了，但是dashboard更好用！

用dashboard直接创建topic：非常方便！

![image-20220515214712250](rocketmq.assets/image-20220515214712250.png)

![image-20220515214740435](rocketmq.assets/image-20220515214740435.png)

## Broker配置

broker所有配置如下：

| 参数名                  | 默认值                 | 说明                                                         |
| ----------------------- | ---------------------- | ------------------------------------------------------------ |
| listenPort              | 10911                  | 接受客户端连接的监听端口                                     |
| namesrvAddr             | null                   | nameServer 地址                                              |
| brokerIP1               | 网卡的 InetAddress     | 当前 broker 监听的 IP                                        |
| brokerIP2               | 跟 brokerIP1 一样      | 存在主从 broker 时，如果在 broker 主节点上配置了 brokerIP2 属性，broker 从节点会连接主节点配置的 brokerIP2 进行同步 |
| brokerName              | null                   | broker 的名称                                                |
| brokerClusterName       | DefaultCluster         | 本 broker 所属的 Cluster 名称                                |
| brokerId                | 0                      | broker id, 0 表示 master, 其他的正整数表示 slave             |
| storePathRootDir        | $HOME/store/           | 存储根路径                                                   |
| storePathCommitLog      | $HOME/store/commitlog/ | 存储 commit log 的路径                                       |
| mappedFileSizeCommitLog | 1024 * 1024 * 1024(1G) | commit log 的映射文件大小                                    |
| deleteWhen              | 04                     | 在每天的什么时间删除已经超过文件保留时间的 commit log        |
| fileReservedTime        | 72                     | 以小时计算的文件保留时间                                     |
| brokerRole              | ASYNC_MASTER           | SYNC_MASTER/ASYNC_MASTER/SLAVE                               |
| flushDiskType           | ASYNC_FLUSH            | SYNC_FLUSH/ASYNC_FLUSH SYNC_FLUSH 模式下的 broker 保证在收到确认生产者之前将消息刷盘。ASYNC_FLUSH 模式下的 broker 则利用刷盘一组消息的模式，可以取得更好的性能。 |

- Broker 角色：分为 ASYNC_MASTER（异步主机）、SYNC_MASTER（同步主机）以及SLAVE（从机）。如果对消息的可靠性要求比较严格，可以采用 SYNC_MASTER加SLAVE的部署方式。如果对消息可靠性要求不高，可以采用ASYNC_MASTER加SLAVE的部署方式。如果只是测试方便，则可以选择仅ASYNC_MASTER或仅SYNC_MASTER的部署方式。

- 落盘策略：SYNC_FLUSH（同步刷新）相比于ASYNC_FLUSH（异步处理）会损失很多性能，但是也更可靠，所以需要根据实际的业务场景做好权衡。

## 最佳实践

官方文档最佳实践(中文)：https://github.com/apache/rocketmq/blob/master/docs/cn/best_practice.md

一定要看！！！非常重要！！！

### 生产者

看官方文档：https://github.com/apache/rocketmq/blob/master/docs/cn/best_practice.md

### 消费者

看官方文档：https://github.com/apache/rocketmq/blob/master/docs/cn/best_practice.md

### broker

看官方文档：https://github.com/apache/rocketmq/blob/master/docs/cn/best_practice.md

## RocketMQ Dashboard

地址：https://github.com/apache/rocketmq-dashboard

可以直接用docker部署，非常简单，缺点是要用8080端口！

```shell
docker pull apacherocketmq/rocketmq-dashboard:latest

docker run -d --name rocketmq-dashboard -e "JAVA_OPTS=-Drocketmq.namesrv.addr=你的RocketMQ地址:9876" -p 8080:8080 -t apacherocketmq/rocketmq-dashboard:latest
```

要想改端口的话，只能按照文档教程，从源码下载然后改`application.properties`，并用maven构建jar包部署，也不是很麻烦。

颜值还可以：

![image-20220512221004325](rocketmq.assets/image-20220512221004325.png)

# Java操作rocketmq

使用的库是rocketmq-client

在官方文档中，还有一个库值得关注rocketmq-spring。

引入依赖

```xml
<!--rocketmq 依赖-->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.3</version>
</dependency>
```

## 生产者

### 3种发送方式

RocketMQ发送消息有3中方式，分别为：

- 同步发送，即Producer发送消息后会阻塞等待broker返回ACK，消息可靠性最高，效率低
- 异步发送，Producer发送消息不等待ACK，而是注册一个回调方法处理ACK，有一定可靠性，发送效率高
- 单向发送，Producer只负责发消息，不等待、不处理MQ的ACK，且此方式broker不会返回ACK，效率最高，可靠性最差

```java
/**
 * @author fzk
 * @date 2022-05-30 15:02
 */
public class MyProducer {
    private static final String TestTopic = "test_topic";
    private static final String TestTag = "test_tag";
    private static final String TestKey = "test_key";
    // 需要注意的是NameServer集群需要以分好;分隔
    private static final String NameServer = "124.223.192.8:9876;101.34.5.36:9876";
    private static final String ProducerGroupName = "producerGroup1";
    private static DefaultMQProducer producerGroup = null;

    public static void main(String[] args) throws MQClientException, MQBrokerException, RemotingException, UnsupportedEncodingException, InterruptedException {
        try {
            // 1.初始化生产者并构造消息
            initProducer();
            Message msg = new Message(TestTopic, TestTag, TestKey, "hello world".getBytes(StandardCharsets.UTF_8));
            // 发送方式1：同步发消息
            syncSendMsg(msg);
            // 发送方式2：异步发消息
            asyncSendMsg(msg);
            /* 发送方式3：单向发消息
            单向发送消息是指，Producer仅负责发送消息，不等待、不处理MQ的ACK；该发送方式时MQ也不返回ACK。
            该方式的消息发送效率最高，但消息可靠性较差*/
            oneWaySendMsg(msg);
            Thread.sleep(1 << 11);// 等待2s等单向发消息执行完成
        } finally {
            // 关闭生产者
            closeProducer();
        }
    }

    static void initProducer() throws MQClientException {
        // 1.初始化生产者组
        producerGroup = new DefaultMQProducer(ProducerGroupName);
        // 2.指定NameServer集群
        producerGroup.setNamesrvAddr(NameServer);
        // 3.启动生产者
        producerGroup.start();
        // 设置异步发送的失败重试次数，默认2次
        producerGroup.setRetryTimesWhenSendAsyncFailed(0);
    }

    static void closeProducer() {
        // 关闭生产者
        producerGroup.shutdown();
    }

    // 同步发送消息
    public static void syncSendMsg(Message msg) throws UnsupportedEncodingException, MQBrokerException, RemotingException, InterruptedException, MQClientException {
        for (int i = 0; i < 3; i++) {
            // 发送消息到某个broker
            SendResult sendResult = producerGroup.send(msg);
            System.out.printf("%s\n", sendResult);
        }
    }

    // 异步发送消息
    public static void asyncSendMsg(Message msg) throws InterruptedException {
        int messageCount = 3;
        final CountDownLatch countDownLatch = new CountDownLatch(messageCount);
        for (int i = 0; i < messageCount; i++) {
            try {
                final int index = i;
                producerGroup.send(msg, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        countDownLatch.countDown();
                        System.out.printf("%-10d OK %s %n", index, sendResult.getMsgId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        countDownLatch.countDown();
                        System.out.printf("%-10d Exception %s %n", index, e);
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    // 单向发送，即调用此API将直接返回，不等消息结果也不注册回调函数，只管发！
    public static void oneWaySendMsg(Message msg) throws UnsupportedEncodingException, RemotingException, InterruptedException, MQClientException {
        for (int i = 0; i < 3; i++) {
            producerGroup.sendOneway(msg);
        }
    }
}
```

### 发送结果

同步发送会阻塞到broker返回ACK，这个被包装到`SendResult`类中

```java
public class SendResult {
    private SendStatus sendStatus;// 发送状态
    private String msgId;// 由broker端生产的消息id，不能确保唯一性
    private MessageQueue messageQueue;// 这个就包含了3个属性topic、brokerName、queueId
    private long queueOffset;// 此消息的队内偏移量
    private String transactionId;
    private String offsetMsgId;
    private String regionId;
    private boolean traceOn = true;
}

public enum SendStatus {
    SEND_OK, // 发送成功
    FLUSH_DISK_TIMEOUT, // 刷盘超时，只有在broke设置刷盘策略为同步刷盘时才可能出现，默认的异步刷盘不会出现
    FLUSH_SLAVE_TIMEOUT,// slave同步超时，当Broker集群设置的Master-Slave的复制方式为同步复制时才可能出现这种异常状态。异步复制不会出现
    SLAVE_NOT_AVAILABLE, // 没有可用的Slave。当Broker集群设置为Master-Slave的复制方式为同步复制时才可能出现这种异常状态。异步复制不会出现
}
```

## 消费者

```java
/**
 * @author fzk
 * @date 2022-05-15 22:24
 */
public class MyConsumer {
    private static final String TestTopic = "test_topic";
    private static final String TestTag = "test_tag";
    private static final String NameServer = "124.223.192.8:9876;101.34.5.36:9876";
    private static final String ConsumerGroupName = "consumerGroup1";
    private static DefaultMQPushConsumer consumerGroup = null;

    static void initConsumer() throws MQClientException {
        // 1.初始化消费者组
        consumerGroup = new DefaultMQPushConsumer(ConsumerGroupName);
        // 2.指定NameServer集群
        consumerGroup.setNamesrvAddr(NameServer);
        // 设置初始偏移量
        consumerGroup.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        /*
         设置消费模型：
         1.广播消费：消费者组内每个消费者都能消费每条消息，消费偏移offset保存在消费者端
         2.集群消费(默认)：消费者组内每条消息只能被一个消费者消费，消费偏移offset保存在broker端
         */
        consumerGroup.setMessageModel(MessageModel.CLUSTERING);
        // 3.订阅topic的所有tag消息
        // 标签过滤表达式格式："*tag1 || tag2 || tag3* || *"
        consumerGroup.subscribe(TestTopic, TestTag);
        // 4.注册消息监听器
        consumerGroup.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 5.启动消费者
        consumerGroup.start();
        System.out.printf("Consumer Started.%n");
    }

    public static void main(String[] args) throws InterruptedException, MQClientException {
        try {
            // 1.初始化消费者
            initConsumer();
            Thread.sleep(1000 * 60);// 休眠60s
        } finally {
            consumerGroup.shutdown();
        }
    }
}
```

## 顺序消息

顺序消息是严格按照发送顺序进行消费的消息(FIFO)。

默认情况下生产者会把消息以Round Robin轮询方式发送到不同的Queue队列；而消费消息时会从多个Queue上拉取消息，这种情况下的发送和消费是不能保证顺序的。如果将消息仅发送到同一个 Queue中，消费时也只从这个Queue上拉取消息，就严格保证了消息的顺序性。

要实现消息的业务顺序性，需要自定义消息队列选择器`MessageQueueSelector`。

可以像kafka那样以key的hash进行分区，可以自定义实现，也可以直接用官方提供的消息选择器：`SelectMessageQueueByHash`，可以看到代码非常简单。

```java
public class SelectMessageQueueByHash implements MessageQueueSelector {

    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        int value = arg.hashCode() % mqs.size();
        if (value < 0) {
            value = Math.abs(value);
        }
        return mqs.get(value);
    }
}
```

只需要在发送消息时指定消息选择器就可以了：

```java
producer.send(msg,new SelectMessageQueueByHash(),msg.getKeys());
```

这样就实现了以key选择队列，并使得业务消息存在依赖性的消息的key保持一致就好了，如用户id或者订单id。

## 延时消息

延时消息：消息写入broker后，在**等待一定时长后**才能被消费者消费的消息。

RocketMQ的延时消息可以实现一些定时任务功能，而无需使用定时器。典型场景：电商交易超时未支付订单的关闭

> 在电商平台中，订单创建时会发送一条延迟消息，30分钟后投递给后台业务系统(Consumer)。
>
> 后台业务系统收到该消息后会判断对应的订单是否已经支付，如果未完成，则取消订单，将商品放回库存；如果完成支付，则忽略。

像这种场景，实现方案有多种，除了采用MQ，还有定时任务去数据库轮询等。相对于数据库轮询，肯定是MQ相对开销低一点。那么如果放入Redis再进行轮询呢？这个也许得看RocketMQ如何实现的延时消息了。

### 延时等级

延时消息的延迟时长不支持随意时长，是通过特定的延迟等级来指定的。延时等级定义在 RocketMQ服务端的`org.apache.rocketmq.store.config.MessageStoreConfig`类中的如下变量中：

```java
private String messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h";
```

当然如果需要自定义延时等级，肯定是可以在broker的配置文件中进行配置的。

若指定的延时等级为3，则表示延迟时长为10s，即延迟等级是从1开始计数的。

### 实现原理

实现原理如图：[延时消息全流程](https://www.processon.com/view/link/6297271f7d9c085adb7d4597)

#### 生产者端

延时消息的发送只需要在发送消息前给消息标记一下就可以了：

```java
Message message = new Message("TestTopic", ("Hello scheduled message " + i).getBytes());
// 延时级别3，即10s此消息才能被消费
message.setDelayTimeLevel(3);
producer.send(message);
```

设置延时标记只是向消息属性中加一个`DELAY=延时级别`的键值对：

```java
public void setDelayTimeLevel(int level) {
    this.putProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL, String.valueOf(level));
}
```

#### 消息存储

延迟消息其实和正常消息一样都是立刻写入CommitLog的，如果对RocketMQ的消息转发到ConsumeQueue有了解的话，很可能会认为延迟消息是靠延迟转发来实现的。但是其实延迟消息也是立刻转发到ConsumeQueue。

其实，延迟消息和正常消息在存储流程中收到的待遇是一样的，只不过是**转发到了一个特殊的延迟topic和queue从而使得消费者不可见**。

服务端的`CommitLog.asyncPutMessage(MessageExtBrokerInner)`方法会先进行一些前置处理，这里可以看一下消息发送的存储流程。在这些前置处理中，当发现此消息属性中有`DELAY`键值对时则说明是延迟消息，并隐藏真正的topic和queueId，设为延迟topic和延迟级别queueId：

```java
// 1.3 延迟消息处理
if (msg.getDelayTimeLevel() > 0) {// 这里直接查消息属性的`DELAY`，如果有则是延迟消息
    if (msg.getDelayTimeLevel() > this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel()) {
        msg.setDelayTimeLevel(this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel());
    }

    topic = TopicValidator.RMQ_SYS_SCHEDULE_TOPIC;
    int queueId = ScheduleMessageService.delayLevel2QueueId(msg.getDelayTimeLevel());

    /*  将真正的topic和queueId保存到消息属性中，
        并把延迟消息topic和延迟级别的queueId设置到消息的topic和queueId中
        从而把消息发到延迟topic的ConsumeQueue里去，进而实现消息的延迟交付*/
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_TOPIC, msg.getTopic());
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_QUEUE_ID, String.valueOf(msg.getQueueId()));
    msg.setPropertiesString(MessageDecoder.messageProperties2String(msg.getProperties()));

    msg.setTopic(topic);// topic=SCHEDULE_TOPIC_XXXX
    msg.setQueueId(queueId);// queueId=延迟级别-1
}
```

在这里的前置处理后，消息会按照正常消息那样写入CommitLog文件，然后消息转发到topic为`SCHEDULE_TOPIC_XXXX`的ConsumeQueue文件中，queueId就是延迟级别-1，因为延迟级别从1开始嘛。

#### 延迟消息调度服务(轮询)

服务端延时任务调度，检查延时消息队列中是否有消息需要处理：延时消息服务由`ScheduleMessageService`实现：

```java
/**
 * 延迟消息服务线程
 * @see ScheduleMessageService#start()
 */
public class ScheduleMessageService extends ConfigManager {
    private static final long FIRST_DELAY_TIME = 1000L;// 初次调度延时
    private static final long DELAY_FOR_A_WHILE = 100L;// 每次调度延迟100ms
    private static final long DELAY_FOR_A_PERIOD = 10000L;
    private static final long WAIT_FOR_SHUTDOWN = 5000L;
    private static final long DELAY_FOR_A_SLEEP = 10L;

    // 延迟级别与延迟时间对应关系
    private final ConcurrentMap<Integer /* level */, Long/* delay timeMillis */> delayLevelTable =
            new ConcurrentHashMap<Integer, Long>(32);
    // 延迟级别对应的延迟消息队列待处理的最小偏移量
    private final ConcurrentMap<Integer /* level */, Long/* offset */> offsetTable =
            new ConcurrentHashMap<Integer, Long>(32);
    // 调度线程池，所有的延迟消息服务调度任务都是注册在这个线程池
    private ScheduledExecutorService deliverExecutorService;
}
```

它的start()方法在`DefalutMessageStore.start()`中调用，即随着存储服务的启动，延迟消息调度服务也跟着启动了：

```java
    public void start() {
        if (started.compareAndSet(false, true)) {
            // 1.加载延迟等级
            this.load();
            // 2.根据延迟等级个数指定core线程数量构建ScheduledThreadPoolExecutor
            this.deliverExecutorService = new ScheduledThreadPoolExecutor(this.maxDelayLevel, new ThreadFactoryImpl("ScheduleMessageTimerThread_"));
            // 省略部分代码
            for (Map.Entry<Integer, Long> entry : this.delayLevelTable.entrySet()) {
                Integer level = entry.getKey();
                Long timeDelay = entry.getValue();
                Long offset = this.offsetTable.get(level);
                if (null == offset) {
                    offset = 0L;
                }

                if (timeDelay != null) {
                    if (this.enableAsyncDeliver) {
                        this.handleExecutorService.schedule(new HandlePutResultTask(level), FIRST_DELAY_TIME, TimeUnit.MILLISECONDS);
                    }
                    // 3.为每个延迟等级安排1个调度任务
                    this.deliverExecutorService.schedule(new DeliverDelayedMessageTimerTask(level, offset), FIRST_DELAY_TIME, TimeUnit.MILLISECONDS);
                }
            }
			// 省略部分代码
        }
    }
```

步骤：1.加载延迟等级；2.创建调度线程池；3.每个调度等级都注册1个调度任务

`start()`方法对每个延迟级别都设置一个调度任务，默认会注册18个延迟任务，这18个延迟任务每次执行完成后都会再把自己注册回去，并设置延迟时间为100ms，**这轮询频率有点高啊**。

接下来看看调度任务每次调度会干什么：run()方法会调用executeOnTimeup()

```java
class DeliverDelayedMessageTimerTask implements Runnable {
    private final int delayLevel;// 此调度任务的延迟等级
    private final long offset;	// 此延迟等级对应的延迟消息队列ConsumeQueue的逻辑偏移量

    /**
     * run()方法调用这个方法
     * 此方法在每次执行完成后，都会再次以100ms延迟注册到调度线程池中
     */
    public void executeOnTimeup() {
        // 1.获取该延迟等级对应的Topic=SCHEDULE_TOPIC_XXXX的消息队列
        // queueId=delayLevel-1，因为延迟等级从1开始算的
        ConsumeQueue cq =
            ScheduleMessageService.this.defaultMessageStore.findConsumeQueue(TopicValidator.RMQ_SYS_SCHEDULE_TOPIC,
                                                                             delayLevel2QueueId(delayLevel));
        // 说明此queue文件都没创建，直接安排下次调度
        if (cq == null) {
            this.scheduleNextTimerTask(this.offset, DELAY_FOR_A_WHILE);
            return;
        }
        // 2.获取在ConsumeQueue的从当前处理的逻辑偏移量到当前读指针的缓冲区切片
        SelectMappedBufferResult bufferCQ = cq.getIndexBuffer(this.offset);
        if (bufferCQ == null) {  /* 省略部分代码*/ }

        long nextOffset = this.offset;
        try {
            int i = 0;
            ConsumeQueueExt.CqExtUnit cqExtUnit = new ConsumeQueueExt.CqExtUnit();
            // 3.对缓冲区切片的消息进行顺序处理
            for (; i < bufferCQ.getSize() && isStarted(); i += ConsumeQueue.CQ_STORE_UNIT_SIZE) {
                // 3.1 取出此消息的物理偏移量和大小，以及交付时间戳(原本存放hash码的地方)
                // 普通消息的条目：|   phyOffset 8byte   | size 4byte |   tag hashcode 8byte    |
                // 延迟消息的条目：|   phyOffset 8byte   | size 4byte |   deliverTimestamp 8byte|
                long offsetPy = bufferCQ.getByteBuffer().getLong();// 消息物理偏移量
                int sizePy = bufferCQ.getByteBuffer().getInt();// 消息长度
                long tagsCode = bufferCQ.getByteBuffer().getLong();// 本来是消息tag哈希码，但这里是交付时间戳
	
				// 省略部分代码
                // 计算交付时间
                long now = System.currentTimeMillis();
                long deliverTimestamp = this.correctDeliverTimestamp(now, tagsCode);
                nextOffset = offset + (i / ConsumeQueue.CQ_STORE_UNIT_SIZE);

                long countdown = deliverTimestamp - now;
                // 3.2 交付时间未到，则安排下一次调度并直接返回此次调度
                if (countdown > 0) {
                    this.scheduleNextTimerTask(nextOffset, DELAY_FOR_A_WHILE);
                    return;
                }
                // 3.3 交付时间到了，则从CommitLog文件中获取此消息
                MessageExt msgExt = ScheduleMessageService.this.defaultMessageStore.lookMessageByOffset(offsetPy, sizePy);
                if (msgExt == null) { continue; }

                // 3.4 将延迟消息恢复为普通消息
                // 设置真正的topic和queueId，移除属性中的延迟标记键值对`DELAY`
                MessageExtBrokerInner msgInner = ScheduleMessageService.this.messageTimeup(msgExt);
                // 省略

                boolean deliverSuc;
                // 4.交付消息：同步或异步，这里会去调用存储服务的putMessage()方法
                if (ScheduleMessageService.this.enableAsyncDeliver) {
                    deliverSuc = this.asyncDeliver(msgInner, msgExt.getMsgId(), nextOffset, offsetPy, sizePy);
                } else {
                    // 默认同步交付
                    deliverSuc = this.syncDeliver(msgInner, msgExt.getMsgId(), nextOffset, offsetPy, sizePy);
                }
                // 4.1 交付失败则安排下次调度重试
                if (!deliverSuc) {
                    this.scheduleNextTimerTask(nextOffset, DELAY_FOR_A_WHILE);
                    return;
                }
            }
            // 5.本地调度能交付的消息都交付完成后，则计算逻辑偏移量，安排下次调度
            nextOffset = this.offset + (i / ConsumeQueue.CQ_STORE_UNIT_SIZE);
        } catch (Exception e) {
            log.error("ScheduleMessageService, messageTimeup execute error, offset = {}", nextOffset, e);
        } finally {
            bufferCQ.release();
        }
        this.scheduleNextTimerTask(nextOffset, DELAY_FOR_A_WHILE);// 下次调度，100ms延迟任务
    }
}
```

每次调度任务的重要流程就是从指定的延迟topic的某个延迟队列中获取未交付的消息，将其中已经到达交付时间的消息取出来，重新封装之后，**再次发给了存储服务的putMessage()方法**。**消息持久化两次！**

注意：这里很可能有遗漏的地方：

> 我感觉我可能有什么地方没看懂，如果直接发给putMessage()方法，那它会将消息再次存入CommitLog，然后再转发给ConsumeQueue，虽然目的是达到了，但是多存了一条消息啊。而且，消息转发给Index服务后，那Index岂不是要存两条相同消息的索引了？
>
> 那为什么不直接把消息的物理偏移量转发到相应的ConsumeQueue呢？

## 事务消息



## 批量消息

生产者进行消息发送时可以一次发送多条消息，这可以大大提升Producer的发送效率。不过需要注意以下几点： 

- 批量发送的消息必须具有**相同的Topic** 
- 批量发送的消息必须具有**相同的刷盘策略**
- 批量发送的消息**不能是延时消息与事务消息**

默认情况下，批量消息大小不能超过4MB。

Producer发送的消息结构如下：

![消息发送结构](rocketmq.assets/消息发送结构.png)

其中properties是一堆属性，包括生产者地址、生产时间、queueId、是否为延迟消息等。









# 消息发送分析

## 发送流程

发送流程如下图：由于图片较大，建议看[processon原图](https://www.processon.com/view/link/6294c102e401fd2eed167ac6)

![消息发送流程](rocketmq.assets/消息发送流程.png)

# 消息存储分析

从存储方式和效率来看，`文件系统>KV存储>关系型数据库`，直接操作文件系统是最快的，但是可靠性是最低的。

- CommitLog

RocketMQ将所有主题topic的消息都存在同一个CommitLog文件中，确保顺序写消息，尽最大化保证消息发送的高性能和高吞吐量，但是消费消息时的拉取性能相对kafka较弱。

- ConsumeQueue

每个消息topic包含多个消息队列，每个消息队列有一个ConsumeQueue文件。queue上每个消息记录的是在CommitLog的物理偏移量。可以看作是基于topic的CommitLog索引文件

ConsumeQueue文件能提供消费者消费消息。该文件**给消费端提供了消息按topic分类的假象**，但**也实现了消息按Queue分区负载均衡**。

- Index

Index索引文件加速消息按key检索的性能，便于消息查询。存储key和消息在CommitLog的物理偏移量phyoffset对应关系。

存储store目录如下：

```shell
[root@k8s-master store]# ll
total 24
-rw-r--r-- 1 root root    0 May 15 21:34 abort
-rw-r--r-- 1 root root 4096 May 30 22:46 checkpoint
drwxr-xr-x 2 root root 4096 May 15 22:32 commitlog
drwxr-xr-x 2 root root 4096 May 30 22:46 config
drwxr-xr-x 3 root root 4096 May 15 22:32 consumequeue
drwxr-xr-x 2 root root 4096 May 15 22:32 index
-rw-r--r-- 1 root root    4 May 15 21:34 lock
```

> abort：该文件在Broker启动后会自动创建，正常关闭Broker，该文件会自动消失。若在没有启动Broker的情况下，发现这个文件是存在的，则说明之前Broker的关闭是非正常关闭。
>
> checkpoint：其中存储着commitlog、consumequeue、index文件的最后刷盘时间戳。
>
> commitlog：其中存放着commitlog文件，而消息是写在commitlog文件中的。
>
> config：存放着Broker运行期间的一些配置数据。
>
> consumequeue：其中存放着consumequeue文件，队列就存放在这个目录中。
>
> index：其中存放着消息索引文件indexFile。
>
> lock：运行期间使用到的全局资源锁，文件锁

## DefaultMessageStore

消息存储服务是`DefaultMessageStore`这个类提供的，其中`CommitLog`的落盘实现在CommitLog类中，`ConsumeQueue`和`Index`文件同理。

此类提供对`Message`消息的接受、写入CommitLog并转发到ConsumeQueue和Index中的功能。当然了这些的具体实现肯定都是在各自的实现类里。

### 启动流程

首先先看`DefaultMessageStore`这个类，下面只给出一些重要的属性：

```java
/**
 * 默认消息存储实现类
 *
 * @see DefaultMessageStore#start() 启动方法
 * @see DefaultMessageStore#addScheduleTask() 添加定时任务方法，如定时清理过期文件
 */
public class DefaultMessageStore implements MessageStore {
    private final MessageStoreConfig messageStoreConfig;// 消息存储配置
    private final BrokerConfig brokerConfig;// broker配置
   
    private final CommitLog commitLog; // CommitLog服务
    // Topic的ConsumeQueue队列
    private final ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;
    
    // ------------------------文件服务-------------------------------------
    // ConsumeQueue文件的刷盘服务线程
    private final FlushConsumeQueueService flushConsumeQueueService;
    // CommitLog文件过期清理服务--以定时任务形式
    private final CleanCommitLogService cleanCommitLogService;
    // ConsumeQueue文件过期清理服务--以定时任务形式
    private final CleanConsumeQueueService cleanConsumeQueueService;
    // 提供索引服务
    private final IndexService indexService;
    // MappedFile分配服务线程
    private final AllocateMappedFileService allocateMappedFileService;
    // ------------------------文件服务-------------------------------------
    
    
    // ------------------------消息服务--------------------------------------
    /*
     转发消息服务线程
     作用是转发CommitLog文件的更新事件到ConsumeQueue和Index以使其更新
     */
    private final ReputMessageService reputMessageService;
    // 延迟消息服务
    private final ScheduleMessageService scheduleMessageService;
	// ------------------------消息服务--------------------------------------
    
    // 瞬态内存池，里面有1个ByteBuffer的队列可用于分配使用
    private final TransientStorePool transientStorePool;

    // 定时任务调度器，上面两个清理文件服务线程都是以这个进行的定时调度
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("StoreScheduledThread"));
}
```

在这些属性中，需要更加重点关注的是CommitLog服务、刷盘服务线程、转发消息服务线程、瞬态缓冲池。

在上面的属性中，一部分是其内部类：

![DefaultMessageStore内部类](rocketmq.assets/DefaultMessageStore内部类.png)

其它的类大部分在CommitLog中。服务类按功能分散于不同的基类中。

在这个存储类中，重要的方法有它的构造方法`DefaultMessageStore()`和启动方法`start()`

这个建议直接看源码，这里给出我绘制的[DefaultMessageStore启动流程图](https://www.processon.com/view/link/62962c1c5653bb788c85bdaa)

### 发送存储流程

消息在broker收到后的存储流程入口是`org.apache.rocketmq.store.DefaultMessageStore#putMessage(MessageExtBrokerInner)`。

```java
    /**
     * TODO 消息存储入口函数
     *
     * @param msg Message instance to store
     */
    @Override
    public PutMessageResult putMessage(MessageExtBrokerInner msg) {
        return waitForPutResult(asyncPutMessage(msg));
    }
```







## CommitLog

 消息主题及元数据的存储主题，消息内容不定长。

存储目录为`$ROCKET_HOME/store/commitlog`，文件默认大小1GB，文件名长度20为，即起始偏移量，左边补0。第一个文件名为00000000000000000000，第二个为00000000001073741824。



## ConsumeQueue

消息消费队列的引入目的是提高消息消费性能，并对消息进行topic模拟分类，消费者通过订阅消息队列来消费消息。ConsumeQueue可以看作是基于topic的CommitLog索引文件。

具体存储路劲为`$ROCKET_HOME/store/consumequeue/{topic}/{queueId}/{fileName}`。

消息采用定长20字节设计，30万条目组成，可以像数组一样随机访问，每个文件大约5.72MB。

## Index























# 消息消费分析



# RocketMQ与Kafka比较

Kafka性能强于RocketMQ

RocketMQ功能性更好：

> 1.消息名称空间
>
> 2.三种发送方式--单向发送
>
> 3.消息标签tag，消息key+消息时间戳过滤查询
>
> 4.顺序消息
