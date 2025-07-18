# 资料

官网：https://netty.io

书籍：《Netty权威指南(第2版)》

芋道源码-Netty源码分析：http://svip.iocoder.cn/Netty

技术文章摘抄：https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/Netty%20%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E5%89%96%E6%9E%90%E4%B8%8E%20RPC%20%E5%AE%9E%E8%B7%B5-%E5%AE%8C

本文将根据当前netty最新稳定版本`4.1.83.Final-SNAPSHOT`进行源码分析。

# 概述

Netty是**异步事件驱动**的网络应用程序框架，用于**快速**和**简单**的开发可维护的**高性能协议**服务器和客户端。

Netty是NIO客户端服务器框架，实现多种协议(FTP、SMTP、HTTP以及各种二进制和基于文本的旧协议)。

Netty在Java网络编程应用框架中的地位就如Spring在JavaEE中的地位。

以下的框架都使用了 Netty：

* Cassandra - nosql 数据库
* Spark - 大数据分布式计算框架
* Hadoop - 大数据分布式存储框架
* RocketMQ - 阿里开源的消息队列
* ElasticSearch - 搜索引擎
* gRPC - rpc 框架
* Dubbo - rpc 框架
* Spring 5.x - flux api 完全抛弃了 tomcat ，使用 netty 作为服务器端
* Zookeeper - 分布式协调框架

## 整体架构

![components](netty.assets/components.png)

- Core：核心部分，是底层的网络通用抽象和部分实现。
  - Extensible Event Model ：可拓展的事件模型。Netty 是基于事件模型的网络应用框架。
  - Universal Communication API ：通用的通信 API 层。Netty 定义了一套抽象的通用通信层的 API 。
  - Zero-Copy-Capable Rich Byte Buffer ：支持零拷贝特性的 Byte Buffer 实现。
- Transport Services：传输( 通信 )服务，具体的网络传输的定义与实现。
  - Socket & Datagram ：TCP 和 UDP 的传输实现。
  - HTTP Tunnel ：HTTP 通道的传输实现。
  - In-VM Piple ：JVM 内部的传输实现。
- **Protocol Support** ：协议支持。Netty 对于一些通用协议的编解码实现。例如：HTTP、Redis、DNS 等等。

## 模块结构

![项目依赖](netty.assets/项目依赖.png)

common模块：该项目是一个通用的工具类项目，几乎被所有的其它项目依赖使用，它提供了一些数据类型处理工具类，并发编程以及多线程的扩展，计数器等等通用的工具类。

buffer模块：Netty 自行实现的一个 Byte Buffer 字节缓冲区。该包的实现相对于 JDK 自带的 ByteBuffer 有很多**优点**：无论是 API 的功能，使用体验，性能都要更加优秀。它提供了**一系列( 多种 )**的抽象定义以及实现，以满足不同场景下的需要。核心类`ByteBuf`

transport模块：该项目是网络传输通道的抽象和实现。它定义通信的统一通信 API ，统一了 JDK 的 OIO、NIO ( 不包括 AIO )等多种编程接口。核心类`Channel`

codec模块：实现Protocol Support，协议编解码的抽象与部分实现：JSON、protobuf、Base64、XML 等等。

handler模块：提供**内置的**连接通道处理器( ChannelHandler )实现类。例如：SSL 处理器、日志处理器等等。

## 线程模型

Netty基于主从Reactor多线程模型：

- Netty抽象出两组线程池：NioEventLoopGroup
  - BossGroup线程专门接受客户端连接
  - WorkerGroup专门负责网络的读写
- NioEventLoopGroup是事件循环组，组内可以含有多个事件循环NioEventLoop
- NioEventLoop表示一个不断循环的执行处理任务的线程，并含有1个Selector，监听读写事件
- Boss NioEventLoop循环执行3个步骤：
  - 轮询accept事件
  - 与客户端建立连接，生成NioSocketChannel，注册到某个worker NioEventLoop的selector中
  - 处理任务队列的任务，即runAllTasks
- Worker NioEventLoop循环执行3个步骤：
  - 在Selector轮询read/write事件
  - 在对应的NioSocketChannel处理I/O事件，调用pipeline维护的handler处理业务
  - 处理任务队列的任务，即runAllTasks
- NioEventLoop内部采用串行化设计，从消息的`读取-->解码-->处理-->编码-->发送`，始终由IO线程NioEventLoop负责

![image-20221010224119801](netty.assets/image-20221010224119801.png)

- NioEventLoopGroup包含多个NioEventLoop
- NioEventLoop包含1个Selector，1个taskQueue
- NioEventLoop的selector可以注册多个NioChannel
- 每个NioChannel只会绑定到1个NioEventLoop上
- 每个NioChannel会绑定1个自己的ChannelPipeline

## 简单入门

使用案例可以按照netty提供的example模块进行学习，下面以其中的echo案例进行小改：

服务端EchoServer：

```java
public final class EchoServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx = ServerUtil.buildSslContext();

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(serverHandler);
                 }
             });

            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

服务端Handler：

```java
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        System.out.printf("服务端收到来自 %s 消息 %s\n", ctx.channel().remoteAddress(), buf.toString(CharsetUtil.UTF_8));
        if ("ping".equals(buf.toString(CharsetUtil.UTF_8))) {
            // 1.自定义普通任务放入Worker线程池
            ctx.channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    ctx.writeAndFlush(Unpooled.copiedBuffer("pong", CharsetUtil.UTF_8));
                }
            });

            // 2.自定义定时任务放入Worker线程池
            ctx.channel().eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    ctx.writeAndFlush(Unpooled.copiedBuffer("pong", CharsetUtil.UTF_8));
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
```

客户端EchoClient：

```java
public final class EchoClient {
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.git
        final SslContext sslCtx = ServerUtil.buildSslContext();

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                        }
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(new EchoClientHandler());
                    }
                });

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // 1.客户端发送心跳
            Thread.sleep(100);
            f.channel().writeAndFlush(Unpooled.copiedBuffer("ping", CharsetUtil.UTF_8));
            for (int i = 0; i < 10; i++) {
                Thread.sleep(100);
                f.channel().writeAndFlush(Unpooled.copiedBuffer("ping" + i, CharsetUtil.UTF_8));
            }
 			// 2.关闭通道连接
            f.channel().close().sync(); // 手动关闭通道并等待
            // f.channel().closeFuture().sync(); // 等待直至通道关闭
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}
```

客户端Handler：

```java
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

    private final ByteBuf firstMessage;

    public EchoClientHandler() {
        firstMessage = Unpooled.copiedBuffer("hello i am client", CharsetUtil.UTF_8);
    }

    // 通道就绪时触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ChannelFuture channelFuture = ctx.writeAndFlush(firstMessage);
    }

    // 通道可读时触发
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //        ctx.write(msg);
        System.out.println("客户端收到消息：" + ((ByteBuf) msg).toString(CharsetUtil.UTF_8));
    }

    // 读取完成时触发
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
```

## HTTP服务端示例

在netty项目的example模块的http服务端示例的基础上修改了一点：

```java
// 一个简单HTTP服务器
public final class HttpHelloWorldServer {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        // 配置SSL
        final SslContext sslCtx = ServerUtil.buildSslContext();

