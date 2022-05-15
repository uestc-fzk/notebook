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

# Java操作rocketmq

使用的库是rocketmq-client

在官方文档中，还有一个库值得关注rocketmq-spring。

1、引入依赖

```xml
<!--rocketmq 依赖-->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.3</version>
</dependency>
```

2、生产者

```java
/**
 * @author fzk
 * @date 2022-05-15 22:04
 */
public class RocketMQProducer {
    private static final String TestTopic = "test_topic";
    private static final String TestTag = "test_tag";
    private static final String NameServer = "124.223.192.8:9876;101.34.5.36:9876";
    private static final String ProducerGroupName = "producerGroup1";
    private static DefaultMQProducer producerGroup = null;

    public static void main(String[] args) throws MQClientException, MQBrokerException, RemotingException, UnsupportedEncodingException, InterruptedException {
        try {
            // 1.初始化生产者
            initProducer();
            // 2.同步发消息
            syncSendMsg();
            // 3.异步发消息
            asyncSendMsg();
            // 4.单向发消息
            oneWaySendMsg();
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
        producerGroup.setRetryTimesWhenSendAsyncFailed(0); // 设置异步发送的失败重试次数
    }

    static void closeProducer() {
        // 关闭生产者
        producerGroup.shutdown();
    }

    // 同步发送消息
    public static void syncSendMsg() throws UnsupportedEncodingException, MQBrokerException, RemotingException, InterruptedException, MQClientException {
        for (int i = 0; i < 3; i++) {
            // 创建消息
            Message msg = new Message(TestTopic /* Topic */,
                    TestTag /* Tag */,
                    ("Hello RocketMQ " +
                            i).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
            );
            // 发送消息到某个broker
            SendResult sendResult = producerGroup.send(msg);
            System.out.printf("%s\n", sendResult);
        }
    }

    // 异步发送消息
    public static void asyncSendMsg() throws InterruptedException {
        int messageCount = 3;
        final CountDownLatch countDownLatch = new CountDownLatch(messageCount);
        for (int i = 0; i < messageCount; i++) {
            try {
                final int index = i;
                Message msg = new Message(TestTopic,
                        TestTag,
                        "k" + i,
                        "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET));
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
    public static void oneWaySendMsg() throws UnsupportedEncodingException, RemotingException, InterruptedException, MQClientException {
        for (int i = 0; i < 3; i++) {
            Message msg = new Message(TestTopic /* Topic */,
                    TestTag /* Tag */,
                    ("Hello RocketMQ " +
                            i).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
            );
            producerGroup.sendOneway(msg);
        }
    }
}
```

3、消费者

```java
/**
 * @author fzk
 * @date 2022-05-15 22:24
 */
public class RocketMQConsumer {
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
        // 3.订阅topic的所有tag消息
        // 标签过滤表达式格式："*tag1 || tag2 || tag3* || *"
        consumerGroup.subscribe(TestTopic, "*");
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
            Thread.sleep(1000 * 20);// 休眠20s
        } finally {
            consumerGroup.shutdown();
        }
    }
}
```

