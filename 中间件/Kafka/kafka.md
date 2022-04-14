# 资料

官网：https://kafka.apache.org/#

尚硅谷资料及视频：https://www.bilibili.com/video/BV1vr4y1677k?p=1

Go操作kafka的sarama库：https://github.com/Shopify/sarama
还有一个库：https://github.com/segmentio/kafka-go
这两个的stars都挺多的

# 概述

Apache Kafka 是一个开源**分布式事件流平台**，被数千家公司用于高性能数据管道、流分析、数据集成和任务关键型应用程序。

## 简介

**事件流**

事件流是从事件源（如数据库、传感器、移动设备、云服务和软件应用程序）以事件流的形式实时捕获数据的实践；持久存储这些事件流以供以后检索；实时和回顾性地操纵、处理和响应事件流；并根据需要将事件流路由到不同的目标技术。因此，事件流确保了数据的连续流动和解释，以便正确的信息在正确的时间出现在正确的位置。

**事件**：在文档中也称为记录或消息。事件具有键、值、时间戳和元数据。
**生产者：**是那些向 Kafka 发布（写入）事件的客户端应用程序。
**消费者：**是订阅（读取和处理）这些事件的那些客户端应用程序。

**Topic：**

topic中的事件可以根据需要随时读取——与传统的消息传递系统不同，事件在消费后不会被删除。可以通过每个topic的配置设置来定义 Kafka 应该将事件保留多长时间，之后旧事件将被丢弃。Kafka 的性能在数据大小方面实际上是恒定的，因此长时间存储数据是非常好的。

## 应用场景

传统消息队列主要应用场景：缓存、消峰、解耦、异步通信。

用例：

- 消息传递

- 网站活动跟踪
- 指标
- 日志聚合
- 流处理
- 事件溯源
- 提交日志

![image-20220411223600585](kafka.assets/image-20220411223600585.png)

## 基础架构

![image-20220411224434587](kafka.assets/image-20220411224434587.png)

Consumer Group：消费者组，由多个 consumer 组成。消费者组内每个消费者负责消费不同分区的数据，一个分区只能由一个组内消费者消费；消费者组之间互不影响。所有的消费者都属于某个消费者组，即消费者组是逻辑上的一个订阅者。

Broker：一台 Kafka 服务器就是一个 broker。一个集群由多个 broker 组成。一个broker 可以容纳多个 topic。

Topic：可以理解为一个队列，生产者和消费者面向的都是一个 topic。 

Partition：为了实现扩展性，一个非常大的 topic 可以分布到多个 broker（即服务器）上*一* 个topic 可以分为多个Partition，每个 partition 是一个有序的队列。 

Replica：副本。一个 topic 的每个分区都有若干个副本，一个 Leader 和若干个Follower。 

Leader：每个分区多个副本的“主”，生产者发送数据的对象，以及消费者消费数据的对象都是 Leader。 

Follower：每个分区多个副本中的“从”，实时从 Leader 中同步数据，保持和Leader 数据的同步。Leader 发生故障时，某个 Follower 会成为新的 Leader。

# 命令行操作

## 快速开始

官方文档单节点kafka快速开始：https://kafka.apache.org/documentation/#quickstart

## topic命令

在kafka的安装包的bin目录下，有一个kafka-topic.sh的shell脚本可以操作topic。

```shell
bin/kafka-topic.sh [--help]  #不加任何参数的话，就会返回有哪些参数可选，类似于--help

# 下面列举一些常用的
Option                                   Description                            
------                                   -----------        
--bootstrap-server 					     必须: 连接kafka服务器,如localhost:9092
--topic <String: topic>                  指定topic名称
--alter                                  修改partition数量，replica assignment,configuration 										 for the topic.
--create                                 Create a new topic.                    
--delete                                 Delete a topic  
--list                                   列出所有topic 
--describe								 查看topic具体描述
--partitions <Integer>  				 指定partitions数量
--replication-factor <Integer>           指定每个分区的副本因子
```

测试一下：

```shell
# 新建topic
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic my_topic1 --create
Created topic my_topic1.
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
my_log
my_topic
my_topic1
web_log
# 修改分区数量为3
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic web_log --alter --partitions 3
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic web_log --describe
Topic: web_log	TopicId: mpav7dCVQWSJYovnmjBlpw	PartitionCount: 3	ReplicationFactor: 1	Configs: segment.bytes=1073741824
	Topic: web_log	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: web_log	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: web_log	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
[root@k8s-master kafka_2.12-3.0.1]# 
```