        // 1.创建两个EventLoopGroup对象
        // boss线程组：用于服务端接受客户端连接，一般1个Acceptor线程就够了
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // worker线程组：用于Channel的IO操作和业务处理，默认线程数=CPU*2
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 2.创建并配置服务端启动器
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup) // 配置主从线程组
                .channel(NioServerSocketChannel.class) // 配置要使用的Channel类
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc()));
                        }
                        // 3.设置接入服务端的SocketChannel的处理器管道
                        // 向管道内加入HTTP协议的编码/解码器
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpServerExpectContinueHandler());
                        p.addLast(new HttpHelloWorldServerHandler());// 自定义handler
                    }
                });

            // 4.绑定端口，同步等待
            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Open your web browser and navigate to " +
                               (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            // 5.同步等待直至监听服务端关闭
            ch.closeFuture().sync();
        } finally {
            // 6.优雅的关闭两个线程组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

这里向pipeline中首先添加的是HTTP协议的编码解码器HttpServerCodec，它将HTTP请求数据转换为HttpRequest对象交由后面的pipeline继续处理。

处理器Handler：

```java
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            System.out.printf("收到客户端：%s 的HTTP请求\n", ctx.channel().remoteAddress());
            if("/favicon.ico".equals(req.uri())){
                System.out.println("请求了favicon.ico");
            }

            // 构造响应并输出
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK,
                    Unpooled.wrappedBuffer(CONTENT));
            response.headers()
                    .set(CONTENT_TYPE, TEXT_PLAIN)
                    .setInt(CONTENT_LENGTH, response.content().readableBytes());

            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                // Tell the client we're going to close the connection.
                response.headers().set(CONNECTION, CLOSE);
            }

            ChannelFuture f = ctx.write(response);

            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

从此案例可以看出来用Netty实现HTTP服务器非常简单，在处理器这边只需要根据业务进行分派处理即可。

## 粘包和拆包

关于粘包和拆包说明：Netty权威指南中p80-92

Netty中提供的处理TCP粘包拆包问题的解码器有：

- LineBasedFrameDecoder：基于行区分消息
- DelimiterBasedFrameDecoder：基于自定义分隔符区分消息，注意会将分隔符从消息中过滤掉
- FixedLengthFrameDecoder：定长消息
- **LengthFieldBasedFrameDecoder：指定自定义长度字段划分消息**

## 核心组件

Netty 有如下几个核心组件：

- Bootstrap & ServerBootstrap
- Channel
- ChannelFuture
- EventLoop & EventLoopGroup
- ChannelHandler
- ChannelPipeline

### Bootstrap

![image-20221012095044357](netty.assets/image-20221012095044357.png)

Bootstrap意思为引导启动器，Netty应用通常由Bootstrap引导启动，配置整个netty应用.

Bootstrap对于Netty就如同SpringBoot是Spring的启动器。

- Bootstrap：引导客户端Channel
  - `connect()`方法用于连接远程服务端，作为TCP客户端
  - `bind()`方法绑定本地一个端口，作为UDP的一端
- ServerBootstrap：引导服务端ServerChannel
  - `bind()`方法绑定本地端口，作为服务端
  - 使用**两个EventLoopGroup**对象，第1个接受Socket连接，第2个处理Socket连接IO事件

先看看两者的父类AbstractBootstrap：

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {
    volatile EventLoopGroup group;
    private volatile SocketAddress localAddress;
    private volatile ChannelHandler handler;

    public B group(EventLoopGroup group) {
        // 省略参数检查
        this.group = group;
        return self();
    }

    // 设置Channel的实现类
    public B channel(Class<? extends C> channelClass) {
        return channelFactory(new ReflectiveChannelFactory<C>(
            ObjectUtil.checkNotNull(channelClass, "channelClass")
        ));
    }

    // 配置Channel，一般用于服务端ServerChannel或客户端的Channel
    public <T> B option(ChannelOption<T> option, T value) {
        // 省略
    }
    
    // 用于服务端则是绑定监听端口，用于客户端则往往用于UDP
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }
}
```

父类AbstractBootstrap提供了一些共有实现，在使用服务端ServerBootstrap时，父类的group及其handler则扮演主从Reactor模型中的主Reactor，负责监听新连接，子类ServerBootstrap的childGroup和childHandler则判断主从Reactor模型中的从Reactor，负责监听连接的IO事件和业务处理。

```java
// 引导ServerChannel
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {
    private volatile EventLoopGroup childGroup;
    private volatile ChannelHandler childHandler;

    /**
     * 为父级（Acceptor）和子级(client)设置EventLoopGroup。
     * 这些EventLoopGroup用于处理ServerChannel和Channel的所有事件和 IO。
     */
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (this.childGroup != null)
            throw new IllegalStateException("childGroup set already");
        this.childGroup = ObjectUtil.checkNotNull(childGroup, "childGroup");
        return this;
    }
    
    // 此处的配置将用于配置Acceptor接受新连接所建立的Channel
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        // 省略
    }

    // 设置用于为Channel的请求提供服务的ChannelHandler
    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        this.childHandler = ObjectUtil.checkNotNull(childHandler, "childHandler");
        return this;
    }
}
```

### Channel

Channel 是 Netty 网络操作抽象类，它除了包括基本的 I/O 操作，如 bind、connect、read、write 之外，还包括了 Netty 框架相关的一些功能，如获取该 Channel 的 EventLoop 。

不同协议的连接有不同Channel，常见如下：

- NioSocketChannel：客户端TCP Socket连接
- NioServerSocketChannel：服务端TCP Socket连接
- NioDatagramChannel：UDP连接
- NioSctpChannel：客户端Sctp连接
- NioSctpServerChannel：Sctp服务端连接

为什么Netty要额外提供一个Channel呢？JDK提供的Socket对程序员不是很友好，使用成本较高。而相对于原生 NIO 的 Channel，Netty 的 Channel 具有如下优势( 摘自《Netty权威指南( 第二版 )》) ：

- 在 Channel 接口层，采用 Facade 模式进行统一封装，将网络 I/O 操作、网络 I/O 相关联的其他操作封装起来，统一对外提供。
- Channel 接口的定义尽量大而全，为 SocketChannel 和 ServerSocketChannel 提供统一的视图，由不同子类实现不同的功能，公共功能在抽象父类中实现，最大程度地实现功能和接口的重用。
- 具体实现采用聚合而非包含的方式，将相关的功能类聚合在 Channel 中，由 Channel 统一负责和调度，功能实现更加灵活。

可以把 Netty Channel 和 Java 原生 Socket 对应，而 Netty NIO Channel 和 Java 原生 NIO SocketChannel 对象。

### EventLoopGroup

EventLoopGroup是一组EventLoop的抽象，一般有多个EventLoop同时工作，每个EventLoop维护一个Selector实例。

在Netty的服务端中，一般会提供2个EventLoopGroup：BossEventLoopGroup和WorkerEventLoopGroup。

Channel 为Netty 网络操作抽象类，EventLoop 负责处理注册到其Selector的 Channel 处理 I/O 操作，两者配合参与 I/O 操作。

- 一个 EventLoopGroup 包含一个或多个 EventLoop ，即 EventLoopGroup : EventLoop = `1 : n` 。
- 一个 EventLoop 在它的生命周期内，只能与一个 Thread 绑定，即 EventLoop : Thread = `1 : 1` 。
- 所有有 EventLoop 处理的 I/O 事件都将在它**专有**的 Thread 上被处理，从而保证线程安全，即 Thread : EventLoop = `1 : 1`。
- 一个 Channel 在它的生命周期内只能注册到一个 EventLoop 上，即 Channel : EventLoop = `n : 1` 。
- 一个 EventLoop 可被分配至一个或多个 Channel ，即 EventLoop : Channel = `1 : n` 。

当一个连接到达时，Netty 就会创建一个 Channel，然后从 EventLoopGroup 中分配一个 EventLoop 来给这个 Channel 绑定上，在该 Channel 的整个生命周期中都是有这个绑定的 EventLoop 来服务的。

### ChannelFuture

**Netty中所有I/O操作都是异步的**，可以等待其操作完成或者注册一个监听器：

- `ChannelFuture#sync()`将等待异步操作执行结束
- `ChannelFuture#addListener()`注册一个ChannelFutureListener对象，在I/O操作完成后回调执行

```java
/**
 * Netty中所有I/O操作都是异步的，即任何I/O调用都会立刻返回ChannelFuture对象
 * <pre>
 *                                      +---------------------------+
 *                                      | Completed successfully    |
 *                                      +---------------------------+
 *                                 +---->      isDone() = true      |
 * +--------------------------+    |    |   isSuccess() = true      |
 * |        Uncompleted       |    |    +===========================+
 * +--------------------------+    |    | Completed with failure    |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * | isCancelled() = false    |    |    |       cause() = non-null  |
 * |       cause() = null     |    |    +===========================+
 * +--------------------------+    |    | Completed by cancellation |
 *                                 |    +---------------------------+
 *                                 +---->      isDone() = true      |
 *                                      | isCancelled() = true      |
 *                                      +---------------------------+
 * </pre>
 *
 * 建议addListener()添加监听器，以便在 I/O 操作完成时收到通知并执行任何后续任务
 * 不要在ChannelHandler中调用await()，很可能出现死锁
 */
public interface ChannelFuture extends Future<Void> {
    // 添加监听器
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    // 移除监听器
    ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    // 等待此I/O操作直至完成
    ChannelFuture sync() throws InterruptedException;

    // 等待指定时间
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;
}
```

### ChannelHandler

连接通道处理器，我们使用 Netty 中**最常用**的组件。ChannelHandler 主要用来处理各种事件，这里的事件很广泛，比如可以是连接、数据接收、异常、数据转换等。

通常实现ChannelHandler 的两个核心子类：

- `ChannelInboundHandler`：处理入站IO事件
- `ChannelOutboundHandler`：处理出站IO事件

更方便的可以实现以下适配器类：

- `ChannelInboudHandlerAdapter`：处理入站IO，实现类还包括一系列的 **Decoder** 类，对输入字节流进行解码。
- `ChannelOutboundHandlerAdapter`：处理出站IO，实现类还包括一系列的 **Encoder** 类，对输入字节流进行编码。
- `ChannelDuplexHandler`：处理入站和出站IO事件，经典实现类如HttpServerCodec，解码和编码HTTP服务请求和响应

![image-20221012092442445](netty.assets/image-20221012092442445.png)

`@Shareble`注解，如果ChannelHandler**标注有@Shareble注解**，则程序员应该只创建1次ChannelHandler实例，将其添加到多个ChannelPipeline中，即该ChannelHandler是线程安全的，如下：

```java
public static void main(String[] args) throws Exception {
    // Configure SSL.
    final SslContext sslCtx = ServerUtil.buildSslContext();

    // Configure the server.
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    // 只创建1次
    HttpHelloWorldServerHandler h=new HttpHelloWorldServerHandler();
    try {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    // 向管道内加入HTTP协议的编码/解码器
                    p.addLast(new HttpServerCodec());
                    p.addLast(new HttpServerExpectContinueHandler());
                    p.addLast(h);// 对于每个ChannelPipeline都添加同1个ChannelHandler
                }
            });

        Channel ch = b.bind(PORT).sync().channel();

        System.err.println("Open your web browser and navigate to " +
                           (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

        ch.closeFuture().sync();
    } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
```

而如果ChannelHandler中**保存有状态信息，非线程安全的**，则最好是每个Channel连接创建1个ChannelHandler。只需要改动如下：

```java
p.addLast(new HttpHelloWorldServerHandler());// 每个Channel连接1个ChannelHandler
```

注意：`@Shareble`注解仅是标注作用。

### ChannelPipeline

ChannelPipeline是ChannelHandler链，负责处理和拦截inbound或outbound事件。

```java
/**
 * 处理或拦截Channel的入站事件和出站操作的ChannelHandler列表
 * 拦截过滤器模式：用户可以完全控制事件的处理方式以及管道中ChannelHandler的相互交互方式。
 * 
 * 每个Channel都有自己的ChannelPipeline，在创建Channel时自动创建pipeline
 *
 * 下图描述了ChannelPipeline中的ChannelHandler通常如何处理 I/O 事件。 
 * I/O 事件由ChannelInboundHandler或ChannelOutboundHandler处理，并通过调用ChannelHandlerContext中定义的事件传播方法（例如ChannelHandlerContext.fireChannelRead(Object)和ChannelHandlerContext.write(Object)转发到其最近的处理程序。
 *
 * <pre>
 *                                                 I/O Request
 *                                            via {@link Channel} or
 *                                        {@link ChannelHandlerContext}
 *                                                      |
 *  +---------------------------------------------------+---------------+
 *  |                           ChannelPipeline         |               |
 *  |                                                  \|/              |
 *  |    +---------------------+            +-----------+----------+    |
 *  |    | Inbound Handler  N  |            | Outbound Handler  1  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  .               |
 *  |               .                                   .               |
 *  | ChannelHandlerContext.fireIN_EVT() ChannelHandlerContext.OUT_EVT()|
 *  |        [ method call]                       [method call]         |
 *  |               .                                   .               |
 *  |               .                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  1  |            | Outbound Handler  M  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  +---------------+-----------------------------------+---------------+
 *                  |                                  \|/
 *  +---------------+-----------------------------------+---------------+
 *  |               |                                   |               |
 *  |       [ Socket.read() ]                    [ Socket.write() ]     |
 *  |                                                                   |
 *  |  Netty Internal I/O Threads (Transport Implementation)            |
 *  +-------------------------------------------------------------------+
 * </pre>
 *
 * 例如，假设我们创建了以下管道：
 * <pre>
 * {@link ChannelPipeline} p = ...;
 * p.addLast("1", new InboundHandlerA());
 * p.addLast("2", new InboundHandlerB());
 * p.addLast("3", new OutboundHandlerA());
 * p.addLast("4", new OutboundHandlerB());
 * p.addLast("5", new InboundOutboundHandlerX());
 * </pre>
 * 入站事件处理顺序为：1->2->5
 * 出站事件处理顺序为：5->4->3
 *
 * 典型的服务器将在每个通道的管道中具有以下处理程序：
 *  1.协议解码器 - 将二进制数据（例如ByteBuf ）转换为 Java 对象。
 *  2.协议编码器 - 将 Java 对象转换为二进制数据。
 *  3.Business Logic Handler - 执行实际的业务逻辑（例如数据库访问）
 */
public interface ChannelPipeline
    extends ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Entry<String, ChannelHandler>> {

    // 将ChannelHandler插入管道头部
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    // 将ChannelHandler插入管道尾部
    ChannelPipeline addLast(String name, ChannelHandler handler);

    // 返回此管道中具有指定名称的ChannelHandler的上下文对象。
    ChannelHandlerContext context(String name);
    
    Channel channel();// 返回此pipeline所属的Channel
}
```

ChannelPipeline中还有很多执行链向后传递的入站方法和出站方法，这里不列出了。

### ChannelHandlerContext

保存和Channel相关的上下文信息，关联有1个ChannelHandler对象。同时也绑定对应的Channel和ChannelPipeline。

ChannelPipeline中就是以ChannelHandlerContext封装ChannelHandler，并将其组装成链。

```java
/**
 * 使ChannelHandler能够与其ChannelPipeline和其他处理程序进行交互。
 *
 * 存储有状态信息
 * attr(AttributeKey)允许您存储和访问与ChannelHandler/Channel及其上下文相关的有状态信息
 * 因为1个ChannelHandler实例可以添加到多个ChannelPipeline中，因此可以用此上下文保存一些状态信息
 * 另请注意，添加到多个ChannelPipeline的ChannelHandler应标记为@Sharable注解
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    // 此绑定的Channel
    Channel channel();

    // 将用于此ChannelHandler的EventExecutor线程池
    EventExecutor executor();

    // 此绑定的ChannelHandler
    ChannelHandler handler();

    // 请求通过此 ChannelOutboundInvoker 刷新所有待处理消息。
    ChannelHandlerContext flush();

    // 此绑定的pipeline
    ChannelPipeline pipeline();
}
```

# Reactor模型

Reactor模型核心思想：


> 将关注的 I/O 事件注册到多路复用器上，一旦有 I/O 事件触发，将事件分发到事件处理器中，执行就绪 I/O 事件对应的处理函数中。模型中有三个重要的组件：
>
> **多路复用器**：由操作系统提供接口，Linux 提供的 I/O 复用接口有select、poll、epoll 。
> **事件分发器**：将多路复用器返回的就绪事件分发到事件处理器中。
> **事件处理器**：处理就绪事件处理函数。

Reactor有3种模型实现：

- 单Reactor单线程模型
- 单Reactor多线程模型
- 多Reactor多线程模型

可以看看这篇文章：https://mp.weixin.qq.com/s/GRkZ1IEfTalQSkErWe1SAg

当下开源软件如Redis、Nginx、Netty做到网络高性能的原因就是使用I/O多路复用，而多路复用网络编程一般是面向过程方式写代码，开发效率不高，于是大佬们基于面向对象的思想，对 I/O 多路复用作了一层封装，让使用者不用考虑底层网络 API 的细节，只需要关注应用代码的编写，这种模式就是Reactor模式。

## 单Reactor单线程

![单Reactor单线程](netty.assets/单Reactor单线程.png)

- Reactor通过Selector监听到事件后，进行dispatch分发，连接事件分发给Acceptor，读写事件分发给Handler
- Acceptor 通过accept()方法获取连接，并创建一个handler绑定到此连接
- Handler 对象的作用是处理业务，当监听到连接的读写事件则交由其绑定的handler处理，Handler 对象通过 `read -> 业务处理 -> send` 的流程来完成完整的业务流程。

Redis在未采用I/O多线程之前呢，正是这种单Reactor单进程/线程模型，后来Redis把响应send交给专门I/O线程去输出了。业务处理依旧是单线程，因为Redis的内存操作不会阻塞，单个线程不用上下文切换更能长时间占有CPU。

## 单Reactor多线程

![单Reactor多线程](netty.assets/单Reactor多线程.png)

该模型**和单Reactor单线程的区别在于将业务处理逻辑分派到线程池中处理**，其它如I/O事件的监听/分派、read、send依旧由主线程处理。

这里很明显，出现性能瓶颈的地方肯定会是Reactor所在主线程。

Tomcat大致采用此模型，一个Acceptor线程负责监听新连接，Poller线程负责监听IO事件，监听到的Socket的IO事件将其包装为`read->业务处理->send`任务放入线程池处理。和这个模型的区别在于将耗时的read和send也交由线程池来处理。

## 多Reactor多线程

绝大多数场景下，Reactor多线程模型已经满足性能要求，但在百万并发连接、服务端需对客户端握手进行安全认证且认证较损耗性能时，**单个Acceptor线程/Selector线程可能存在性能不足问题**。

![多Reactor多线程](netty.assets/多Reactor多线程.png)

主Reactor就负责监听新连接，通过Acceptor的accept获取连接后分配给某个子线程SubReactor(一个SubReactor可以注册监听多个连接，不是一个连接一个线程哈)。

SubReactor就负责监听IO事件，并进行`read -> 业务处理 -> send`流程。

Netty采用此模型，其EventLoopGroup和EventLoop就以这种模型开发。

1. mainReactor 负责监听 ServerSocketChannel ，用来处理客户端新连接的建立，并将建立的客户端的 SocketChannel 指定注册给 subReactor 。
2. subReactor 维护自己的 Selector ，基于 mainReactor 建立的客户端的 SocketChannel 多路分离 IO 读写事件，读写网络数据。对于业务处理的功能，另外扔给 worker 线程池来完成。

### 案例

最佳示例：我写的简单http协议解析库：https://github.com/uestc-fzk/http_server，其中core模块就是主从Reactor模型，且引入了ChannelHandler和ChannelContext机制。

下面代码是多Reactor多线程简单示例：

> 注意：以下实现有问题，Reactor模型中，每个SubReactor仅绑定一个线程运行，它负责处理IO事件和任务队列，并控制IO处理时间和任务队列运行时间稳定在一定比例。
>
> 而下面实现中每个SubReactor绑定的是一个线程池，与Reactor模型相悖。

MainReactor如下：

```java
/**
 * @author fzk
 * @datetime 2022-10-14 10:15
 */
public class MainReactor implements Runnable {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress("localhost", 8080));
        serverSocketChannel.configureBlocking(false);
        MainReactor mainReactor = new MainReactor(serverSocketChannel);
        mainReactor.start();
    }

    private final Selector mainSelector;
    private final ServerSocketChannel serverSocketChannel;
    private final Acceptor acceptor;
    private final SubReactor[] subReactors;
    private volatile int nextSunReactor = 0;

    public MainReactor(ServerSocketChannel serverSocketChannel) throws IOException {
        this.serverSocketChannel = serverSocketChannel;
        this.mainSelector = SelectorProvider.provider().openSelector();
        this.acceptor = new Acceptor(mainSelector);
        this.serverSocketChannel.register(mainSelector, SelectionKey.OP_ACCEPT);

        this.subReactors = new SubReactor[Runtime.getRuntime().availableProcessors() + 1];
        for (int i = 0; i < subReactors.length; i++) {
            subReactors[i] = new SubReactor();
        }
    }

    public void start() throws IOException {
        // 1.启动SubReactor线程组
        ThreadGroup subReactorGroup = new ThreadGroup("subReactors");
        for (int i = 0; i < subReactors.length; i++) {
            new Thread(subReactorGroup, subReactors[i], "subReactor-" + i).start();
        }

        // 2.启动MainReactor/Acceptor线程
        new Thread(this, "mainReactor").start();
    }

    /**
     * 将SocketChannel转发到各个SubReactor中
     *
     * @param socketChannel 新连接
     */
    private void dispatch(SocketChannel socketChannel) throws IOException {
        // 将SocketChannel分派到各个SubReactor中
        synchronized (subReactors) {
            System.out.printf("监听到新连接: %s 即将分派到%s\n", socketChannel.getRemoteAddress(), "subReactor-" + nextSunReactor);
            SubReactor subReactor = subReactors[nextSunReactor++];
            if (nextSunReactor >= subReactors.length) nextSunReactor = 0;
            subReactor.registerChannel(socketChannel);
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // 监听新连接并转发
                List<SocketChannel> list = this.acceptor.accept();
              	if(list==null||list.size()==0) continue;
                for(SocketChannel sc : list){
                    dispatch(socketChannel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }   
}
```

Acceptor如下：感觉这个Acceptor有点不必要啊？

```java
static class Acceptor {
    private final Selector mainSelector;

    public Acceptor(Selector selector) {
        this.mainSelector = selector;
    }

    public List<SocketChannel> accept() throws IOException {
      	List<SocketChannel> list=new ArrayList<>();
        // 接收器获取新连接并返回
        if (mainSelector.select(1000) > 0) {
            Iterator<SelectionKey> keys = mainSelector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey selectionKey = keys.next();
                keys.remove();

                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) (selectionKey.channel());
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.write(ByteBuffer.wrap("您已经连上服务器".getBytes(StandardCharsets.UTF_8)));
                    list.add(sc);
                }
            }
        }
        return list;
    }
}
```

SubReactor如下：每个SubReactor都有1个Selector和1个线程池。

```java
static class SubReactor implements Runnable {
    final Selector subSelector;

    //多线程处理业务逻辑
    ExecutorService executorService;

    SubReactor() throws IOException {
        this.subSelector = SelectorProvider.provider().openSelector();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // 注册chanel，关注读事件
    public void registerChannel(SocketChannel sc) throws IOException {
        sc.configureBlocking(false);
        sc.register(subSelector, SelectionKey.OP_READ);
        wakeup();// 必须唤醒Selector才能检测到新加入的SocketChannel
    }

    /**
     * wakeup的目的在于唤醒Selector，原因如下：
     * 1.新加入的SocketChannel必须让Selector重新调用select()方法才能检测到
     * 2.SocketChannel修改兴趣集也必须让Selector重新调用select()方法才能检测到新兴趣集
     * 因此必须调用Selector#wakeup()让其立刻从select()阻塞调用中唤醒并重新发起select()监听
     */
    public void wakeup() {
        this.subSelector.wakeup();
    }

    public void updateInterestOps(SelectionKey key, int interest) {
        key.interestOps(interest);
        wakeup();// 修改兴趣集必须唤醒Selector让其重新发起select()监听才能监听到新兴趣集
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                if (subSelector.select() > 0) {
                    Set<SelectionKey> keys = subSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        // 监听到可读事件，进行处理
                        if (key.isReadable()) {
                            // 暂时将其兴趣集设置为0，防止多次触发可读操作
                            updateInterestOps(key, 0);
                            processRead(key);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processRead(SelectionKey key) {
        // 线程池异步处理
        executorService.execute(() -> {
            try {
                // 前置检查：key是否有效
                if(!key.isValid()){
                    cancelKey(key);
                    return;
                }
                // 前置检查：channel是否连接
                SocketChannel sc = (SocketChannel) key.channel();
                if(sc==null||!sc.isConnected()){
                    cancelKey(key);
                    return;
                }
                StringBuilder sb = new StringBuilder(128);
                ByteBuffer buf = ByteBuffer.allocate(128);

                // 注意：这里必须是-1，当没有内容时返回-1不是0!
                if (sc.read(buf) == -1) {
                    // 注意：没有内容说明客户端主动关闭连接
                    cancelKey(key);
                    return;
                }
                // 读内容
                do{
                    buf.flip();
                    sb.append(StandardCharsets.UTF_8.decode(buf));
                    buf.clear();
                } while (sc.read(buf) > 0);

                String message = sb.toString();
                System.out.printf("服务端收到来自%s的消息：%s\n", sc.getRemoteAddress(), message);
                if ("ping".equals(message)) sc.write(ByteBuffer.wrap("pong".getBytes()));// 心跳

                // 将兴趣集恢复，监听读事件
                updateInterestOps(key, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    // 取消key监听并关闭channel
    private void cancelKey(SelectionKey key) {
        if (key != null) {
            key.cancel();
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel != null) {
                try {
                    channel.close();
                    MyLogger.logger.fine(String.format("close connection from %s", channel.getRemoteAddress()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

客户端代码示例：

```java
/**
 * @author fzk
 * @datetime 2022-10-14 11:19
 */
public class Client {
    public static void main(String[] args) {
        new Client().startClient("client");
    }

    public void startClient(String clientName) {
        try (
            // 1.新建客户端通道
            SocketChannel sc = SocketChannel.open(new InetSocketAddress("localhost", 8080));
            // 2.新建选择器
            Selector selector = Selector.open();
        ) {
            sc.configureBlocking(false);// 设为非阻塞模式
            // 3.客户端通道注册到选择器
            sc.register(selector, SelectionKey.OP_READ);

            // 4.接受者线程
            Thread sender = new Thread(new Receiver(selector, clientName));
            sender.start();

            // 5.主线程做发送者
            System.out.printf("%s 启动成功...\n", clientName);
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                try {
                    sc.write(ByteBuffer.wrap(nextLine.getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Receiver implements Runnable {
        private Selector selector;
        private String clientName;

        public Receiver(Selector selector, String clientName) {
            this.selector = selector;
            this.clientName = clientName;
        }

        @Override
        public void run() {
            try {
                // 1.监听通道
                while (selector.select() > 0) {
                    // 2.取出可操作的通道
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey next = iterator.next();
                        iterator.remove();
                        // 2.读取消息
                        if (next.isReadable()) {
                            SocketChannel socketChannel = (SocketChannel) next.channel();
                            ByteBuffer buf = ByteBuffer.allocate(128);
                            StringBuilder sb = new StringBuilder();
                            while (socketChannel.read(buf) > 0) {
                                buf.flip();
                                sb.append(StandardCharsets.UTF_8.decode(buf));
                            }
                            // 注意：没消息说明对端关闭
                            if(sb.length()==0){
                                next.cancel();
                                socketChannel.close();
                                return;
                            }
                            String message = sb.toString();
                            System.out.printf("客户端%s收到消息: %s\n", clientName, message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
```

# Netty全流程

原图：https://www.processon.com/view/link/63f7273c075f2179bc9966bb

![Netty全流程](netty.assets/Netty全流程.png)

# BootStrap启动器

![image-20221012095044357](netty.assets/image-20221012095044357.png)

因为ServerBootstrap启动器和Bootstrap启动器大部分方法和职责是相同的，所以接下来将先分析AbstractBootstrap启动器，再分析服务端ServerBootstrap启动器，最后分析客户端Serverstrap启动器。

![NioSocketChannel](netty.assets/NioServerSocketChannel.png)

## AbstractBootstrap

在以下的HTTP服务端示例中可以得知：服务端启动的入口是`AbstractBootstrap#bind()`

```java
public static void main(String[] args) throws Exception {
    // 配置SSL
    final SslContext sslCtx = ServerUtil.buildSslContext();

    // 1.创建两个EventLoopGroup对象
    // boss线程组：用于服务端接受客户端连接，一般1个Acceptor线程就够了
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // worker线程组：用于Channel的IO操作和业务处理，默认线程数=CPU*2
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
        // 2.创建并配置服务端启动器
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(bossGroup, workerGroup) // 配置主从线程组
            .channel(NioServerSocketChannel.class) // 配置要使用的Channel类
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    // 3.设置接入服务端的SocketChannel的处理器管道
                    // 向管道内加入HTTP协议的编码/解码器
                    p.addLast(new HttpServerCodec());
                    p.addLast(new HttpServerExpectContinueHandler());
                    p.addLast(new HttpHelloWorldServerHandler());// 自定义handler
                }
            });

        // 4.绑定端口，同步等待
        Channel ch = b.bind(PORT).sync().channel();

        System.err.println("Open your web browser and navigate to " +
                           (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');

        // 5.同步等待直至监听服务端关闭
        ch.closeFuture().sync();
    } finally {
        // 6.优雅的关闭两个线程组
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
```

可以看到启动器调用bind()方法后就启动服务了，接下来分析这个AbstractBootstrap#bind()方法。

### bind

`AbstractBootstrap#bind()`方法核心流程如下图：

![bind](netty.assets/bind.png)

```java
// AbstractBootstrap.java
private ChannelFuture doBind(final SocketAddress localAddress) {
    // 1.创建Channel并注册到EventLoop中
    final ChannelFuture regFuture = initAndRegister();
    final Channel channel = regFuture.channel();
    if (regFuture.cause() != null) return regFuture;

    if (regFuture.isDone()) {
        // 如果已经完成异步操作注册成功了则直接调用doBind0()
    } else {
        // 添加监听器，在注册成功后回调doBind0()方法
        final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
        regFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // 省略部分代码
                // 2.执行channel的端口绑定逻辑
                doBind0(regFuture, channel, localAddress, promise);
            }
        });
        return promise;
    }
}
```

`AbstractBootstrap#doBind()`方法：

- 先调用initAndREgister()：根据配置创建Channel，将Channel注册到EventLoopGroup中，最后再执行Channel的端口绑定并注册到SelectionKey中。
- 再以回调方式注册doBind0()：主要是绑定Channel的端口，同时触发`ChannelPipeline#fireChannelActive()`回调

> 为什么这里要以回调方式调用doBind0()进行端口绑定呢?
>
> 因为注册Channel到EventLoop，其实就是包装为task扔到EventLoop的taskQueue中，让EventLoop自己绑定的线程将Channel注册到Selector上。
>
> 这就是**全异步**的设计，凡是和EventLoop相关的操作统一包装为task由其自己绑定线程处理，不阻塞主线程或boss线程。

### initAndRegister

`AbstractBootstrap#initAndRegister()`方法将根据传入的Channel实现类Class反射创建其实例，并对其初始化后，注册到EventLoopGroup中。

```java
// AbstractBootstrap.java
final ChannelFuture initAndRegister() {
    Channel channel = null;
    try {
        // 1.根据启动器配置的Channel类，反射创建对象
        channel = channelFactory.newChannel();
        // 2.初始化Channel对象
        init(channel);
    } catch (Throwable t) {/*省略*/ }
    
    // 3.注册Channel到EventLoopGroup中
    // 在方法内部会分配一个EventLoop对象并将channel注册到其上,boss Group一般设置1个EventLoop
    ChannelFuture regFuture = config().group().register(channel);
    if (regFuture.cause() != null) {/*省略*/  }

    return regFuture;
}
```

#### 创建Channel

从上面代码分析可知，创建Channel就是反射调用启动器传入的Channel类对象的构造器来创建实例，服务端一般传入的是NioServerSocketChannel，而客户端一般是NioSocketChannel。

从下图可以知道，创建Channel过程中会相应的创建ChannelPipeline，并创建JavaNIO下的SelectableChannel，将其包装起来，如果是服务端传入`SelectionKey.OP_ACCEPT`，用于之后监听连接事件，而客户端传入`SelectionKey.OP_READ`监听读事件：

![NioServerSocketChannel](netty.assets/NioServerSocketChannel.png)

#### 初始化Channel

`AbstractBootstrap#init()`初始化方法是空实现，交予子类实现：

- 在`ServerBootstrap#init()`中会往Pipeline中添加一个`ChannelInitializer`并重写其`initChannel()`方法用于将`ServerBootstrapAcceptor`这个handler加入pipeline，它会将新连接注册到workerEventLoopGroup中。具体分析在ServerBootstrap中分析。
- 在`Bootstrap#init()`中则仅仅设置Channel属性，并将启动器中用户配置的handler添加到pipeline，一般用户配置的也是`ChannelInitializer`，并重写其`initChannel()`方法用于在Channel注册到EventLoop后添加自定义handler。

#### 注册Channel到Group

在`NioEventLoopGroup#register(Channel channel)`方法中，注册流程大致如下：

![EventLoopGroup](netty.assets/EventLoopGroup#register.png)

EventLoopGroup会调用某个`EventLoop#register()`注册Channel，即将Channel绑定到某个EventLoop，其实就是将SocketChannel注册到EventLoop的Selector上。一般来说服务端或客户端的boss EventLoopGroup只会设置1个EventLoop。

> 注意：将Channel注册到EventLoopGroup具体分析看下面EventLoopGroup注册Channel部分。

### 触发bind出站事件并绑定端口

由上诉的bind()方法得知，此doBind0()以监听器形式监听到Channel注册Selector成功后回调。

`AbstractBootstrap#doBind0()`方法主要是绑定Channel的端口，然后会触发`ChannelPipeline#fireChannelActive()`回调以重置兴趣集。

```java
// AbstractBootstrap.java
private static void doBind0(
    final ChannelFuture regFuture, final Channel channel,
    final SocketAddress localAddress, final ChannelPromise promise) {

    channel.eventLoop().execute(()->channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE));
}
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {
    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.bind(localAddress, promise);// 传播出站事件bind
    }
}
```

这里的`AbstractChannel#bind(SocketAddress localAddress, ChannelPromise promise)`是一个pipeline出站事件，从`tail->head`，最终会调用HeadContext这个pipeline头节点里保存的`AbstractUnsafe#bind()`进行端口绑定：(关于bind事件在pipeline的传播，可以看后面专门的分析)

```java
// AbstractChannel.AbstractUnsafe.java
public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {  
    // 1.Channel 绑定端口
    doBind(localAddress);
		// 2.ChannelPipeline#fireChannelActive()回调
    if (isActive()) {
        invokeLater(()->pipeline.fireChannelActive());
    }
    safeSetSuccess(promise);
}
```

第1步`AbstractChannel#doBind()`方法进行端口绑定，抽象方法，在不同Channel实现类中调用不同的绑定方式，最终都是以JavaNio的Socket去绑定该端口地址。

#### 触发active入站事件

在上面AbstractChannel.AbstractUnsafe#bind()方法绑定完端口后，判断Channel是否活跃，绑定端口后一定是活跃的。

第2步回调该AbstractChannel绑定的`ChannelPipeline#fireChannelActive()`回调，这将触发该pipeline内所有的`ChannelInboundHandler#channelActive()`回调。

关于`pipeline#fireChannelActive出站事件`，它会在`HeadContext#channelActive()`方法中再触发`pipeline#read入站事件`，而`HeadContext#read()`方法会重置兴趣集。

在上面注册Channel时可知，注册时兴趣集设置为0，触发`read入站事件`才会重置兴趣集为监听连接或监听读取。

关于active入站事件的分析看EventLoop注册Channel部分的解析。

## ServerBootstrap

### 初始化Channel

在AbstractBootstrap#initAndRegister()中，创建Channel之后，将对其进行初始化，ServerBootstrap的init()方法实现如下：

在`ServerBootstrap#init()`中会往ChannelPipeline中添加`ChannelInitializer`并重写其`initChannel()`方法用于将`ServerBootstrapAcceptor`加入pipeline，它会将新连接注册到workerEventLoopGroup中。

```java
void init(Channel channel) {
    // 将配置是Option设置到Channel中
    setChannelOptions(channel, newOptionsArray(), logger);
    setAttributes(channel, newAttributesArray());

    ChannelPipeline p = channel.pipeline();

    final EventLoopGroup currentChildGroup = childGroup;
    final ChannelHandler currentChildHandler = childHandler;
    final Entry<ChannelOption<?>, Object>[] currentChildOptions = newOptionsArray(childOptions);
    final Entry<AttributeKey<?>, Object>[] currentChildAttrs = newAttributesArray(childAttrs);

    // 这里添加的ChannelInitializer将在Channel注册Selector完成后进行回调
    p.addLast(new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(final Channel ch) {
            final ChannelPipeline pipeline = ch.pipeline();
            // 将启动器配置的ChannelHandler加入到pipeline中
            ChannelHandler handler = config.handler();
            if (handler != null)
                pipeline.addLast(handler);
            
            // 同时再添加一个ServerBootstrapAcceptor的handler到pipeline中
            ch.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    pipeline.addLast(new ServerBootstrapAcceptor(
                        ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                }
            });
        }
    });
}
```

这个ServerBootstrapstrapAcceptor作为ChannelHandler放入Bossgroup的ChannelPipeline中，用于将监听到的新连接放入注册到workgroup中。

> 注意：这里使用ChannelInitializer方式将ServerBootstrapAcceptor加入到Pipeline中，在ServerSocketChannel注册到EventLoopGroup后将触发handlerAdded回调，此初始化器的handlerAdded()方法会调用重写的initChannel()方法，以将ServerBootstrapAcceptor这个handler放入ServerSocketChannel的pipeline中。

## Bootstrap

如何以Bootstrap创建客户端可以看官方案例。

### connect

从官方案例可知客户端的启动入口是`Bootstrap#connect()`，其核心流程如下图：

![connect](netty.assets/connect.png)



```java
public ChannelFuture connect(SocketAddress remoteAddress) {
    ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");
    validate();
    return doResolveAndConnect(remoteAddress, config.localAddress());
}

private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
    // 1.创建并初始化Channel，将其注册到EventLoopGroup
    final ChannelFuture regFuture = initAndRegister();
    final Channel channel = regFuture.channel();

    // 省略异步处理
    // 2.解析远程地址并进行连接
    doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
}
```

- 第1步调用`AbstractBootstrap#initAndRegister()`方法创建并初始化Channel，并将其注册到EventLoopGroup，分析见上文。
- 第2步就是这个客户端启动器线程将真正连接操作交给EventLoop绑定线程去连接到服务端。

第2步最终会来到这里：

```java
private static void doConnect(
    final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {
    final Channel channel = connectPromise.channel();
    // 包装为task，会发布pipeline#connect出站事件
    channel.eventLoop().execute(()->channel.connect(remoteAddress, connectPromise));
}
```

这里将连接操作包装为task放入EventLoop的taskQueue，由EventLoop绑定线程去发布connect出站事件。

### 初始化Channel

在上面的分析可知AbstractBootstrap#initAndRegister()方法创建完Channel后会初始化Channel，这个初始化方法由各个启动器重写，客户端启动器的实现如下：

仅仅将启动器配置的Channel配置设置到Channel中。

```java
// Bootstrap.java
void init(Channel channel) {
    ChannelPipeline p = channel.pipeline();
    p.addLast(config.handler());// 将启动器配置到ChannelHandler配置到pipeline中
	// 将启动器配置的Channel配置设置到Channel中
    setChannelOptions(channel, newOptionsArray(), logger);
    setAttributes(channel, newAttributesArray());
}
```

一般来说，用户在启动器中配置的handler是初始化器`ChannelInitializer`，用于添加自定义handler。

### 发布connect出站事件

在上面的connect分析处可知，客户端启动器线程将真正连接操作交给EventLoop绑定线程去连接到服务端。

```java
private static void doConnect(
    final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {
    final Channel channel = connectPromise.channel();
    // 包装为task，会发布pipeline#connect出站事件
    channel.eventLoop().execute(()->channel.connect(remoteAddress, connectPromise));
}
```

这里将连接操作包装为task放入EventLoop的taskQueue，由EventLoop绑定线程去发布connect出站事件。

connect出站事件默认仅有HeadContext重写：它会调用`AbstractNioUnsafe#connect()`：

```java
// DefaultChannelPipeline.HeadContext.java
public void connect(
    ChannelHandlerContext ctx,
    SocketAddress remoteAddress, SocketAddress localAddress,
    ChannelPromise promise) {
    unsafe.connect(remoteAddress, localAddress, promise);
}

// AbstractNioChannel.AbstractNioUnsafe.java
public final void connect(
    final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
    // 省略检查
    boolean wasActive=isActive()
    // 1.连接远程地址，最终调用SocketChannel#connect()方法连接服务端
    if (doConnect(remoteAddress, localAddress)) {
        // 2.这里面会发布ChannelPipeline#fireChannelActive()入站事件
        fulfillConnectPromise(promise, wasActive);
    }
    // 省略异常连接关闭
}
```

#### 发布active入站事件

上面第2步将发布`pipeline#fireChannelActive入站事件`：

```java
// AbstractNioChannel.AbstractNioUnsafe.java
private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
    // 省略
    boolean active = isActive();
    // 发布pipeline#fireChannelActive入站事件
    if (!wasActive && active) {// 连接成功这里必然是活跃的
        pipeline().fireChannelActive();
    }
    // 省略
}
```

`pipeline#fireChannelActive入站事件`在HeadContext处覆盖，其发布`pipeline#read出站事件`，而这个出站事件在HeadContext处又覆盖，其将重置兴趣集。一般客户端Channel的兴趣集为读事件`OP_READ`。

# EventLoop

Netty的EventLoop基于Reactor模型思想设计的，所以需要先理解Reactor模型。

EventLoop对应一个Reactor，EventLoopGroup是EventLoop分组，相当于一组Reactor。

对于Netty NIO客户端而言，一般仅创建1个EventLoopGroup，因为客户端启动器Bootstrap只能连接一个远程服务端，一般仅分配一个EventLoop，即一个Reactor。

Netty NIO服务端一般会创建2个EventLoopGroup：

- `bossGroup`：对应Reactor模型MainReactor，一般会传入参数`nThreads=1`，表示只使用1个EventLoop，即1个Reactor。
- `wokerGroup`：对应Reactor模型SubReactor，默认SubReactor个数和CPU核心数*2相等。

`bossGroup` 如果配置多个线程，是否可以使用**多个 mainReactor** 呢？一个 Netty NIO 服务端**同一时间**，只能 bind 一个端口，那么只能使用一个 Selector 处理客户端连接事件。又因为，Selector 操作是非线程安全的，所以无法在多个 EventLoop ( 多个线程 )中，同时操作。所以这样就导致，即使 `bossGroup` 配置多个线程，实际能够使用的也就是一个线程。

但是如果启动多个ServerBootstrap启动器并且每个都传入同一个bossGroup，此时多个ServerSocketChannel可以注册到不同的mainReactor，那么配置多个EventLoop是有意义的。

## NioEventLoopGroup

NioEventLoopGroup就是**管理着一组NioEventLoop(作用如Reactor线程池)**，当设置数量为1时可作为主Reactor，数量大于1时可作为从Reactor组。

![NioEventLoopGroup](netty.assets/NioEventLoopGroup.png)

先分析具有chooser和children属性的MultithreadEventExecutorGroup：

```java
// EventExecutorGroup实现的抽象基类，它同时使用多个线程处理其任务
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {
    private final EventExecutor[] children;// 类似Reactor组

    // 选择器：选择哪个EventExecutor，选择方式就是平均分配，即自增序列取余
    private final EventExecutorChooserFactory.EventExecutorChooser chooser;

    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, Object... args) {
        checkPositive(nThreads, "nThreads");
        // 1.创建执行器线程池
        if (executor == null) {
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }
        // 2.创建EventExecutor数组，默认是NioEventLoop
        children = new EventExecutor[nThreads];
        for (int i = 0; i < nThreads; i++) {
            boolean success = false;
            children[i] = newChild(executor, args);// 由子类实现
            success = true;
        }
        // 3.创建EventExecutor选择器
        chooser = chooserFactory.newChooser(children);
    }

    // 将Channel注册到此EventLoopGroup的某个EventLoop
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    // 由选择器选择下个EventExecutor，默认是平均选择
    public EventExecutor next() {
        return chooser.next();
    }
    // 创建新的EventExecutor，由子类实现，如NioEventLoopGroup就会创建NioEventLoop
    protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;
}
```

接下来分析NioEventLoopGroup，它主要提供了创建EventExecutor即Reactor的方法newChild()：

```java
// MultithreadEventLoopGroup实现，用于基于 NIO Selector的Channel
public class NioEventLoopGroup extends MultithreadEventLoopGroup {
    // 设置子事件循环中用于 I/O 所需时间量的百分比。
    // 默认值为50 ，这意味着事件循环将尝试在 I/O 上花费与非 I/O 任务相同的时间。
    public void setIoRatio(int ioRatio) {
        // 设置所有 EventLoop 的 IO 任务占用执行时间的比例，默认50，即I/O和非I/O各占50%
        for (EventExecutor e : this) {
            ((NioEventLoop) e).setIoRatio(ioRatio);
        }
    }

    // 用新创建的Selector替换子事件循环的当前Selector以解决臭名昭著的 epoll 100% CPU 错误
    public void rebuildSelectors() {
        // 因为 JDK 有 epoll 100% CPU Bug
        // 实际上，NioEventLoop 当触发该 Bug 时，也会自动调用 NioEventLoop#rebuildSelector() 方法，
        // 进行重建 Selector 对象，以修复该问题。
        for (EventExecutor e : this) {
            ((NioEventLoop) e).rebuildSelector();
        }
    }

    // 创建child EventLoop，即Reactor模型中的子/从Reactor
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        // 省略参数获取
        return new NioEventLoop(this, executor, selectorProvider,
           selectStrategyFactory.newSelectStrategy(),
           rejectedExecutionHandler, taskQueueFactory, tailTaskQueueFactory);
    }
}
```

### 注册Channel

在`NioEventLoopGroup#register(Channel channel)`方法中，注册流程大致如下：

![EventLoopGroup](netty.assets/EventLoopGroup#register.png)

```java
    // 将Channel注册到此EventLoopGroup的某个EventLoop
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    // 由选择器选择下个EventExecutor，默认是平均选择
    public EventExecutor next() {
        return chooser.next();
    }
```

EventLoopGroup选个某个EventLoop注册，调用`EventLoop#register()`进行注册。

> 注：EventLoop#register()具体分析看下面的NioEventLoop注册Channel部分。

1、一般boss NioEventLoopGroup只会设置一个EventLoop，一般仅注册1个NioServerSocketChannel，该Channel的pipeline上的ServerBootstrapAcceptor处理器会将监听到的新连接SocketChannel注册到worker group上。

2、一般worker NioEventLoopGroup会设置多个EventLoop，SocketChannel注册到group时，将根据EventExecutor选择器（默认是`GenericEventExecutorChooser`）选择下个EventLoop注册，`EventLoop#register()`会将SocketChannel注册到该EventLoop上的Selector。

### EventLoop选择器

EventExecutor选择器默认是`GenericEventExecutorChooser`：就是自增序列取余，平均分配到每个EventExecutor。

```java
private static final class GenericEventExecutorChooser implements EventExecutorChooser {
    private final AtomicLong idx = new AtomicLong();
    private final EventExecutor[] executors;
    GenericEventExecutorChooser(EventExecutor[] executors) {
        this.executors = executors;
    }
    @Override
    public EventExecutor next() {
        return executors[(int) Math.abs(idx.getAndIncrement() % executors.length)];
    }
}
```

## NioEventLoop

**NioEventLoop绑定单独I/O线程**，进行了无锁化设计，单个I/O线程进行串行的操作。这种**局部无锁化设计**相比于一个工作队列多个工作线程的竞争模型实际上性能更优。

当用户线程操作NioEventLoop的资源时，如手动注册Channel时，会将它们包装为任务调用NioEventLoop的execute(Runnable)提交到任务队列，由其自己绑定的I/O线程负责具体执行，**防止锁竞争，从而实现局部无锁化**。

![NioEventLoop](netty.assets/NioEventLoop.png)

NioEventLoop就是Reactor模型中的Reactor，保存有Selector和线程池。

```java
// SingleThreadEventLoop实现将Channel注册到Selector并且在事件循环中对这些进行多路复用
public final class NioEventLoop extends SingleThreadEventLoop {
    // 是否禁用SelectionKey的优化，默认开启
    private static final boolean DISABLE_KEY_SET_OPTIMIZATION =
        SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);
    // 少于该值，不开启空轮训重建新的Selector对象
    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    // Selector空轮询N次后，重建新的Selector对象，用以解决JDK NIO的epoll空轮询bug，默认512
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    // 经过优化后包装的Selector对象
    private Selector selector;
    private Selector unwrappedSelector;// 未包装的原始Selector对象
    // 注册的SelectedKey集合，Netty自己实现，经过优化
    private SelectedSelectionKeySet selectedKeys;

    // nextWakeupNanos is: 下次唤醒时间，因为Selector#wakeup()开销较大，尽量减少调用
    //    AWAKE            when EL is awake
    //    NONE             when EL is waiting with no wakeup scheduled
    //    other value T    when EL is waiting with wakeup scheduled at time T
    private final AtomicLong nextWakeupNanos = new AtomicLong(AWAKE);

    /**
     * 处理 Channel 的就绪的 IO 事件，占处理任务的总时间的比例
     * 有3中类型任务：
     * 1.Channel的就绪I/O事件
     * 2.普通任务
     * 3.定时任务
     * */
    private volatile int ioRatio = 50;
}
```

### 注册Channel

`EventLoopGroup#register()`会选择一个EventLoop注册Channel。

```java
// SingleThreadEventLoop.java
/**
 * 这里会调用{@link AbstractChannel.AbstractUnsafe#register(EventLoop, ChannelPromise)} 将Channel注册到该EventLoop的Selector上
 */
@Override
public ChannelFuture register(final ChannelPromise promise) {
    ObjectUtil.checkNotNull(promise, "promise");
    promise.channel().unsafe().register(this, promise);
    return promise;
}
```

调用`AbstractChannel.AbstractUnsafe#register(EventLoop, ChannelPromise)`：

```java
// AbstractChannel.AbstractUnsafe.java
@Override
public final void register(EventLoop eventLoop, final ChannelPromise promise) {
    // 省略
    // 1.将EventLoop绑定到此Channel
    AbstractChannel.this.eventLoop = eventLoop;
    // 2.将Channel注册到EventLoop的Selector上
    eventLoop.execute(()->register0(promise));
}

// AbstractChannel.AbstractUnsafe.java
private void register0(ChannelPromise promise) {
    // 1.执行注册逻辑
    // 将SelectableChannel注册到此EventLoop的Selector上，不过此时注册的兴趣集为0
    doRegister();

    // 2.执行可能的挂起的ChannelHandler#handlerAdded()回调任务
    // 比如ChannelInitializer就是在这回调初始化，此回调常用来添加ChannelHandler
    pipeline.invokeHandlerAddedIfNeeded();

    // 3.向管道传播入站事件pipeline#channelRegistered() 
    pipeline.fireChannelRegistered();
    // 4.可能触发pipeline.fireChannelActive入站事件
    if (isActive()) {
        pipeline.fireChannelActive();
    }
}
```

注册到EventLoop后有2个重要回调：

- `pipeline.invokeHandlerAddedIfNeeded()`：如ChannelInitializer初始化器这个ChannelHandler在这回调用于添加自定义处理器，并在完成后从pipeline中删除此初始化器。
- `pipeline.fireChannelRegistered()`：向通道中发布channelRegistered入站事件

这里第2步骤执行挂起的回调任务在后面的`ChannelPipeline#addLast()`方法添加ChannelHandler时会分析到。

#### 注册到Selector

register0()方法第一步是将Channel注册到此EventLoop的Selector上：

```java
// AbstractNioChannel.java
protected void doRegister() throws Exception {
    // 将SelectableChannel注册到Selector，不过此时的兴趣集为0?
    selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
}
```

这里为什么兴趣集设置0呢？因为这里是Channel注册到EventLoopGroup中，可能是ServerSocketChannel注册到boss EventLoopGroup中监听连接事件，也可能是SocketChannel注册到worker EventLoopGroup中监听读事件。

这里注册兴趣集为0**目的是获取`SelectionKey`保存起来**。因为`SelectionKey#interestOps(int ops)`方法可以随时修改兴趣集，所以此处将其保存到了AbstractNioChannel中。

要重置兴趣集，一般需要触发`pipeline#read()出站事件`，由`HeadContext#read()`方法进行兴趣集重置。注意到`HeadContext#channelActive()`和`HeadContext#ChannelReadComplete()`两个入站事件方法会触发`pipeline#read()`出站事件。

#### 添加handler回调

`AbstractChannel.AbstractUnsafe.register0()`方法第2步骤是执行可能的挂起的`ChannelHandler#handlerAdded()`回调任务，比如`ChannelInitializer`就是在这回调初始化，此回调常用来添加自定义ChannelHandler。

#### 触发registerd入站事件

`AbstractChannel.AbstractUnsafe.register0()`方法第3步骤是发布`pipeline#fireChannelRegisterd`入站事件，通知各个Handler此时Channel已经注册成功。

#### 触发active入站事件

上面register0()方法第4步会根据Channel的活跃状态判断是否触发active入站事件。

一般NioServerSocketChannel由启动器注册时还未绑定端口，不是活跃的，此时不会触发，服务端Channel的active事件只有在绑定端口时由bind出站事件触发。

如果是NioSocketChannel则必然是活跃的，一定触发active入站事件进而重置兴趣集以监听读事件。

第一站是HeadContext，将调用HeadContext#channelActive()：

```java
// DefaultChannelPipeline.HeadContext.java
@Override
public void channelActive(ChannelHandlerContext ctx) {
    ctx.fireChannelActive();// 先向后传播此入站事件, 目前基本无官方处理器覆盖此方法
    readIfIsAutoRead();
}

private void readIfIsAutoRead() {
    if (channel.config().isAutoRead()) {
        channel.read();
    }
}
```

AbstractChannel的读操作触发`ChannelPipeline#read()`，它是出站事件。

目前ChannelActive入站事件就HeadContext重写，并在这里触发read出站事件用于准备监听连接/读取数据操作。

##### 触发read出站事件以重置兴趣集

**read事件是出站事件，它的作用从代码来看就是重置兴趣集**，将兴趣集从0改回监听连接或监听读取事件。

```java
// DefaultChannlePipeline.java
@Override
public final ChannelPipeline read() {
    tail.read(); // 出站事件从TailContext开始
    return this;
}
```

read出站事件基本不会有覆盖，最后来到HeadContext#read()：

```java
// DefaultChannelPipeline.HeadContext.java   
@Override
public void read(ChannelHandlerContext ctx) {
    unsafe.beginRead();
}
```

这最终会调用AbstractNioChannel#doBeginRead()：

```java
// AbstractNioChannel.java  
@Override
protected void doBeginRead() throws Exception {
    // Channel.read() or ChannelHandlerContext.read() was called
    final SelectionKey selectionKey = this.selectionKey;
    if (!selectionKey.isValid()) {
        return;
    }

    readPending = true;

    final int interestOps = selectionKey.interestOps();
    if ((interestOps & readInterestOp) == 0) {
        selectionKey.interestOps(interestOps | readInterestOp);
    }
}
```

这个readInterestOp属性在创建Channel时根据是服务端还是客户端传入不同的兴趣集，如下图：

![NioServerSocketChannel](netty.assets/NioServerSocketChannel.png)



### openSelector

NioEventLoop持有selector，所以其构造方法会调用openSelector()来打开复用器：

```java
private SelectorTuple openSelector() {
    // 1.创建JavaNIO的Selector
    final Selector unwrappedSelector;
    unwrappedSelector = provider.openSelector();

    if (DISABLE_KEY_SET_OPTIMIZATION) // 如果禁用优化则直接返回，默认开始优化
        return new SelectorTuple(unwrappedSelector);
    
    // 2.获得SelectorImpl类
    Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {/*省略*/});
	// 省略获取失败的异常处理

    final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;
    // 3.创建 SelectedSelectionKeySet 对象，这是 Netty 对 Selector 的 selectionKeys 的优化
    final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();
    
    // 4.设置 SelectedSelectionKeySet 对象到 unwrappedSelector中，替换其以HashSet实现的selectedKey
    Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
		// 省略
    });
	// 省略异常处理
    
    // 5.创建 SelectedSelectionKeySetSelector 对象，这是 Netty 对 Selector 的优化实现类
    return new SelectorTuple(unwrappedSelector,
                             new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
}
```

从上面的代码知道，Netty以`SelectedSelectionKeySetSelector`包装了Selector，将其selectedKey()方法返回的HashSet替换为了`SelectedSelectionKeySet`，为什么要替换呢？

这就得从`SelectedSelectionKeySet`和`SelectedSelectionKeySetSelector`来分析了。

#### 优化的SelectedKeySet

Netty以`SelectedSelectionKeySetSelector`作为JavaNIO Selector的包装类，并以`SelectedSelectionKeySet`替换了原本Selector容纳选中的selectedKey的集合HashSet，使得其处理发生IO事件的selectionKeys的平均添加/删除时间复杂度从O(logN)下降至O(1)。

```java
/**
 * 这是Netty对SelectorImpl的selectedKeys的小优化，相比于其HashSet的实现，此实现事件复杂度从 O(logn) 降低到 O(1)
 * 因为 #remove(Object o)、#contains(Object o) 不会使用到，索性不进行实现
 */
final class SelectedSelectionKeySet extends AbstractSet<SelectionKey> {
    SelectionKey[] keys; // 以数组容纳发生IO事件的SelectionKey
    int size;// 数组可读大小

    SelectedSelectionKeySet() {
        keys = new SelectionKey[1024];// 默认1024大小数组
    }

    @Override
    public boolean add(SelectionKey o) {
        if (o == null) 
            return false;
        if (size == keys.length) 
            increaseCapacity();// 2倍扩容
        keys[size++] = o;
        return true;
    }

    // 每次读取使用完数据，调用该方法，进行重置。
    void reset() {
        reset(0);
    }

    void reset(int start) {
        Arrays.fill(keys, start, size, null);
        size = 0;
    }

    private void increaseCapacity() {
        SelectionKey[] newKeys = new SelectionKey[keys.length << 1];
        System.arraycopy(keys, 0, newKeys, 0, size);
        keys = newKeys;
    }
}
```

这个类以数组容纳新发生IO事件的SelectionKey，而原本Selector的以HashSet作为容器。

这样在Selector检测到新IO事件时添加操作为O(1)，而原本的HashSet为O(logN)。

在我们自己写网络编程时，处理发生IO事件的SelectionKey集合时，会获取其迭代器并每处理一个就移除一个，每次移除的时间复杂度为O(log N)，总得时间复杂度为O(NlogN)，而这个Netty简单优化的容器类则不会移除，而是直接`reset()`方法清空数组就行了，时间复杂度为O(1)。

那么何时清空该集合呢？`SelectedSelectionKeySetSelector#select()`包装类会先清空集合，再调用包装的`selector#select()`。

#### 优化的Selector

Netty以`SelectedSelectionKeySetSelector`作为JavaNIO Selector的包装类，并以`SelectedSelectionKeySet`替换了原本Selector容纳选中的selectedKey的集合HashSet，使得其处理发生IO事件的selectionKeys的平均添加/删除时间复杂度从O(logN)下降至O(1)。

```java
// 基于 Netty SelectedSelectionKeySet 作为 selectionKeys 的 Selector 实现类
// 其每次select()调用前都会调用 SelectedSelectionKeySet#reset() 方法，重置 selectionKeys
final class SelectedSelectionKeySetSelector extends Selector {
    private final SelectedSelectionKeySet selectionKeys;
    private final Selector delegate;// 包装的原始Selector

    SelectedSelectionKeySetSelector(Selector delegate, SelectedSelectionKeySet selectionKeys) {
        this.delegate = delegate;
        this.selectionKeys = selectionKeys;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return delegate.selectedKeys();
    }

    @Override
    public int select(long timeout) throws IOException {
        selectionKeys.reset();
        return delegate.select(timeout);
    }

    @Override
    public Selector wakeup() {
        return delegate.wakeup();
    }
}
```

上面代码只列出了一些关键方法。

### 专属线程运行run

如下图所示，每个EventLoop所绑定线程都会运行如下3个阶段，有任务时会优先将任务和IO处理完成后，再进行select监听。

- select：select监听是运行`Selector#select(timeout)`方法查询是否有到达事件，有则放入优化后的SelectedSelectionKeySet。

- process selected keys：处理SelectionKey，服务端NioServerSocketChannel会调用`accept()`接受新连接，然后将连接包装为task扔到taskQueue。客户端连接SocketChannel则会处理读事件，将读取内容包装为task扔到taskQueue。这两个任务都是触发`pipeline#fireChannelRead入站事件`。
- runTasks：处理普通任务和定时任务。比如上面放进来的channelRead入站事件任务，服务端pipeline中默认有ServerBootstrapAcceptor处理器，它会把新连接注册到worker group。客户端pipeline一般是自定义处理器。

运行IO时间占用总时间由参数`ioRatio`控制，默认50%，即处理selected keys的时间和run tasks的时间控制为一样的。

select操作不计入耗时，因为在有任务或有待处理selected keys时不会运行select监听。

![NioEventLoop](netty.assets/NioEventLoop#run.png)

每个NioEventLoopGroup创建的NioEventLoop都会**创建并绑定专属自己的一个线程运行run()方法**：

```java
protected void run() {
    int selectCnt = 0;
    for (;;) {
        // 省略最外层try/catch/finally
        int strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
        switch (strategy) { // 省略其它情况
            case SelectStrategy.SELECT:
                // 1.监听到下个定时任务达到时间，没有则-1
                long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
                if (curDeadlineNanos == -1L) 
                    curDeadlineNanos = NONE; // nothing on the calendar
                nextWakeupNanos.set(curDeadlineNanos);

                // 2.所有任务都处理完了再进行IO事件监听
                if (!hasTasks()) strategy = select(curDeadlineNanos);
            default:
        }

        selectCnt++;
        cancelledKeys = 0;
        needsToSelectAgain = false;
        final int ioRatio = this.ioRatio;
        boolean ranTasks;
        // 3.根据设置的IO处理时间占比，运行IO处理和普通任务处理，默认50，即各占一半
        if (ioRatio == 100) {
            // 3.1 处理检测到的IO事件
            if (strategy > 0) processSelectedKeys();
            // 3.2 运行所有普通任务和定时任务，不限制时间
            ranTasks = runAllTasks();
        } else if (strategy > 0) {
            final long ioStartTime = System.nanoTime();
            // 4.处理IO事件
            processSelectedKeys();
            // 5.按设置的IO运行时间比例限制运行普通任何和定时任务的时间
            final long ioTime = System.nanoTime() - ioStartTime;
            ranTasks = runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
        } else ranTasks = runAllTasks(0); // This will run the minimum number of tasks

        if (ranTasks || strategy > 0) {
            selectCnt = 0;
        }
        // 用新创建的Selector替换此事件循环的当前Selector以解决臭名昭著的 epoll 100% CPU 错误。
        else if (unexpectedSelectorWakeup(selectCnt)) { // Unexpected wakeup (unusual case)
            selectCnt = 0;
        }
    }
}
```

#### 执行3个任务队列

在上面的`NioEventLoop#run()`方法中，默认配置下处理IO的时间和运行普通任务的时间是相同的，在处理完检测到的IO事件后，将花费相同时间去执行任务队列taskQueue中的任务：

```java
// 从任务队列中轮询所有任务并通过Runnable.run()方法运行它们。此方法停止运行任务队列中的任务，如果运行时间超过timeoutNanos则返回
protected boolean runAllTasks(long timeoutNanos) {
    // 1.将定时任务队列scheduledTaskQueue中已经到达时间的任务取出并放入taskQueue中等待执行
    fetchFromScheduledTaskQueue();
    // 2.从taskQueue中取出1个任务
    Runnable task = pollTask();
    if (task == null) {
        afterRunningAllTasks();
        return false;
    }

    final long deadline = timeoutNanos > 0 ? getCurrentTimeNanos() + timeoutNanos : 0;
    long runTasks = 0;
    long lastExecutionTime;
    for (;;) {
        // 3.安全的执行任务，即出现异常仅日志提醒
        safeExecute(task);// 调用Runnable#run()
        runTasks ++;

        // 4.每64个任务检查1次超时，因为nanoTime()调用比较昂贵
        // 目前是硬编码，如果它真会出现问题的话会考虑将其设为可配置
        if ((runTasks & 0x3F) == 0) {
            lastExecutionTime = getCurrentTimeNanos();
            if (lastExecutionTime >= deadline) {// 运行时间超过限制则跳出
                break;
            }
        }

        task = pollTask();
        if (task == null) {
            lastExecutionTime = getCurrentTimeNanos();
            break;
        }
    }
    // 5.任务执行时间完成回调
    // 目前的实现是执行tailQueue中所有任务
    afterRunningAllTasks();
    this.lastExecutionTime = lastExecutionTime;
    return true;
}
```

> 注意：这里发现一个小优化，因为`System.nanoTime()`调用相对昂贵，因此每执行64个任务才进行1次超时检查。Netty真是优化到极致了。

这里出现了3个任务队列：`scheduledTaskQueue`、`taskQueue`、`tailQueue`。

将定时任务队列中到达的任务放入taskQueue末尾待执行，执行taskQueue中的任务，到达时间限制则执行tailQueue中所有任务。

到底什么样的任务适合放入到tailQueue中呢？观察如下自定义批量刷新的Handler：

```java
public class BatchFlushHandler extends ChannelOutboundHandlerAdapter {
	// 组合ByteBuf
    private CompositeByteBuf compositeByteBuf;
    /**
    * 是否使用 CompositeByteBuf 对象，用于数据写入
    **/
    private boolean preferComposite;

    private SingleThreadEventLoop eventLoop;

    private Channel.Unsafe unsafe;

   /**
    * 是否添加任务到 tailTaskQueue 队列中
    */
    private boolean hasAddTailTask = false;

    public BatchFlushHandler() {
        this(true);
    }

    public BatchFlushHandler(boolean preferComposite) {
        this.preferComposite = preferComposite;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // 初始化 CompositeByteBuf 对象，如果开启 preferComposite 功能
        if (preferComposite) {
            compositeByteBuf = ctx.alloc().compositeBuffer();
        }
        eventLoop = (SingleThreadEventLoop) ctx.executor();
        unsafe = ctx.channel().unsafe();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        // 写入到 CompositeByteBuf 对象中
        if (preferComposite) {
            compositeByteBuf.addComponent(true, (ByteBuf) msg);
            // 普通写入
        } else {
            ctx.write(msg);
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        // 通过 hasAddTailTask 有且仅有每个 EventLoop 执行循环( run )，只添加一次任务
        if (!hasAddTailTask) {
            hasAddTailTask = true;

            // 【重点】添加最终批量提交( flush )的任务
            // 【重点】添加最终批量提交( flush )的任务
            // 【重点】添加最终批量提交( flush )的任务
            eventLoop.executeAfterEventLoopIteration(() -> {
                if (preferComposite) {
                    ctx.writeAndFlush(compositeByteBuf).addListener(future -> compositeByteBuf = ctx.alloc().compositeBuffer());
                } else {
                    unsafe.flush();
                }

                // 重置 hasAddTailTask ，从而实现下个 EventLoop 执行循环( run )，可以再添加一次任务
                hasAddTailTask = false;
            });
        }
    }
}
```

作用：如此能减少 `pipeline` 的执行次数，同时提升吞吐量。这个模式在低并发场景，并没有什么优势，而在高并发场景下对提升吞吐量有不小的性能提升。

这样做会有好处呢？在 [《蚂蚁通信框架实践》](https://mp.weixin.qq.com/s/JRsbK1Un2av9GKmJ8DK7IQ) 的 **批量解包与批量提交**章节有相关分享。

#### 处理IO事件

在run()方法调用`NioEventLoop#selector#select()`检测到新IO事件的SelectionKey后，就会调用`processSelectedKeys()`方法处理IO事件：

```java
private void processSelectedKeys() {
    // 当 selectedKeys 非空，意味着使用优化的 SelectedSelectionKeySetSelector
    if (selectedKeys != null) processSelectedKeysOptimized();// 默认会优化调用这个
    else processSelectedKeysPlain(selector.selectedKeys());
}

private void processSelectedKeysOptimized() {
    // 优化后以数组存selectedKey，比HashSet迭代器方便多了
    for (int i = 0; i < selectedKeys.size; ++i) {
        final SelectionKey k = selectedKeys.keys[i];
        // 优化后的兴趣集合以数组形式存储SelectionKey，直接置null即可移除，效率高达O(1)
        selectedKeys.keys[i] = null;

        final Object a = k.attachment();

        // 1.处理Channel的就绪IO事件
        if (a instanceof AbstractNioChannel) 
            processSelectedKey(k, (AbstractNioChannel) a);
        // 2.如果是用户自己注册的Channel，则交由用户程序NioTask处理就绪的 IO 事件
        else {
            NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
            processSelectedKey(k, task);
        }
        // 省略
    }
}
```

这里以优化后的SeletedKeySet集合处理选中key时就无须原来那种迭代器访问并移除了。

对于选中的每个key，判断其`SelectionKey#attachment()`粘贴：

- `AbstractNioChannel`：说明是Netty创建的Channel，则以`NioEventLoop#processSelectedKey()`处理该IO事件，这将涉及pipeline的处理。
- `NioTask`：说明是用户手动创建的Channel，则以将回调`NioTask#channelReady()`自定义处理程序。

### 处理selectedKey

在上面分析的`NioEventLoop#processSelectedKeys()`处理选中的key集合，对每个选中key都会先判断其`SelectionKey#attachment()`是否为用户自定义`NioTask`，默认是Netty的`AbstractNioChannel`则调用`NioEventLoop#processSelectedKey()`处理该Channel的IO事件：

```java
private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
    // 1.如果SelectionKey失效了则关闭Channel
    if (!k.isValid()) { /*省略*/ }

    // 2.获取就绪集操作
    int readyOps = k.readyOps();
    // 3.OP_CONNECT 事件就绪，需要先完成连接建立
    if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
        int ops = k.interestOps();
        ops &= ~SelectionKey.OP_CONNECT;
        k.interestOps(ops);// 修改兴趣集取消对OP_CONNECT事件关注，避免重复触发
        unsafe.finishConnect();
    }

    // 4.OP_WRITE事件就绪
    if ((readyOps & SelectionKey.OP_WRITE) != 0)
        unsafe.forceFlush();// 将缓冲的数据写入Channel

    // 5.SelectionKey.OP_READ 或 SelectionKey.OP_ACCEPT 就绪
    // readyOps == 0 是对 JDK Bug 的处理，防止空的死循环
    if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0)
        unsafe.read();
}
```

从第5步可知，unsafe.read()会处理选中的SelectionKey.OP_ACCEPT或SelectionKey.OP_READ事件，其根据该EventLoop中注册的Channel类型的不同有不同Unsafe实现，则有不同的处理逻辑：

- 服务端即`NioServerSocketChannel`的`NioMessageUnsafe#read()`处理accept事件，封装JavaNIO的SocketChannel为Netty的NioSocketChannel并注册到某个wokerEventLoop中。
- 客户端或服务端创建的`NioSocketChannel`的`NioByteUnsafe#read()`处理read事件，并发布pipeline#fireChannelRead()事件。

关于其read()的具体实现解析在后面的Channel分析部分进行解析。

### 自定义NioTask处理selectedKey

在上面分析的`NioEventLoop#processSelectedKeys()`处理选中的key集合，对每个选中key都会先判断其`SelectionKey#attachment()`是否为用户自定义`NioTask`，如果是则调用用户自定义程序处理IO事件。

这里的NioTask接口在Netty中并没有相应的实现类，是留给用户进行自定义扩展处理IO事件的扩展接口：

```java
/**
 * 当SelectableChannel准备好时， NioEventLoop可以执行的任意任务
 * @see NioEventLoop#register(SelectableChannel, int, NioTask)
 */
public interface NioTask<C extends SelectableChannel> {
    // 当Selector选中该SelectableChannel时调用，用户可自定义Channel的IO事件处理
    void channelReady(C ch, SelectionKey key) throws Exception;

    /**
     * 指定SelectableChannel的SelectionKey被取消时调用
     * @param cause 注销的原因。如果用户调用了SelectionKey.cancel()或事件循环已关闭，则为null
     */
    void channelUnregistered(C ch, Throwable cause) throws Exception;
}
```

那么这个NioTask接口该如何使用呢？即如何将NioTask绑定到指定的SelectableChannel上呢？

官方案例：

```java
public void testSelectableChannel() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    NioEventLoop loop = (NioEventLoop) group.next();

    try {
        Channel channel = new NioServerSocketChannel();
        loop.register(channel).syncUninterruptibly();
        channel.bind(new InetSocketAddress(0)).syncUninterruptibly();

        SocketChannel selectableChannel = SocketChannel.open();
        selectableChannel.configureBlocking(false);
        selectableChannel.connect(channel.localAddress());

        final CountDownLatch latch = new CountDownLatch(1);

        loop.register(selectableChannel, SelectionKey.OP_CONNECT, new NioTask<SocketChannel>() {
            @Override
            public void channelReady(SocketChannel ch, SelectionKey key) {
                latch.countDown();
            }

            @Override
            public void channelUnregistered(SocketChannel ch, Throwable cause) {
            }
        });

        latch.await();

        selectableChannel.close();
        channel.close().syncUninterruptibly();
    } finally {
        group.shutdownGracefully();
    }
}
```

从这个案例得知，需要手动创建SelectableChannel并将其注册到NioEventLoop的Selector中。

> 从此案例可知，用户可以自己创建Channel，并自定义处理程序`NioTask`以`attachment()`方法粘贴到Channel上，将Channel注册到Netty提供的`NioEventLoop`的`Selector`中，享受Netty`主从Reactor模型`带来的高性能。

#### 注册

从上面案例得知，用户创建的Channel可调用`NioEventLoop#register()`将Channel进行注册：

```java
// 将不由 Netty 创建的SelectableChannel注册到此EventLoop的Selector 
// 当SelectableChannel准备好时，该事件循环将执行指定的NioTask
public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task) {
    // 省略参数检查

    if (inEventLoop()) {
        register0(ch, interestOps, task);
    } else {
        // 省略丢进线程池处理
    }
}
// 注册方式就是将NioTask作为粘贴，注册到该EventLoop的非包装的原始Selector中
private void register0(SelectableChannel ch, int interestOps, NioTask<?> task) {
    ch.register(unwrappedSelector, interestOps, task);
}
```

#### 处理IO事件

在上面分析的`NioEventLoop#processSelectedKeys()`处理选中的key集合，对每个选中key都会先判断其`SelectionKey#attachment()`是否为用户自定义`NioTask`，如果是则调用用户自定义程序处理IO事件。

```java
// NioEventLoop.java    
private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
    int state = 0;
    try {
        // 1.直接调用用户自定义处理程序捏
        task.channelReady(k.channel(), k);
        state = 1;
    } catch (Exception e) {
        k.cancel();// 2.出现异常则将其从EventLoop的Selector取消注册
        invokeChannelUnregistered(task, k, e);// 取消注册回调
        state = 2;
    } finally {
        switch (state) {
            case 0:
                k.cancel();
                invokeChannelUnregistered(task, k, null);
                break;
            case 1:
                if (!k.isValid()) { // Cancelled by channelReady()
                    invokeChannelUnregistered(task, k, null);
                }
                break;
            default:
                break;
        }
    }
}
```

> 注意：从这里的处理程序来看，当自定义处理逻辑出现了异常，Netty并不会帮我们关闭Channel，因此如有必要，可以在`NioTask#channelUnregistered()`回调方法中手动关闭Channel捏。

## 最佳实践

Netty的线程模型实际上取决于用户的启动BootStrap参数配置，通过设置不同的参数可以支持Reactor单线程模型、Reactor多线程模型、主从Reactor多线程模型。

Netty的NioEventLoop内有任务队列和定时任务队列，可以提交普通任务和定时任务，但是毕竟是单I/O线程执行，不适合处理复杂计算任务。

业务复杂，如存在阻塞线程的磁盘操作、数据库操作、网络操作等，**最好将解码后的消息封装为Task交给业务线程池处理**，NIO线程仅负责I/O处理，以保证NIO线程尽快释放处理其它IO操作。

**最佳实践：bossGroup+workerGroup+业务线程池**

**关于EventLoopGroup的EventLoop数量和业务线程池的参数设置**，只能根据用户场景进行测试，性能测试选出相对合理范围。

# Channel

`io.netty.channel.Channel`是 Netty 网络操作抽象类，它除了包括基本的 I/O 操作，如 bind、connect、read、write 之外，还包括了 Netty 框架相关的一些功能，如获取该 Channel 的 EventLoop。

在传统的网络编程中，作为核心类的 Socket ，它对程序员来说并不是那么友好，直接使用其成本还是稍微高了点。而 Netty 的 Channel 则提供的一系列的 API ，它大大降低了直接与 Socket 进行操作的复杂性。而相对于原生 NIO 的 Channel，Netty 的 Channel 具有如下优势( 摘自《Netty权威指南( 第二版 )》) ：

- 在 Channel 接口层，采用 Facade 模式进行统一封装，将网络 I/O 操作、网络 I/O 相关联的其他操作封装起来，统一对外提供。
- Channel 接口的定义尽量大而全，**为 SocketChannel 和 ServerSocketChannel 提供统一的视图**，由不同子类实现不同的功能，公共功能在抽象父类中实现，最大程度地实现功能和接口的重用。
- 具体实现采用聚合而非包含的方式，将相关的功能类聚合在 Channel 中，由 Channel 统一负责和调度，功能实现更加灵活。

![NioSocketChannel](netty.assets/NioServerSocketChannel.png)

oio是old io，即bio阻塞io。

![Channel体系](netty.assets/Channel体系.png)

## Unsafe

Unsafe接口定义在`io.netty.channel.Channel`内部，和Channel的操作紧密结合，是Channel的辅助接口。

Unsafe接口不应在用户代码中调用unsafe方法。

![NioSocketChannelUnsafe](netty.assets/NioSocketChannelUnsafe.png)

**Unsafe和Channel的关系：**

- EventLoop检测到一些入站事件如accept和read，交由该Channel关联的Unsafe处理并**发布入站pipeline事件**。

- Channel实现了ChannelOutboundInvoker接口，其实现了各个出站方法如write()，Channel出站方法仅仅负责发布`pipeline#write()`这类出站事件，最后由Pipeline内的HeadContext覆盖出站方法，调用Channel关联的Unsafe处理具体的逻辑。

即Unsafe通过与Channel关联的pipeline交互，并通过ByteBuf等缓冲，实现了Netty Channel的各个具体处理逻辑，并发布入站pipeline事件。
注意其实真正和JavaNio的SocketChannel交互部分还是有Netty Channel去实现，并命名为doWrite()这种方法名字。

## accept处理

accept处理由bossEventLoop中注册的NioServerSocketChannel触发。

在前面分析EventLoop轮询到IO事件并处理`NioEventLoop#processSelectedKey()`部分第5步：

```java
private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
    // 1.如果SelectionKey失效了则关闭Channel
    // 省略

    // 省略try/catch
    // 2.获取就绪集操作
    int readyOps = k.readyOps();
    // 3.OP_CONNECT 事件就绪，需要先完成连接建立
    // 4.OP_WRITE事件就绪
    // 省略
    
    // 5.SelectionKey.OP_READ 或 SelectionKey.OP_ACCEPT 就绪
    // readyOps == 0 是对 JDK Bug 的处理，防止空的死循环
    if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0)
        unsafe.read();
}
```

unsafe.read()会处理选中的SelectionKey.OP_ACCEPT或SelectionKey.OP_READ事件，其根据该EventLoop中注册的Channel类型的不同有不同Unsafe实现，则有不同的处理逻辑：

- 服务端即`NioServerSocketChannel`的`NioMessageUnsafe#read()`处理accept事件，封装JavaNIO的SocketChannel为Netty的NioSocketChannel并注册到某个wokerEventLoop中。
- 客户端或服务端创建的`NioSocketChannel`的`NioByteUnsafe#read()`处理read事件，并发布`pipeline#fireChannelRead()`事件。

### 接受新连接

服务端bossEventLoop轮询到新连接事件，调用`NioMessageUnsafe#read()`，新建JavaNio的SocketChannel并封装为Netty的NioSocketChannel对象，选择某个workerEventLoop注册并监听读事件：

```java
private final class NioMessageUnsafe extends AbstractNioUnsafe {
    // 保存新连接
    private final List<Object> readBuf = new ArrayList<Object>();

    // NioServerSocketChannel的NioMessageUnsafe#read()用于接受新连接
    @Override
    public void read() {
     	// 省略
        do {
            // 1.通过ServerSocketChannel#accept()获取新连接
            // 并封装为NioSocketChannel后保存到readBuf中
            int localRead = doReadMessages(readBuf);// 默认只读1个新连接
            if (localRead == 0) // 没有新连接
                break;
            if (localRead < 0) {// 读取出错则标记关闭
                closed = true;
                break;
            }
            // 读取连接数量 + localRead
            allocHandle.incMessagesRead(localRead);
        } while (continueReading(allocHandle));

        // 2.处理每个新连接
        int size = readBuf.size();
        for (int i = 0; i < size; i++) {
            readPending = false;
            // 3.发布服务端管道事件pipeline#fireChannelRead()
            // 服务端管道中的ServerBootstrapAcceptor处理器，将创建的NioSocketChannel 注册到 workerEventLoop 上
            pipeline.fireChannelRead(readBuf.get(i));
        }
        readBuf.clear();
        allocHandle.readComplete();
        // 4.发布pipeline#fireChannelReadComplete()事件
        pipeline.fireChannelReadComplete();
    }
}
```

这里通过`pipeline#fireChannelRead()`将新连接NioSocketChannel发布到管道中，入站事件，流向为`head-->tail`。

在前面分析的服务端启动器初始化Channel的过程中会往pipeline中添加`ServerBootstrapAcceptor`处理器，它的作用就是将新连接注册到workerEventLoop中。

### 触发channelRead入站事件

服务端Channel触发channelRead入站事件时，应该是仅有ServerBootstrapAcceptor有覆盖此事件。

#### 注册新连接到workerEventLoop

服务端Channel对应的pipeline中ServerBootstrapAcceptor处理器在channelRead()回调事件中将新连接NioSocketChannel注册到选出的某个EventLoop的Selector上。

```java
private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

    private final EventLoopGroup childGroup;// 即workerGroup
    private final ChannelHandler childHandler;// 保存用户指定的handler，一般是ChannelInitializer

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 1.取出新连接NioSocketChannel
        final Channel child = (Channel) msg;

        // 2.将用户配置的childHandler放入管道，一般是ChannelInitializer，用于用户添加一些ChannelHandler
        child.pipeline().addLast(childHandler);
        // 3.配置Channel
        setChannelOptions(child, childOptions, logger);
        setAttributes(child, childAttrs);

        // 4.将此新连接NioSocketChannel注册到workerEventLoopGroup中
        childGroup.register(child).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    forceClose(child, future.cause());
                }
            }
        });
    }
}
```

注册到workerEventLoop是`EventLoopGroup#register(Channel)`方法，在前面分析NioEventLoopGroup部分分析过此方法会选择下一个EventLoop，并将Channel注册到其持有的Selector上。

### 触发readComplete入站事件

NIOServerSocketChannel的pipeline就HeadContext这个handler有覆盖，作用是触发read出站事件以重置兴趣集。

不过服务端Channel在接受新连接之前并不会像客户端SocketChannel那样将兴趣集设置为0，因此这里并无实际作用。

## read处理

在前面分析EventLoop轮询到IO事件并处理`NioEventLoop#processSelectedKey()`部分可知，客户端或服务端建立的NioSocketChannel注册的EventLoop监听到其读事件后，将调用`NioByteUnsafe#read()`处理读事件，并发布`pipeline#fireChannelRead()`事件。

```java
protected class NioByteUnsafe extends AbstractNioUnsafe {
    // NioSocketChannel 处理读I/O事件
    public final void read() {
        // 1.获得缓冲区分配器和预测器
        // 1.1 获取ByteBuf分配器
        final ByteBufAllocator allocator = config.getAllocator();// 默认是PooledByteBufferAllocator
        // 默认是AdaptiveRecvByteBufAllocator: 动态接收缓冲区分配器
        // 1.2 从unsafe获取绑定的 缓冲区预测处理器，第1次调用没有则新建
        final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
        allocHandle.reset(config);

        ByteBuf byteBuf = null;
        boolean close = false;// 是否关闭连接

        do {
            // 2.缓冲区预测处理器确定此次接收缓冲区ByteBuf
            byteBuf = allocHandle.allocate(allocator);
            // 3.从SocketChannel读取数据到ByteBuf并设置此次读取字节数
            // 若此次读取填满缓冲区会调用record方法扩容
            allocHandle.lastBytesRead(doReadBytes(byteBuf));
            // 3.1 没读取到数据说明读完了或对端关闭连接
            if (allocHandle.lastBytesRead() <= 0) {
                // 3.2 释放缓存区，即放回PooledByteBufferAllocator
                byteBuf.release();
                byteBuf = null;
                // 3.3 如果读取字节数为-1，说明对端已关闭连接，收到了EOF
                close = allocHandle.lastBytesRead() < 0;
                if (close) {
                    // There is nothing left to read as we received an EOF.
                    readPending = false;
                }
                break;
            }
            // 读取消息数量 + localRead
            allocHandle.incMessagesRead(1);
            readPending = false;
            // 4.发布pipeline#fireChannelRead(byteBuf)事件
            pipeline.fireChannelRead(byteBuf);
            byteBuf = null;
        } while (allocHandle.continueReading());// 此次读取填满缓冲区则继续循环读取

        // 5.缓冲区预测处理器 readComplete回调，
        // 调用record方法以此次IO事件读取的所有字节数来调整预测缓冲区容量
        allocHandle.readComplete();
        // 6.发布pipeline#fireChannelReadComplete()
        pipeline.fireChannelReadComplete();
        // 7.如果读取字节数为-1，说明对端已关闭连接，此时关闭Channel
        if (close) { closeOnRead(pipeline); }
    }
}
```

对于NioSocketChannel的读事件，将数据读入ByteBuf后发布`pipeline#fireChannelRead(byteBuf)`事件，将byteBuf交由各个ChannelHandler进行处理，一般情况下都会设置一些编解码器，**半包处理器**，最后设置业务处理器。

这里需要注意这个动态接受缓冲区分配器/预测器，它的解析看下面。

### 动态接收缓冲区分配器

read()方法第一步是获取动态接收缓冲区分配器，默认是`AdaptiveRecvByteBufAllocator`

```java
/**
 * 动态接收缓冲区分配器会自动增加和减少反馈的预测缓冲区大小。
 * 如果前一次读取完全填满分配的缓冲区，它会逐渐增加预期分配缓冲区大小。
 * 如果读取操作无法连续两次填充满已分配缓冲区，它会逐渐减少预期分配缓冲区大小
 */
public class AdaptiveRecvByteBufAllocator extends DefaultMaxMessagesRecvByteBufAllocator {
    static final int DEFAULT_MINIMUM = 64;// 最小缓冲区长度
    // Use an initial value that is bigger than the common MTU of 1500
    static final int DEFAULT_INITIAL = 2048;// 默认缓冲区初始容量, 稍大于MTU 1500B
    static final int DEFAULT_MAXIMUM = 65536;// 默认缓冲区最大容量

    // 动态调整容量的步进索引参数
    private static final int INDEX_INCREMENT = 4;// 扩张的步进索引为4
    private static final int INDEX_DECREMENT = 1;// 收缩的步进索引为1

    private static final int[] SIZE_TABLE;// 长度向量表，向量数组每个值对应1个ByteBuf容量

    static {
        // 初始化向量表
        List<Integer> sizeTable = new ArrayList<Integer>();
        // 小容量每间隔16一个容量
        for (int i = 16; i < 512; i += 16) {
            sizeTable.add(i);
        }

        // 大容量说明解码的消息码流较大，采用2倍扩张以减少动态扩容频率
        for (int i = 512; i > 0; i <<= 1) { // lgtm[java/constant-comparison]
            sizeTable.add(i);
        }

        SIZE_TABLE = new int[sizeTable.size()];
        for (int i = 0; i < SIZE_TABLE.length; i++) {
            SIZE_TABLE[i] = sizeTable.get(i);
        }
    }

    // 二分查找大于size的第一个向量表索引
    private static int getSizeTableIndex(final int size) {
        // 二分查找实现，有手就行!
    }

    // 缓冲区预测处理器
    public Handle newHandle() {
        return new HandleImpl(minIndex, maxIndex, initial);
    }

    @Override
    public AdaptiveRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData) {
        super.respectMaybeMoreData(respectMaybeMoreData);
        return this;
    }
}
```

NioSocketChannel每次处理读取事件时调用`AdaptiveRecvByteBufAllocator`新建一个**缓冲区预测处理器**用于调整此次事件读取过程中循环读取时的ByteBuf缓冲区容量。缓冲区预测处理器为其静态内部类：`HandlerImpl`

```java
// io/netty/channel/AdaptiveRecvByteBufAllocator.java
// 缓冲区预测处理器
private final class HandleImpl extends MaxMessageHandle {
    private final int minIndex;// 向量表最小索引
    private final int maxIndex;// 向量表最大索引
    private int index;// 当前向量表索引, 默认为33，即缓冲区容量默认2048, 刚好大于MTU 1500B
    private int nextReceiveBufferSize;// 下次预分配ByteBuf大小
    private boolean decreaseNow;// 是否立刻执行容量收缩操作

    // 返回调整/预测的下次缓冲区分配容量
    public int guess() {return nextReceiveBufferSize;}

    // 每次NioSocketChannel执行读取操作后会record方法以对下次读取时预分配ByteBuf进行容量设定
    // 根据每次实际读取字节数调整下次读取缓冲区容量
    private void record(int actualReadBytes) {
        // 1.若此次读取字节数<此次分配的ByteBuf容量在向量表中前一个向量容量，
        // 则判断需要缩容，缩容步进索引为1
        if (actualReadBytes <= SIZE_TABLE[max(0, index - INDEX_DECREMENT)]) {
            if (decreaseNow) {
                // 1.2 第2次则立刻缩减下次预分配ByteBuf容量
                index = max(index - INDEX_DECREMENT, minIndex);// 向量表索引-1
                nextReceiveBufferSize = SIZE_TABLE[index];
                decreaseNow = false;
            } else {
                // 1.1 第1次先标记需要缩容，但不立刻缩减下次分配ByteBuf
                decreaseNow = true;
            }
        } else if (actualReadBytes >= nextReceiveBufferSize) {
            // 2.若此次读取字节数>=此次分配的ByteBuf容量，说明读取填满缓冲区
            // 将下次分配ByteBuf直接扩容,扩容步进索引为4
            index = min(index + INDEX_INCREMENT, maxIndex);// 向量表索引+4
            nextReceiveBufferSize = SIZE_TABLE[index];
            decreaseNow = false;
        }
    }

    // 读取完成时回调，用本次循环读取总字节数来调整下次IO事件发生时的预测缓冲区容量
    // 目的在于下次读取事件发生时可一次性分配能完全读取的缓冲区，避免动态扩容
    public void readComplete() {
        record(totalBytesRead());
    }
}
```

从这个缓冲区预测处理器可知，默认缓冲区为2048B，读取时动态调整：

- **此次读取填满缓冲区，则扩容**，步进索引为4，即小容量时每次增加16*4=64B，大容量时每次扩容2^4=16倍。
- **连续两次读取无法填满已分配缓冲区的前一个向量表容量，则缩容**，步进索引为1，即小容量时每次减少16B，大容量时每次缩容一半。

最后循环读取完成时回调readComplete()方法来确保下次读取事件发生时**预测器分配一次性即可读完所有内容的缓冲区容量，避免多次触发pipeline#fireChannelRead事件，减少handler对TCP拆包的逻辑处理**。

为什么使用动态缓冲区，而不是固定1024或2048容量呢？

> **Netty作为通用IO框架，需适配不同场景，不同场景下码流大小千差万别**，聊天场景则码流很小，文件传输则码流很大。因此Netty需根据上次读取码流大小动态预测下次接收缓冲区容量。
>
> 设置合适的缓冲区可以减少read IO 调用，同时节省内存。缓冲区过小会频繁read io调用，过大则浪费内存。

## write->内存队列

Netty的Channel有3中写入API方法，都继承自`ChannelOutboundInvoker`接口：

```java
ChannelFuture write(Object msg)
ChannelFuture write(Object msg, ChannelPromise promise);

ChannelOutboundInvoker flush();

ChannelFuture writeAndFlush(Object msg);
ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);
```

- write()：将数据写入内存缓存ByteBuf中，还没写入套接字
- flush()：将内存缓存ByteBuf中数据写入到套接字
- writeAndFlush()：前两者的结合

> 有一点要注意，write()返回的ChannelFuture只有在数据真正flush()到套接字后才会执行相应的回调通知。

AbstractChannel中各个方法的实现如下：

```java
@Override
public ChannelFuture write(Object msg) {
    return pipeline.write(msg);
}

@Override
public ChannelFuture write(Object msg, ChannelPromise promise) {
    return pipeline.write(msg, promise);
}

@Override
public ChannelFuture writeAndFlush(Object msg) {
    return pipeline.writeAndFlush(msg);
}

@Override
public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
    return pipeline.writeAndFlush(msg, promise);
}

@Override
public Channel flush() {
    pipeline.flush();
    return this;
}
```

write和flush都是outbound事件，最终会由`HeadContext`节点的`Unsafe`处理。

```java
protected abstract class AbstractUnsafe implements Unsafe {
    // 写入缓冲区, write操作将消息写入此缓冲区暂存
    private volatile ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);

    @Override
    public final void write(Object msg, ChannelPromise promise) {
        ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
        // 内存队列为空，一般是 Channel 已经关闭，所以通知 Promise 异常结果
        if (outboundBuffer == null) {/*...*/}

        int size;
        // 1.过滤写入的消息，默认转为直接内存ByteBuf
        msg = filterOutboundMessage(msg);
        size = pipeline.estimatorHandle().size(msg);// 计算消息大小
        if (size < 0) size = 0;
        // 2.写入内存队列中暂存
        outboundBuffer.addMessage(msg, size, promise);
    }
}
// AbstractNioByteChannel.java
protected final Object filterOutboundMessage(Object msg) {
    // 将堆内存ByteBuf转换为直接内存的ByteBuf
    if (msg instanceof ByteBuf) {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.isDirect()) {return msg; }
        return newDirectBuffer(buf);
    }
	// 如果是文件块则不转换
    if (msg instanceof FileRegion) { return msg; }
    // 不支持类型抛出异常
    throw new UnsupportedOperationException(
        "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
}
```

> 从消息过滤方法可以看出**ctx.write()写入响应消息仅支持写入ByteBuf和FileRegion类型**，其余均不支持。

**将堆内存的ByteBuf转换为直接内存ByteBuf的原因是JVM堆内存写入Socket套接字时拷贝次数较多。**

**注意：在自定义ChannelHandler中想要写数据到对端时，一般有2种写法：**

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ctx.write(msg); // 1
    ctx.channel().write(msg); // 2
}
```

- 第2 种，实际就是本文所描述的，将 write 事件，从 pipeline 的 `tail` 节点到 `head` 节点的过程。
- 第1种，将 write 事件，从当前的 `ctx` 节点的**下一个**节点传播到 `head` 节点的过程。

这两种都会交由`head`节点处理，都能成功发送到对端Socket，具体使用哪一种呢，看自己的ChannelHandler是否有拦截`write()出站事件`的需求(一般不会拦截)，有则必须第2种。

## flush->Socket套接字

Netty `Channel#write()`方法最后仅将数据写入了内存缓冲ByteBuf中，调用`Channel#flush()`才会将数据写入Socket套接字。

flush同write一样是出站事件，最后由`HeadContext`节点的`Unsafe`进行处理：

```java
protected abstract class AbstractUnsafe implements Unsafe {
    // 写入缓冲区，flush将缓冲区内消息写入socket
    private volatile ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);

    private boolean inFlush0;
    @Override
    public final void flush() {
        // 内存队列为 null ，一般是 Channel 已经关闭，所以直接返回
        ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
        if (outboundBuffer == null || outboundBuffer.isEmpty()) return;
        // 标记内存队列开始 flush
        outboundBuffer.addFlush();
        flush0();
    }

    protected void flush0() {
        // 正在刷，避免重复刷
        if (inFlush0) return;
        // 内存队列为 null ，一般是 Channel 已经关闭，所以直接返回
        final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
        if (outboundBuffer == null || outboundBuffer.isEmpty()) return;

        inFlush0 = true;
        // 真正写入到套接字
        doWrite(outboundBuffer);
        inFlush0 = false;
    }
}
```

flush方法标记内存队列开始flush并调用NioSocketChannel#doWrite()将消息写入Socket。

### doWrite

一般真正和JavaNio的SocketChannel套接字交互的方法还是有Netty Channel实现，并命名为doXxx()。比如将内存队列中暂存的消息真正写入Channel时将调用`NioSocketChannel#doWrite()`：

```java
// NioSocketChannel.java
protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    // 1.取出此Netty Channel包装的JavaNio SocketChannel
    SocketChannel ch = javaChannel();
    // 2.配置的自旋写入次数，默认16
    int writeSpinCount = config().getWriteSpinCount();
    do {
        // 内存队列为空，结束循环，直接返回
        if (in.isEmpty()) {
            // 取消对 SelectionKey.OP_WRITE 的感兴趣
            clearOpWrite();
            return;
        }

        // 配置的每次写入最大字节数，默认无限制
        int maxBytesPerGatheringWrite = ((NioSocketChannelConfig) config).getMaxBytesPerGatheringWrite();
        // 3.从内存队列中获得待写入的JavaNio的ByteBuffer数组，1次最多1024个
        ByteBuffer[] nioBuffers = in.nioBuffers(1024, maxBytesPerGatheringWrite);
        int nioBufferCnt = in.nioBufferCount();

        // 4.将数据写入Socket，若无法写入则注册SelectionKey.OP_WRITE监听可写入事件
        switch (nioBufferCnt) {
                // 4.1 内部的数据为 FileRegion, 调用FileChannel.transferTo()零拷贝文件传输
            case 0:
                writeSpinCount -= doWrite0(in);
                break;
                // 4.2 1个ByteBuffer，调用java.nio.channels.SocketChannel.write(ByteBuffer)
            case 1: {
                ByteBuffer buffer = nioBuffers[0];
                int attemptedBytes = buffer.remaining();
                // 将数据写入SocketChannel套接字
                final int localWrittenBytes = ch.write(buffer);
                // 5.写入字节小于等于 0 ，说明 NIO Channel 不可写，
                // 所以注册 SelectionKey.OP_WRITE ，等待 NIO Channel 可写，并返回以结束循环
                if (localWrittenBytes <= 0) {
                    incompleteWrite(true);
                    return;
                }
                adjustMaxBytesPerGatheringWrite(attemptedBytes, localWrittenBytes, maxBytesPerGatheringWrite);
                in.removeBytes(localWrittenBytes);// 从内存队列中，移除已经写入的数据
                --writeSpinCount;
                break;
            }
                // 4.3 多个ByteBuffer, 调用java.nio.channels.SocketChannel.write(ByteBuffer[])
            default: {/*步骤和4.2基本相同，故省略*/}
        }
    } while (writeSpinCount > 0);// 循环自旋写入
    // 6.多次自旋写入后内存队列中还有数据，注册 SelectionKey.OP_WRITE
    // 因为不能让Reactor线程一直写，它得处理其它IO操作和任务队列
    incompleteWrite(writeSpinCount < 0);
}
```

可以看到所谓的FileRegion最终是调用`FileChannel#transferTo()`直接将文件内容发送到SocketChannel内核缓冲区以减少拷贝，即所谓**文件传输零拷贝**。

从内存队列中写入消息到SocketChannel每次自旋最多写入1024个ByteBuffer，最多自旋16次，**避免线程长时间处于处理消息写入过程，导致任务队列和其它IO事件长时间等待**。

注意：写入过程中会判断写入量是否小于0，**如果小于0说明此时SocketChannel的内核缓冲区已经写满**，所以注册 `SelectionKey.OP_WRITE` ，等待 NIO Channel 可写。因此调用 `incompleteWrite(true)` 方法注册对写事件的监听并返回。

## 发送队列

`Channel#write()`写入消息时，pipeline末尾的HeadContext的`AbstractUnsafe#write()`先将消息写入`ChannelOutboundBuffer内存队列`中，调用flush()方法时才将内存队列所有消息写入SocketChannel套接字。

写入内存队列过程如下：

`Unsafe#write()`-->转化为直接内存ByteBuf-->`outboundBuffer#addMessage(msg)`

flush过程如下：

`Unsafe#flush()`-->`outboundBuffer#addFlush()`-->`Unsafe#flush0()`-->`NioSocketChannel#doWrite()`-->`outboundBuffer#nioBuffers()`-->`SocketChannel#write(buf)`-->`outboundBuffer#removeBytes()`

```java
public final class ChannelOutboundBuffer {
    // Assuming a 64-bit JVM:
    //  - 16 bytes object header --> 16B
    //  - 6 reference fields     --> 48B
    //  - 2 long fields          --> 16B
    //  - 2 int fields           --> 8B
    //  - 1 boolean field        --> 1B
    //  - padding  补齐8B整数倍    --> 7B
    // 默认每个Entry占用96B
    static final int CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD =
        SystemPropertyUtil.getInt("io.netty.transport.outboundBufferEntrySizeOverhead", 96);

    /**
     * 线程对应的 ByteBuffer 数组缓存
     * 每次调用 {@link #nioBuffers(int, long)} 会重新生成
     */
    private static final FastThreadLocal<ByteBuffer[]> NIO_BUFFERS = new FastThreadLocal<ByteBuffer[]>() {
        @Override
        protected ByteBuffer[] initialValue() throws Exception {
            return new ByteBuffer[1024];
        }
    };

    private final Channel channel;// 所属Channel

    // Entry(flushedEntry) --> ... Entry(unflushedEntry) --> ... Entry(tailEntry)
    // 第1个标记flush的Entry，即还未写入套接字的第一个Entry
    private Entry flushedEntry;
    // 第1个未flush的Entry
    private Entry unflushedEntry;

    private Entry tailEntry;
    // 已标记 flush 但未写入SocketChannel套接字的 Entry 数量
    private int flushed;

    private int nioBufferCount;
    private long nioBufferSize;

    // 以下几个字段用于限流
    // 所有 Entry 预计占用的内存大小，通过 Entry.pendingSize 来合计
    private volatile long totalPendingSize;
	// 此ChannelOutboundBuffer是否可写入，即限流
    private static final AtomicIntegerFieldUpdater<ChannelOutboundBuffer> UNWRITABLE_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "unwritable");
    private volatile int unwritable;
}
```

### Entry单链表

内存队列以`Entry单向链表`组织待写入消息：

```java
static final class Entry {
    // Recycler 对象池，用于重用 Entry 对象
    private static final ObjectPool<Entry> RECYCLER = ObjectPool.newPool(new ObjectCreator<Entry>() {
        @Override
        public Entry newObject(Handle<Entry> handle) {
            return new Entry(handle);
        }
    });
    // Recycler处理器，用于回收Entry对象
    private final Handle<Entry> handle;
    Entry next;
    Object msg;// 保存的消息，一般是ByteBuf
    ByteBuffer[] bufs;// 当 count > 1 时使用，表示 msg 属性转化的 NIO ByteBuffer 数组
    ByteBuffer buf;// 当 count = 1 时使用，表示 msg 属性转化的 NIO ByteBuffer 对象
    ChannelPromise promise;
    long progress;// 当前这个Entry已写入套接字的字节数
    long total;// msg可读字节数
    int pendingSize;//  每个 Entry 预计占用的内存大小，计算方式为消息msg的字节数 + Entry 对象自身占用内存的大小。
    /**
     * {@link #msg} 转化的 NIO ByteBuffer 的数量
     * <p>
     * 当 = 1 时，使用 {@link #buf}
     * 当 > 1 时，使用 {@link #bufs}
     */
    int count = -1;
    boolean cancelled;

    // 从对象池内获取一个新的Entry并初始化
    static Entry newInstance(Object msg, int size, long total, ChannelPromise promise) {
        Entry entry = RECYCLER.get();
        entry.msg = msg;
        entry.pendingSize = size + CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD;
        entry.total = total;
        entry.promise = promise;
        return entry;
    }
}
```

### addMessage

`AbstractUnsafe#write()`写入消息时调用`ChannelOutboundBuffer#addMessage()`方法将消息放入内存队列：`Unsafe#write()`-->转化为直接内存ByteBuf-->`outboundBuffer#addMessage(msg)`

```java
/**
 * 将指定消息放入内存队列中.
 * 注意：只有真正写入套接字后才会通知ChannelPromise
 */
public void addMessage(Object msg, int size, ChannelPromise promise) {
    // 1.从对象池获取一个Entry对象
    Entry entry = Entry.newInstance(msg, size, total(msg), promise);
    if (tailEntry == null) {
        flushedEntry = null;
    } else {
        Entry tail = tailEntry;
        tail.next = entry;
    }
    tailEntry = entry;
    if (unflushedEntry == null)
        unflushedEntry = entry;// 此时相当于首节点

    // 增加 totalPendingSize 计数
    incrementPendingOutboundBytes(entry.pendingSize, false);
}
```

在前面分析该`AbstractUnsafe#write()`可知Netty的堆内存ByteBuf消息会转换为直接内存ByteBuf，总之`Entry#msg字段`一般是直接内存ByteBuf。

调用多次`#addMessage(Object msg, int size, ChannelPromise promise)` 之后：

![ChannelOutboundBuffer](netty.assets/ChannelOutboundBufferEntry.png)

### addFlush

`Unsafe#flush()`将内存队列中数据刷入套接字过程如下：

`Unsafe#flush()`-->`outboundBuffer#addFlush()`-->`Unsafe#flush0()`-->`NioSocketChannel#doWrite()`-->`outboundBuffer#nioBuffers()`-->`SocketChannel#write(buf)`-->`outboundBuffer#removeBytes()`

addFlush如下：它就是标记现有的Entry全部为已刷新

```java
// 标记内存队列中Entry都已刷新
public void addFlush() {
    Entry entry = unflushedEntry;
    if (entry != null) {
        if (flushedEntry == null) {
            // there is no flushedEntry yet, so start with the entry
            flushedEntry = entry;
        }
        // 计算 flush 的数量，并设置每个 Entry 对应的 Promise 不可取消
        do {
            flushed++;
            if (!entry.promise.setUncancellable()) {
               // 省略
            }
            entry = entry.next;
        } while (entry != null);

        // 设置 unflushedEntry 为空，表示所有都 flush
        unflushedEntry = null;
    }
}
```

调用此方法后Entry队列如下：

![ChannelOutboundBuffer](netty.assets/ChannelOutboundBuffer#addFlush.png)

我觉得叫flushedEntry不如叫flushingEntry更见名知意。

### 其他flush相关方法

`Unsafe#flush()`将内存队列中数据刷入套接字过程如下：

`Unsafe#flush()`-->`outboundBuffer#addFlush()`-->`Unsafe#flush0()`-->`NioSocketChannel#doWrite()`-->`outboundBuffer#nioBuffers()`-->`SocketChannel#write(buf)`-->`outboundBuffer#removeBytes()`

前面分析了addFlush()方法，就是将所有unFlushedEntry指向的新的Entry移到flushedEntry后面表示待写入套接字。

剩下两个方法内容较多，就不列出了，其实完全可以根据ChanneOutboundBuffer和Entry的各个属性猜测出其实现逻辑，具体实现逻辑就不列出了：

```java
// 将每个标记flush的Entry持有的Netty ByteBuf对象转为JavaNio的ByteBuffer并返回其数组
// 用于SocketChannel写入
public ByteBuffer[] nioBuffers(int maxCount, long maxBytes) {}

// 根据写入SocketChannel套接字的字节数，对Entry单链表进行删除
// 对于某个只写了一部分的ByteBuffer所属的Entry，其中有个属性`long progress`记录当前写入进度
public void removeBytes(long writtenBytes) {}
```

### 高低水位限流

在上面介绍的几个方法里，似乎有几个字段从未使用：

```java
// 以下几个字段用于限流
// 所有 Entry 预计占用的内存大小，通过 Entry.pendingSize 来合计
private volatile long totalPendingSize;
// 此ChannelOutboundBuffer是否可写入，即限流
private static final AtomicIntegerFieldUpdater<ChannelOutboundBuffer> UNWRITABLE_UPDATER =
    AtomicIntegerFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "unwritable");
private volatile int unwritable;
```

当不断调用 `#addMessage(Object msg, int size, ChannelPromise promise)` 方法，添加消息到 ChannelOutboundBuffer 内存队列中，如果**不及时** flush 写到对端( 例如程序一直未调用 `Channel#flush()` 方法，或者对端接收数据比较慢导致 Channel 不可写 )，可能会导致 **OOM 内存溢出**。所以，在 ChannelOutboundBuffer 使用 `totalPendingSize` 属性，存储所有 Entry 预计占用的内存大小(`pendingSize`)。

- 当`totalPendingSize`大于高水位阈值`ChannelConfig.writeBufferHighWaterMark`默认64KB，关闭写开关`unwritable`
- 当`totalPendingSize`小于低水位阈值`ChannelConfig.writeBufferLowWaterMark`默认32KB，打开写开关`unwritable`

`ChannelOutboundBuffer#addMessage()`添加待写入套接字的消息时，会增加`totalPendingSize`属性：

```java
private void incrementPendingOutboundBytes(long size, boolean invokeLater) {
    if (size == 0) return;
    // 增加 totalPendingSize 计数
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, size);
    // totalPendingSize 大于高水位阀值时，设置为不可写
    if (newWriteBufferSize > channel.config().getWriteBufferHighWaterMark()) {
        setUnwritable(invokeLater);
    }
}

private void setUnwritable(boolean invokeLater) {
    for (; ; ) {
        final int oldValue = unwritable;
        // 或位操作，修改第 0 位 bits 为 1
        final int newValue = oldValue | 1;
        if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
            // 若之前可写，现在不可写，触发pipeline#fireChannelWritabilityChanged()事件
            if (oldValue == 0) {
                fireChannelWritabilityChanged(invokeLater);
            }
            break;
        }
    }
}

public long bytesBeforeUnwritable() {
    // 根据高水位阈值计算还有多少字节不可写，如果已经不可写则返回0
    long bytes = channel.config().getWriteBufferHighWaterMark() - totalPendingSize;
    if (bytes > 0) return isWritable() ? bytes : 0;
    return 0;
}
```

当消息写入套接字后会调用`ChannelOutboundBuffer#removeBytes(long writtenBytes)`移除Entry，它会减少`totalPendingSize`属性：

```java
private void decrementPendingOutboundBytes(long size, boolean invokeLater, boolean notifyWritability) {
    if (size == 0) return;
    // 减少 totalPendingSize 计数
    long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);
    // totalPendingSize 小于低水位阀值时，设置为可写
    if (notifyWritability && newWriteBufferSize < channel.config().getWriteBufferLowWaterMark()) {
        setWritable(invokeLater);
    }
}

private void setWritable(boolean invokeLater) {
    for (; ; ) {
        final int oldValue = unwritable;
        // 并位操作，修改第 0 位 bits 为 0
        final int newValue = oldValue & ~1;
        if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
            // 若之前不可写，现在可写，触发pipeline#fireChannelWritabilityChanged()事件
            if (oldValue != 0 && newValue == 0) {
                fireChannelWritabilityChanged(invokeLater);
            }
            break;
        }
    }
}

public long bytesBeforeWritable() {
    // 根据低水位阈值计算还有多少字节可以变为可写，如果已经可写则返回0
    long bytes = totalPendingSize - channel.config().getWriteBufferLowWaterMark();
    if (bytes > 0) return isWritable() ? 0 : bytes;
    return 0;
}
```

从上面这点代码来看，这个`unwritable`属性仅仅当前ChannelOutboundBuffer是否可写的标记，并没有去实现限流，而是通过`pipeline#fireChannelWritabilityChanged()`这个inbound事件通知ChannelHandler，**具体的限流逻辑交由用户实现！**

即我们要实现`ChannelHandler#channelWritabilityChanged()`方法监听`unwritable`状态变化，从而进行限流：

```java
@Override
public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().unsafe().outboundBuffer().isWritable()) {
		// 限制业务继续往Channel写入数据
    }
    ctx.fireChannelWritabilityChanged();// 继续传播该事件
}
```

#### 自定义流控标记bit位

`unwritable`属性是`int`类型，而判断其高低水位限流仅用了1位bit，剩下的31个bit可用于用户扩展。

有时用户业务需求不仅想基于内存使用限流，还想根据一些业务规则来判断是否可发消息到套接字，且还想用到netty的pipeline自动事件发布功能，那么下面几个API可以考虑：

`#getUserDefinedWritability(int index)` 方法，获得指定 bits 是否可写。代码如下：

```java
public boolean getUserDefinedWritability(int index) {
    return (unwritable & writabilityMask(index)) == 0;
}

private static int writabilityMask(int index) {
    // 不能 < 1 ，因为第 0 bits 为 ChannelOutboundBuffer 自己使用
    // 不能 > 31 ，因为超过 int 的 bits 范围
    if (index < 1 || index > 31) {
        throw new IllegalArgumentException("index: " + index + " (expected: 1~31)");
    }
    return 1 << index;
}
```

`#setUserDefinedWritability(int index, boolean writable)` 方法，设置指定 bits 是否可写。代码如下：

```java
public void setUserDefinedWritability(int index, boolean writable) {
    // 设置可写
    if (writable) {
        setUserDefinedWritability(index);
    // 设置不可写
    } else {
        clearUserDefinedWritability(index);
    }
}

private void setUserDefinedWritability(int index) {
    final int mask = ~writabilityMask(index);
    for (;;) {
        final int oldValue = unwritable;
        final int newValue = oldValue & mask;
        // CAS 设置 unwritable 为新值
        if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
            // 若之前不可写，现在可写，触发 Channel WritabilityChanged 事件到 pipeline 中。
            if (oldValue != 0 && newValue == 0) {
                fireChannelWritabilityChanged(true);
            }
            break;
        }
    }
}

private void clearUserDefinedWritability(int index) {
    final int mask = writabilityMask(index);
    for (;;) {
        final int oldValue = unwritable;
        final int newValue = oldValue | mask;
        if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
            // 若之前可写，现在不可写，触发 Channel WritabilityChanged 事件到 pipeline 中。
            if (oldValue == 0 && newValue != 0) {
                fireChannelWritabilityChanged(true);
            }
            break;
        }
    }
}
```

# ChannelPipeline

ChannelPipeline是ChannelHandler链，负责传播inbound或outbound事件流。内部维护了ChannelHandler双向链表和迭代器，方便的进行ChannelHandler的查找、添加、替换、删除。

![ChannelPipeline](netty.assets/ChannelPipeline.png)



```java
// 通常在创建Channel时一起创建
public class DefaultChannelPipeline implements ChannelPipeline {
    // 自定义的ChannelHandler加入在head和tail之间
    final HeadContext head;// pipeline执行链头结点
    final TailContext tail;// pipeline执行链尾结点

    private final Channel channel;// 所属的Channel
    private final ChannelFuture succeededFuture;
    private final VoidChannelPromise voidPromise;

    // 挂起的ChannelHandler回调任务
    // 如可以存放的是还未执行ChannelHandler#handlerAdded()回调的处理器Context任务PendingHandlerAddedTask
    private PendingHandlerCallback pendingHandlerCallbackHead;
    private boolean registered;// 所属的Channel是否注册到EventLoop

    protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise = new VoidChannelPromise(channel, true);

        tail = new TailContext(this);
        head = new HeadContext(this);

        head.next = tail;
        tail.prev = head;
    }
}
```

## 添加ChannelHandler

注意：在pipeline的双向执行链中，以`AbstractChannelHandlerContext`作为执行节点，其中封装有ChannlerHandler，默认实现是`DefaultChannelHandlerContext`。

往ChannelPipeline中添加ChannelHandler的方法是`DefaultChannelPipeline#addLast()`：

```java
public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
    final AbstractChannelHandlerContext newCtx;
    synchronized (this) {// 加锁防止多线程并发操作pipeline的双向链表
        // 1.检查重复handler，若某个handler被多次添加到pipeline，且未标注@Shareble注解则抛异常
        checkMultiplicity(handler);

        // 2.创建DefaultChannelHandlerContext封装handler作为新节点，并添加到pipeline的双向执行链中
        newCtx = newContext(group, filterName(name, handler), handler);
        addLast0(newCtx);

        // 3.如果此时pipeline所属的Channel还未注册到EventLoop，
        // 则包装为PendingHandlerAddedTask任务挂起，该任务将在通道注册后再回调此ChannelHandler.handlerAdded()
        // 详细见 DefaultChannelPipeline#invokeHandlerAddedIfNeeded()
        if (!registered) {
            newCtx.setAddPending();
            callHandlerCallbackLater(newCtx, true);
            return this;// 直接返回
        }

        // 省略
    }
    // 4.回调ChannelHandler#handlerAdded()方法
    callHandlerAdded0(newCtx);
    return this;
}
```

注意第3步的挂起回调任务，下面分析为什么要挂起而不是如第4步那样直接回调。

### 挂起Handler回调任务

DefaultChannelPipeline中有个属性`PendingHandlerCallback pendingHandlerCallbackHead`，它保存了一些挂起任务，比如像这个`PendingHandlerAddedTask`任务，它其实就是保存了ChannelHandlerContext，以执行`ChannelHandler#handlerAdded()`回调。

```java
private abstract static class PendingHandlerCallback implements Runnable {
    final AbstractChannelHandlerContext ctx;// 保存的ChannelHandler上下文
    PendingHandlerCallback next;// 下一个挂起的回调任务
    PendingHandlerCallback(AbstractChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
    abstract void execute();
}

// 挂起的ChannelHandler#handlerAdded()回调任务
private final class PendingHandlerAddedTask extends PendingHandlerCallback {
    @Override
    void execute() {
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            callHandlerAdded0(ctx);// 这里就是调用ChannelHandler#handlerAdded()回调
        } else {
            // 省略
        }
    }
}
```

**那为什么会有`PendingHandlerCallback`挂起任务，而不直接执行handlerAdded()回调？**

> 因为 ChannelHandler 添加到 pipeline 中，会触发 `ChannelHandler#handlerAdded()`回调，并且该事件需要在 Channel 所属的 EventLoop 中执行。
>
> 但是 Channel 并未注册在 EventLoop 上时，需要暂时将“触发 ChannelHandler 的添加完成( added )事件”的逻辑，作为一个 PendingHandlerCallback 进行“缓存”。在 Channel 注册到 EventLoop 上时，进行回调执行。
>
> 在`EventLoop#register()`注册Channel到EventLoop的Selector完成后，会调用`DefaultChannelPipeline#invokeHandlerAddedIfNeeded()`去执行这些挂起的回调任务。
>
> 移除ChannelHandler时也会有一个挂起任务PendingHandlerRemovedTask，就不细究了。

如下面是Channel注册到EventLoop的方法：第2步会触发这些挂起任务的执行。

```java
// AbstractChannel.AbstractUnsafe.java
private void register0(ChannelPromise promise) {
    if (!promise.setUncancellable() || !ensureOpen(promise)) {return;}
    boolean firstRegistration = neverRegistered;
    // 1.执行注册逻辑
    // 将SelectableChannel注册到此EventLoop的Selector上，不过此时注册的兴趣集为0
    doRegister();
    neverRegistered = false;
    registered = true;

    // 2.执行可能的挂起的ChannelHandler#handlerAdded()回调任务
    // 比如ChannelInitializer就是在这回调初始化，此回调常用来添加ChannelHandler
    pipeline.invokeHandlerAddedIfNeeded();

    safeSetSuccess(promise);
    // 3.触发ChannelInboundHandler#channelRegistered()回调
    pipeline.fireChannelRegistered();
}
```

## ChannelHandlerContext

注意：在pipeline的双向执行链中，以`AbstractChannelHandlerContext`作为执行节点，其中封装有ChannlerHandler，默认实现是`DefaultChannelHandlerContext`。

![DefaultChannelHandlerContext](netty.assets/DefaultChannelHandlerContext.png)

### Abstract

```java
abstract class AbstractChannelHandlerContext implements ChannelHandlerContext, ResourceLeakHint {
    volatile AbstractChannelHandlerContext next;// 上一个节点
    volatile AbstractChannelHandlerContext prev;// 下一个节点

    private final DefaultChannelPipeline pipeline;// 所属pipeline
    private final String name;// 此context在pipeline中的唯一名字
    private final boolean ordered;
    // 此context的执行码，可用于快速判断此handler是否拦截入站和出站各个事件
    private final int executionMask;

    AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor,
                                  String name, Class<? extends ChannelHandler> handlerClass) {
        this.name = ObjectUtil.checkNotNull(name, "name");
        this.pipeline = pipeline;
        this.executor = executor;
        this.executionMask = mask(handlerClass);// 根据传入的Handler类型计算执行掩码
        ordered = executor == null || executor instanceof OrderedEventExecutor;
    }
}
```

### 执行掩码

在上面AbstractChannelHandlerContext中有`executionMask`执行掩码属性，它用于快速判断此context包含的handler是否能拦截某个出站或入站事件，它由`ChannelHandlerMask`管理：

```java
// 入站和出站的执行掩码，这种以掩码方式快速计算某个方法是否拦截执行的方式值得学习
final class ChannelHandlerMask {
    // Using to mask which methods must be called for a ChannelHandler.
    static final int MASK_EXCEPTION_CAUGHT = 1;
    static final int MASK_CHANNEL_REGISTERED = 1 << 1;
    static final int MASK_CHANNEL_UNREGISTERED = 1 << 2;
    static final int MASK_CHANNEL_ACTIVE = 1 << 3;
    static final int MASK_CHANNEL_INACTIVE = 1 << 4;
    static final int MASK_CHANNEL_READ = 1 << 5;
    static final int MASK_CHANNEL_READ_COMPLETE = 1 << 6;
    static final int MASK_USER_EVENT_TRIGGERED = 1 << 7;
    static final int MASK_CHANNEL_WRITABILITY_CHANGED = 1 << 8;
    static final int MASK_BIND = 1 << 9;
    static final int MASK_CONNECT = 1 << 10;
    static final int MASK_DISCONNECT = 1 << 11;
    static final int MASK_CLOSE = 1 << 12;
    static final int MASK_DEREGISTER = 1 << 13;
    static final int MASK_READ = 1 << 14;
    static final int MASK_WRITE = 1 << 15;
    static final int MASK_FLUSH = 1 << 16;
    // 所有入站事件掩码
    static final int MASK_ONLY_INBOUND = MASK_CHANNEL_REGISTERED |
        MASK_CHANNEL_UNREGISTERED | MASK_CHANNEL_ACTIVE | MASK_CHANNEL_INACTIVE | MASK_CHANNEL_READ |
        MASK_CHANNEL_READ_COMPLETE | MASK_USER_EVENT_TRIGGERED | MASK_CHANNEL_WRITABILITY_CHANGED;
    private static final int MASK_ALL_INBOUND = MASK_EXCEPTION_CAUGHT | MASK_ONLY_INBOUND;
    // 所有出站事件掩码
    static final int MASK_ONLY_OUTBOUND = MASK_BIND | MASK_CONNECT | MASK_DISCONNECT |
        MASK_CLOSE | MASK_DEREGISTER | MASK_READ | MASK_WRITE | MASK_FLUSH;
    private static final int MASK_ALL_OUTBOUND = MASK_EXCEPTION_CAUGHT | MASK_ONLY_OUTBOUND;

    private static final FastThreadLocal<Map<Class<? extends ChannelHandler>, Integer>> MASKS =
        new FastThreadLocal<Map<Class<? extends ChannelHandler>, Integer>>() {
        @Override
        protected Map<Class<? extends ChannelHandler>, Integer> initialValue() {
            return new WeakHashMap<Class<? extends ChannelHandler>, Integer>(32);
        }
    };

    // 根据ChannelHandler的具体类型获取执行掩码
    static int mask(Class<? extends ChannelHandler> clazz) {
        // 先从缓存获取，没有则计算再放入缓存
        Map<Class<? extends ChannelHandler>, Integer> cache = MASKS.get();
        Integer mask = cache.get(clazz);
        if (mask == null) {
            mask = mask0(clazz);
            cache.put(clazz, mask);
        }
        return mask;
    }

    // 判断handler的某个方法上是否标有@Skip注解以判断是否在执行链中跳过该方法
    private static boolean isSkippable(
        final Class<?> handlerType, final String methodName, final Class<?>... paramTypes) throws Exception {
        return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
            @Override
            public Boolean run() throws Exception {
                // 省略try/catch
                Method m = handlerType.getMethod(methodName, paramTypes);
                return m.isAnnotationPresent(Skip.class);// 是否标有@Skip
            }
        });
    }

    // ChannelHandler中的带此注解的事件处理方法不会被pipeline调用
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Skip {}
}
```

根据handler类型计算掩码的细节如下：

```java
/**
 * 计算某个ChannelHandler类型的执行掩码
 * 这种以掩码方式快速计算某个方法是否拦截执行的方式值得学习
 */
private static int mask0(Class<? extends ChannelHandler> handlerType) {
    int mask = MASK_EXCEPTION_CAUGHT;
	// 省略try/catch
    // 1.如果实现了入站事件接口
    if (ChannelInboundHandler.class.isAssignableFrom(handlerType)) {
        mask |= MASK_ALL_INBOUND;
        // 判断是否以@Skip注解跳过某些入站事件回调
        if (isSkippable(handlerType, "channelRegistered", ChannelHandlerContext.class)) {
            mask &= ~MASK_CHANNEL_REGISTERED;
        }
        if (isSkippable(handlerType, "channelUnregistered", ChannelHandlerContext.class)) {
            mask &= ~MASK_CHANNEL_UNREGISTERED;
        }
        if (isSkippable(handlerType, "channelActive", ChannelHandlerContext.class)) {
            mask &= ~MASK_CHANNEL_ACTIVE;
        }
        if (isSkippable(handlerType, "channelInactive", ChannelHandlerContext.class)) {
            mask &= ~MASK_CHANNEL_INACTIVE;
        }
        if (isSkippable(handlerType, "channelRead", ChannelHandlerContext.class, Object.class)) {
            mask &= ~MASK_CHANNEL_READ;
        }
        if (isSkippable(handlerType, "channelReadComplete", ChannelHandlerContext.class)) {
            mask &= ~MASK_CHANNEL_READ_COMPLETE;
        }
        if (isSkippable(handlerType, "channelWritabilityChanged", ChannelHandlerContext.class)) {
            mask &= ~MASK_CHANNEL_WRITABILITY_CHANGED;
        }
        if (isSkippable(handlerType, "userEventTriggered", ChannelHandlerContext.class, Object.class)) {
            mask &= ~MASK_USER_EVENT_TRIGGERED;
        }
    }
    // 2.如果实现了出站事件接口
    if (ChannelOutboundHandler.class.isAssignableFrom(handlerType)) {
        mask |= MASK_ALL_OUTBOUND;
        // 判断是否以@Skip注解跳过某些出站事件回调
        if (isSkippable(handlerType, "bind", ChannelHandlerContext.class,
                        SocketAddress.class, ChannelPromise.class)) {
            mask &= ~MASK_BIND;
        }
        if (isSkippable(handlerType, "connect", ChannelHandlerContext.class, SocketAddress.class,SocketAddress.class, ChannelPromise.class)) {
            mask &= ~MASK_CONNECT;
        }
        if (isSkippable(handlerType, "disconnect", ChannelHandlerContext.class, ChannelPromise.class)) {
            mask &= ~MASK_DISCONNECT;
        }
        if (isSkippable(handlerType, "close", ChannelHandlerContext.class, ChannelPromise.class)) {
            mask &= ~MASK_CLOSE;
        }
        if (isSkippable(handlerType, "deregister", ChannelHandlerContext.class, ChannelPromise.class)) {
            mask &= ~MASK_DEREGISTER;
        }
        if (isSkippable(handlerType, "read", ChannelHandlerContext.class)) {
            mask &= ~MASK_READ;
        }
        if (isSkippable(handlerType, "write", ChannelHandlerContext.class,
                        Object.class, ChannelPromise.class)) {
            mask &= ~MASK_WRITE;
        }
        if (isSkippable(handlerType, "flush", ChannelHandlerContext.class)) {
            mask &= ~MASK_FLUSH;
        }
    }

    if (isSkippable(handlerType, "exceptionCaught", ChannelHandlerContext.class, Throwable.class)) {
        mask &= ~MASK_EXCEPTION_CAUGHT;
    }

    return mask;
}
```

这种通过执行掩码以**位运算**判断是否执行某些回调方法的方式非常值得学习，远远比每次都通过反射调用判断是否某个注解再决定是否执行的方式快的多。

## outbound事件传播

outbound出站事件通常由用户主动发起的网络I/O操作，如用户发起连接、绑定、write等操作。

**outbound事件发起者是Channel，处理者是`Unsafe`**，在pipeline中传播方向：`tail->head`。

在 ChannelHandler 中处理事件时, 如果这个 Handler 不是最后一个 Handler ，则需要调用 `ctx.xxx` (如 `ctx.connect()` ) 将此事件继续传播下去，否则此事件的传播会提前终止。

在 `io.netty.channel.ChannelOutboundInvoker` 接口中，定义了所有 Outbound 事件对应的方法：

```java
ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise);
ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);
ChannelFuture disconnect(ChannelPromise promise);
ChannelFuture close(ChannelPromise promise);
ChannelFuture deregister(ChannelPromise promise);
ChannelOutboundInvoker read();// HeadContext#read()会重置兴趣集
ChannelFuture write(Object msg, ChannelPromise promise);
ChannelOutboundInvoker flush();
ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);
```

pipeline的处理节点DefaultChannelHandlerContext实现了该出站事件调用器，用于出站事件传播。

### bind事件传播

在上面分析AbstractBootstrap启动器时分析了其bind()方法，其在初始化并注册了Channel后会调用`AbstractBootstrap#doBind0()`方法：

```java
// AbstractBootstrap.java
private static void doBind0(
    final ChannelFuture regFuture, final Channel channel,
    final SocketAddress localAddress, final ChannelPromise promise) {
    channel.eventLoop().execute(new Runnable() {
        @Override
        public void run() {
            if (regFuture.isSuccess()) {
                channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else promise.setFailure(regFuture.cause());
        }
    });
}
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {
    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.bind(localAddress, promise);// 想管道pipeline传播出站事件bind
    }
}
```

这里的`AbstractChannel#bind(SocketAddress localAddress, ChannelPromise promise)`是一个pipeline的出站事件，从`tail->head`，最终会调用HeadContext这个pipeline头节点里保存的`AbstractUnsafe#bind()`进行端口绑定。

**bind事件的出站传播过程如下**：其它出站事件传播过程和这个一模一样，将bind换成write是完全可以的。

![outbound事件传播](netty.assets/outbound事件传播.png)

pipeline以`AbstractChannelHandlerContext`作为处理节点构建成双向链表，其实现了`ChannelOutboundInvoker`接口和`ChannelInboundInvoker`接口作为**出站和入站事件传播调用器**，其实现的各个方法如bind(args)唯一作用是将事件传播到下个节点并调用其handler相应回调函数。

> 每个节点处理出站或入站事件时以内置的`ChannelHandler`调用相应的出站或入站事件回调，并**将自己作为ctx传入回调函数**如bind(ctx,args)，**处理完成后需要调用ctx#bind(args)将此事件向后传播**，否则该事件传播就将提前终止了。

从这可以猜测`ChannelOutboundHandler`的实现`ChannelOutboundHandlerAdapter`的每个方法肯定是直接调用`ctx#xxx(args)`进行事件传播，而当我们要实现某个handler时，就重写其中某些方法即可：

```java
public class ChannelOutboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelOutboundHandler {

    // 调用ChannelHandlerContext.bind(SocketAddress, ChannelPromise)以转发到ChannelPipeline中的下一个ChannelOutboundHandler 
    // 子类可以会覆盖此方法以更改行为。
    @Skip
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                     ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }
    // 其它方法是一样的，不列出
}
```

注意到还多了一个注解`@Skip`，根据前面分析的ChannelHandlerContext的执行掩码可知，如果我们自定义handler未覆盖某个事件方法如bind(args)，bind事件根本不会传播到此处理节点，也就是说只会传播到覆盖了某个事件处理方法并且未标注`@Skip`注解的节点。不得不感叹优化到极致了。

## inbound事件传播

inbound入站事件通常由I/O线程触发，如TCP链路建立、读事件等。

**inbound事件发起者是Unsafe，处理者是TailContext**，如果用户没有实现自定义的处理方法, 那么Inbound 事件默认的处理者是 TailContext, 并且其处理方法是空实现。

inbound事件传播方向：`head->tail`，如果这个 Handler 不是最后一个 Handler，则需要调用 `ctx.fireIN_EVT` (如 `ctx.fireChannelActive` ) 将此事件继续传播下去，否则此事件的传播会提前终止。

在 `io.netty.channel.ChannelInboundInvoker` 接口中，定义了所有 Inbound 事件传播对应的方法：

```java
ChannelInboundInvoker fireChannelRegistered();
ChannelInboundInvoker fireChannelUnregistered();

ChannelInboundInvoker fireChannelActive();// 触发read出站事件以重置兴趣集
ChannelInboundInvoker fireChannelInactive();

ChannelInboundInvoker fireExceptionCaught(Throwable cause);

ChannelInboundInvoker fireUserEventTriggered(Object event);

ChannelInboundInvoker fireChannelRead(Object msg);
ChannelInboundInvoker fireChannelReadComplete(); // 触发read出站事件以重置兴趣集

ChannelInboundInvoker fireChannelWritabilityChanged();
```

`ChannelInboundInvoker`接口的入站事件传播方法多了fire前缀，`ChannelInboundHandler`接口的入站事件处理方法没有此前缀，这和出站事件那边全是同名方法有了明显的区分性。

### channelActive事件传播

`pipeline#fireChannelActive()`事件触发点如下：

- 服务端启动器`AbstractBootstrap#bind()`方法绑定端口，绑定成功向服务端Channel对应的pipeline发布此事件

- 客户端启动器`Bootstrap#connect()`连接服务端，连接成功向客户端Channel对应的pipeline发布此事件

在DefaultChannelPipeline中实现如下：

```java
// DefaultChannelPipeline.java
public final ChannelPipeline fireChannelActive() {
    AbstractChannelHandlerContext.invokeChannelActive(head);
    return this;
}
```

**channelActive事件的出站传播过程如下**：其它入站事件传播过程和这个一模一样，将channelActive换成channelRead是完全可以的。

![inbound事件传播](netty.assets/inbound事件传播.png)

每个节点处理出站或入站事件时以内置的`ChannelHandler`调用相应的出站或入站事件回调，并**将自己作为ctx传入回调函数**如channelActive(ctx)，**处理完成后需要调用ctx#fireChannelActive()将此事件向后传播**，否则该事件传播就将提前终止了。

如`io.netty.handler.logging.LoggingHandler`处理器的方法如下：

```java
final class LoggingHandler implements ChannelInboundHandler, ChannelOutboundHandler {
    //... 省略无关方法
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 打印日志
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "ACTIVE"));
        }
        // 传递 Channel active 事件，给下一个节点
        ctx.fireChannelActive();
    }
}
```

# ChannelHandler

ChannelHandler是通道处理器，拦截处理各个入站出站事件，并**控制事件的传递或终止**。ChannelHandler支持2个注解：

- `@Sharable`：标注在类上，表示可以将带注释的ChannelHandler的同一实例多次添加到一个或多个ChannelPipeline中，而不会出现竞争条件。
- `@Skip`：标注在方法上，用于计算此ChannelHandler的**执行掩码**，在Pipeline事件传播此方法时，该handler被直接忽略，跳到下一个ChannelHandler。

```java
public interface ChannelHandler {

    // handler添加到pipeline后回调，一般用于handler初始化
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    // hanler从Pipeline移除后回调，一般用于handler的资源回收操作等
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;


    /**
     * 表示是否可以将1个handler实例添加到多个pipeline中，而不会出现竞争条件，即此handler是否可共享(线程安全)
     * 若handler未标注此注解，则每次添加到pipeline中必须创建新的实例
     * 一般来说，若handler中有成员变量，记录此SocketChannel的一些数据，都必须每个pipeline创建一个新实例
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {}
}
```

![ChannelHandler层次](netty.assets/ChannelHandler层次.png)

通常自定义处理器都是继承ChannelInboundHandlerAdapter或ChannelOutboundHandlerAdapter，这两个adapter实现了入站或出站接口的各个事件方法，**默认以`@Skip`标注方法以直接将事件透传到pipeline的下一个handler**，自定义handler覆盖其中部分事件方法即可实现事件拦截，未覆盖的方法将直接透传到下个handler。

> 关于如何以`@Skip`计算执行掩码以及其如何通过位运算快速判断handler是否拦截了某个事件方法的分析看ChannelPipeline分析部分。

```java
/**
 * ChannelInboundHandler实现的抽象基类，它提供了所有方法的实现。
 * 此实现只是将操作转发给ChannelPipeline中的下一个ChannelHandler 。子类可以覆盖方法实现来改变它。
 * 请注意， channelRead(ChannelHandlerContext, Object)方法自动返回后不会释放消息。
 * 如果您正在寻找自动释放接收到的消息的ChannelInboundHandler实现，请参阅SimpleChannelInboundHandler 
 */
public class ChannelInboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelInboundHandler {
    @Skip // 此注解表明此handler在此事件方法可直接忽略，直接透传到下个handler
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }
    // 其它入站方法实现类似则忽略
}
```

接下来分析几个常用的ChannelHandler实现类。

## ChannelInitializer初始化器

它是一个特殊的ChannelHandler，用于Channel的自定义初始化，一般是添加自定义ChannelHandler到Channel绑定的pipeline中。

比如以下案例：

```java
ServerBootstrap b = new ServerBootstrap();
b.option(ChannelOption.SO_BACKLOG, 1024);
b.group(bossGroup, workerGroup) // 配置主从线程组
    .channel(NioServerSocketChannel.class) // 配置要使用的Channel类
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            // 3.设置接入服务端的SocketChannel的处理器管道
            // 向管道内加入HTTP协议的编码/解码器
            p.addLast(new HttpServerCodec());
            p.addLast(new HttpServerExpectContinueHandler());
            p.addLast(new HttpHelloWorldServerHandler());// 自定义handler
        }
    });
```

启动器以childHandler()方法指定了worker group的handler，每个新建的SocketChannel注册到worker group时都会把这个handler放入其pipeline中，也就是说**每个SocketChannel刚创建完成时pipeline中只有3个handler：`HeadContext、childHander、TailContext`。**

一般来说这个childHandler都会设置为初始化器ChannelInitializer并重写其`initChannel()`方法以自定义初始化。

从之前分析的NioEventLoop注册SocketChannel可知，注册到Selector后会触发`handler#handlerAdded()`回调方法。

> 初始化器重写handlerAdded()方法会调用initChannel()方法以执行自定义的初始化操作，并最后将自己从pipeline中移除。一般都是向pipeline中添加自定义handler。

比如ServerBootstrap将ServerSocketChannel注册到boss EventLoopGroup后，初始化器将ServerBootstrapAcceptor连接接收handler添加到ServerSocketChannel的pipeline中，并移除自己。

SocketChannel注册到worker EventLoopGroup后，初始化器将自定义handler添加到其绑定的pipeline中，并移除自己。

## SimpleChannelInboundHandler

从netty的example模块可以得知，自定义handler一般是继承自`SimpleChannelInboundHandler<T>`，并重写`channelRead0()`方法。

该抽象类继承自`ChannelInboundHandlerAdapter`并重写了`channelRead()`方法。主要是**实现了ByteBuf的自动资源释放**，避免了用户忘记释放资源时发生内存泄漏。其次实现了消息类型的匹配。

```java
/**
 * 它可以根据泛型解析，从而只处理特定类型的消息，不匹配消息将传给下个handler
 * 它实现了消息资源的自动释放，避免用户释放忘记发生内存泄漏
 * 注意：由于自动资源释放的存在，若您需要将消息传给下一个handler，可能需要调用ReferenceCountUtil.retain(Object)以增加引用数，否则可能会报错
 */
public abstract class SimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter {
    // 类型匹配器，将只匹配给定泛型的消息，不匹配的直接传给下个handler
    private final TypeParameterMatcher matcher;

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean release = true;
        try {
            // 1.消息匹配泛型时，调用channelRead0()方法
            if (acceptInboundMessage(msg)) {
                I imsg = (I) msg;
                channelRead0(ctx, imsg);
            } else {
                // 2.泛型不匹配时，则直接将消息传入下个handler
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (autoRelease && release) {
                ReferenceCountUtil.release(msg);// 释放资源，即ByteBuf引用数-1
            }
        }
    }

    // 消息类型匹配泛型时将调用此方法，用户自定义handler一般重写此方法
    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
```

> 注意：若**用户自定义`channelRead0()`方法**中将消息传入了下个handler，则必须调用`ReferenceCountUtil.retain(Object)`以增加引用数，因为此handler会自动调用`ReferenceCountUtil.release(msg)`以减少引用数。
>
> 若不增加引用数，下个handler执行完成并减少引用数后(正常逻辑都会减少)，此handler自动减少引用数会因为引用数不正常而抛出错误。

若定义了多个业务handler，必须将消息传入下个handler的情况下，示例如下：

```java
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        // ...逻辑处理...
        
        if(/*条件*/){
            // 若需要将此消息传入下个handler，则必须增加引用数
            ReferenceCountUtil.retain(msg);
            ctx.fireChannelRead(msg);// 将消息继续传入下个handler
        }
        // 一般情况下，自定义handler往往放在最后(TailContext之前)，无须再将消息后传了
    }
}
```

`TailContext#channelRead()`默认实现是也是调用`ReferenceCountUtil.release(msg)`以减少引用计数进而释放资源。

关于ByteBuf的引用计数增加retain()与减少release()是如何实现内存自动释放与内存泄漏检测的请看ByteBuf分析部分。

## 编码解码器

![编解码器](netty.assets/编解码器.png)

Codec(编解码)=Encode(编码)+Decode(解码)

HttpRequestDecoder继承ByteToMessageDecoder，将ByteBuf解码为HttpRequest。

HttpResponseEncoder继承MessageToMessageEncoder，将HttpResponse编码为ByteBuf。

HttpServerCodec继承ChannelDuplexHandler，聚合了HTTP的编码和解码能力。

### ByteToMessageDecoder

ByteToMessageDecoder将ByteBuf对象转换为其它/ByteBuf对象，一般需要处理TCP的粘包与拆包。

![ByteToMessageDecoder](netty.assets/ByteToMessageDecoder.png)

这4种消息帧解码器基本能解决绝大部分用户场景的拆解包问题，其中`LengthFieldBasedFrameDecoder`使用最多。

```java
/**
 * 帧完整性检测
 * 一般可以向pipeline中添加上述几个消息帧解码器实现帧检测
 * 若需自定义帧解码器，需注意不要修改ByteBuf的readerIndex
 * 要在不修改读取器索引的情况下检查完整的帧，请使用ByteBuf.getInt(int)之类的方法
 * 注意ByteBuf的有效起始位置不一定是0，请改用in.getInt(in.readerIndex())
 * 
 * 请注意， ByteToMessageDecoder的子类不得使用@Sharable注释。
 * 一些方法如ByteBuf.readBytes(int)如果返回的缓冲区没有被释放或添加到out List中，将导致内存泄漏。
 * 使用像ByteBuf.readSlice(int)这样的派生缓冲区来避免内存泄漏。
 */
public abstract class ByteToMessageDecoder extends ChannelInboundHandlerAdapter {

    // 合并累加器：使用内存拷贝
    public static final Cumulator MERGE_CUMULATOR = new Cumulator() {
        // 省略
    };

    // 组合累加器：使用组合缓冲区
    public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator() {
        // 省略
    };

    ByteBuf cumulation;// 累加缓冲区
    private Cumulator cumulator = MERGE_CUMULATOR;// 默认累加器为合并拷贝累加器
    private int discardAfterReads = 16;// 读取释放阈值
    private int numReads;// 再读取 {@link #discardAfterReads} 次数据后，如果无法全部解码完，则进行释放，避免OOM

}
```

ByteToMessageDecoder默认接受16次ByteBuf后若还未解码完成就将释放读取缓冲区，避免OOM。

#### 累加器

Cumulator ，是 ByteToMessageDecoder 的**内部**接口。中文翻译为“累加器”，用于将读取到的数据进行累加到一起，然后再尝试**解码**，从而实现**拆包**。

也是因为 Cumulator 的累加，所以能将不完整的包累加到一起，从而完整。当然，累加的过程，没准又进入了一个不完整的包。所以，这是一个不断累加，不断解码拆包的过程。

```java
// 累加ByteBuf
public interface Cumulator {
    /**
     * 将原有的cumulation累加上新到的in，返回累加后的ByteBuf
     * 该实现负责正确处理给定ByteBuf的生命周期，因此如果ByteBuf被完全消耗，则调用ByteBuf.release()
     */
    ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in);
}
```

在ByteToMessageDecoder中提供了2种默认实现：

- 缓冲区**拷贝累加器**（默认）
- 缓冲区**组合累加器**

```java
// 通过拷贝合并为一个ByteBuf来累加
public static final Cumulator MERGE_CUMULATOR = new Cumulator() {
    @Override
    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
        // 在累加缓冲区为空，且in是单个缓存区(不是组合缓冲区Composite)，直接使用in
        if (!cumulation.isReadable() && in.isContiguous()) {
            cumulation.release();
            return in;
        }
        try {
            final int required = in.readableBytes();
            if (required > cumulation.maxWritableBytes() ||
                required > cumulation.maxFastWritableBytes() && cumulation.refCnt() > 1 ||
                cumulation.isReadOnly()) {
                // 在无法扩容、引用数大于1(说明用户使用了 slice().retain())、无法写的情况下
                // 扩展并替换新的ByteBuf
                return expandCumulation(alloc, cumulation, in);
            }
            // 将in拷贝到累加缓冲区
            cumulation.writeBytes(in, in.readerIndex(), required);
            in.readerIndex(in.writerIndex());
            return cumulation;
        } finally {in.release();/*in已读完, 必须释放in*/  }
    }
};

/**
 * 通过将ByteBuf添加到CompositeByteBuf来累积 ByteBuf，内存零拷贝
 * 注意，CompositeByteBuf维护复杂索引，可能比MERGE_CUMULATOR慢
 */
public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator() {
    @Override
    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
        if (!cumulation.isReadable()) {
            cumulation.release();
            return in;
        }
        CompositeByteBuf composite = null;
        try {
            // 原本是CompositeByteBuf类型或引用数为1则直接添加
            if (cumulation instanceof CompositeByteBuf && cumulation.refCnt() == 1) {
                composite = (CompositeByteBuf) cumulation;
                // Writer index must equal capacity if we are going to "write"
                // new components to the end
                if (composite.writerIndex() != composite.capacity()) {
                    composite.capacity(composite.writerIndex());
                }
            } else {// 否则需要分配一个组合缓冲区
                composite = alloc.compositeBuffer(Integer.MAX_VALUE).addFlattenedComponents(true, cumulation);
            }
            // 将in直接累加到组合缓冲区
            composite.addFlattenedComponents(true, in);
            in = null;
            return composite;
        } finally {
            if (in != null) {// 引用数-1
                // We must release if the ownership was not transferred as otherwise it may produce a leak
                in.release();
                // Also release any new buffer allocated if we're not returning it
                if (composite != null && composite != cumulation) {
                    composite.release();
                }
            }
        }
    }
};
```



#### channelRead

EventLoop解析读事件并将内容读取到ByteBuf后发布pipeline#fireChannelRead事件。ByteToMessageDecoder作为一个handler在其channelRead()方法处理3件事情：

- 首先把ByteBuf**消息进行累加**
- 然后调用解码方法**解码消息**，解码方法由子类实现
- 若解码出消息，则将解码的每条消息向后**发布pipeline#fireChannelRead事件**

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    selfFiredChannelRead = true;
    // 存放解码后的消息集合
    CodecOutputList out = CodecOutputList.newInstance();
    try {
        // 1.使用累加器对缓冲区进行累加
        first = cumulation == null;
        cumulation = cumulator.cumulate(ctx.alloc(), first ? Unpooled.EMPTY_BUFFER : cumulation, (ByteBuf) msg);
        // 2.执行解码，将调用decode()方法解码消息
        callDecode(ctx, cumulation, out);
    } finally {
        // 累加缓冲区中数据已读取完成，释放缓冲区
        if (cumulation != null && !cumulation.isReadable()) {
            numReads = 0;
            cumulation.release();
            cumulation = null;
        } else if (++numReads >= discardAfterReads) {
            // 读取次数打到阈值，释放已读取缓冲区，避免OOM
            numReads = 0;
            discardSomeReadBytes();
        }

        // 3.将集合内每个解码消息向pipeline发布 fireChannelRead事件
        int size = out.size();
        firedChannelRead |= out.insertSinceRecycled();
        fireChannelRead(ctx, out, size);
    }
}

// 从累加缓冲区中解码消息，由子类实现
protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;
```

### 消息帧解码器

ByteToMessageDecoder将ByteBuf对象转换为其它/ByteBuf对象，一般需要处理TCP的粘包与拆包。

![ByteToMessageDecoder](netty.assets/ByteToMessageDecoder.png)

这4种消息帧解码器基本能解决绝大部分用户场景的拆解包问题，其中`LengthFieldBasedFrameDecoder`使用最多。

其中固定长度是指定长度字段帧解码器的一种特例，行分割又是自定义特定分隔符帧解码器的一种特例。

另外3个消息帧解码器实现比较简单，直接看源码即可，这里不再分析。

下面就LengthFieldBaseFrameDecoder具体分析。

### LengthFieldBasedFrameDecoder

资料：https://www.jianshu.com/p/a0a51fd79f62

在目前自定义RPC中，一般都会有长度字段指定消息体或整个消息的长度，由netty提供的通用消息帧解码器`LengthFieldBasedFrameDecoder`将根据消息中指定字段的长度解码消息，非常适合自定义消息格式的场景。

它有5个参数：

- `lengthFieldOffset`：长度字段偏移量，即消息中第几个字节开始为长度字段。一般来说自定义协议开头有魔数、版本号等信息，然后才会是长度字段。
- `lengthFieldLength`：长度字段长度，只支持1/2/3/4/8.
- `lengthAdjustment`：长度修正，默认0。比如长度字段偏移量为0，消息内容长12B，若长度字段设置为(14B=长度字段长度2B+12B消息内容，即长度字段是整个消息的长度)，此时需设置长度修正值为-2，以正确调整剩余消息长度。
- `initailBytesToStrip`：舍弃开头的几个字节，默认0。一般将长度字段放于消息开头，可以配置此参数将传递给业务的内容舍弃长度字段
- `maxFrameLength`：消息帧最大长度，超出将抛出异常

接下来就分析它的decode方法如何解析消息帧：

```java
@Override
protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    Object decoded = decode(ctx, in);
    if (decoded != null) {
        out.add(decoded);// 解析出的消息帧放入
    }
}
```

从上面解析的累加器可知，这个`ByteBuf in`就是累加缓冲区

```java
// 从累加缓冲区中切片出消息帧
protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
    long frameLength = 0;
    if (frameLengthInt == -1) { // new frame
        // 是否处于discard模式，将丢弃此超过长度限制的帧内容
        if (discardingTooLongFrame) {
            discardingTooLongFrame(in);
        }

        // 长度字段还未出现，稍后等到下个ByteBuf到来再解析
        if (in.readableBytes() < lengthFieldEndOffset) {
            return null;
        }

        // 1.获取长度字段的内容，即消息帧长度
        int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
        frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder);

        // 2.消息帧长度矫正
        frameLength += lengthAdjustment + lengthFieldEndOffset;
        
        // 3.如果帧长度超过帧最大限制，进入discard模式，丢弃此帧内容重新解析下一帧
        if (frameLength > maxFrameLength) {
            exceededFrameLength(in, frameLength);
            return null;
        }
        // 解析出的帧长度
        frameLengthInt = (int) frameLength;
    }
    // 消息内容不足一个帧，等待下个ByteBuf到来再解析
    if (in.readableBytes() < frameLengthInt) {
        return null;
    }

    // 跳过指定数量的初始字节
    in.skipBytes(initialBytesToStrip);

    // extract frame
    int readerIndex = in.readerIndex();
    int actualFrameLength = frameLengthInt - initialBytesToStrip;
    // 4.从累加缓冲区ByteBuf中切片即可
    ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
    in.readerIndex(readerIndex + actualFrameLength);// 跳帧累加缓冲区读指针
    frameLengthInt = -1;// 重置为-1
    return frame;
}
// 帧长度超过限制时进入discard模式
private void exceededFrameLength(ByteBuf in, long frameLength) {
    long discard = frameLength - in.readableBytes();
    tooLongFrameLength = frameLength;

    if (discard < 0) {// 缓冲区内已有此帧，丢弃指定长度字节，即丢弃此帧
        in.skipBytes((int) frameLength);
    } else {// 进入discard模式直接完全丢弃此帧
        discardingTooLongFrame = true;
        bytesToDiscard = discard;
        in.skipBytes(in.readableBytes());
    }
    failIfNecessary(true);
}
```

## netty-handler

netty的handler包下提供了一些常用的handler供用户使用：

![netty-handler](netty.assets/netty-handler.png)

### 流量整形

流量整形traffic shaping：是一种**主动调整流量输出速率**的措施，**即带宽限制**：

- 防止上下游网元性能不均衡导致下游网元被压垮
- 防止由于通信模块接受消息过快，**后端业务线程处理不及时导致消息积压**

**流量整形原理**：对每次读取/写入的ByteBuf计算字节，计算当前报文流量，与流量整形阈值比对，超过阈值则计算等待时间

- 读流量超读阈值则**在该时间段内关闭channel的自动读取**。
- 写流量超写阈值则**将消息包装为定时任务，在该等待时间后再放入发送队列**

> 流量整形与流控的最大区别：流控会拒绝消息，**流量整形不会拒绝和丢弃消息**，以恒定速率下发消息，类似于变压器。

`TrafficCounter`：此类实现处理程序所需的计数器。可以访问它以获取一些额外的信息，例如自上次检查以来的读取或写入字节数、自上次检查以来的读取和写入带宽。

要激活或停用统计信息，您可以将延迟调整为低值（出于效率原因建议不少于 200 毫秒）或高值（让假设以毫秒为单位的 24H 足够大而不会出现问题）或者甚至使用0，这意味着不会进行任何计算。如果您想对这些统计数据做任何事情，只需覆盖doAccounting方法。

`AbstractTrafficShapingHandler`：这个抽象类实现了流量整形的内核。它可以扩展以满足您的需求。

`ChannelTrafficShapingHandler`：Channel流量整形，Channel的带宽限制

`GlobalTrafficShapingHandler`：全局流量整形，带宽的全局限制

`GlobalChannelTrafficShapingHandler`：全局和Channel的流量整形，带宽的全局限制

#### 流量统计

先看看AbstractTrafficShapingHandler：

```java
/**
 * 使用TrafficCounter监视几乎实时带宽，该监视器在每个checkInterval回调此程序的doAccounting()方法
 */
public abstract class AbstractTrafficShapingHandler extends ChannelDuplexHandler {
    // Traffic Counter
    protected TrafficCounter trafficCounter;

    // 写流速限制
    private volatile long writeLimit;
    // 读流速限制
    private volatile long readLimit;

    // 最大等待时间, 默认15s
    protected volatile long maxTime = DEFAULT_MAX_TIME;
    // 检查间隔，默认1s
    protected volatile long checkInterval = DEFAULT_CHECK_INTERVAL;

    // 停止写入的最大写时延, 默认4s
    volatile long maxWriteDelay = 4 * DEFAULT_CHECK_INTERVAL;
    // 停止写入的消息存储的最大缓存大小, 默认4MB
    volatile long maxWriteSize = DEFAULT_MAX_SIZE;

    /**
     * 在 UserDefinedWritability 中排名（通道为 1，全局流量整形处理程序为 2）。
     * 在最终构造函数中设置。必须介于 1 和 31 之间
     * 说明Channel流量整形优先级高于全局流量整形
     */
    final int userDefinedWritabilityIndex;
    // Channel UserDefinedWritability 索引的默认值
    static final int CHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 1;
    // 全局 UserDefinedWritability 索引的默认值
    static final int GLOBAL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 2;
    // GlobalChannel UserDefinedWritability 索引的默认值
    static final int GLOBALCHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 3;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        // 1.计算消息字节数，即流量
        long size = calculateSize(msg);
        long now = TrafficCounter.milliSecondFromNano();
        if (size > 0) {
            // 2.根据消息大小(即流量)和给定的读速度限制readLimit计算等待时间
            long wait = trafficCounter.readTimeToWait(size, readLimit, maxTime, now);
            wait = checkWaitReadTime(ctx, wait, now);// 如果超过最大等待时间则设为最大等待时间15s
            // 3.如果等待时间大于最小值10ms，则暂时关闭channel的自动读取并设置定时任务在计算的等待时间之后再打开
            if (wait >= MINIMAL_WAIT) { // At least 10ms seems a minimal
                Channel channel = ctx.channel();
                ChannelConfig config = channel.config();
                // 如果是自动读取则暂时关闭并添加定时任务来恢复自动读取
                if (config.isAutoRead() && isHandlerActive(ctx)) {
                    config.setAutoRead(false);
                    channel.attr(READ_SUSPENDED).set(true);
                    Attribute<Runnable> attr = channel.attr(REOPEN_TASK);
                    Runnable reopenTask = attr.get();
                    if (reopenTask == null) {
                        reopenTask = new ReopenReadTimerTask(ctx);
                        attr.set(reopenTask);
                    }
                    ctx.executor().schedule(reopenTask, wait, TimeUnit.MILLISECONDS);
                }
            }
        }
        // 4.通知处理完成, 由子类实现
        informReadOperation(ctx, now);
        // 5. 将消息向后传递
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
        throws Exception {
        // 1.计算待发送消息字节数，即流量
        long size = calculateSize(msg);
        long now = TrafficCounter.milliSecondFromNano();
        if (size > 0) {
            // 2.根据消息大小(即流量)和给定写速度限制writeLimit计算等待时间
            long wait = trafficCounter.writeTimeToWait(size, writeLimit, maxTime, now);
            if (wait >= MINIMAL_WAIT) {
                // 3.消息延时发送
                submitWrite(ctx, msg, size, wait, now, promise);
                return;
            }
        }
        // 4.立刻发送消息
        submitWrite(ctx, msg, size, 0, now, promise);
    }
	// 由子类实现消息的延时发送或立刻发送
    abstract void submitWrite(
        ChannelHandlerContext ctx, Object msg, long size, long delay, long now, ChannelPromise promise);
}
```

`AbstractTrafficShapingHandler`计算读消息流量并通过关闭Channel的自动读取实现了读流量整形，计算写流量并由子类实现消息的延时或立刻发送，进而实现写流量整形。

#### 全局流量整形

`GlobalTrafficShapingHandler`全局流量整形在继承了AbstractTrafficShapingHandler后仅仅需要实现`submitWrite()`方法以实现写流量整形即可实现全局的带宽限制。

```java
@Sharable // 此handler是共享的，注意其添加到pipeline的方式
public class GlobalTrafficShapingHandler extends AbstractTrafficShapingHandler {
    // channel hashcode-->暂存队列
    private final ConcurrentMap<Integer, PerChannel> channelQueues = PlatformDependent.newConcurrentHashMap();

    long maxGlobalWriteSize = DEFAULT_MAX_SIZE * 100; // default 400MB

    private static final class PerChannel {
        ArrayDeque<ToSend> messagesQueue;
        long queueSize;
        long lastWriteTimestamp;
        long lastReadTimestamp;
    }

    @Override
    void submitWrite(final ChannelHandlerContext ctx, final Object msg,
                     final long size, final long writedelay, final long now,
                     final ChannelPromise promise) {
        // 1.根据Channel的hashcode找到其消息暂存队列
        Channel channel = ctx.channel();
        Integer key = channel.hashCode();
        PerChannel perChannel = channelQueues.get(key);
        if (perChannel == null) {
            // in case write occurs before handlerAdded is raised for this handler
            // imply a synchronized only if needed
            perChannel = getOrSetPerChannel(ctx);
        }
        final ToSend newToSend;
        long delay = writedelay;
        boolean globalSizeExceeded = false;
        // write operations need synchronization
        synchronized (perChannel) {
            // 2.写消息延时为0，即立刻写入
            if (writedelay == 0 && perChannel.messagesQueue.isEmpty()) {
                trafficCounter.bytesRealWriteFlowControl(size);
                ctx.write(msg, promise);
                perChannel.lastWriteTimestamp = now;
                return;
            }
            // 3.将消息暂存到队列并以定时任务延时发送
            if (delay > maxTime && now + delay - perChannel.lastWriteTimestamp > maxTime) {
                delay = maxTime;
            }
            newToSend = new ToSend(delay + now, msg, size, promise);
            perChannel.messagesQueue.addLast(newToSend);
            perChannel.queueSize += size;
            queuesSize.addAndGet(size);
            checkWriteSuspend(ctx, delay, perChannel.queueSize);
            if (queuesSize.get() > maxGlobalWriteSize) {
                globalSizeExceeded = true;
            }
        }
        if (globalSizeExceeded) {
            setUserDefinedWritability(ctx, false);
        }
        final long futureNow = newToSend.relativeTimeAction;
        final PerChannel forSchedule = perChannel;
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                sendAllValid(ctx, forSchedule, futureNow);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    // 将通道内延时结束的消息发送
    private void sendAllValid(final ChannelHandlerContext ctx, final PerChannel perChannel, final long now) {
        // write operations need synchronization
        synchronized (perChannel) {
            ToSend newToSend = perChannel.messagesQueue.pollFirst();
            for (; newToSend != null; newToSend = perChannel.messagesQueue.pollFirst()) {
                if (newToSend.relativeTimeAction <= now) {
                    long size = newToSend.size;
                    trafficCounter.bytesRealWriteFlowControl(size);
                    perChannel.queueSize -= size;
                    queuesSize.addAndGet(-size);
                    ctx.write(newToSend.toSend, newToSend.promise);
                    perChannel.lastWriteTimestamp = now;
                } else {
                    perChannel.messagesQueue.addFirst(newToSend);
                    break;
                }
            }
            if (perChannel.messagesQueue.isEmpty()) {
                releaseWriteSuspended(ctx);
            }
        }
        ctx.flush();
    }
}
```

#### 连接流量整形

ChannelTrafficShapingHandler实现了每个Channel的流量整形，继承自AbstractTrafficShapingHandler，仅仅需要重写`submitWrite()`方法以实现写流量整形即可实现每个连接的带宽限制。

```java
public class ChannelTrafficShapingHandler extends AbstractTrafficShapingHandler {
    // 此Channel的暂存队列
    private final ArrayDeque<ToSend> messagesQueue = new ArrayDeque<ToSend>();
    private long queueSize;
    @Override
    void submitWrite(final ChannelHandlerContext ctx, final Object msg,
                     final long size, final long delay, final long now,
                     final ChannelPromise promise) {
        final ToSend newToSend;
        // write order control
        synchronized (this) {
            // 1.消息延时为0则立即发送
            if (delay == 0 && messagesQueue.isEmpty()) {
                trafficCounter.bytesRealWriteFlowControl(size);
                ctx.write(msg, promise);
                return;
            }
            // 2.将消息暂存到队列中
            newToSend = new ToSend(delay + now, msg, promise);
            messagesQueue.addLast(newToSend);
            queueSize += size;
            checkWriteSuspend(ctx, delay, queueSize);
        }
        // 3.定时任务发送已经到期的消息
        final long futureNow = newToSend.relativeTimeAction;
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                sendAllValid(ctx, futureNow);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    // 发送到期的消息
    private void sendAllValid(final ChannelHandlerContext ctx, final long now) {
        // write order control
        synchronized (this) {
            ToSend newToSend = messagesQueue.pollFirst();
            for (; newToSend != null; newToSend = messagesQueue.pollFirst()) {
                if (newToSend.relativeTimeAction <= now) {
                    long size = calculateSize(newToSend.toSend);
                    trafficCounter.bytesRealWriteFlowControl(size);
                    queueSize -= size;
                    ctx.write(newToSend.toSend, newToSend.promise);
                } else {
                    messagesQueue.addFirst(newToSend);
                    break;
                }
            }
            if (messagesQueue.isEmpty()) {
                releaseWriteSuspended(ctx);
            }
        }
        ctx.flush();
    }
}
```























### 超时控制

![timeout](netty.assets/timeout.png)

超时控制有 3 个 ChannelHandler 实现类：

- `IdleStateHandler` ，当Channel**读空闲或写空闲超时，即链路空闲超时，将触发`IdleStateEvent`事件**。
  用户可自定义ChannelInboundHandler实现类并重写`userEventTriggered()`方法，处理该事件。

- `ReadTimeoutHandler`，继承 IdleStateHandler 类，Channel 的**读空闲超时**，抛出 ReadTimeoutException 异常，并自动关闭Channel 。
  用户可自定义ChannelInboundHandler实现类并重写`exceptionCaught()`方法，处理该异常。

- `WriteTimeoutHandler`，当**写超时**，抛出WriteTimeoutException异常，并自动关闭Channel 。
  用户可自定义ChannelInboundHandler实现类并重写 `#exceptionCaught()` 方法，处理该异常。

例子：当链路超过30秒没有发送数据时则发送心跳；当链路超过60秒没有读到数据时则关闭连接：

```java
// 创建Bootstrap客户端启动器
Bootstrap b = new Bootstrap();
b.group(workerGroup)
    .channel(NioSocketChannel.class)    // 设置Channel类型
    .option(ChannelOption.TCP_NODELAY, true)    // 关闭TCP延迟
    .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            // 链路空闲超时控制
            p.addLast(new IdleStateHandler(60, 30, 0));
            /*
             * 消息帧最大100MB
             * 长度字段偏移量为0，4B
             * 无需长度修正，无需舍弃开头的长度字段
             */
            LengthFieldBasedFrameDecoder frameDecoder = new LengthFieldBasedFrameDecoder(
                1024 * 1024 * 100, 0, 4, 0, 0);
            p.addLast(frameDecoder);// TCP拆包解包
            p.addLast(new ServerHandler());// 添加自定义处理器
        }
    });

// Handler should handle the IdleStateEvent triggered by IdleStateHandler.
public class MyHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new PingMessage());
            }
        }
    }
}
```

> 注意：超时检测Handler最好放在第一个，否则消息很可能不会传递到超时Handler，这将意外触发空闲超时。

#### 链路空闲超时handler

`IdleStateHandler`可用于：

- 单独检测读空闲空时
- 单独检测写空闲超时
- 检测读写空闲超时，即链路空闲超时

检测到超时将触发IdleStateEvent事件，用户可自定义拦截该事件并判断是读空闲或写空闲超时。

其实现很简单：

- EventLoop中有定时任务队列scheduledTaskQueue，此handler会以**定时任务检测读空闲或写空闲时间**
- 重写`channelRead()`入站事件方法和`channelReadComplete()`入站事件方法**记录lastReadTime**
- 重写`write()`出站事件方法向pipeline中添加listener的方式在其将消息暂存到发送队列后**记录lastWriteTime**

如果配置IdleStateHandler为检测链路空闲，其定时任务如下：

```java
private final class AllIdleTimeoutTask extends AbstractIdleTask {
    protected void run(ChannelHandlerContext ctx) {
        // 链路空闲超时时间
        long nextDelay = allIdleTimeNanos;
        // 1.如果不是正在处理消息则计算是否空闲超时
        if (!reading) {
            nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
        }
        // 2.空闲超时了
        if (nextDelay <= 0) {
            // 2.1 调度下次空闲检测定时任务
            allIdleTimeout = schedule(ctx, this, allIdleTimeNanos, TimeUnit.NANOSECONDS);
            // 是否为第一次超时
            boolean first = firstAllIdleEvent;
            firstAllIdleEvent = false;
            // 2.2 发布链路空闲超时事件
            IdleStateEvent event = newIdleStateEvent(IdleState.ALL_IDLE, first);
            channelIdle(ctx, event);
        } else {
            // 3.未超时则继续调度定时任务等待下次检测
            allIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
        }
    }
}

// 空闲超时发布事件
protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
    ctx.fireUserEventTriggered(evt);
}
```

如果配置IdleStateHandler为单独检测读空闲超时或写空闲超时，其定时任务和上面这个大同小异。

用户需要自定义handler重写`userEventTriggered()`方法，处理该事件。

> 注意：出现了超时事件，IdlestateHandler不会关闭channel，若有需要，用户可自己处理超时事件后再关闭channel。

#### 读空闲超时handler

ReadTimeoutHandler继承自IdleStateHandler，IdleStateHandler在检测到读空闲超时将回调channelIdle()方法发布超时事件，而ReadTimeoutHandler重写了`channelIdle()`方法，向pipeline发布读超时异常：

```java
public class ReadTimeoutHandler extends IdleStateHandler {
    private boolean closed;// 防止重入

    @Override
    protected final void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        assert evt.state() == IdleState.READER_IDLE;
        readTimedOut(ctx);
    }
	// 向pipeline发布超时异常并关闭channel
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
        if (!closed) {
            ctx.fireExceptionCaught(ReadTimeoutException.INSTANCE);
            ctx.close();
            closed = true;
        }
    }
}
```

用户需要自定义handler重写`exceptionCaught()`方法处理该异常。

> 注意：它关闭了Channel，而IDLEStateHandler默认不会关闭Channel。

#### 写超时handler

IdleStateHandler检测的写空闲超时，而WriteTimeoutHandler检测的是写超时，不一样的。

它的实现也很简单，定时任务+ChannelPromise：将定时任务同时添加到定时任务队列和Channelpromise的监听器，若定时任务先调度则写超时，发布写超时异常，若监听器先调度说明消息已经flush成功，取消该定时任务。

```java
@Override
public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (timeoutNanos > 0) {
        promise = promise.unvoid();
        scheduleTimeout(ctx, promise);
    }
    ctx.write(msg, promise);
}

private void scheduleTimeout(final ChannelHandlerContext ctx, final ChannelPromise promise) {
    // 添加定时任务
    final WriteTimeoutTask task = new WriteTimeoutTask(ctx, promise);
    task.scheduledFuture = ctx.executor().schedule(task, timeoutNanos, TimeUnit.NANOSECONDS);
    // 任务还没被调度完成, 理论上不至于这么快就调度完成吧?
    if (!task.scheduledFuture.isDone()) {
        // 加入链表末尾
        addWriteTimeoutTask(task);
        // 将检测超时任务作为监听器添加到该promise，若其消息发送完成, 将取消此检测超时任务
        promise.addListener(task);
    }
}

private final class WriteTimeoutTask implements Runnable, ChannelFutureListener {

    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;

    // 为什么要将定时任务另外维护一个双向链表呢???
    WriteTimeoutTask prev;
    WriteTimeoutTask next;

    Future<?> scheduledFuture;

    @Override
    public void run() {
        // 定时任务调度了，且消息未处理完则发布异常并关闭channel
        if (!promise.isDone()) {
            writeTimedOut(ctx);
        }
        removeWriteTimeoutTask(this);// 从链表中移除此任务
    }

    // promise完成消息发送将回调监听器的这个方法
    public void operationComplete(ChannelFuture future) throws Exception {
        // 取消定时任务
        scheduledFuture.cancel(false);
        removeWriteTimeoutTask(this);// 从链表移除
    }
}

// 发布写超时异常并关闭channel
protected void writeTimedOut(ChannelHandlerContext ctx) throws Exception {
    if (!closed) {
        ctx.fireExceptionCaught(WriteTimeoutException.INSTANCE);
        ctx.close();
        closed = true;
    }
}
```

为什么要维护这个双向链表???

> 警告：出现写超时会自动关闭channel并释放发送队列，**它不会把这些未发送完成的消息返给用户，如果这些消息非常重要，一定要提前保存在其它地方，发送成功则标记已处理，否则定时重发。**

# Bytebuf

Java nio的ByteBuffer的缺点：

- ByteBuffer分配时长度固定，不能动态扩容。
- ByteBuffer只有1个位置指针position，读写模式切换需手动调用flip()和rewind()进行翻转，使用必须小心谨慎。
- ByteBuffer的功能API有限，如不支持组合。

所以Netty自己实现了一个ByteBuf

`io.netty.buffer.ByteBuf`位于buffer模块，功能定位上和JavaNio的ByteBuffer一样，并提供了额外优点：

- 它可以被用户自定义的**缓冲区类型**扩展
- 通过内置的符合缓冲区类型实现了透明的**零拷贝**
- **动态扩容**
- 有2个**不同的读/写位置指针**，readIndex和writeIndex，读写模式切换无须像JavaNio手动切换
- 支持方法的**链式调用**
- 支持**引用计数**
- 支持**池化**

```java
/**
 * 建议使用Unpooled中的辅助方法创建一个新缓冲区，而不是调用单个实现的构造函数
 *
 * ByteBuf提供了两个指针变量来支持顺序读取和写入操作——分别用于读取操作的readerIndex和用于写入操作的writerIndex 。下图显示了缓冲区如何通过两个指针分割成三个区域：
 * <pre>
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      |                   |     (CONTENT)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 *
 * 如果一个ByteBuf可以转换成一个 NIO ByteBuffer共享它的内容（即视图缓冲区），你可以通过nioBuffer()方法得到它。
 * 要确定缓冲区是否可以转换为 NIO 缓冲区，请使用nioBufferCount() 。
 */
public abstract class ByteBuf implements ReferenceCounted, Comparable<ByteBuf>, ByteBufConvertible {

    /**
     * 丢弃索引0和readerIndex之间的字节。
     * 它将readerIndex和writerIndex之间的字节移动到索引0，并将readerIndex和writerIndex分别设置为0和oldWriterIndex - oldReaderIndex 。
     * 
     * <pre>
 	 *  BEFORE discardReadBytes()
     *
     *      +-------------------+------------------+------------------+
     *      | discardable bytes |  readable bytes  |  writable bytes  |
     *      +-------------------+------------------+------------------+
     *      |                   |                  |                  |
     *      0      <=      readerIndex   <=   writerIndex    <=    capacity
     *
     *
     *  AFTER discardReadBytes()
     *
     *      +------------------+--------------------------------------+
     *      |  readable bytes  |    writable bytes (got more space)   |
     *      +------------------+--------------------------------------+
     *      |                  |                                      |
     * readerIndex (0) <= writerIndex (decreased)        <=        capacity
     * </pre>
     */
    public abstract ByteBuf discardReadBytes();

    /**
     * 将此缓冲区的readerIndex和writerIndex设置为0 。此方法与setIndex(0, 0)相同。
     * 请注意，此方法的行为与 NIO 缓冲区的行为不同，后者将limit设置为缓冲区的capacity 。
     * <pre>
     *  AFTER clear()
     *
     *      +---------------------------------------------------------+
     *      |             writable bytes (got more space)             |
     *      +---------------------------------------------------------+
     *      |                                                         |
     *      0 = readerIndex = writerIndex            <=            capacity
     * </pre>
     */
    public abstract ByteBuf clear();
}
```

1、ByteBuf提供的读写方法较多，总结有4类：

- `#getXxx(index)`方法，读取指定位置数据，不改变`readerIndex`索引
- `#readXxx()`方法，读取`readerIndex`位置数据，改变`readerIndex`索引
- `#setXxx(index,value)`方法，写入数据到指定位置，不改变`writeIndex`索引
- `#writeXxx(value)`方法，写入数据到`writeIndex`位置，会改变`writeIndex`索引

2、释放/清除操作：

`#discardSomeReadBytes()`方法，将可读段数据移动到废弃段，解析见上面

`#clear()`清空各个段，直接重置`readerIndex`和`writeIndex`为0

3、视图/拷贝操作：

```java
// 返回此缓冲区的可读字节的拷贝副本，独立的
public abstract ByteBuf copy(int index, int length);

/** 
 * 返回此缓冲区子区域的切片，共享同一个缓冲数组，持有单独的索引和标记
 * 注意：此方法不会调用retain()，因此不会增加引用计数
 */
public abstract ByteBuf slice(int index, int length);
```

4、转换为Java Nio ByteBuffer操作：

```java
// ByteBuf 包含 ByteBuffer 数量。
// 如果返回 = 1 ，则调用 `#nioBuffer()` 方法，获得 ByteBuf 包含的 ByteBuffer 对象。
// 如果返回 > 1 ，则调用 `#nioBuffers()` 方法，获得 ByteBuf 包含的 ByteBuffer 数组。
public abstract int nioBufferCount();

// 将此缓冲区的子区域公开为 NIO ByteBuffer
public abstract ByteBuffer nioBuffer(int index, int length);
// 为什么会产生数组的情况呢？例如 CompositeByteBuf 
public abstract ByteBuffer[] nioBuffers(int index, int length);
```

5、引用计数相关：继承自`ReferenceCounted`接口

```java
public interface ReferenceCounted {
    // 此对象的引用计数
    int refCnt();

    // 引用数+1
    ReferenceCounted retain();

    /**
     * 记录此对象的当前访问位置以及用于调试目的的附加任意信息。
     * 如果确定该对象被泄露，该操作记录的信息将通过ResourceLeakDetector提供给您
     */
    ReferenceCounted touch(Object hint);

    // 引用数-1
    boolean release();
}
```

## AbstractByteBuf

![ByteBuf实现类](netty.assets/ByteBuf实现类.png)

```java
public abstract class AbstractByteBuf extends ByteBuf {
    int readerIndex;// 读指针
    int writerIndex;// 写指针
    private int markedReaderIndex;// 读标记指针
    private int markedWriterIndex;// 写标记指针
    private int maxCapacity;// 最大容量
}
```

- `capacity` 属性，在 AbstractByteBuf 未定义，而是由子类来实现。为什么呢？ByteBuf 根据**内存类型**分成 Heap 和 Direct ，它们获取 `capacity` 的值的方式不同。

AbstractByteBuf子类非常多，总结起来是3种组合8个核心子类：

![ByteBuf核心子类](netty.assets/ByteBuf核心子类.png)

上面8个类从名字来看由以下3个维度进行正交组合：

- 按**内存类型**分类：
  - **堆内存**(HeapByteBuf)：字节数组分配在JVM堆，分配回收速度快，受GC管理影响，写入或从Socket缓冲区读数据会先把数据拷贝到直接内存，再从直接内存拷贝到堆内存(此过程没有safepoint，以避免出现GC)。
  - **直接内存**(DirectByteBuf)：堆外内存，写入或从Socket缓冲区读数据，相比于堆内存少1次内存拷贝，IO效率高一点
- 按**对象池**分类：
  - 对象池(PooledByteBuf)：基于**对象池技术**可以重用ByteBuf，减少对象创建和回收，减少GC次数
  - 不使用对象池(UnpooledByteBuf)：在不需要大量创建缓冲区对象时才建议使用

- 按照**Unsafe**分类：
  - 使用Unsafe：基于jdk的`sun.misc.Unsafe.Unsafe`的API，直接访问内存数据
  - 不使用该API，基于Java的标准API进行数据访问。
  - 关于 Unsafe ，JVM 大佬 R 大在知乎上有个回答：[《为什么 JUC 中大量使用了 sun.misc.Unsafe 这个类，但官方却不建议开发者使用？》](https://www.zhihu.com/question/29266773) 。关于为什么 Unsafe 的性能会更好：”其中一种是嫌 Java 性能不够好，例如说数组访问的边界检查语义，嫌这个开销太大，觉得用 Unsafe 会更快；”。

默认情况下使用的是`PooledUnsafeDirectByteBuf`。

最佳实践：在I/O通信时使用DirectByteBuf，在业务编解码模块中使用HeapByteBuf。

## 内存泄漏检测

暂未深入分析：http://svip.iocoder.cn/Netty/ByteBuf-1-3-ByteBuf-resource-leak-detector

从上面那张图可知，所有ByteBuf实现类都继承了AbstractReferenceCountedByteBuf以实现引用计数，主要是用于直接内存的ByteBuf的安全释放，以及内存池分配的ByteBuf的回收，(Unpooled堆内存ByteBuf由GC释放)。

```java
public abstract class AbstractReferenceCountedByteBuf extends AbstractByteBuf {
    // 标识refCnt字段的内存地址
    private static final long REFCNT_FIELD_OFFSET =
        ReferenceCountUpdater.getUnsafeOffset(AbstractReferenceCountedByteBuf.class, "refCnt");
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> AIF_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCountedByteBuf.class, "refCnt");
	// 原子更新器，对refCnt字段原子更新以实现线程安全
    private static final ReferenceCountUpdater<AbstractReferenceCountedByteBuf> updater =
        new ReferenceCountUpdater<AbstractReferenceCountedByteBuf>() {
        @Override
        protected AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> updater() {
            return AIF_UPDATER;
        }
        @Override
        protected long unsafeOffset() {
            return REFCNT_FIELD_OFFSET;
        }
    };
	/**
     * 对其所有访问必须通过更新器updater
     * 引用计数初始值为1，申请增加，释放减少，若回到1则调用deallocate()回收对象内存
     */
    private volatile int refCnt;

    protected AbstractReferenceCountedByteBuf(int maxCapacity) {
        super(maxCapacity);
        updater.setInitialValue(this);// 设置引用值为2，因为初始值为1，构造函数引用为1
    }

    // 增加引用计数
    public ByteBuf retain(int increment) {
        return updater.retain(this, increment);
    }

    // 记录此对象的当前访问位置以及用于调试目的的附加任意信息。
    // 如果确定该对象被泄露，该操作记录的信息将通过ResourceLeakDetector提供给您。
    public ByteBuf touch(Object hint) {
        return this;
    }

    // 减少引用计数
    public boolean release(int decrement) {
        boolean result=updater.release(this, decrement);
        if (result) deallocate();
        return result;
    }

    // 释放内存对象，由不同子类实现
    protected abstract void deallocate();
}
```

释放内存方法由各个子类自行实现，如`UnpooledDirectByteBuf`会调用`PlatformDependent.freeDirectBuffer(ByteBuffer)`方法释放直接内存。而`UnpooledHeapByteBuf`就简单将byte[]数组字段设置为null即可。

对于实现了内存池的PooledByteBuf抽象类及其子类，其deallocate()方法将释放内存回到Arena中并回收对象。

## CompositeByteBuf

CompositeByteBuf将多个ByteBuf实例组装在一起，对外提供统一视图。

它在解析协议场景下很有用，如消息头和消息体分别以ByteBuf对象封装，若要将整个消息整合在一起，用jdk的nio ByteBuffer则只能复制拷贝新的ByteBuffer或以List数组等方式组合。但Netty通过CompositeByteBuf提供统一展示和处理。

```java
// 将多个缓冲区显示为单个合并缓冲区的虚拟缓冲区
// 建议使用ByteBufAllocator.compositeBuffer()或Unpooled.wrappedBuffer(ByteBuf...)
// 而不是显式调用构造函数
public class CompositeByteBuf extends AbstractReferenceCountedByteBuf implements Iterable<ByteBuf> {
    private final ByteBufAllocator alloc;
    private final boolean direct;
    private final int maxNumComponents;

    private int componentCount;
    private Component[] components; // resized when needed

    private boolean freed;
}
```

它定义了一个Component数组，Component实际是对ByteBuf的封装，聚合了ByteBuf对象，维护其位置偏移量信息：

```java
 private static final class Component {
        final ByteBuf srcBuf; // the originally added buffer
        final ByteBuf buf; // srcBuf unwrapped zero or more times

        int srcAdjustment; // index of the start of this CompositeByteBuf relative to srcBuf
        int adjustment; // index of the start of this CompositeByteBuf relative to buf

        int offset; // offset of this component within this CompositeByteBuf
        int endOffset; // end offset of this component within this CompositeByteBuf

        private ByteBuf slice; // cached slice, may be null
 }
```

# 内存管理

资料：https://juejin.cn/post/7051200855415980069；http://svip.iocoder.cn/Netty/ByteBuf-3-2-Jemalloc-chunk/

Netty为何要实现内存管理？**Java的GC只能对堆内存进行管理，直接内存需要手动管理，需要有一个高效的内存管理算法，高效分配内存且仅有较小的内存碎片。**

Netty 4.x增加了Pooled Buffer，实现了高性能buffer池，分配策略结合了buddy allocation和slab allocation的jemalloc变种，代码位于`io.netty.buffer.PoolArena`中。

借鉴 **`jemalloc4`** 用来解决两个问题：

- 多线程下的内存回收与分配
- 内存的碎片化问题(不断分配和回收过程中会产生，jemalloc4 进一步优化了内存碎片产生)

netty内存池的层级结构如下：(这是以前的旧版)

![netty内存池层级结构](netty.assets/netty内存池层级结构.png)

这是现在的Netty内存池层级图：

![netty内存池层级结构new](netty.assets/netty内存池层级结构new.png)

**Arena**表示一个内存区域，netty内存池由多个Arena组成，分配时每个线程按照轮询策略选择某个Arena进行内存分配。

## 内存分配器Allocator

一个简单的内存分配示例：

```java
public class MyExample {
    public static void main(String[] args) {
        // 传入true表示默认使用直接内存池
        PooledByteBufAllocator allocator = new PooledByteBufAllocator(true);

        ByteBuf buf = allocator.buffer(1 << 10, 1 << 11);
        buf.clear();

        ByteBuf buf2 = allocator.buffer(1 << 12, 1 << 13);
        buf2.clear();

        ByteBuf buf3 = allocator.buffer(1 << 14, 1 << 14);
        buf3.clear();

        ByteBuf buf4 = allocator.buffer(1 << 19, 1 << 19);
        buf4.clear();
    }
}
```

缓冲池内存分配器PooledByteBufAllocator：

```java
public class PooledByteBufAllocator extends AbstractByteBufAllocator implements ByteBufAllocatorMetricProvider {
    // 默认的堆内存Arena数量为CPU核心数*2
    private static final int DEFAULT_NUM_HEAP_ARENA;
    // 默认的直接内存Arena数量为CPU核心数*2
    private static final int DEFAULT_NUM_DIRECT_ARENA;
    private static final int DEFAULT_PAGE_SIZE;// Page默认8KB
    private static final int DEFAULT_MAX_ORDER;// 默认9，即8192 << 9 = 4 MiB per chunk

    // 默认Allocator是直接内存分配器
    public static final PooledByteBufAllocator DEFAULT =
        new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
    // 堆内存Arena数组
    private final PoolArena<byte[]>[] heapArenas;
    // 直接内存Arena数组
    private final PoolArena<ByteBuffer>[] directArenas;
    private final int chunkSize;// 默认4MB

    public PooledByteBufAllocator(
        boolean preferDirect, int nHeapArena, 
        int nDirectArena, int pageSize, 
        int maxOrder,int smallCacheSize, 
        int normalCacheSize, boolean useCacheForAllThreads, 
        int directMemoryCacheAlignment) {
        // 省略赋值操作

        // 1.计算Chunk大小，默认4MB
        chunkSize = validateAndCalculateChunkSize(pageSize, maxOrder);
        // 省略一堆检查

        // 2.计算pageShifts，默认13，即 2<<<13 == 8K
        int pageShifts = validateAndCalculatePageShifts(pageSize, directMemoryCacheAlignment);

        // 3.创建CPU核心数*2个堆内存PoolArena
        if (nHeapArena > 0) {
            heapArenas = new PoolArena[nHeapArena];
            for (int i = 0; i < heapArenas.length; i++) {
                heapArenas[i] = new PoolArena.HeapArena(/*省略参数*/);
            }
        } 
        // 4.创建CPU核心数*2个直接内存PoolArena
        if (nDirectArena > 0) {
            directArenas = new PoolArena[nDirectArena];
            for (int i = 0; i < directArenas.length; i++) {
                directArenas[i] =new PoolArena.DirectArena(/*省略参数*/);
            }
        }
    }
}
```

分配内存方法有`allocator#heapBuffer()`和`allocator#directBuffer()`，它们具体实现将调用`allocator#newHeapBuffer()`和`allocator#newDirectBuffer()`：

```java
// 创建满足指定大小的直接内存
protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
    // 1.获取此线程绑定的直接内存directPoolArena
    PoolThreadCache cache = threadCache.get();
    PoolArena<ByteBuffer> directArena = cache.directArena;
		
    // 2.分配内存块
    final ByteBuf buf = directArena.allocate(cache, initialCapacity, maxCapacity);
    return toLeakAwareBuffer(buf);
}
```

堆内存、直接内存分配都是以当前线程绑定的`PoolArena#allocate()`分配内存。

在Allocator分配器的构造函数中，创建了CPU核心数*2的堆内存和直接内存的PoolArena，**每个线程都将绑定一个PoolArena进行内存分配，可以避免线程竞争。**

## 内存规格对齐SizeClasses

Netty的内存对齐类为`io.netty.buffer.SizeClasses`，为Netty内存池中的内存块提供大小对齐，索引计算等服务方法。

**`4.1.72.Final`** 是 **`jemalloc4`** 的实现。**`jemalloc4`** 进一步优化了内存碎片的问题。jemalloc4 相较于 jemalloc3 最大的提升是进一步优化内存碎片问题，因为**在 jemalloc3 中使用伙伴分配算法最多可能会导致 50% 内存碎片**，但 jemalloc4 通过划分更细粒度的内存规格在一定程度上改善了这一问题，这也是 SizeClasses 的由来。

> Tips: [github.com/netty/netty…](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Fnetty%2Fnetty%2Fissues%2F3910) (Netty Issues) 这里说明了jemalloc4的提升`

`jemalloc4`内存规格划分：

![Netty内存块规格划分](netty.assets/Netty内存块规格划分.png)

> `jemalloc4`划分的更细粒度的内存规格产生的`内零头`要远小于伙伴分配算法，比如申请(32KB+1B)内存，在伙伴分配算法中将申请64KB内存，而在上图内存规格划分中可知需分配40KB内存块，内零头更小。

**`jemalloc4`** 取消了 **`Tiny`** 内存的规格。只保留了 **`small`** 、 **`normal`** 、 **`huge`** 三种规格。下面要分析的 **`SizeClasses`** 就是记录了 small和normal规格值的一张表。

```java
/**
 * Netty的内存对齐类，为Netty内存池中的内存块提供大小对齐，索引计算等服务方法。
 * <p>
 * LOG2_SIZE_CLASS_GROUP: 每次大小加倍时，size类计数的对数。值为：2
 * LOG2_MAX_LOOKUP_SIZE: Log of max size class in the lookup table.
 * sizeClasses: Complete table of [index, log2Group, log2Delta, nDelta, isMultiPageSize,
 * isSubPage, log2DeltaLookup] tuples.
 * index: 内存块size的索引
 * log2Group: 内存块分组
 * log2Delta: 增量大小的log2值.
 * nDelta: 增量乘数.
 * isMultiPageSize: 表示size是否为pageSize的整数.
 * isSubPage: 是否为subpage类型
 * log2DeltaLookup: Same as log2Delta if a lookup table size class, 'no'
 * otherwise. 
 * <p>
 * smallMaxSizeIdx: small规格内存的索引.
 * <p>
 * lookupMaxClass: Maximum size class included in lookup table.
 * log2NormalMinClass: Log of minimum normal size class.
 */
abstract class SizeClasses implements SizeClassesMetric {

    static final int LOG2_QUANTUM = 4;

    private static final int LOG2_SIZE_CLASS_GROUP = 2;
    private static final int LOG2_MAX_LOOKUP_SIZE = 12;


    protected final int pageSize;// 默认8KB
    protected final int pageShifts;// 默认13
    protected final int chunkSize;// 默认4MB
    protected final int directMemoryCacheAlignment;

    final int nSizes;// sizeClass个数.默认68
    final int nSubpages;// subPage sizeClass的数量.默认39
    final int nPSizes;// size为pageSize整数倍的sizeClass的个数.默认32
    final int lookupMaxSize;// 4096
    final int smallMaxSizeIdx;// Small区域最大size索引，默认38，指向28KB

    // 以下3个是查找表
    // 保存的是isMultiPageSize为true的列的size
    private final int[] pageIdx2sizeTab;
    // sizeIdx --> size的查找表
    // 从16B...4MB 共68个
    private final int[] sizeIdx2sizeTab;
    // size<=lookupMaxClass的查找表，可根据size快速查到sizeIdx，大于该值的只能计算得出sizeIdx了
    private final int[] size2idxTab;

    protected SizeClasses(int pageSize, int pageShifts, int chunkSize, int directMemoryCacheAlignment) {
        int group = log2(chunkSize) + 1 - LOG2_QUANTUM;// 22+1-4

        // 1.生成 size classes 共19<<2=76条
        //[index, log2Group, log2Delta, nDelta, isMultiPageSize, isSubPage, log2DeltaLookup]
        short[][] sizeClasses = new short[group << LOG2_SIZE_CLASS_GROUP][7];
        // 省略

        // 2.计算SizeClasses
        // 省略

        // 3.统计nPSize、nSubpages
        for (int idx = 0; idx < nSizes; idx++) {
            // 省略
        }

        // 4.生成3个查找表
        sizeIdx2sizeTab = newIdx2SizeTab(sizeClasses, nSizes, directMemoryCacheAlignment);
        pageIdx2sizeTab = newPageIdx2sizeTab(sizeClasses, nSizes, nPSizes, directMemoryCacheAlignment);
        size2idxTab = newSize2idxTab(lookupMaxSize, sizeClasses);
    }

    // 根据给定size向上对齐并查找到sizeIdx
    public int size2SizeIdx(int size) {
        // 省略检查
        // 根据查找表直接查
        if (size <= lookupMaxSize)
            //size-1 / MIN_TINY
            return size2idxTab[size - 1 >> LOG2_QUANTUM];
        // 向上对齐并计算sizeIdx
        // 省略
    }
}
```

构造方法中生成的SizeClasses数组非常重要：内容如下

`isMultiPageSize`列表示是否为pageSize整数倍。

`isSubPage`列可知，内存块小于28KB时表示Subpage。

数组中是没有size列的，不过都有源码了，完全可以一起打印出来。

`size=(1 << log2Group) + (nDelta << log2Delta)`

| index | log2Group | log2Delta | nDelta | isMultiPageSize | isSubPage | log2DeltaLookup | size   |
| ----- | --------- | --------- | ------ | --------------- | --------- | --------------- | ------ |
| 0     | 4         | 4         | 0      | 0               | 1         | 4               | 16B    |
| 1     | 4         | 4         | 1      | 0               | 1         | 4               | 32B    |
| 2     | 4         | 4         | 2      | 0               | 1         | 4               | 48B    |
| 3     | 4         | 4         | 3      | 0               | 1         | 4               | 64B    |
| 4     | 6         | 4         | 1      | 0               | 1         | 4               | 80B    |
| 5     | 6         | 4         | 2      | 0               | 1         | 4               | 96B    |
| 6     | 6         | 4         | 3      | 0               | 1         | 4               | 112B   |
| 7     | 6         | 4         | 4      | 0               | 1         | 4               | 128B   |
| 8     | 7         | 5         | 1      | 0               | 1         | 5               | 160B   |
| 9     | 7         | 5         | 2      | 0               | 1         | 5               | 192B   |
| 10    | 7         | 5         | 3      | 0               | 1         | 5               | 224B   |
| 11    | 7         | 5         | 4      | 0               | 1         | 5               | 256B   |
| 12    | 8         | 6         | 1      | 0               | 1         | 6               | 320B   |
| 13    | 8         | 6         | 2      | 0               | 1         | 6               | 384B   |
| 14    | 8         | 6         | 3      | 0               | 1         | 6               | 448B   |
| 15    | 8         | 6         | 4      | 0               | 1         | 6               | 512B   |
| 16    | 9         | 7         | 1      | 0               | 1         | 7               | 640B   |
| 17    | 9         | 7         | 2      | 0               | 1         | 7               | 768B   |
| 18    | 9         | 7         | 3      | 0               | 1         | 7               | 896B   |
| 19    | 9         | 7         | 4      | 0               | 1         | 7               | 1KB    |
| 20    | 10        | 8         | 1      | 0               | 1         | 8               | 1.25KB |
| 21    | 10        | 8         | 2      | 0               | 1         | 8               | 1.5KB  |
| 22    | 10        | 8         | 3      | 0               | 1         | 8               | 1.75KB |
| 23    | 10        | 8         | 4      | 0               | 1         | 8               | 2KB    |
| 24    | 11        | 9         | 1      | 0               | 1         | 9               | 2.5KB  |
| 25    | 11        | 9         | 2      | 0               | 1         | 9               | 3KB    |
| 26    | 11        | 9         | 3      | 0               | 1         | 9               | 3.5KB  |
| 27    | 11        | 9         | 4      | 0               | 1         | 9               | 4KB    |
| 28    | 12        | 10        | 1      | 0               | 1         | 0               | 5KB    |
| 29    | 12        | 10        | 2      | 0               | 1         | 0               | 6KB    |
| 30    | 12        | 10        | 3      | 0               | 1         | 0               | 7KB    |
| 31    | 12        | 10        | 4      | 1               | 1         | 0               | 8KB    |
| 32    | 13        | 11        | 1      | 0               | 1         | 0               | 10KB   |
| 33    | 13        | 11        | 2      | 0               | 1         | 0               | 12KB   |
| 34    | 13        | 11        | 3      | 0               | 1         | 0               | 14KB   |
| 35    | 13        | 11        | 4      | 1               | 1         | 0               | 16KB   |
| 36    | 14        | 12        | 1      | 0               | 1         | 0               | 20KB   |
| 37    | 14        | 12        | 2      | 1               | 1         | 0               | 24KB   |
| 38    | 14        | 12        | 3      | 0               | 1         | 0               | 28KB   |
| 39    | 14        | 12        | 4      | 1               | 0         | 0               | 32KB   |
| 40    | 15        | 13        | 1      | 1               | 0         | 0               | 40KB   |
| 41    | 15        | 13        | 2      | 1               | 0         | 0               | 48KB   |
| 42    | 15        | 13        | 3      | 1               | 0         | 0               | 56KB   |
| 43    | 15        | 13        | 4      | 1               | 0         | 0               | 64KB   |
| 44    | 16        | 14        | 1      | 1               | 0         | 0               | 80KB   |
| 45    | 16        | 14        | 2      | 1               | 0         | 0               | 96KB   |
| 46    | 16        | 14        | 3      | 1               | 0         | 0               | 112KB  |
| 47    | 16        | 14        | 4      | 1               | 0         | 0               | 128KB  |
| 48    | 17        | 15        | 1      | 1               | 0         | 0               | 160KB  |
| 49    | 17        | 15        | 2      | 1               | 0         | 0               | 192KB  |
| 50    | 17        | 15        | 3      | 1               | 0         | 0               | 224KB  |
| 51    | 17        | 15        | 4      | 1               | 0         | 0               | 256KB  |
| 52    | 18        | 16        | 1      | 1               | 0         | 0               | 320KB  |
| 53    | 18        | 16        | 2      | 1               | 0         | 0               | 384KB  |
| 54    | 18        | 16        | 3      | 1               | 0         | 0               | 448KB  |
| 55    | 18        | 16        | 4      | 1               | 0         | 0               | 512KB  |
| 56    | 19        | 17        | 1      | 1               | 0         | 0               | 640KB  |
| 57    | 19        | 17        | 2      | 1               | 0         | 0               | 768KB  |
| 58    | 19        | 17        | 3      | 1               | 0         | 0               | 896KB  |
| 59    | 19        | 17        | 4      | 1               | 0         | 0               | 1MB    |
| 60    | 20        | 18        | 1      | 1               | 0         | 0               | 1.25MB |
| 61    | 20        | 18        | 2      | 1               | 0         | 0               | 1.5MB  |
| 62    | 20        | 18        | 3      | 1               | 0         | 0               | 1.75MB |
| 63    | 20        | 18        | 4      | 1               | 0         | 0               | 2MB    |
| 64    | 21        | 19        | 1      | 1               | 0         | 0               | 2.5MB  |
| 65    | 21        | 19        | 2      | 1               | 0         | 0               | 3MB    |
| 66    | 21        | 19        | 3      | 1               | 0         | 0               | 3.5MB  |
| 67    | 21        | 19        | 4      | 1               | 0         | 0               | 4MB    |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |
| 0     | 0         | 0         | 0      | 0               | 0         | 0               | 0      |

后面几个全是0，这是因为原本Chunk默认是16MB，现在是4MB，所以后面就空白了。

## 内存池数据结构

### 内存层次结构

这是现在的Netty内存池层级图：

![netty内存池层级结构new](netty.assets/netty内存池层级结构new.png)

**Arena**表示一个内存区域，netty内存池由多个Arena组成，分配时每个线程以线程缓存中绑定的Arena进行内存分配。

每个Arena由以下几个ChunkList组成：

```java
private final PoolChunkList<T> qInit;// 存储内存利用率0-25%的chunk
private final PoolChunkList<T> q000;// 存储内存利用率1-50%的chunk
private final PoolChunkList<T> q025;// 存储内存利用率25-75%的chunk
private final PoolChunkList<T> q050;// 存储内存利用率50-100%的chunk
private final PoolChunkList<T> q075;// 存储内存利用率75-100%的chunk
private final PoolChunkList<T> q100;// 存储内存利用率100%的chunk
```

Jemalloc算法将每个Arena切分为多个小块Chunk，netty中默认4MB。

**Chunk**：Netty每次向操作系统申请内存块的最小单位(4MB)，是run的集合

**Run**：对应一块连续的内存，是page的集合，最少1page

**Page**：Chunk的最小分配单元，默认8KB，每个Chunk默认512个page

**SubPage**：**特殊的run，将run划分为1个或多个等大小内存块elem，进一步减少内存的浪费**。如果需分配内存小于Page的大小(8KB)，比如只有100B，如果直接分配一个Page(8KB)太浪费了，此时先根据SizeClasses将100B对齐为112B，申请run为1个page大小8KB，将其包装为Subpage，Subpage中将其划分为(8*1024/112=73)个elemSize为112B的elem内存块。

Subpage内存块的划分由内存对齐类SizeClasses定义好了：

![Netty内存块规格划分](netty.assets/Netty内存块规格划分.png)

### PoolArena

**Arena**表示一个内存区域，netty内存池由多个Arena组成，分配时每个线程按照轮询策略选择某个Arena进行内存分配。

PooledByteBufAllocator创建 Arena 的默认数量通常是CPU核数*2（因为现在多核处理器都是超线程），创建多个 Arena 来缓解资源竞争问题，从而提高内存分配效率。

线程在首次申请分配内存时，会通过 round-robin 的方式轮询 Arena 数组，选择一个固定的 Arena，**在线程的生命周期内只与该 Arena 打交道**，所以每个线程都保存了 Arena 信息，从而提高访问效率。

```java
/**
 * Jemalloc 算法中 Arena 是 Chunk的集合
 * 根据每块Chunk的内存使用率有这几种状态集合：QINIT、Q00、Q25、Q50、Q75、Q100。
 * Chunk 块可以在这几种状态间随着内存使用率的变化进行转移，从而提高分配效率。
 * PoolArena有2个子类：
 * HeapArena 对堆内存的分配管理
 * DirectArena 对直接内存的分配管理
 * @param <T>
 */
abstract class PoolArena<T> extends SizeClasses implements PoolArenaMetric {
    final PooledByteBufAllocator parent;// 所属 PooledByteBufAllocator 对象

    final int numSmallSubpagePools;// Small类型内存块有默认39种

    // 保存有Small类型内存块的subpage链表数组，数组中每个位置都是相同大小内存块的链表，不同位置代表不同大小
    // 保存的subpage来自于所有Chunk，从SizeClasses得知subpage有39种内存块大小
    private final PoolSubpage<T>[] smallSubpagePools;

    private final PoolChunkList<T> q050;// 存储内存利用率50-100%的chunk
    private final PoolChunkList<T> q025;// 存储内存利用率25-75%的chunk
    private final PoolChunkList<T> q000;// 存储内存利用率1-50%的chunk
    private final PoolChunkList<T> qInit;// 存储内存利用率0-25%的chunk
    private final PoolChunkList<T> q075;// 存储内存利用率75-100%的chunk
    private final PoolChunkList<T> q100;// 存储内存利用率100%的chunk
}
```

包含了一个 **`smallSubpagePools（ PoolSubpage<T>[]）`** 和**6个PoolChunkList**，Arena申请small类型内存块时，**先从`smallSubpagePools`分配，如果没有再从PoolChunkList中某个Chunk分配调用`Chunk#allocate()` 。**

- smallSubpagePools存放small Subpage类型的内存块

![smallSubpagePools](netty.assets/smallSubpagePools.png)

- 6个PoolChunkList 存放使用率不同的Chunk，构成一个双向循环链表

![PoolArena](netty.assets/PoolArena.png)

**qIint和q000为何设计为2个而不是合二为一：**

qInit 用于存储初始分配的 PoolChunk，因为在第一次内存分配时，PoolChunkList 中并没有可用的 PoolChunk，所以需要新创建一个 PoolChunk 并添加到 qInit 列表中。qInit 中的 PoolChunk 即使内存被完全释放也不会被回收，避免 PoolChunk 的重复初始化工作。

PoolArena中分配内存方法如下：它将根据请求的内存大小调用不同分配方法：

```java
// PoolArena.class
private void allocate(PoolThreadCache cache, PooledByteBuf<T> buf, final int reqCapacity) {
    final int sizeIdx = size2SizeIdx(reqCapacity);
    // 1.分配small类型内存块，<=28KB
    if (sizeIdx <= smallMaxSizeIdx) tcacheAllocateSmall(cache, buf, reqCapacity, sizeIdx);
    // 2.分配Normal类型内存块，<=4MB
    else if (sizeIdx < nSizes) tcacheAllocateNormal(cache, buf, reqCapacity, sizeIdx);
    // 3.分配Huge类型内存块，大于4MB
    else {
        int normCapacity = directMemoryCacheAlignment > 0
            ? normalizeSize(reqCapacity) : reqCapacity;
        // Huge allocations are never served via the cache so just call allocateHuge
        allocateHuge(buf, normCapacity);
    }
}
```

#### 分配Small

从内存对齐类SizeClasses可知，Small类型内存块在16B...28KB之间。

PoolArena分配Small内存块时先从smallSubpagePools缓存中分配，没有再去各个ChunkList申请subpage后再分配。

因为16B...28KB之间很多内存规格并不是pageSize的整数倍，这就会对Run按照各个内存规格进行subpage划分，并放入smallSubpagePools数组中缓存，比如28KB，会申请56KB的run，subpage划分为2个elem。

```java
// PoolArena.class
private void tcacheAllocateSmall(PoolThreadCache cache, PooledByteBuf<T> buf, final int reqCapacity, final int sizeIdx) {
    // 1.直接从smallSubpagePools缓存数组中分配
    // 查找该sizeIdx对应的SubPage的头结点，就是这个smallSubpagePools[sizeIdx]
    final PoolSubpage<T> head = findSubpagePoolHead(sizeIdx);
    final boolean needsNormalAllocation;
    // 省略锁操作
    final PoolSubpage<T> s = head.next;
    // 1.1 以头结点判断是否存在可分配的SubPage，若相等则说明没有找到可分配的Subpage
    needsNormalAllocation = s == head;
    if (!needsNormalAllocation) {// 有可分配的Subpage
        // 1.2 调用PoolSubpage#allocate()进行分配并返回内存块的句柄
        long handle = s.allocate();
        s.chunk.initBufWithSubpage(buf, null, handle, reqCapacity, cache);
    }

    // 2.subpage缓冲池中没有则调用allocateNormal方法分配
    // 则需要从各个ChunkList去分配，甚至新建Chunk
    if (needsNormalAllocation) {
        // 省略锁操作
        allocateNormal(buf, reqCapacity, sizeIdx, cache);
    }
}
```

如果`smallSubpagePools`数组缓存中有对应内存规格的Subpage，则调用`PoolSubpage#allocate()`直接分配获取句柄handle。

因为Subpage是将Run按照第1次请求划分为等大小内存块，所以调用`PoolSubpage#allocate()`将直接分配其中某块内存。

**smallSubpagePools数组缓存是从哪来的呢？**

> allocateNormal()方法从某个Chunk划分出一个Run后，将根据此次请求的大小将Run划分为均等份，并封装为PoolSubpage放入该缓存数组中。

#### 分配Normal

从内存对齐类SizeClasses可知，Normal类型内存块在28KB...ChunkSize（默认4MB）之间。

Normal类型内存块没有smallSubpagePools缓存，因为从内存对齐类的下一个内存规格32KB开始，所有内存块都是pageSize的整数倍，无须再对page进行划分为subpage的情况。

```java
// PoolArena.java
private void allocateNormal(PooledByteBuf<T> buf, int reqCapacity, int sizeIdx, PoolThreadCache threadCache) {
    assert lock.isHeldByCurrentThread();
    // 1.从各个ChunkList中分配
    if (q050.allocate(buf, reqCapacity, sizeIdx, threadCache) ||
        q025.allocate(buf, reqCapacity, sizeIdx, threadCache) ||
        q000.allocate(buf, reqCapacity, sizeIdx, threadCache) ||
        qInit.allocate(buf, reqCapacity, sizeIdx, threadCache) ||
        q075.allocate(buf, reqCapacity, sizeIdx, threadCache)) {
        return;
    }

    // 2.如果现有的ChunkList都不满足需要的内存块，则新建一个Chunk
    // Add a new chunk.
    PoolChunk<T> c = newChunk(pageSize, nPSizes, pageShifts, chunkSize);
    boolean success = c.allocate(buf, reqCapacity, sizeIdx, threadCache);
    assert success;
    qInit.add(c);// 将新建的Chunk添加到qInit队列头部
}
```

为什么选择q050作为首选分配队列呢？**分配顺序为：q050-->q025-->q000-->qInit-->q075**，除了最后一个几乎是倒序分配，原因在于从q050倒序开始既能使Chunk使用率上升，又不会像q075那样可能出现某个Chunk空间不足分配的情况。

#### 分配Huge

大于ChunkSize默认4MB时即为大内存块，直接申请Chunk即可，并不再将其缓存到PoolArena的ChunkList中。

```java
// PoolArena.java
private void allocateHuge(PooledByteBuf<T> buf, int reqCapacity) {
    PoolChunk<T> chunk = newUnpooledChunk(reqCapacity);
    activeBytesHuge.add(chunk.chunkSize());
    buf.initUnpooled(chunk, reqCapacity);
    allocationsHuge.increment();
}
```

### PoolChunk

![PoolChunk](netty.assets/PoolChunk.png)

Jemalloc算法将每个Arena切分为多个小块Chunk，netty中默认4MB。

**Chunk**：Netty每次向操作系统申请的最小单位(4MB)，是run的集合

**Run**：对应一块连续的内存，是page的集合，最少1page。最开始时整个Chunk就是一个Run。

**Page**：Chunk的最小分配单元，默认8KB，每个Chunk默认512个page。

**SubPage**：**特殊的run，将run划分为1个或多个等大小Small内存规格内存块elem，进一步减少内存的浪费**。如果需分配内存小于Page的大小(8KB)，比如只有100B，如果直接分配一个Page(8KB)太浪费了，此时先根据SizeClasses将100B对齐为112B，申请run为1个page大小8KB，将其包装为Subpage，Subpage中将其划分为(8*1024/112=73)个elemSize为112B的elem内存块。

Subpage内存块的划分由内存对齐类SizeClasses定义好了：

![Netty内存块规格划分](netty.assets/Netty内存块规格划分.png)

```java
final class PoolChunk<T> implements PoolChunkMetric {
    final PoolArena<T> arena;// 所属Arena
    
    // 当前Chunk申请的内存块，比如对于堆内存，T就是一个byte数组，对于直接内存，T就是ByteBuffer，
    final T memory;

    /**
     * a map 管理所有的run
     * 每个run，第1页page和最后1页page的runOffset作为key，
     * run的句柄handle作为value都保存于runsAvailMap
     */
    private final LongLongHashMap runsAvailMap;

    /**
     * 每个优先级队列管理着相同大小的runs，以runOffset排序，
     * 所以总用持有较小偏移量的首页page的run进行分配
     * 数组长度为68，即16B...4MB之间的68个内存块规格，储存各个内存规格的run
     */
    private final LongPriorityQueue[] runsAvail;

    // 存储此Chunk分配的所有Subpage的引用
    // 长度为chunkSize >> pageShifts，默认512
    private final PoolSubpage<T>[] subpages;

    private final int pageSize;// Page大小默认8KB
    private final int chunkSize;// Chunk 内存块占用大小。默认为 4M

    int freeBytes;// 此chunk剩余的空间

    PoolChunkList<T> parent;
    PoolChunk<T> prev;
    PoolChunk<T> next;

    PoolChunk(PoolArena<T> arena, Object base, T memory, int pageSize, int pageShifts, int chunkSize, int maxPageIdx) {
        // 省略
        // 插入初始Run, offset = 0, pages = chunkSize / pageSize
        int pages = chunkSize >> pageShifts;
        long initHandle = (long) pages << SIZE_SHIFT;
        insertAvailRun(0, pages, initHandle);
    }
}
```

PoolChunk的`memory`属性就是申请的内存块，堆内存为`byte[]`，直接内存为`ByteBuffer`，默认4MB，Netty按照每页8KB将其划分为512页，分配内存块必须以page进行分配，每个内存块叫做run，含多页page。

PoolChunk内存结构如下：

![Chunk内存结构](netty.assets/Chunk内存结构.png)

#### handle访问句柄

在Chunk中内存块以long变量`handle`句柄便捷表示：

![runHandle](netty.assets/runHandle.png)

> 初始Run的handle如下：runOffset=0，size=512，isUsed=0，isSubpage=0，bitmapIdx=0
>
> **注意：初始整个Chunk就是一个Run，随着后续的申请对Run进行切分为小Run，又随着Run的回收合并等，`runAvails`中可能会存有很多不同内存规格的Run。**

从Chunk中申请内存块的方法如下：

```java
// PoolChunk.java
boolean allocate(PooledByteBuf<T> buf, int reqCapacity, int sizeIdx, PoolThreadCache cache) {
    final long handle;
    // 1.small内存块分配，则尝试从某个subpage分配内存块
    if (sizeIdx <= arena.smallMaxSizeIdx) {
        handle = allocateSubpage(sizeIdx);
        if (handle < 0) return false;
        assert isSubpage(handle);
    } else {
        // 2.normal内存块分配
        // runSize必须是pageSize倍数
        int runSize = arena.sizeIdx2size(sizeIdx);
        handle = allocateRun(runSize);
        if (handle < 0) return false;
        assert !isSubpage(handle);
    }

    // 3.将分配到的内存块初始化到buf中
    ByteBuffer nioBuffer = cachedNioBuffers != null ? cachedNioBuffers.pollLast() : null;
    initBuf(buf, nioBuffer, handle, reqCapacity, cache);
    return true;
}
```

#### runAvails可分配run

```java
    /**
     * 每个优先级队列管理着相同大小的runs，以runOffset排序，
     * 所以总用持有较小偏移量的首页page的run进行分配
     * 数组长度为68，即16B...4MB之间的68个内存块规格，储存各个内存规格的run
     */
    private final LongPriorityQueue[] runsAvail;
```

`runAvails`属性非常重要，默认情况下保存有来自SizeClasses内存对其类的68个内存规格的Run队列，各个优先队列以runOffset排序。

申请内存时如申请28KB则计算其在SizeClasses的sizeIdx，从之前分析可知如28KB为38，则从`runAvails`的28偏移量开始查找各个队列，找到第1个满足该请求且非空的Run队列，将队首Run从`runAvails`移除并切分，剩余的run再放回`runAvails`。

LongPriorityQueue是Netty是实现的一个`long[]`数组元素的优先级队列，上沉下浮，比较简单，自己实现估计是觉得jdk提供的优先队列的泛型有点搓吧。

将run放入runAvails中：

```java
private void insertAvailRun(int runOffset, int pages, long handle) {
    // 1.将此run包含的页面数量转换为SizeClasses的sizeIdx，找到该内存规格的run队列
    // floor是指如果页面数无法完全找到对应的sizeIdx则向下找，如9个页面72KB就没有，将其向下找到64KB的sizeIdx 43
    int pageIdxFloor = arena.pages2pageIdxFloor(pages);
    LongPriorityQueue queue = runsAvail[pageIdxFloor];
    queue.offer(handle);// 将此run加入该队列中

    // 2.插入每个run的第1页和最后1页到runsAvailMap中
    insertAvailRun0(runOffset, handle);
    if (pages > 1) {
        //insert last page of run
        insertAvailRun0(lastPage(runOffset, pages), handle);
    }
}
```

将run从其中移除：

```java
private void removeAvailRun(long handle) {
    // 1.将此run包含的页面数量转换为SizeClasses的sizeIdx，找到该内存规格的run队列
    // floor是指如果页面数无法完全找到对应的sizeIdx则向下找，如9个页面72KB就没有，将其向下找到64KB的sizeIdx 43
    int pageIdxFloor = arena.pages2pageIdxFloor(runPages(handle));
    LongPriorityQueue queue = runsAvail[pageIdxFloor];
    // 2.从队列移除该run的句柄handle
    removeAvailRun(queue, handle);
}

private void removeAvailRun(LongPriorityQueue queue, long handle) {
    queue.remove(handle);// 队列中移除即可

    int runOffset = runOffset(handle);
    int pages = runPages(handle);
    //remove first page of run
    runsAvailMap.remove(runOffset);
    if (pages > 1) {
        //remove last page of run
        runsAvailMap.remove(lastPage(runOffset, pages));
    }
}
```

#### 分配run

`allocateRun(int runSize)`方法用于从runAvails中找到第1个满足请求runSize的Run并对其进行分裂，剩余run再放回`runAvails`中。

申请内存时如申请28KB则计算其在SizeClasses的sizeIdx，从之前分析可知如28KB为38，则从`runAvails`的28偏移量开始查找各个队列，找到第1个满足该请求且非空的Run队列，将队首Run从`runAvails`移除并切分，剩余的run再放回`runAvails`。

```java
/**
 * 1) 找到在runsAvails中第1个满足的run
 * 2) 如果找到的run的pages大于请求的pages，则分裂该run，并保存剩下的run之后使用
 */
private long allocateRun(int runSize) {
    // 1.计算请求的runSize需要page页数量
    int pages = runSize >> pageShifts;// 右移13位，即除以pageSize 8K
    // 根据需要page数量计算SizeClasses的sizeIdx，作为从runAvail数组寻找的起始偏移量
    int pageIdx = arena.pages2pageIdx(pages);

	// 省略同步操作
    // 2.从runsAvail中找到第1个满足该请求页面数量大小且非空的run的队列
    int queueIdx = runFirstBestFit(pageIdx);// 从pageIdx开始查找runAvails数组队列
    if (queueIdx == -1) return -1;

    // 3.获取队列第1个run，即最小runOffset的run
    LongPriorityQueue queue = runsAvail[queueIdx];
    long handle = queue.poll();// 弹出队列第1个run

    // 4.从runAvail队列中移除选出的这个run
    removeAvailRun(queue, handle);

    // 5.按照请求的页面数量将此run分裂，返回分裂后请求到的run句柄
    if (handle != -1) handle = splitLargeRun(handle, pages);

    // 剩余空间减少
    int pinnedSize = runSize(pageShifts, handle);
    freeBytes -= pinnedSize;
    return handle;// 返回请求到的run的句柄
}
```

第2步根据计算得到的sizeIdx查找run队列：

```java
private int runFirstBestFit(int pageIdx) {
    if (freeBytes == chunkSize) return arena.nPSizes - 1;
    for (int i = pageIdx; i < arena.nPSizes; i++) {
        LongPriorityQueue queue = runsAvail[i];
        if (queue != null && !queue.isEmpty()) {
            return i;
        }
    }
    return -1;
}
```

第5步，将查到的Run切分为2个Run，返回分裂后请求到的run句柄，并且将剩余Run放回`runAvails`中：

```java
private long splitLargeRun(long handle, int needPages) {
    int totalPages = runPages(handle);// 此run的总页面
    int remPages = totalPages - needPages;// 分配后剩余页面数

    if (remPages > 0) {// run分裂，将剩下的run插入availRun和runasAvailMap中
        int runOffset = runOffset(handle);

        // 1.计算剩余run的句柄handle并插入runAvails
        int availOffset = runOffset + needPages;// 剩下的首页面位移
        long availRun = toRunHandle(availOffset, remPages, 0);// 分配后的run的新句柄handle
        insertAvailRun(availOffset, remPages, availRun);// 将分裂后的run插入availRun队列和runsAvailMap中

        // 2.计算并返回划分出来的run的句柄
        return toRunHandle(runOffset, needPages, 1);// 计算分配出去的run的句柄handle
    }

    //mark it as used
    handle |= 1L << IS_USED_SHIFT;
    return handle;
}
```

对于从runAvails中插入或移除run看上面的分析。

#### 分配Subpage

**SubPage**：**特殊的Run，不过将Run内存均等划分为几份，目的是为了减少内存的浪费**。

Small类型内存块在PoolArena中分配内存时优先从`smallSubpagePools`数组缓存中找到满足该内存块大小的Subpage，没有时再从ChunkList的某个Chunk分配Subpage后再分配。

从Chunk中分配Subpage：`Chunk#allocateSubpage(int sizeIdx)`

- 先调用`Chunk#allocateRun(int runSize)`分配run
- 再调用`new PoolSubpage()`将run封装为`PoolSubpage`

```java
/**
 * 创建并初始化一个新的 PoolSubpage，并放入PoolArena的smallSubpagePools的
 * @param sizeIdx SizeClasses的内存规格偏移索引
 * @return index in memoryMap
 */
private long allocateSubpage(int sizeIdx) {
    // 1.获取 PoolArena持有的 smallSubpagePools 池的sizeIdx队列的头部节点，
    // 准备将新分配的subPage插入该队列
    PoolSubpage<T> head = arena.findSubpagePoolHead(sizeIdx);// smallSubpagePools[sizeIdx];
    // 省略锁操作
    
    // 2.分配一个新run
    int runSize = calculateRunSize(sizeIdx);// 根据sizeIdx计算出runSize，它必须是pageSize整数倍
    long runHandle = allocateRun(runSize);
    if (runHandle < 0) return -1;
    
    int runOffset = runOffset(runHandle);
    // 3.根据sizeIdx计算elemSize，即将Run划分均等份的大小
    int elemSize = arena.sizeIdx2size(sizeIdx);
    // 4.将run新建SubPage，并将其添加到该head节点后面，即放入PoolArena的subpagesPool池中
    // 同时这里会将Run均等划分为多个或1个elemSize内存块
    PoolSubpage<T> subpage = new PoolSubpage<T>(head, this, pageShifts, runOffset,
                                                runSize(pageShifts, runHandle), elemSize);

    subpages[runOffset] = subpage;// 将subpage引用存入数组，后续可以直接根据handle取出
    // 5.将subpage的某一块elemSize分配出去
    return subpage.allocate();// 这里不用传入大小，是因为该subpage已经根据第1次请求的大小划分为均等份了
}
```

这里的RunSize肯定是大于等于elemSize的。

比如sizeIdx为19时，申请1KB内存块，计算出的RunSize为1个page即8KB，而elemSize则为1KB，所以将Run封装为Subpage后将按照第1次申请的elemSize划分为8个1KB的均等块。

并将这个Subpage加入PoolArena的`smallSubpagePools[sizeIdx]`位置的链表，供后续直接分配该sizeIdx的内存规格。

关于PoolSubpage的分析后续会专门解析。

#### 释放run

当申请的内存块不再使用时调用`PoolChunk#free()`释放指定handle的run或subpage的elem：

```java
// PoolChunk.java
// 是否指定handle的run，如果此run是subpage则释放该handle指定的elem
void free(long handle, int normCapacity, ByteBuffer nioBuffer) {
    int runSize = runSize(pageShifts, handle);
    // 1.如果此run划分了subpage，则先释放handle指定的subpage的elem
    // 从run handle判断是否划分为subpage
    if (isSubpage(handle)) {
        int sizeIdx = arena.size2SizeIdx(normCapacity);
        // 1.1 从Arena的subpage池数组找到该大小对应的head节点
        // smallSubpagePools[sizeIdx]
        PoolSubpage<T> head = arena.findSubpagePoolHead(sizeIdx);

        // 1.2  根据runOffset从数组中取出待释放的subpage
        int sIdx = runOffset(handle);// 从handle中得到runOffset
        PoolSubpage<T> subpage = subpages[sIdx];

        // 1.3 调用subpage.free()释放bitmapIdx指定的subpage的某elem
        // 返回true表示subpage尚在使用，不释放该subpage的run，直接返回
        // 返回false则表示该subpage内所有elem都未使用，则需要释放该subpage的run
        if (subpage.free(head, bitmapIdx(handle)))
            return;
        // 1.4 走到这说明此subpage的run需要释放，则先置空subpages数组的指定槽位
        subpages[sIdx] = null;
    }

    // 2.释放此run
    // 2.1 合并前后相邻runs, 成功合并的run将从runsAvails和runsAvailMap移除
    long finalRun = collapseRuns(handle);

    // 2.2 设置Run句柄为未使用
    finalRun &= ~(1L << IS_USED_SHIFT);
    // 如果此run为Subpage，设置其句柄为非subpage
    finalRun &= ~(1L << IS_SUBPAGE_SHIFT);
    // 2.3 将合并后run插入到runsAvails中
    insertAvailRun(runOffset(finalRun), runPages(finalRun), finalRun);
    freeBytes += runSize;
    // 省略部分代码
}
```

1.3步调用`subpage.free(head, bitmapIdx)`释放该subpage的指定elem，此方法在PoolSubpage处进行详细解析。

释放run的时候会先去合并前后相邻的run为一个可以分配的更大的run，从而减少内存碎片。

### PoolSubpage

**SubPage**：**特殊的run，将run划分为1个或多个等大小Small内存规格内存块elem，进一步减少内存的浪费**。如果需分配内存小于Page的大小(8KB)，比如只有100B，如果直接分配一个Page(8KB)太浪费了，此时先根据SizeClasses将100B对齐为112B，申请run为1个page大小8KB，将其包装为Subpage，Subpage中将其划分为(8*1024/112=73)个elemSize为112B的elem内存块。

在PoolChunk中申请Small类型内存块时，将调用`PoolChunk#allocateSubpage(sizeIdx)`将申请的run创建为PoolSubpage：并按照第1次请求大小将此run划分为多个elem内存块，如下图所示：

![image-20221113154352338](netty.assets/Subpage.png)

```java
final class PoolSubpage<T> implements PoolSubpageMetric {
    final PoolChunk<T> chunk;// 所属chunk
    final int elemSize;// 切分的等量小块内存大小，最小16B
    private final int pageShifts;// 默认13
    private final int runOffset;// 此subpage所属run的首页page在Chunk的位移
    private final int runSize;// 所属run的大小

    /**
     * 位图数组
     * 每个bit位记录一个elem的使用情况
     * 因为elemSize最小为16B，即每个bit可能最小表示16B使用情况，则bitmap初始化时如下：
     * bitmap = new long[runSize >>> 6 + LOG2_QUANTUM]; // 即runSize / 1024
     * 即初始化位图数组时按最大长度初始化，然后再以bitmapLength来计算真正使用的位图长度
     * 我觉得有点多余了，完全可以根据具体使用长度初始化位图数组呀
     */
    private final long[] bitmap;
    // 位图数组实际使用长度= maxNumElems/64 + (maxNumElems % 64 == 0 ? 0 : 1)
    private int bitmapLength;
    private int maxNumElems;// 此run划分的等大小内存块数量 = `runSize / elemSize`

    private int nextAvail;// 下个可使用内存块指针
    private int numAvail;// 此subpage剩余的可分配等大小内存块

    PoolSubpage<T> prev;
    PoolSubpage<T> next;

    PoolSubpage(PoolSubpage<T> head, PoolChunk<T> chunk, int pageShifts, int runOffset, int runSize, int elemSize) {
        // 省略属性赋值
        // 1.初始化位图数组
        bitmap = new long[runSize >>> 6 + LOG2_QUANTUM]; // runSize / 64 / QUANTUM(默认4)

        doNotDestroy = true;
        // 2.将此run按照第1次请求的大小均等划分
        if (elemSize != 0) {
            // 计算elem内存块切分数量
            maxNumElems = numAvail = runSize / elemSize;
            nextAvail = 0;
            // 计算位图数组实际使用长度
            bitmapLength = maxNumElems >>> 6;
            if ((maxNumElems & 63) != 0)
                bitmapLength++;
            for (int i = 0; i < bitmapLength; i++) 
                bitmap[i] = 0;
        }
        // 3.添加到PoolArena是subpage池数组中，这里就是将此subpage插入给定的head节点之后
        addToPool(head);
    }
}
```

我觉得在构造函数的第1步初始化位图数组长度时以每个bit位表示最小elemSize 16B进行长度计算，之后再以实际elemSize计算实际使用长度bitmapLength，有点多于了，**可以直接以实际elemSize计算位图长度来初始化位图数组呀**。

位图数组bitmap**以每个bit位置记录每个elem内存块的使用状态**，每个内存块elemSize为small类型内存块大小，即最小16B，最大28KB。

**每个PoolSubpage创建后都会按照其elemSize放入PoolArena的`smallSubpagePools`属性指定槽位**，用于small类型内存块的申请分配：

![smallSubpagePools](netty.assets/smallSubpagePools.png)

#### allocate

在PoolSubpage中申请内存块时，无须传入sizeIdx内存对齐索引，因为Subpage早已按照elemSize划分为等大小内存块，只需要调用allocate()方法申请某块内存即可：

```java
// PoolSubpage.java
long allocate() {
    // 省略检查
    // 1.找到未使用的elem的索引bitmapIdx，就是第几个elem，从0开始
    final int bitmapIdx = getNextAvail();

    // 2.标记位图中该elem索引为1，表示该elem已使用
    int q = bitmapIdx >>> 6;
    int r = bitmapIdx & 63;
    bitmap[q] |= 1L << r;

    // 3.如果此subpage分配完了就从PoolArena的subPage池子中移除
    if (--numAvail == 0)
        removeFromPool();

    // 4.返回此subpage中分配的elem内存块的访问句柄handle
    // 该句柄和此subpage所属run在前32位是一样的，
    // 后面32位为给定的bitmapIdx表示此subpage的哪个elem
    return toHandle(bitmapIdx);
}
```

#### free

从subpage中释放某个elem内存块，需要传入该内存块的索引bitmapIdx：

```java
/**
 * 释放指定bitmapIdx的elem
 * @return true 如果subpage中有elem仍在使用
 * false subpage中所有elem都未使用，可以释放subpage的run
 */
boolean free(PoolSubpage<T> head, int bitmapIdx) {
    // 1.标记位图中该bitmapIdx索引处为0，表示该elem未使用
    int q = bitmapIdx >>> 6;
    int r = bitmapIdx & 63;
    assert (bitmap[q] >>> r & 1) != 0;
    bitmap[q] ^= 1L << r;

    // 2.可使用elem数量numAvail+1
    // 如果之前为0，从allocate()方法知道该subpage已经从PoolArena的subpage池子移除，
    // 此时释放了一个elem，再将其放回池子，供后续内存申请分配
    if (numAvail++ == 0) 
        addToPool(head);

    // 3.如果subpage中有elem正在使用则返回true，表示不释放run
    if (numAvail != maxNumElems) {
        return true;
    } else {
        // 4.此时subpage中所有elem都未使用
        // 4.1 如果此subpage是该内存规格在PoolArena的subpage池子中唯一的subpage则返回true
        if (prev == next) 
            return true;

        // 4.2 若有其它相同内存规格的subpage从arena的subpage池子移除，并返回false
        doNotDestroy = false;
        removeFromPool();
        return false;
    }
}
```

## 总结

Netty内存管理基于jemalloc4实现，结合以下关键类实现：

- `PooledByteBufAllocator`：内存分配器，会创建CPU核心数*2个Arena，线程在首次申请分配内存时，会通过 round-robin 的方式轮询 Arena 数组，选择一个固定的 Arena，**在线程的生命周期内只与该 Arena 打交道**，避免锁竞争。
- `SizeClasses`：内存规格对齐类，在Netty的jemalloc4实现中不再以伙伴分配算法管理内存，而是将内存划分为更细粒度的内存规格，**其产生的`内零头`要远小于伙伴分配算法**。在`SizeClasses`中各个内存规格对应一个`sizeIdx`，如16B对应0，28KB对应38，4MB对应67。

- `PoolArena`：**Arena**表示一个内存区域，netty内存池由多个Arena组成，分配时每个线程按照轮询策略选择某个Arena进行内存分配。
  - `PoolSubpage<T>[] smallSubpagePools`属性缓存各个不同内存规格的subpage，便于**快速分配Small类型内存；**
  - `PoolChunkList<T> qxxx`这6个属性分别存放存储内存利用率不同的chunk，**分配顺序为：q050-->q025-->q000-->qInit-->q075**；

- `PoolChunk`：Jemalloc算法将每个Arena切分为多个小块Chunk（默认4MB），**Chunk是Netty每次向操作系统申请内存块的最小单位(4MB)**，是run的集合。
  - `page`：Chunk的最小分配单元，默认8KB，每个Chunk默认512个page。
  - `run`：对应一块连续的内存，是page的集合，最少1page。最开始时整个Chunk就是一个Run。每次从Chunk申请内存时，将内存规格对齐后以一个run来管理申请的page页。
  - `Subpage`：**特殊的run，将run划分为1个或多个等大小Small内存规格内存块elem，进一步减少内存的浪费**。
  - 如果需分配内存小于Page的大小(8KB)，比如只有100B，如果直接分配一个Page(8KB)太浪费了，此时先根据SizeClasses将100B对齐为112B，申请run为1个page，将其包装为Subpage，Subpage中将其划分为(8*1024/112=73)个elemSize为112B的elem内存块。
  - 如申请28KB内存，内存规格对齐后runSize为56KB，7个page，则将连续的7个page作为run分配出去并划分为2个elem，包装为subpage。

### 内存申请流程图

从`PooledByteBufAllocator#directBuffer(capacity)`申请内存块流程如下：[原图](https://www.processon.com/view/link/63706c2e6376897f2b680e96)

![内存申请流程图](netty.assets/内存申请流程图.png)

# SSL(待续)



# 总结

1、Netty高性能设计：

- NIO、**Reactor模型、局部无锁化**、事件驱动
- 零拷贝：TCP接收和发送缓冲区采用**直接内存**，减少内存拷贝。环形数组缓冲区减少channel写次数。文件传输采用DefaultFileRegion通过`FileChannel#transferTo()`实现**文件传输零拷贝**。
- 内存管理：内存池、引用计数器、jemalloc算法等实现的**内存管理**减少了内存申请/释放和GC；**ByteBuf切片**、Composite组合等方式减少内存拷贝。

2、优化建议：

- **发送队列容量限制**：Netty的NIO消息发送队列ChannelOutboundBuffer没有设置容量上限，如果对方网络差，会造成消息积压，进而可能造成内存溢出。
  可以在Bootstrap启动器指定参数ChannelOption设置发送队列长度。
- **消息发送失败处理**：当发生网络故障时，Netty会关闭链路，并释放发送队列中的消息，再通知listener。它并没有将发送队列中的失败消息一起通知给listener。
  **因此对于重要消息，一定要提前存起来，发送成功再标记发送成功，发送失败则等待网络恢复重新发送。**



## 与Tomcat比较

netty相比于Tomcat，在大文件流式处理上更有优势，因为netty暴露nio接口和自定义高效零拷贝内存管理(减少数据拷贝)，而Tomcat默认将请求全部读出反序列化后交由业务接口(servlet)处理。