> 注意：分区数量修改的时候，只能增加不能减少，否则报错。

## 生产者命令

在kafka的安装包bin目录下，有一个kafka-console-producer.sh的shell脚本可以模拟生产者。

```shell
bin/kafka-console-producer.sh [--help]  #不加任何参数的话，就会返回有哪些参数可选，类似于--help
# 列举常用的参数
Option                                   Description                            
------                                   -----------                                
--bootstrap-server						 必须：kafka服务器地址,如localhost:9092
--topic <String: topic>                  必须: 连接的主题topic                     
--sync                                   If set message send requests to the    
                                           brokers are synchronously, one at a  
                                           time as they arrive.                 
--timeout <Integer: timeout_ms>          If set and the producer is running in  
                                           asynchronous mode, this gives the    
                                           maximum amount of time a message     
                                           will queue awaiting sufficient batch 
                                           size. The value is given in ms.      
                                           (default: 1000)                      
```

## 消费者命令

在kafka的安装包bin目录下，有一个kafka-console-consumer.sh的shell脚本可以模拟消费者。

```shell
bin/kafka-console-consumer.sh [--help]  # 不加任何参数的话，就会返回有哪些参数可选，类似于--help
# 列举常用的参数
Option                                   Description                            
------                                   -----------                            
--bootstrap-server 						 必须：连接的kafka服务器地址,如localhost:9092
--topic <String: topic>                  指定topic                             
--from-beginning                         指定消费者也消费日志中存在的历史消息             
--group <String: consumer group id>      指定消费者group id       
--offset <String: consume offset>        指定消费消息的偏移量,or 'earliest'      
                                           which means from beginning, or       
                                           'latest' which means from end        
                                           (default: latest)                    
--partition <Integer: partition>         指定分区        
                                           Consumption starts from the end of   
                                           the partition unless '--offset' is   
                                           specified.  
--timeout-ms <Integer: timeout_ms>       如果设置，指定时间内未收到消息就退出
```

生产者和消费者命令一般一起使用：

```shell
# xshell打开一个窗口执行生产者命令，然后会让你一直输入信息，每次输入的信息都会发送到kafka
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic web_log
>hello kafka
>hello

# 此时再新开窗口，执行消费者命令，消费者命令会显示接收到的信息；
# 消费者配置的未收到消息1s后自动关闭
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic web_log --from-beginning --timeout-ms 1000
hello kafka
hello
[2022-04-12 22:19:10,904] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
org.apache.kafka.common.errors.TimeoutException
Processed a total of 21 messages
```

# kafka生产者

## 发送流程

在消息发送的过程中，涉及到了**两个线程——****main** **线程和** **Sender** **线程**。在 main 线程中创建了**一个双端队列** **RecordAccumulator**。main 线程将消息发送给 RecordAccumulator，Sender 线程不断从 RecordAccumulator 中拉取消息发送到 Kafka Broker。

![image-20220412223725676](kafka.assets/image-20220412223725676.png)

## 生产者API

需要引入生产者API依赖

```xml
 <dependency>
     <groupId>org.apache.kafka</groupId>
     <artifactId>kafka-clients</artifactId>
     <version>3.1.0</version>
 </dependency>
```

**生产者代码如下：异步发送**

```java
/**
 * @author fzk
 * @date 2022-04-12 22:53
 */
public class Producer {
    public static void main(String[] args) {
        // 0.准备配置
        Properties properties = new Properties();
        // 0.1 必须：连接kafka集群：bootstrap.servers
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "124.223.192.8:9092");
        // 0.2 必须：指定key和value的序列化方式
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 1.创建kafka生产者
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // 2.发送消息
        for (int i = 0; i < 5; i++) {
            // 发送消息并设置回调函数
            // 注意：消息发送失败会自动重试，不需要我们在回调函数中手动重试
            producer.send(new ProducerRecord<>("my_topic1", "k" + i, "v" + i),
                    (RecordMetadata metadata, Exception exception) -> {
                        if (exception == null) {
                            System.out.println("主题：" + metadata.topic() + "\t分区：" + metadata.partition());
                        }
                    });
        }
        // 3.关闭资源
        producer.close();
    }
}
```

执行上面的生产者代码，然后去xshell连接上消费者端后查看信息：可以看到成功了。

```shell
# 消费者命令连接上kafka并查看my_topic1主题的消息
[root@k8s-master kafka_2.12-3.0.1]# bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my_topic1 --from-beginning
v0
v1
v2
v3
v4
```

**同步发送**：只需在异步发送的基础上，再调用一下 get()方法即可。因为send()方法返回的是一个`Future`对象，直接调用get()方法会阻塞直到结果出现。(所以呢也不算真正的同步发送，毕竟不是主线程去发送消息，只是主线程在等待发送线程完成工作而已)。改成下面这样：

```java
        // 2.发送消息
        for (int i = 0; i < 5; i++) {
            // 发送消息并设置回调函数
            // 注意：消息发送失败会自动重试，不需要我们在回调函数中手动重试
            producer.send(
                    new ProducerRecord<>("my_topic1", "k" + i, "v" + i),
                    (RecordMetadata metadata, Exception exception) -> {
                        if (exception == null) {
                            System.out.println("主题：" + metadata.topic() + "\t分区：" + metadata.partition());
                        }
                    }).get(); // 就是这个加个get()
        }
```

## 生产者分区

### 分区好处

![image-20220412234110753](kafka.assets/image-20220412234110753.png)

### 分区策略

默认分区策略的分区器是`DefaultPartitioner`

```java
/**默认分区策略
如果指定了分区则直接使用
未指定分区但是有key，则根据key进行hash选择分区
未指定分区或且未指定key，选择粘性分区直至它满为止
*/
public class DefaultPartitioner implements Partitioner {
}
```

![image-20220412235453256](kafka.assets/image-20220412235453256.png)

比如上面的生产者同步方式发送消息代码，就未指定分区，指定了key，那么看看它发送的分区情况呢：
前提：需要配置topic的分区数量大于1哦

```shell
bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic my_topic1 --alter --partitions 3
```

然后结果如下：

![image-20220413000214072](kafka.assets/image-20220413000214072.png)

### 自定义分区器

在上面的默认分区器`DefaultPartitioner`中可以看到它是实现了`Partitioner`分区器接口，那么自定义分区器也需要从它下手。

```java
/**
 * 自定义分区器
 *
 * @author fzk
 * @date 2022-04-13 0:07
 */
public class MyPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        //System.out.println("自定义分区器运行...");
        // 分区数量
        int partitionCount = cluster.partitionsForTopic(topic).size();
        // 对value进行hash选择分区
        if (partitionCount > 1) {
            return Utils.toPositive(Utils.murmur2(valueBytes)) % partitionCount;
        }
        return 0;
    }
    @Override
    public void close() {}
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

如何配置使用自定义的分区器呢？
在上面的生产者发送消息代码中，配置生产者的地方增加配置自定义分区器即可：

```java
// 0.3 optional：自定义分区器
properties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, MyPartitioner.class.getName());
```

运行效果如下：为什么会运行两次分区器呢？

```
自定义分区器运行...
自定义分区器运行...
主题：my_topic1	分区：2
自定义分区器运行...
自定义分区器运行...
主题：my_topic1	分区：1
自定义分区器运行...
自定义分区器运行...
主题：my_topic1	分区：0
自定义分区器运行...
自定义分区器运行...
主题：my_topic1	分区：2
自定义分区器运行...
自定义分区器运行...
主题：my_topic1	分区：0
```

## 生产经验调优

### 提高吞吐量

`ProducerConfig`类中与吞吐量有关的几个配置如下：

```java
public static final String BATCH_SIZE_CONFIG = "batch.size";// 默认16K
public static final String LINGER_MS_CONFIG = "linger.ms";// 默认0
/*压缩方式. 默认为none，即不压缩. 可选的有none,gzip,snappy,lz4,zstd.Compression is of full batches of data, so the efficacy of batching will also impact the compression ratio (more batching means better compression).*/
public static final String COMPRESSION_TYPE_CONFIG = "compression.type";// 默认none
public static final String BUFFER_MEMORY_CONFIG = "buffer.memory";// 默认32M
```

建议将上面分别修改为：

```java
// 0.4 optional: 配置和吞吐量相关的配置
properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16 * 1024);// 默认16k
properties.put(ProducerConfig.LINGER_MS_CONFIG, 100);// 默认0，修改为500ms
properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");// 默认none，开启snappy压缩
properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64 * 1024 * 1024);// 默认32M，改为64M
```

### 数据可靠性：ACK应答

在官方文档中对于配置`acsk`说明如下：

生产者要求leader在请求完成请求之前收到的确认数量。这控制了发送的记录的持久性。允许以下设置：
- `acks=0`：生产者不会等待服务器的确认。该记录将立即添加到套接字缓冲区并被视为已发送。这种情况下不能保证服务器已经收到记录，`retries`配置不会生效（因为客户端一般不会知道有任何故障）。为每条记录返回的偏移量将始终设置为`-1`.
- `acks=1`：leader会将记录写入其本地日志，在不会等待所有replica的完全确认的情况下做出响应。在这种情况下，如果leader在确认消息后但在replica复制它之前立即失败，那么消息将丢失。
- `acks=all`：leader将等待完整的同步replica set来确认记录。这保证了只要至少一个同步副本保持活动状态，记录就不会丢失。这是最有力的保证。相当于 `acks=-1` 设置。

直接粘贴尚硅谷的图：

![image-20220413213828392](kafka.assets/image-20220413213828392.png)

![image-20220413215415174](kafka.assets/image-20220413215415174.png)

![image-20220413215705191](kafka.assets/image-20220413215705191.png)

生产者配置增加如下：

```java
        // 0.5 optional：配置应答策略acks，默认为all
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        // 配置重试次数retries，默认Integer.MAX_VALUE
        // 官方文档：用户通常应该更喜欢不设置此配置，而是使用delivery.timeout.ms来控制重试行为
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
```

但是只是这样配置的话，可能会导致数据重复问题。

### 数据去重

在上面的acks设置为`all`后的可能会产生一个数据重复问题：

![image-20220413220946469](kafka.assets/image-20220413220946469.png)

#### 幂等性

生产者保证每个消息的序列号是不变的，并且单调递增，类似于计算机网络中TCP协议的可靠传输机制呢。

![image-20220413221504046](kafka.assets/image-20220413221504046.png)

在官方文档的生产者配置中，[enable.idempotence](https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence)配置是否开启幂等性，默认为true，当它为true时，要求`max.in.flight.requests.per.connection`小于或等于 5（保留任何允许值的消息顺序），`retries`大于 0，并且`acks=all`。

#### 事务

![image-20220413223939489](kafka.assets/image-20220413223939489.png)

kafka的官方文档关于事务`transactional.id`的说明：

这启用了跨越多个生产者会话的可靠性语义，因为它允许客户端保证在开始任何新事务之前已完成使用相同TransactionalId 的事务。如果没有提供 TransactionalId，则生产者仅限于幂等交付。默认情况下没有配置TransactionId，即不能使用事务。请注意，**默认情况下，事务需要至少三个代理的集群，这是生产的推荐设置**；对于开发，您可以通过调整代理设置来更改此设置`transaction.state.log.replication.factor`

kafka的事务API：在KafkaProducer类中有如下方法：

![kafka事务API](kafka.assets/kafka事务API.png)

使用事务保证消息的可靠发送：要使用事务，必须开启事务配置，即配置事务id：`transactional.id`

```java
        // 0.6 optional: 如果开启事务，则是必须指定事务id并保证全局唯一
        properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "transaction_id_01");
```

使用事务的示例：

```java
private static void kafkaSendMsgWithTransaction(KafkaProducer<String, String> producer) {
    // 1.初始化事务
    producer.initTransactions();
    // 2.开启事务
    producer.beginTransaction();

    try {
        // 3.发送消息
        for (int i = 0; i < 5; i++) {
            // 发送消息并设置回调函数
            // 注意：消息发送失败会自动重试，不需要我们在回调函数中手动重试
            producer.send(
                new ProducerRecord<>("my_topic1", "k" + i, "v" + i),
                (RecordMetadata metadata, Exception exception) -> {
                    if (exception == null) {
                        System.out.println("主题：" + metadata.topic() + "\t分区：" + metadata.partition());
                    }
                });
            //.get(); // 主线程同步等待发送线程发送完成
        }

        // 4.提交事务
        producer.commitTransaction();
    } catch (Exception e) {
        // 5.异常则取消事务
        producer.abortTransaction();
    }
}
```

### 数据有序性

![image-20220413231658514](kafka.assets/image-20220413231658514.png)

官方文档中关于[max.in.flight.requests.per.connection](https://kafka.apache.org/documentation/#producerconfigs_max.in.flight.requests.per.connection)的说明：类似于TCP中的发送窗口。

客户端在阻塞之前将在单个连接上发送的未收到ack确认请求的最大数量。默认为5。

# kafka Broker

## broker总体工作流程

![image-20220413233415711](kafka.assets/image-20220413233415711.png)

如果leader挂了：

![image-20220413233529297](kafka.assets/image-20220413233529297.png)

## 节点服役与退役

看pdf和视频资料：https://www.bilibili.com/video/BV1vr4y1677k?p=24

# Golang操作kafka

golang操作kafka的库有两个，一个是[sarama](https://github.com/Shopify/sarama)，一个是[kafka-go](https://github.com/segmentio/kafka-go)，目前第一个star数更多，本笔记也使用第一个库。

## 生产者

1、用到的常量定义：一般来说，下面这些都是要配置到配置文件中去的，这里简化为写死到常量中

```go
const (
	UserRoleAllSyncRequestTopic  = "iam.sync.event.user-role-req.0"    // 全量同步请求topic
	UserRoleAllSyncResponseTopic = "iam.sync.message.user-role.0"      // 全量同步响应topic
	UserRoleChangeSyncTopic      = "iam.sync.event.user-role-change.0" // 增量同步topic
	UserRoleSyncReceiverKey      = "receiver"                          // 消息头中接受者key
    TestKafkaAddr                = "你的kafka地址host:port"              // kafka地址
)
```

2、创建生产者连接的代码：

```go
var (
	// 同步生产者
	producer sarama.SyncProducer
	// 异步生产者
	asyncProducer sarama.AsyncProducer
)

// InitProducer 初始化生产者，在main函数中调用
func InitProducer() {
	// 1.准备配置
	config := sarama.NewConfig()
	config.Producer.RequiredAcks = sarama.WaitForAll // 发送完数据需要leader和follow都确认，默认WaitForLocal RequiredAcks，即1
	// 分区器建议用默认的就好，不用改啦，默认情况：有key则hash，无key则粘性分区
	//config.Producer.Partitioner = sarama.NewRandomPartitioner // 新选出一个partition
	config.Producer.Return.Errors = true // 发送消息出现异常把错误放入管道，默认true

	//  2.初始化同步生产者
	initSyncProducerClient(config)

	// 3.初始化异步生产者
	config.Producer.Return.Successes = true // 异步发送最好开启消息发送成功后返回通知并放入管道，默认false
	initAsyncProducerClient(config)
}

// CloseProducer 关闭生产者连接，如果有初始化生产者则必须在main函数中调用
func CloseProducer() {
	closeSyncProducerClient()  // 关闭同步生产者
	closeAsyncProducerClient() // 关闭异步生产者
}

// SyncPublishMsg 同步推送消息
func SyncPublishMsg(producerMsg *sarama.ProducerMessage) error {
	if producer == nil {
		log.Fatalln("同步生产者客户端未进行初始化，请在main函数中调用InitProducerClient()手动初始化并注册关闭函数CloseProducerClient()")
	}
	// 发送消息
	partitionId, offset, err := producer.SendMessage(producerMsg)
	if err != nil {
		fmt.Printf("发送消息失败，产生错误%+v \n", err)
		return err
	}
	fmt.Printf("消息已经成功发送到：topic: %v, partitionId:%v , offset:%v\n", producerMsg.Topic, partitionId, offset)
	return nil
}

// AsyncPublishMsg 异步推送消息
func AsyncPublishMsg(producerMsg *sarama.ProducerMessage) error {
	if asyncProducer == nil {
		log.Fatalln("异步生产者客户端未进行初始化，请在main函数中调用InitAsyncProducerClient()手动初始化并注册关闭函数CloseAsyncProducerClient()")
	}
	// 异步发送消息： 从AsyncProducer接口的注释得知
	// 1.必须从asyncProducer.Errors()管道中读取错误信息，否则会造成死锁，除非手动将Producer.Return.Errors设置为false
	// 2.如果手动将Producer.Return.Successes设置为true，则必须从asyncProducer.Successes()管道中读取成功信息，否则会造成死锁
	select {
	// 把消息往管道一放就完事了，再读取一下错误和成功消息免得死锁就行啦
	case asyncProducer.Input() <- producerMsg:
		log.Printf("消息已经放入管道，等待异步发送\n")
	case err := <-asyncProducer.Errors():
		log.Printf("发送消息失败了: %+v \n", err)
	case <-asyncProducer.Successes():
		log.Printf("消息已经成功交付\n")
	}
	return nil
}

// initSyncProducerClient 初始化同步生产者客户端，应在main函数中调用
func initSyncProducerClient(config *sarama.Config) {
	var err error

	// 同步的生产者客户端连接
	producer, err = sarama.NewSyncProducer([]string{TestKafkaAddr}, config) // note：这里先连接自己的kafka
	if err != nil {
		log.Fatalln(err)
	}
	log.Printf("同步生产者客户端初始化成功\n")
}

// closeSyncProducerClient 关闭同步生产者客户端连接，最好放在main函数中
func closeSyncProducerClient() {
	if err := producer.Close(); err != nil {
		fmt.Printf("关闭同步生产者客户端出现异常：%+v \n", err)
	}
}

// InitAsyncProducerClient 初始化异步生产者客户端，应在main函数中调用
func initAsyncProducerClient(config *sarama.Config) {
	var err error

	// 同步的生产者客户端连接
	asyncProducer, err = sarama.NewAsyncProducer([]string{TestKafkaAddr}, config) // note：这里先连接自己的kafka
	if err != nil {
		log.Fatalln(err)
	}
	log.Printf("异步生产者客户端初始化成功\n")
}

// closeAsyncProducerClient 关闭异步生产者客户端，应在main函数中调用
func closeAsyncProducerClient() {
	if err := asyncProducer.Close(); err != nil {
		log.Printf("关闭异步生产者客户端出现异常：%+v\n", err)
	}
}
```

3、使用一波

```go
func main() {
	user_role_sync.InitProducer()
	defer user_role_sync.CloseProducer()

	var tenantKey = "CTS"
	var users = []*user_role_sync.UserMsg{{Username: "fzk", Roles: []*user_role_sync.RoleMsg{{Name: "developer"}}}}
	if err := user_role_sync.PublishUserChangeSyncMsg(user_role_sync.OptUserAdd, tenantKey, users); err != nil {
		log.Fatalln(err)
	}
}

// PublishUserChangeSyncMsg 广播用户信息改变消息，即增量同步用户信息
// @param opt 指定操作行为，可选的有user:add,user:update,user:delete
// @param tenantKey 指定租户的key
// @param users 用户信息列表
func PublishUserChangeSyncMsg(opt string, tenantKey string, users []*UserMsg) error {
	// 0.参数检查
	if opt != OptUserAdd && opt != OptUserUpdate && opt != OptUserDelete {
		return errors.New(fmt.Sprintf("增量同步用户信息: opt 只能是 %s 或 %s 或%s，然后传入的opt是%s", OptUserAdd, OptUserUpdate, OptUserDelete, opt))
	}
	if tenantKey == "" {
		return errors.New("增量同步用户信息: 租户key不能为空")
	}
	if users == nil || len(users) == 0 {
		return errors.New("增量同步用户信息: 用户数组不能为空")
	}
	// 1.构造消息体
	valBuf, err := json.Marshal(&UserRoleChangeSyncMsg{
		Opt:   opt,
		Users: users,
	})
	if err != nil {
		return err
	}

	// 2.构造一个消息
	producerMsg := &sarama.ProducerMessage{
		Topic:   UserRoleChangeSyncTopic, // 增量同步topic
		Headers: []sarama.RecordHeader{{Key: []byte(UserRoleSyncReceiverKey), Value: []byte(tenantKey)}},
		Key:     sarama.StringEncoder("user:" + strconv.FormatInt(users[0].ID, 10)), // 以user:id为key
		Value:   sarama.ByteEncoder(valBuf),
	}

	// 3.发送消息
	return SyncPublishMsg(producerMsg)
}

```

