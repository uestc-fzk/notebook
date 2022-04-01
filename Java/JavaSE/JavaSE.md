# 前言

本md文件将记录对JDK相关学习的笔记。

# 环境搭建

将以Java17为例子，Jetbrains IDEA为开发IDE，Maven为包依赖管理工具搭建开发环境

## JDK17安装

下载Java直接百度搜索Java即可，进入Oracle官网下载就是。但是呢，下载速度有点慢，可以从国内镜像下载：

> 1、编程宝库镜像源：http://www.codebaoku.com/jdk/jdk-oracle.html
> 2、华为云镜像：https://repo.huaweicloud.com/java/jdk/
> 3、清华镜像源：https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/

需要注意的是上面的镜像中有的是Oracle JDK有的是Open JDK

如果是windows下载，用exe文件进行安装的情况下，默认会把某个目录配置到系统变量path中，直接`java -version`验证是没问题的。最好还是配置系统变量`JAVA_HOME`指向安装目录，并将`%JAVA_HOME%\bin`添加到系统变量path中。

## Maven安装和环境配置

1、百度搜索Maven来到Apache关于Maven的网页，下载Maven最新版压缩包解压即为安装完成，然后把安装目录作为`MAVEN_HOME`配置到系统变量，并在系统变量path增加`%MAVEN_HOME\bin`，然后命令行测试即可

2、新建目录mavenRepository用来作为仓库目录存放Maven下载的依赖包（放在D盘，因为后期依赖包会越来越多）

3、打开 Maven 的配置文件(windows机器一般在maven安装目录的conf/settings.xml)(建议直接拖入VSCode中)

4、配置仓库目录如下图：将其指向上面新建的仓库目录

![image-20220307174259858](JavaSE.assets/image-20220307174259858.png)

5、配置阿里云镜像源：这样下载依赖速度飞起！
去阿里云官网找到镜像站中的Maven镜像，有配置教程
在`<mirrors></mirrors>`标签中添加 mirror 子节点:

![image-20220307174534200](JavaSE.assets/image-20220307174534200.png)

配置完后保存退出。接下来即可进入IDEA新建Maven项目了

## IDEA安装激活和配置

安装及破解教程：https://www.exception.site/essay/how-to-free-use-intellij-idea-2020 未验证过，不知是否有效
还有一个更牛逼的激活网站：https://idea.medeming.com 缺点是要关注公众号，不过它的激活码确实是可以用的，非常方便。

更推荐：还有一种是学生身份申请IDEA的免费教育许可证，每年可以申请一次，每次有效期一年，这个虽然麻烦，但是学生账户可以登录使用所有Jetbrains系列产品如GoLand，Pycharm。
申请地址：https://www.jetbrains.com.cn/community/education/#students

### IDEA全局配置配置

IDEA还需要好好配置一下才能更好用

![IDEA全局配置1](JavaSE.assets/IDEA全局配置1.png)

![IDEA全局配置2](JavaSE.assets/IDEA全局配置2.png)

这个是自动导入依赖：

![IDEA全局配置3](JavaSE.assets/IDEA全局配置3.png)

![IDEA全局配置4](JavaSE.assets/IDEA全局配置4.png)

![IDEA全局配置5](JavaSE.assets/IDEA全局配置5.png)

![IDEA全局配置6](JavaSE.assets/IDEA全局配置6.png)

给Java的class文件设置文件头：超级好用

![IDEA全局配置7](JavaSE.assets/IDEA全局配置7.png)

设置文件的编码为utf8，避免文件出现乱码

![IDEA全局配置8](JavaSE.assets/IDEA全局配置8.png)

最后，一定要点击上图中右下角的Apply，然后再点ok，这样设置才会生效噶。

### IDEA配置本地Maven

IDEA中默认有Maven插件，也不是不能用，但是捏，它不符合国情！它下载的依赖包默认放在`$USER/.m2`目录，就是在C盘里，然后默认的配置从官方源下载依赖库，慢的一批。本地安装Maven后，可以设置maven本地仓库放在D盘啊，还可以配置阿里云镜像加速，还可以在命令行中敲mvn命令。

注意：这里也必须在IDEA的全局配置中配置哦！

![image-20220307175527312](JavaSE.assets/image-20220307175527312.png)

此时就能去删掉系统默认用户目录下的.m2目录了（默认的Maven仓库地址），新建项目如果说又出现了.m2目录，说明IDEA中Maven配置失效了，再像上图这样配置一下。

# JUC

资料来源：《Java并发编程的艺术》、JUC包下类源码

`java.util.concurrent`的简称是JUC，即Java自带的一些并发工具类，提供了如Lock、阻塞队列、并发集合、并发映射这些工具供咱们使用。

## 队列同步器AQS

```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {
    /**
     * 等待队列的头部，延迟初始化。 
     * 除初始化外，只能通过 setHead 方法进行修改。 
     * 注意：如果head存在，它的waitStatus保证不会被CANCELLED。
     */
    private transient volatile Node head;
 
    /** 等待队列的尾部，延迟初始化。 仅通过方法 enq 修改以添加新的等待节点  */
    private transient volatile Node tail;

    /** 同步状态     */
    private volatile int state;
}
```

队列同步器`AbstractQueuedSynchronizer`，使用一个int变量表示同步状态，FIFO队列完成线程排队。

这是一个抽象类，各个不同实现的锁机制内部定义一个静态内部类来继承这个抽象类，并实现它的抽象方法管理同步状态。

其可以重写的方法如下：

![image-20220109202121767](JavaSE.assets/image-20220109202121767.png)

这里无法看到对state变量的操作啊？因为像getState()、setState(int newState)、compareAndSetState(int expect,int update)这些都是final修饰的，不让子类进行覆盖。

同步器也提供了一些模板方法，主要有acquire(int arg)，acquireShared(int arg)，release(int arg)，release(int arg)，以及一些限时获取的方法。acquire和acquireShared方法会在必要时将线程加入等待队列并阻塞。接下来就先研究一下这个等待队列。



### 同步队列

同步队列是一个双向队列，由AQS的内部类Node构成，其每个节点保存同步状态失败的线程引用、等待状态、以及前后继结点

```java
static final class Node {
    /** 对于同步队列，初始化为0；对于condition队列，初始化为CONDITION，即-2 */
    volatile int waitStatus;
    
    /** 取消：线程由于超时或被中断，从同步队列中取消等待 */
    static final int CANCELLED =  1;
    /** 后继节点的线程处于等待状态 
    此节点的后继节点已（或即将）被阻塞（通过停放），因此当前节点在释放或取消时必须取消停放其后继节点。 
    为了避免竞争，获取方法必须首先表明它们需要一个信号，然后重试原子获取，然后在失败时阻塞*/
    static final int SIGNAL    = -1;
    /** 表示此节点当前位于condition队列中，在被转换之前不会用作同步队列节点 */
    static final int CONDITION = -2;
    /** 表示下一次共享式同步状态获取会无条件传播下去 */
    static final int PROPAGATE = -3;


    volatile Node prev;
    volatile Node next;

    /** 此节点的线程引用 */
    volatile Thread thread;

    /** condition队列的后继节点。在共享模式下，此值将指向下面的SHARED常量，独占模式则是EXCLUSIVE */
    Node nextWaiter;
    /** 指示节点在共享模式下等待的标记 */
    static final Node SHARED = new Node();
    /** 指示节点以独占模式等待的标记 */
    static final Node EXCLUSIVE = null;
}
```

同步队列的首节点是获取到同步状态的节点，当其释放同步状态时，会唤醒后继节点。

接下来研究一下AQS提供的这几个模板方法，研究其实现逻辑。

### acquire

acquire是以互斥方式获取同步状态，可用于实现写锁。此方法对中断不敏感，即当线程被其他线程中断时，并不会从同步队列中移除。

```java
    /** 独占模式获取，忽略中断。
     * 通过至少调用一次tryAcquire ，成功返回。否则线程会排队，可能会反复阻塞和解除阻塞，调用tryAcquire直到成功
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
```

1、首先是调用了tryAcquire方法，由子类自己去实现，默认是抛异常的
2、如果获取失败，以Node.EXCLUSIVE模式构造同步节点，并加入到同步队列末尾

```java
    /**
     * 为当前线程和给定模式创建和排队节点
     *
     * @param mode Node.EXCLUSIVE for 独占, Node.SHARED for 共享
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(mode);

        for (;;) {
            Node oldTail = tail;
            if (oldTail != null) {
                node.setPrevRelaxed(oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return node;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }
```

3、加入同步队列之后，acquireQueued方法会进行自旋以及阻塞自己，在前继结点是头结点的时候会去尝试获取同步状态，一直循环

```java
    /**
     * 以独占不间断模式获取已在队列中的线程
     * @return 等待中发生中断，则返回true
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean interrupted = false;
        try {
            for (;;) {
                final Node p = node.predecessor();
                // 1、只有当前继结点是头结点，才去尝试获取同步状态
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return interrupted;
                }
                // 2、阻塞自己并将中断情况进行异或处理
                if (shouldParkAfterFailedAcquire(p, node))
                    interrupted |= parkAndCheckInterrupt();
            }
        } catch (Throwable t) {
            cancelAcquire(node);// 取消获取同步状态
            if (interrupted)
                selfInterrupt();
            throw t;
        }
    }
```



#### release

```java
    /**
    以独占模式释放
    如果tryRelease返回 true，则通过解除阻塞一个或多个线程tryRelease实现 */
    public final boolean release(int arg) {
        // 1、尝试释放同步状态
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);// 2、这里会唤醒h的后继节点线程
            return true;
        }
        return false;
    }
```

1、掉用子类实现的释放同步状态方法tryRelease
2、唤醒头结点(即获取同步状态的当前线程)的后继节点线程

### acquireShared

共享式获取同步状态

```java
    /**
	 * 在共享模式下获取，忽略中断
	 * 通过首先调用至少一次tryAcquireShared ，成功返回
	 * 否则线程会排队，可能会反复阻塞和解除阻塞，调用tryAcquireShared直到成功
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }
```

1、调用子类实现的tryAcquireShared方法获取共享同步状态
2、返回0即获取失败，则调用doAcquireShared方法自旋

```java
    private void doAcquireShared(int arg) {
        // 1、以共享模式添加到同步队列
        final Node node = addWaiter(Node.SHARED);
        boolean interrupted = false;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    // 2、在前继结点是头结点时，才去尝试获取共享同步状态
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        return;
                    }
                }
                // 3、获取失败则阻塞
                if (shouldParkAfterFailedAcquire(p, node))
                    interrupted |= parkAndCheckInterrupt();
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        } finally {
            if (interrupted)
                selfInterrupt();
        }
    }
```

可以看到这里的自旋过程其实和acquire那边的自旋差不多，主要差别就在于tryAcquireShared方法，毕竟退出自旋的唯一出口都是try获取同步状态成功。

#### releaseShared

```java
    public final boolean releaseShared(int arg) {
        // 1、调用子类实现的try释放方法
        if (tryReleaseShared(arg)) {
            // 2、唤醒后继节点
            doReleaseShared();
            return true;
        }
        return false;
    }
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }
```

可以看到在AQS层面，这里和独占式的释放区别不大。而在tryReleaseShared需要保证同步状态的安全释放，因为共享模式下，可能会有多个线程同时进行释放操作，这里就需要CAS+失败重试了。



## Condition接口

在synchronized关键字中，有wait()和notify()等监视器方法来实现等待/通知机制。Lock和Condition接口结合使用也能实现等待/通知模式。

Condition对象由Lock对象创建出来，即Condition依赖于Lock。

其使用方式可以去看一下阻塞队列中ArrayBlockingQueue的使用示范。下面这个是自己实现的一个简单的阻塞队列：

```java
/**
 * 借助数组、可重入锁、Condition实现的阻塞队列
 *
 * @author fzk
 * @date 2022-01-03 22:14
 */
public class MyArrayBlockQueue<T> {
    private final Object[] items;
    private int addIndex, removeIndex, count;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    public MyArrayBlockQueue(int capacity) {
        if (capacity < 1)
            throw new IllegalArgumentException("capacity can not smaller than 1");
        items = new Object[capacity];
        count = 0;
        addIndex = 0;
        removeIndex = 0;
        lock = new ReentrantLock();
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    // 添加一个元素，如果队列满了，则进入等待状态
    public void put(T t) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)// while防止虚假唤醒
                notFull.await();// 等非满信号

            items[addIndex] = t;
            if (++addIndex == items.length) addIndex = 0;
            ++count;
            notEmpty.signal();// 放一个非空信号
        } finally {
            lock.unlock();
        }
    }

    // 移除并返回队首元素，如果数组为空，则等待
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)// while防止虚假唤醒
                notEmpty.await();// 等待非空信号

            Object t = items[removeIndex];
            items[removeIndex] = null;
            if (++removeIndex == items.length) removeIndex = 0;
            count--;
            notFull.signal();// 放非满信号
            return (T) t;
        } finally {
            lock.unlock();
        }
    }
}
```

### 等待队列

在可重入锁的newCondition方法中，最终是新建了AQS的内部类`ConditionObject`，其部分代码如下：

```java
public class ConditionObject implements Condition, java.io.Serializable {
    private static final long serialVersionUID = 1173984872572414699L;
    /** condition队列第一个结点 */
    private transient Node firstWaiter;
    /** condition队列最后一个结点 */
    private transient Node lastWaiter;
    // 省略很多方法
}
```

可以看到每个ConditionObject内部包含了一个等待队列，并且这里和AQS的同步队列用的都是AQS的内部类Node来实现。

在对象监视器模型上(即synchronized)，一个对象只有一个同步队列和等待队列，而Lock和Condition拥有一个同步队列和多个等待队列：

![Condition等待队列](JavaSE.assets/Condition等待队列.jpg)



### await

首先从Condition的await()方法入手：当一个线程调用了await()方法，其实是从其同步队列队首(获取到了锁)移到等待队列中

```java
/**
实现可中断条件等待。
1、如果当前线程被中断，则抛出 InterruptedException。
2、保存getState返回的锁定状态。
3、使用保存状态作为参数调用release ，如果失败则抛出 IllegalMonitorStateException。
4、阻塞直到发出信号或中断。
5、通过以保存状态作为参数调用特定版本的acquire 。
6、如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException
 */
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    // 1、加入到等待队列
    Node node = addConditionWaiter();
    // 2、释放锁，并唤醒同步队列的后继结点
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    // 3、只要没有在同步队列中，线程就一直等待
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
	// 4、回到同步队列啦，通过acquireQueued方法要么获取到锁，要么在同步队列中等待
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    // 5、获取到锁了，取消等待队列中已经取消了的结点
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

1、加入到等待队列：addConditionWaiter()，因为没有同步机制，这个方法就很简单

```java
/** 这里没有像同步队列那样进行CAS和失败重试，因为加入等待队列的线程必然获取到了锁，无须对添加操作进行同步 */
private Node addConditionWaiter() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node t = lastWaiter;
    // If lastWaiter is cancelled, clean out.
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        t = lastWaiter;
    }
	// 新建等待状态为CONDITION的结点
    Node node = new Node(Node.CONDITION);

    if (t == null)
        firstWaiter = node;
    else
        t.nextWaiter = node;
    lastWaiter = node;
    return node;
}
```

这里有一个非常有意思的操作，Node原本是双向链表，但是这里仅仅维护了其nextWaiter，其pre和next都是null。可以去看看Node的代码。

2、释放锁，并唤醒同步队列的后继结点，这里的方法内部是调用了AQS的release()方法

### signal

通知信号操作如下：

```java
/** 将等待时间最长的线程（如果存在）从该条件的等待队列移动到拥有锁的同步队列 */
public final void signal() {
    // 1、检查是否获取到排它锁
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    // 2、将第一个等待者从condition队列移到同步队列
    if (first != null)
        doSignal(first);
}
```

1、首先要检查此线程是否获取到排它锁，获取到才能去发出通知信号，否则报异常；

2、将第一个等待者从等待队列移到同步队列

```java
private void doSignal(Node first) {
    // CAS+失败重试，直到将等待队列第一个等待者加入同步队列
    do {
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}
```



## ReentrantLock

来自于微信公众号，具体看水印：

![AQS](JavaSE.assets/AQS.png)



## 读写锁

读写锁和排它锁不同之处在于，读是共享的，分离了读锁和写锁，使得其并发性相对于排它锁有一定的提高。

```java
/** ReadWriteLock维护一对关联的locks ，一个用于只读操作，一个用于写入。 
 * 只要没有写者，读锁可能被多个读线程同时持有。 写锁是独占的
 */
public interface ReadWriteLock {
    /** 返回读锁 */
    Lock readLock();
    /** 返回写锁 */
    Lock writeLock();
}
```

其实现是`ReentrantReadWriteLock`。

```java
/**
 * 公平模式：线程使用近似到达顺序策略竞争进入；如果持有写锁或存在等待写入线程，则尝试获取公平读锁（不可重入）的线程将阻塞
 * 锁定降级：重入还允许从写锁降级为读锁，方法是获取写锁，然后是读锁，然后释放写锁；然而，从读锁定写锁定升级是不可能的
 * Condition支持：写锁提供Condition实现，这个Condition只能与写锁一起使用；读锁不支持Condition
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
       
    private final ReentrantReadWriteLock.ReadLock readerLock;// 读锁
    
    private final ReentrantReadWriteLock.WriteLock writerLock;// 写锁
    
    final Sync sync;// 自定义同步器，AQS的子类
}
```

对于读写锁的分析，主要在于分析：读写状态设计、写锁的获取与释放、读锁的获取与释放、锁降级。
在这个过程中，一个极其重要的角色是Sync这个抽象类，是AQS的子类，它有两个子类分别为：`NonfairSync`和`FairSync`

### 读写状态

在可重入锁ReentrantLock中，同步状态state表示锁被线程重复获取的次数。而读写锁则是在自定义的同步器上去实现一个表示同步状态的整型变量能维护多个读线程或者一个写线程。

可以想象得到的是，要区分读和写，可以从这个int变量的位数上下手，高位用来表示读，低位表示写。

> 写状态表示为：(state & 0x0000FFFF)；写状态+1表示为：state+1
>
> 读状态表示为：(state>>>16)；读状态+1表示为：state+(1<<16)
>
> 所以，当state==0时，无锁；(state>>>16)>0，读锁被获取；(state & 0x0000FFFF)>0，写锁被获取。

### 写锁获取与释放

先从稍微简单的写锁开始。写锁其实和可重入锁ReentrantLock差不多。

读锁的获取首先是AQS的acquire(1)操作：

```java
/**以独占模式获取，忽略中断; 通过至少调用一次tryAcquire ,成功返回。否则线程会排队，可能会反复阻塞和解除阻塞，调用tryAcquire直到成功。 此方法可用于实现方法Lock.lock */
public final void acquire(int arg) {
    if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

首先是tryAcquire进行写锁获取：进入的是读写锁的内部Sync实现：

```java
protected final boolean tryAcquire(int acquires) {
    Thread current = Thread.currentThread();
    int c = getState();
    int w = exclusiveCount(c);// 位操作，取低16位，即写状态
    if (c != 0) {
        // 如果有读线程 或者 写线程不是自己 则失败
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // 写锁重入
        setState(c + acquires);// 写状态+1
        return true;
    }
    // CAS获取锁操作
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```

1、如果读状态非零或写入状态非零且写线程者不是自己，则失败。 
2、如果获取重入锁次数饱和，则失败
3、否则，如果该线程是可重入获取或队列策略允许，则该线程有资格获得锁定。如果是这样，更新状态并设置所有者。

注意：这里的writeShouldBlock()方法在NonFairSync中是默认返回false的，即允许写线程立刻竞争锁。(用于公平锁实现的)

写锁的释放同可重入锁ReentrantLock类似，每次释放都是state-1

### 读锁获取与释放

读锁获取首先是调用`aqs.acquireShared(1)`；

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

接下里进入读写锁内部实现Sync的tryAcquireShared方法：

```java
protected final int tryAcquireShared(int unused) {
    /*

    */
    Thread current = Thread.currentThread();
    int c = getState();
    // 1.如果写锁被其他线程持有，失败
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;
    int r = sharedCount(c);// 将state无符号右移16位，获取读状态
    // 2.等待队列首结点不是写线程，且CAS成功的话，进行一些保存获取读锁次数的操作，保存在ThreadLocal中了(非公平锁下)
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null ||
                rh.tid != LockSupport.getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    // 3.完全重试获取读锁
    return fullTryAcquireShared(current);
}
```

`readerShouldBlock()`这个方法将判断读线程是否需要阻塞，为什么在写锁未被获取的情况下依旧要去判断是否阻塞呢？为了防止**写线程饥饿**！判断方式：如果等待队列头结点是写线程则返回true
1、如果写锁被另一个线程持有，失败
2、当没有等待队列首结点不是写线程，且尝试通过 CASing 状态和更新计数来授予 
3、如果步骤 2 因线程明显不合格或 CAS 失败或计数饱和而失败，则链接到具有完整重试循环的版本

上面第2步失败，进入第3步，有可能是要阻塞以等待写线程，也可能是CAS失败了(有其他读进程操作)，将进入循环尝试获取读锁：

```java
/**读取的获取的完整版本，它处理 CAS 未命中和在 tryAcquireShared 中未处理的重入读取*/
final int fullTryAcquireShared(Thread current) {
    HoldCounter rh = null;
    for (;;) {
        int c = getState();
        // 1.如果写锁被获取了
        if (exclusiveCount(c) != 0) {
            if (getExclusiveOwnerThread() != current)
                return -1;
            // else we hold the exclusive lock; blocking here
            // would cause deadlock.
        } 
        // 2.说明有写线程在队首等待(非公平锁下)
        else if (readerShouldBlock()) {
            // 确保不是读锁的重入，从这里直接根据第一个读线程判断重入，说明当有写线程等待时，最多只能有一个读线程进行读
            if (firstReader == current) {} 
            else {
                if (rh == null) { 省略... }
                if (rh.count == 0) // 如果是首次来获取读锁，需要阻塞等待退让给写锁
                    return -1;
            }
        }
        if (sharedCount(c) == MAX_COUNT) throw new Error("Maximum lock count exceeded");
        // 3.CAS操作
        if (compareAndSetState(c, c + SHARED_UNIT)) {
            // 省略一些保存本线程获取次数的操作
            return 1;
        }
    }
}
```

这里的循环的操作：

1、如果写锁被获取，且此时不是写锁线程来获取读锁，从而锁降级的话，则返回-1
2、如果有写线程在队首等待，则获取其获得读锁的次数，如果为0，意味着第一次获取读锁，则返回-1让其阻塞等待写线程；
3、否则CAS操作获取读锁

这里可以看出，当有写线程在队首等待时，这时只能发生读锁重入。

读锁的释放操作主要是AQS类的操作，这里不作深入探究。

### 公平锁与非公平锁的区别

ReentrantReadWriteLock内的Sync的实现子类有两个，分别是NonfairSync和FairSync，默认是非公平锁

```java
// 非公平锁
static final class NonfairSync extends Sync {
    final boolean writerShouldBlock() {
        return false; // writers can always barge
    }
    final boolean readerShouldBlock() {
        /*作为避免写入线程饥饿的启发式方法，如果暂时显示为队列头的线程（如果存在）是等待写入器，则阻塞。
        这只是一种概率效应，因为如果在其他尚未从队列中耗尽的已启用读取器后面有等待写入器，则新读取器不会阻塞
         */
        return apparentlyFirstQueuedIsExclusive();// 直接判断等待队列首是否为写线程
    }
}

// 公平锁
static final class FairSync extends Sync {
    final boolean writerShouldBlock() {
        return hasQueuedPredecessors();// 在等待队列不为空且队首线程不是本线程时返回true
    }
    final boolean readerShouldBlock() {
        return hasQueuedPredecessors();
    }
}
```

从这里是实现可以看出，非公平锁为了防止写线程饥饿，在读线程获取读锁前先判断等待队列队首是否为写线程，这时是仅允许读锁重入。

可以看出公平锁的话，就是在线程获取读锁或者写锁之前，先去判断等待队列是否有线程在等待，如果已经有了，则加入等待队列。




































## 阻塞队列

![阻塞队列](JavaSE.assets/阻塞队列.png)

在这里面的`ArrayBlockingQueue`和`PriorityBlockingQueue`实现比较简单，由一个可重入锁实现；
`LinkedBlockingQueue`用了两个可重入锁和一个`Condition`，有一定的操作；

### DelayQueue

`Delayed`元素的无界阻塞队列，其中一个元素只能在其延迟到期时被占用。 可以用来实现一些定时任务。

当元素的getDelay(TimeUnit.NANOSECONDS)方法返回小于或等于零的值时，就会发生过期。 
无法使用take或poll删除未过期的元素。

由PriorityQueue+ReentranLock+Condition实现

```java
public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
    implements BlockingQueue<E> {
    
    private final transient ReentrantLock lock = new ReentrantLock();
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    /**
     * 等待队列元素的第一个线程
 	 * Leader-Follower 模式
 	 * 每当队列的头部被一个具有较早到期时间的元素替换时，leader 字段将通过重置为 null 来无效，并且一些等待线程（但不一定是当前的leader）被发出信号。 
 	 * 因此，等待线程必须准备好在等待期间获得和失去领导权。
     */
    private Thread leader;

    /** 当新元素在队列头部可用或新线程可能需要成为领导者时发出条件信号  */
    private final Condition available = lock.newCondition();
}
```

**添加操作offer(E e)**

```java
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.offer(e);
            if (q.peek() == e) {
                leader = null;
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
```

添加就比较简单，直接获取到锁，然后向优先队列加入元素即可。

**阻塞获取take()**


```java
/**检索并删除此队列的头部，必要时等待，直到此队列上有一个具有过期延迟的元素可用*/
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();// 先加锁
    try {
        for (;;) {
            E first = q.peek();
            if (first == null)
                available.await();// 队列无元素直接无限期等待
            else {
                long delay = first.getDelay(NANOSECONDS);// 获取首元素延迟时间，小于等于0即为可用
                if (delay <= 0L)
                    return q.poll();
                first = null; // 手动赋值null，第一个元素是可能被其他线程获取的，这里必须释放引用
                if (leader != null)// 说明此线程不是第一个等待的线程，后面排队去吧
                    available.await();
                else {
                    Thread thisThread = Thread.currentThread();// 说明此线程是第一个要等待的线程
                    leader = thisThread;
                    try {
                        available.awaitNanos(delay);// 作为第一个等待的线程，只需要等待第一个元素需要的时间
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```
1、队列无元素，则直接等待
2、第一个元素没到期，如果leader不为null，则说明已有线程在限时等待队首元素，为了减少不必要的限时等待，直接等待；
3、第一个元素没到期，且leader为null，那么让此线程成为leader并限时等待；等待完成将leader置null；重新循环获取队首元素

需要注意的是，成为leader进程不是一定就能获取到队首元素的，从此方法的流程上来看，只要在队首元素过期时间到了，此时有线程来就直接获取成功，而不会去管有没有leader进程。leader进程的作用主要是**用于最小化不必要的定时等待**.

#### Delayed

DelayQueue中的元素必须实现Delayed接口。

```java
/** 用于标记在给定延迟后应采取行动的对象
 * 此接口的实现必须定义一个compareTo方法，该方法提供与其getDelay方法一致的排序
 */
public interface Delayed extends Comparable<Delayed> {

    /** 以给定的时间单位返回与此对象关联的剩余延迟
     * @param unit 时间单位
     * @return 剩余的延迟； 零或负值表示延迟已经过去
     */
    long getDelay(TimeUnit unit);
}
```

从这里的注释可以看出，compareTo方法的返回值应该和getDelay拥有同样的语义顺序。
换一句话说就是，最早过期的，其compareTo实现也应该是要排在队首的。
为什么呢：优先队列队首必须是最早过期的，但是其排序使用的是compareTo方法，而不是getDelay方法。

如何实现Delayed接口呢？可以从ScheduledThreadPoolExecutor.ScheduledFutureTask这个内部类来看.

下面是自己在看了这个内部类后，实现的一个类：

```java
/**
 * @author fzk
 * @date 2022-01-06 23:20
 */
public class MyDelayed implements Delayed {
    private volatile long expireTime;// 到期时间
    public MyDelayed(long expireTime){
        this.expireTime=expireTime;
    }

    // 返回的值代表还有多久到期，一般使用纳秒
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expireTime - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        // 这里必须保持和getDelay一致语义
        if (o == this) return 0;
        if (o instanceof MyDelayed)
            return Long.compare(expireTime, ((MyDelayed) o).expireTime);
        else
            return Long.compare(
                    getDelay(TimeUnit.NANOSECONDS),
                    o.getDelay(TimeUnit.NANOSECONDS));
    }
}
```

### SynchronousQueue

留坑...





## ConcurrentHashMap

一个哈希表，支持检索的完全并发性和更新的高预期并发性。所有操作都是线程安全的，检索操作也不需要锁定.

检索操作（包括get ）通常不会阻塞，因此可能与更新操作（包括put和remove ）重叠。

检索反映了最近完成的更新操作的结果。 

迭代器被设计为一次只能被一个线程使用。 

请记住，包括size 、 isEmpty和containsValue在内的聚合状态方法的结果通常仅在map未在其他线程中进行并发更新时才有用。 否则，这些方法的结果反映的瞬态状态可能足以用于监测或估计目的，但不适用于程序控制。

此外，为了与此类的先前版本兼容，构造函数可以选择指定预期的concurrencyLevel作为内部大小调整的附加提示

ConcurrentHashMap在1.8版本采用的是CAS+synchronized来实现的并发处理。

### 属性

下面是ConcurrentHashMap的重要的属性：

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V
    implements ConcurrentMap<K,V>, Serializable {
    /** bin 数组。 第一次插入时延迟初始化。 大小始终是 2 的幂。 由迭代器直接访问 */
    transient volatile Node<K,V>[] table;

    /** 下一个要使用的表； 仅在调整大小时非空 */
    private transient volatile Node<K,V>[] nextTable;

    /** 基本计数器值，主要在没有争用时使用，但也用作表初始化竞争期间的后备。 通过 CAS 更新 */
    private transient volatile long baseCount;

    /** 表初始化和调整大小控制。 
    如果为负，则表正在初始化或调整大小：-1 表示初始化，否则 -（1 + 活动调整大小线程的数量）。 
    否则，当 table 为空时，保存创建时使用的初始表大小，或默认为 0。 
    初始化后，保存下一个要调整表格大小的元素计数值
     */
    private transient volatile int sizeCtl;

    /** 调整大小时要拆分的下一个表索引（加一个）*/
    private transient volatile int transferIndex;

    /** 调整大小和/或创建 CounterCell 时使用自旋锁（通过 CAS 锁定）*/
    private transient volatile int cellsBusy;

    /** 计数单元表。 当非空时，大小是 2 的幂。主要是size()统计个数会使用 */
    private transient volatile CounterCell[] counterCells;
    
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;// 这里的val和next都用volatile保证可见性
        // 方法省略
    }
}
```

ConcurrentHashMap中保存元素的是Node数组，此Node用volatile修饰其val和next指针，保证了并发可见性。

### put

首先来看它的put操作：

```java
/** key和value都不能为null */
public V put(K key, V value) {
    return putVal(key, value, false);
}

/** Implementation for put and putIfAbsent */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());// 高16位与低16位异或，再通过与运算屏蔽掉符号位
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh; K fk; V fv;
        // 0、第一次插入，初始化表
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();// 初始化表结构，具体见下面
        // 1、桶内元素为空，CAS设置头结点，成功就直接退出了
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                break;                   // no lock when adding to empty bin
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else if (onlyIfAbsent // check first node without acquiring lock
                 && fh == hash
                 && ((fk = f.key) == key || (fk != null && key.equals(fk)))
                 && (fv = f.val) != null)
            return fv;
        else {
            V oldVal = null;
            // 2、使用synchronized锁住头结点
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 2.1 是链表
                    if (fh >= 0) {
                        binCount = 1;
                        // 遍历链表找是否有相同的key，或者插入到末尾，同时统计链表长度
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value);
                                break;
                            }
                        }
                    }
                    // 2.2 说明是红黑树
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                              value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                    else if (f instanceof ReservationNode)
                        throw new IllegalStateException("Recursive update");
                }
            }
            if (binCount != 0) {
                // 链表长度超过8，树化：方法内会判断容量，小于64则优先选择扩容
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    // 3、数量加+1操作，这里的操作相当的复杂，暂时看不懂
    addCount(1L, binCount);
    return null;
}
```

0、第一次插入，初始化表
1、桶内元素为空，CAS设置头结点，成功就直接退出了
2、使用synchronized锁住头结点
2.1、是链表的话，则遍历链表，有则修改，无则插入末尾，同时统计链表长度，如果长度超过8，尝试树化方法。树化方法判断hash表容量小于64的话则优先扩容，否则将链表转为红黑树。
2.2、是红黑树的话，调用红黑树的putVal方法
3、数量+1操作，非常复杂

#### initTable

```java
/** 使用 sizeCtl 中记录的大小初始化表 */
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        // 负数表示有线程正在初始化或者调整表大小，这里应该是初始化
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // 初始化竞争失败，让出CPU执行权，自旋
        else if (U.compareAndSetInt(this, SIZECTL, sc, -1)) {// CAS设sizeCTL为-1表示竞争初始化权
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

从源码中可以发现 ConcurrentHashMap 的初始化是通过**自旋和 CAS** 操作完成的。里面需要注意的是变量 `sizeCtl` ，它的值决定着当前的初始化状态：

1、-1  说明正在初始化
2、-N 说明有N-1个线程正在进行扩容
3、表示 table 初始化大小，如果 table 没有初始化
4、表示 table 容量，如果 table已经初始化

### get

get操作非常简单，由于

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    // 1.计算hash
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        // 2、如果头结点就是要找的，直接返回
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        // 3、头结点的hash小于0，说明正在扩容或者是红黑树，用结点类自定义的find方法进行查找
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        // 4、是链表，直接遍历查找
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

1.计算hash
2、如果头结点就是要找的，直接返回
3、头结点的hash小于0，说明正在扩容或者是红黑树，用结点类自定义的find方法进行查找
4、是链表，直接遍历查找

### remove

```java
public V remove(Object key) {
    return replaceNode(key, null, null);
}

/** 四个公共删除/替换方法的实现： 用 v 替换节点值，条件是 cv 匹配（如果非空）。 如果结果值为空，则删除 */
final V replaceNode(Object key, V value, Object cv) {
    // 1、计算hash
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 ||
            (f = tabAt(tab, i = (n - 1) & hash)) == null)
            break;
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            boolean validated = false;
            // 2、锁住头结点
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 2.1、是链表
                    if (fh >= 0) {
                        validated = true;
                        for (Node<K,V> e = f, pred = null;;) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                V ev = e.val;
                                if (cv == null || cv == ev ||
                                    (ev != null && cv.equals(ev))) {
                                    oldVal = ev;
                                    if (value != null)// 如果是替换而不是删除操作的话，直接更新value
                                        e.val = value;
                                    else if (pred != null)// 如果有前继结点，前继结点直接指向后继结点即可
                                        pred.next = e.next;
                                    else// 如果是链表头元素，CAS设置下一个为头结点
                                        setTabAt(tab, i, e.next);
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)
                                break;
                        }
                    }
                    // 2.2、红黑树
                    else if (f instanceof TreeBin) {
                        validated = true;
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> r, p;
                        if ((r = t.root) != null &&
                            (p = r.findTreeNode(hash, key, null)) != null) {
                            V pv = p.val;
                            if (cv == null || cv == pv ||
                                (pv != null && cv.equals(pv))) {
                                oldVal = pv;
                                if (value != null)
                                    p.val = value;
                                else if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                    else if (f instanceof ReservationNode)
                        throw new IllegalStateException("Recursive update");
                }
            }
            if (validated) {
                if (oldVal != null) {
                    // 3、如果是删除操作，数量-1操作
                    if (value == null)
                        addCount(-1L, -1);
                    return oldVal;
                }
                break;
            }
        }
    }
    return null;
}
```

这几步其实和put操作差不了太多。

## ConcurrentLinkedQueue

实现线程安全的队列，有两种方式：
一种是阻塞算法实现的阻塞队列，可以用一个锁(`ArrayBlockingQueue`)或者两个锁(`LinkedBlockingQueue`)来实现；
另一种是非阻塞算法实现的非阻塞队列，使用CAS+失败重试实现(`ConcurrentLinkedQueue`)

先看一下大致代码：

```java
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E>
        implements Queue<E>, java.io.Serializable {

    transient volatile Node<E> head;

    private transient volatile Node<E> tail;
    
    public ConcurrentLinkedQueue() {
        head = tail = new Node<E>();
    }

    static final class Node<E> {
        volatile E item;
        volatile Node<E> next;

        /**
         * Constructs a node holding item.  Uses relaxed write because
         * item can only be seen after piggy-backing publication via CAS.
         */
        Node(E item) {
            ITEM.set(this, item);
        }

        /** Constructs a dead dummy node. */
        Node() {}

        void appendRelaxed(Node<E> next) {
            // assert next != null;
            // assert this.next == null;
            NEXT.set(this, next);
        }

        boolean casItem(E cmp, E val) {
            // assert item == cmp || item == null;
            // assert cmp != null;
            // assert val == null;
            return ITEM.compareAndSet(this, cmp, val);
        }
    } 
}
```

可以看到无参构造函数给头结点设置了一个空元素结点。

### offer

先看插入操作：

```java
/** 在此队列的尾部插入指定元素。 由于队列是无界的，这个方法永远不会返回false */
public boolean offer(E e) {
    // 1、将元素包装为结点
    final Node<E> newNode = new Node<E>(Objects.requireNonNull(e));
	// 2、一直循环寻找正确尾结点并CAS添加新结点
    for (Node<E> t = tail, p = t;;) {
        Node<E> q = p.next;
        // 2.1、如果p此时就是末尾，尝试CAS添加到末尾结点
        if (q == null) {
            // p is last node
            if (NEXT.compareAndSet(p, null, newNode)) {
                // Successful CAS is the linearization point
                // for e to become an element of this queue,
                // and for newNode to become "live".
                // 说明此时tail指针离真正的尾结点至少差了2个结点了，CAS更新tail指针
                if (p != t) // hop two nodes at a time; failure is OK
                    TAIL.weakCompareAndSet(this, t, newNode);
                return true;
            }
            // Lost CAS race to another thread; re-read next
        }
        // 2.2、遇到环结点，说明此节点已被抛弃，需要从新找到新的tail指针或者从head指针向下遍历。如果tail指针未变，以为着tail指针指向的结点也被抛弃了，那就从head指针开始挨个遍历找尾结点。
        else if (p == q)
            // We have fallen off list.  If tail is unchanged, it
            // will also be off-list, in which case we need to
            // jump to head, from which all live nodes are always
            // reachable.  Else the new tail is a better bet.
            p = (t != (t = tail)) ? t : head;
        else
            // 2.3、如果出现新的尾结点，则p指向尾结点，否则指向其next
            p = (p != t && t != (t = tail)) ? t : q;
    }
}
```

这里要注意的是，**tail指针并没有一定就指向了队列的末尾结点**，而是在循环中去判断了是否经过tail找到了真正的尾结点。

这里为什么不强求tail指针指向尾结点呢？按书上所说，是指**通过增加对volatile变量的读操作，来减少对其的写操作，因为volatile变量的写开销远远大于读开销**。

这里的环结点是由于更新head指针后，将旧的head结点指向了自己构成了环，是被队列抛弃的结点。

### poll

出队列的head指针，同上也没有完全就指向队列头元素。当head结点有元素时，弹出元素，而不更新head指针；当head结点内没有元素时，才更新head指针。

```java
public E poll() {
    restartFromHead: for (;;) {
        // 每次循环会将p指向其下一个结点
        for (Node<E> h = head, p = h, q;; p = q) {
            final E item;
            // 1、p中的元素不为空且CAS操作成功的话，直接返回元素
            if ((item = p.item) != null && p.casItem(item, null)) {
                // Successful CAS is the linearization point
                // for item to be removed from this queue.
                // 说明head结点内的元素必然早已弹出，此时更新head指针
                if (p != h) // hop two nodes at a time
                    updateHead(h, ((q = p.next) != null) ? q : p);
                return item;
            }
            // 2、说明队列中没有元素了，更新head指针，返回null
            else if ((q = p.next) == null) {
                updateHead(h, p);
                return null;
            }
            // 3、遇到环结点，说明已被抛弃，重新获取最新的head指针再进行循环弹出元素
            else if (p == q)
                continue restartFromHead;
        }
    }
}
```

这里的操作逻辑就是找到第一个有元素的结点就直接CAS弹出这个元素，并去判断是否需要更新head指针。

这里的updateHead方法很重要：

```java
/** 尝试CAS操作将head指向p，成功的话，将就的head结点指向它自己作为哨兵 */
final void updateHead(Node<E> h, Node<E> p) {
    // assert h != null && p != null && (h == p || h.item == null);
    if (h != p && HEAD.compareAndSet(this, h, p))
        NEXT.setRelease(h, h);// 将就的head结点指向自己形成环
}
```





## 并发工具类

### CountDownLatch

多个线程等待其它的多个线程完成。虽然在下面这个简单任务中，主线程可以直接用Thread.join()方法来完成等待其它线程，但是在线程池这些线程不会死亡的时候，如果需要等待它们完成任务，这个工具就可以很好的派上用场。

使用案例：案例来自源码注释

```java
/**
 * 这种方式可以用来进行大任务分解，主线程等待各个线程完成子任务
 * @author fzk
 * @date 2022-01-15 14:23
 */
public class Driver {
    public static void main(String[] args) throws InterruptedException {
        int N = 10;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(N);

        for (int i = 0; i < N; ++i) // create and start threads
            new Thread(new Worker(startSignal, doneSignal)).start();

        System.out.println("all worker is ready...");
        startSignal.countDown();      // 让线程开始运行
        System.out.println("all worker start running...");
        doneSignal.await();           // 等待所有线程运行结束
    }

    static class Worker implements Runnable {
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        public void run() {
            try {
                startSignal.await();
                doWork();
                doneSignal.countDown();
            } catch (InterruptedException ex) {
            }
        }

        void doWork() throws InterruptedException {
            Thread.sleep(ThreadLocalRandom.current().nextLong(0L, 1000L));
            System.out.println(Thread.currentThread().getName()+" is running...");
        }
    }
}
```

在了解了使用之后，接下来看看如何实现的。想一下其实就能知道，必然是用到了AQS的同步队列了，在初始化的时候，指定参数为AQS的state，然后每次countDown都去用CAS操作将state-1，在state减少到0时，同步队列线程被释放了。这里需要注意的问题在于用AQS的话，需要去重写它的一些try方法，那么怎么重写，重写哪些呢？

```java
public class CountDownLatch {
    /** CountDownLatch的队列同步器. 用 AQS state 代表 需要等待线程countDown操作的数量. */
    private static final class Sync extends AbstractQueuedSynchronizer {
        // 这里构造函数中就将传入的需要等待的线程countDown操作的数量直接转换为AQS的state
        Sync(int count) {
            setState(count);
        }

        int getCount() { return getState(); }
		// 1.await()方法会调用，这里判断能否获取锁是判断state是否为0，并且这里在获取成功之后，并没有去改变state!!!
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }
		// 2.countDown()方法调用，这里返回true的话，将允许同步队列来获取锁
        protected boolean tryReleaseShared(int releases) {
           	// CAS操作将state-1
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;// 这里只有在将state减少到0的时候才会返回true
            }
        }
    }

    private final Sync sync;

    /** 构造一个用给定计数初始化的CountDownLatch */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /** 使当前线程等待直到闩锁倒计时为零，除非线程被中断 */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /** 减少锁存器的计数，如果计数达到零，则释放所有等待线程 */
    public void countDown() {
        sync.releaseShared(1);
    }

    /** 返回当前计数 */
    public long getCount() {
        return sync.getCount();
    }
}
```

其实从这里的代码可以看出CountDownLatch的实现非常简单：
1、它内部实现的队列同步器重写了的AQS的获取共享锁和释放共享锁的try方法
2、它在初始化的时候会将计数count直接作为AQS的state
3、它的await()方法会去获取共享锁，进而调用自己实现的tryAcquireShared()方法，此方法仅仅判断当前state是否为0
4、它的countDown()方法会去调用自己实现的tryReleaseShared()方法，将state进行CAS操作-1，到0则去唤醒所有同步线程上的线程
5、唤醒的线程去获取共享锁进而调用tryAcquireShared()方法发现state为0则直接退出await()方法

真的是非常的简单，自己去实现都是没有任何问题的。

### 同步屏障CyclicBarrier

同步屏障允许一组线程相互等待以达到共同的障碍点。 CyclicBarriers 在涉及固定大小的线程组的程序中很有用，这些线程组必须偶尔相互等待。 屏障被称为循环的，因为它可以在等待线程被释放后重新使用。
CyclicBarrier支持一个可选的Runnable命令，该命令在每个屏障点运行一次，在队伍中的最后一个线程到达之后，但在任何线程被释放之前。 此屏障操作对于在任何一方继续之前更新共享状态很有用。

使用示例：案例来自源码注释

```java
/**
 * CyclicBarrier(int parties,Runnable barrierAction)这个构造函数，
 * 在所有线程都到达屏障之后，优先执行barrierAction
 *
 * @author fzk
 * @date 2022-01-15 14:23
 */
public class Solver {

    final int N;
    final float[][] data;
    final CyclicBarrier barrier;

    public Solver(float[][] matrix) throws InterruptedException {
        data = matrix;
        N = matrix.length;
        Runnable barrierAction = () -> {
            System.out.println("屏障解除啦");
        };
        // 这里有先执行屏障解除动作，然后再去释放等待的线程
        barrier = new CyclicBarrier(N, barrierAction);

        List<Thread> threads = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            Thread thread = new Thread(new Worker(i));
            threads.add(thread);
            thread.start();
        }

        // wait until done
        for (Thread thread : threads)
            thread.join();
    }

    public static void main(String[] args) throws InterruptedException {
        new Solver(
                new float[][]{
                        {1f, 2f, 3f, 4f, 5f},
                        {1.1f, 1.2f, 1.3f, 1.4f, 1.5f}
                }
        );
    }

    class Worker implements Runnable {
        int myRow;

        Worker(int row) {
            myRow = row;
        }

        public void run() {
            try {
                barrier.await();
                System.out.println(Arrays.toString(data[myRow]));
            } catch (InterruptedException | BrokenBarrierException ex) {
            }
        }
    }
}
```

接下来看看源码：

```java
public class CyclicBarrier {
    /** 屏障的每次使用都表示为一个generation实例 */
    private static class Generation {
        Generation() {}                 // prevent access constructor creation
        boolean broken;                 // initially false
    }

    /** 用于保护屏障入口的锁 */
    private final ReentrantLock lock = new ReentrantLock();
    /** 线程需要等待在这个等待队列上 */
    private final Condition trip = lock.newCondition();
    /** 屏障需要等待的线程数量 */
    private final int parties;
    /** 所有线程到达屏障后优先触发的行为 */
    private final Runnable barrierCommand;
    /** 当前的generation */
    private Generation generation = new Generation();

    /** 还需要等待的线程数量；用reset()方法重置屏障的时候，将被重置为parties */
    private int count;
    
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }
    
    /** 下一轮循环了，更新屏障等待数量并唤醒等待队列的线程，由doWait()方法或reset()方法调用  */
    private void nextGeneration() {
        trip.signalAll();// 释放所有等待线程
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    /** 将当前屏障设置为已破坏，释放等待队列  */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }
    
    /** 确认屏障是否处于broken状态 */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /** 重置屏障; 如果任何线程当前在屏障处等待，他们将返回BrokenBarrierException
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // 将当前的屏障破坏
            nextGeneration(); // 创建新屏障
        } finally {
            lock.unlock();
        }
    }
}
```

#### await

```java
    /**
    如果当前线程不是最后到达的，则出于线程调度目的将其禁用并处于休眠状态，直到发生以下情况之一：
    1.最后一个线程到达； 或者
	2.其他一些线程中断当前线程； 或者
	3.其他一些线程中断了其他等待线程之一； 或者
	4.其他一些线程在等待屏障时超时； 或者
	5.其他一些线程在此屏障上调用reset */
	public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }
    /** 主要的屏障策略方法 */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;// 这里的目的在于拿到属于自己的屏障，因为屏障很可能被重置刷新

            if (g.broken)
                throw new BrokenBarrierException();
			// 如果当前线程被中断，则破坏屏障
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
			// 1、先将屏障等待线程数-1
            int index = --count;
            // 2、如果减少到0，则先执行屏障行为，生成下一代屏障
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 一直循环等待直到屏障等待所有线程到达，或出现中断
            for (;;) {
                try {
                    if (!timed)
                        trip.await();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    //省略
                }
                // 省略
            }
        } finally {
            lock.unlock();
        }
    }
```

这里的await操作就比较简单粗暴：
1、获取锁
2、等待数量-1
3、如果都到达了屏障，则优先执行屏障行为，再释放所有等待线程，生成下一代屏障
4、否则，一直等待直到被唤醒或中断或屏障被破坏



### Semaphore

信号量，控制并发访问的线程数量。可以用来做流量控制，如数据库连接。

```java
/**计数信号量，从概念上讲，信号量维护一组许可
初始化为 1 的信号量，并且使用时最多只有一个可用的许可，可以用作互斥锁。 这通常被称为二进制信号量，因为它只有两种状态：一个可用许可，或零个可用许可。 当以这种方式使用时，二进制信号量具有属性（与许多java.util.concurrent.locks.Lock实现不同），“锁”可以由所有者以外的线程释放（因为信号量没有所有权的概念）。 这在一些专门的上下文中很有用，例如死锁恢复

通常，用于控制资源访问的信号量应该被初始化为公平的，以确保没有线程因访问资源而被饿死。 当使用信号量进行其他类型的同步控制时，非公平排序的吞吐量优势通常超过公平性考虑。

此类还提供方便的方法来一次acquire和release多个许可。 这些方法通常比循环更有效和有效。 但是，它们没有建立任何优先顺序。 例如，如果线程 A 调用s.acquire(3 ) 并且线程 B 调用s.acquire(2) ，并且有两个许可可用，则不能保证线程 B 将获得它们，除非它的获取首先出现并且 Semaphore s是在公平模式下
*/
```

案例：来自Semaphore的注释：

```java
// 这是一个使用信号量来控制对项目池的访问的类
public class Pool {
    private static final int MAX_AVAILABLE = 100;
    private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

    public Object getItem() throws InterruptedException {
        available.acquire();
        return getNextAvailableItem();
    }

    public void putItem(Object x) {
        if (markAsUnused(x))
            available.release();
    }

    // Not a particularly efficient data structure; just for demo

    protected Object[] items = new Object[MAX_AVAILABLE];
    protected boolean[] used = new boolean[MAX_AVAILABLE];

    protected synchronized Object getNextAvailableItem() {
        for (int i = 0; i < MAX_AVAILABLE; ++i) {
            if (!used[i]) {
                used[i] = true;
                return items[i];
            }
        }
        return null; // not reached
    }

    protected synchronized boolean markAsUnused(Object item) {
        for (int i = 0; i < MAX_AVAILABLE; ++i) {
            if (item == items[i]) {
                if (used[i]) {
                    used[i] = false;
                    return true;
                } else
                    return false;
            }
        }
        return false;
    }
}
```

先猜测一下如何实现的：AQS的state会被初始化为给定的信号量，每次acquire都回去将state-1，每次release都将state+1

#### 公平锁与非公平锁

Semaphore有公平锁和非公平锁两种实现方式：

```java
public class Semaphore implements java.io.Serializable {
    
    private final Sync sync;
    
    /** Semaphore对于AQS的同步器实现；用AQS的state代表许可证数量，分为公平和非公平两种 */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        Sync(int permits) { setState(permits); }
		// 非公平实现：CAS+失败重试，将许可证数量减少，并返回剩余的许可证数量(可能为负数)
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
		// 释放许可证则CAS操作将state+释放的许可证数量
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next))
                    return true;
            }
        }
		// 循环CAS直至许可证数量减少成功
        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }
		// 将许可证数量归0
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }
    /** 非公平同步器*/
    static final class NonfairSync extends Sync {
        NonfairSync(int permits) { super(permits);}
		// 非公平锁就会直接去尝试获取许可证
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /** 公平同步器 */
    static final class FairSync extends Sync {
        FairSync(int permits) {  super(permits); }
		// 公平锁则先检查同步队列有没有线程在等待，没有再去获取许可证
        protected int tryAcquireShared(int acquires) {
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }
}
```

这里的公平锁和非公平锁与之前那些实现差不多，区别都是在于有没有先去检查同步队列。

接下里就看看acquire操作，Semaphore与之前的那些锁不同之处在于，它可以一次获取多个许可证，已经释放多个许可证

#### acquire

```java
/**
获取给定数量的许可证，阻塞直到所有许可都可用，或者线程被中断。
如果可用的许可不足，则当前线程将被禁用以用于线程调度目的并处于休眠状态，直到发生以下两种情况之一：
其他一些线程为此信号量调用其中一个release方法，并且当前线程接下来被分配许可并且可用许可的数量满足该请求； 或者
其他一些线程中断当前线程。
如果当前线程：
在进入此方法时设置其中断状态； 或者
在等待许可时被打断，
然后抛出InterruptedException并清除当前线程的中断状态。 将分配给该线程的任何许可改为分配给尝试获取许可的其他线程，就好像通过调用release()使许可可用一样。
参数：
许可证 - 获得的许可证数量
抛出：
InterruptedException – 如果当前线程被中断
IllegalArgumentException – 如果permits是负数
*/
public void acquire(int permits) throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException();
    sync.acquireSharedInterruptibly(permits);
}
```





### Exchanger

线程间交换数据



## ThreadPoolExecutor

留坑...





# 序列化与反序列化

Java中的序列化允许将对象转换为流，可以通过网络发送或将其保存为文件或存储在数据库中以供以后使用。

反序列化是将对象流转换为要在我们的程序中使用的实际 Java 对象的过程。

如果需要将某个对象保存到磁盘上或者通过网络传输，那么这个类应该实现**Serializable**接口或者**Externalizable**接口之一。

资料来源：https://www.cnblogs.com/9dragon/p/10901448.html 以及 Serializable接口的注释

## Serializable

Serializable接口是一个标记接口，不用实现任何方法。一旦实现了此接口，该类的对象就是可序列化的。

### 序列化版本号

可序列化的类可以通过声明一个名为"serialVersionUID"的字段来显式声明自己的serialVersionUID，该字段必须是static、final并且类型为long ：`ANY-ACCESS-MODIFIER static final long serialVersionUID = 42L;`

序列化运行时将版本号与每个可序列化类相关联，在反序列化期间使用该版本号来**验证**序列化对象的发送者和接收者是否已为该对象加载了与序列化兼容的类。 

如果接收者为对象加载了一个类，该对象的 serialVersionUID 与相应发送者的类不同，则反序列化将导致InvalidClassException 。 

如果可序列化类没有显式声明 serialVersionUID，则序列化运行时将根据类的各个方面为该类**计算默认的 serialVersionUID 值**。

但是，强烈建议所有可序列化的类都**显式声明 serialVersionUID 值**，因为默认的 serialVersionUID 计算对类细节高度敏感，这些细节可能因编译器实现而异，因此可能会在反序列化期间导致意外的InvalidClassException。同时当修改了这个类的某些字段，就会导致计算的版本号不一致，从而报错。

还强烈建议显式 serialVersionUID 声明尽可能使用**private修饰符**，serialVersionUID 字段不能用作继承成员。 数组类不能显式声明 serialVersionUID，因此它们始终具有默认计算值，但数组类无需匹配 serialVersionUID 值。

最后，当大幅修改了一个类的时候，可以考虑更改版本号，使得以前保存在硬盘的字节流对应的对象无效，不能转换为目前这个新类。

### 简单使用

```java
@Data
@AllArgsConstructor
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient int age;// 瞬态变量不会序列化
    private LocalDateTime localDateTime;
}


public class MySerialTest {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("D:/test.txt");
        try (FileOutputStream out = new FileOutputStream(path.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {

            LocalDateTime localDateTime = LocalDateTime.now();
            Person person1 = new Person("fzk1", 20, localDateTime);
            Person person2 = new Person("fzk2", 20, localDateTime);
            oos.writeObject(person1);
            oos.writeObject(localDateTime);
            oos.writeObject(person2);
        }

        try (InputStream in = new FileInputStream(path.toFile());
             ObjectInputStream oi = new ObjectInputStream(in)
        ) {
            Person person1 = (Person) oi.readObject();
            LocalDateTime localDateTime = (LocalDateTime) oi.readObject();
            Person person2 = (Person) oi.readObject();
            System.out.println(person1 == person2);// false
            System.out.println(person1.getLocalDateTime() == localDateTime);// true
            System.out.println(person1.getLocalDateTime() == person2.getLocalDateTime());// true
        }
    }
}
```

从输出结果可以看出，**Java序列化同一对象，并不会将此对象序列化多次得到多个对象。**

**Java序列化算法**

> 1、所有保存到磁盘的对象都有一个序列化编码号
> 2、当程序试图序列化一个对象时，会先检查此对象是否已经序列化过，只有此对象从未（在此虚拟机）被序列化过，才会将此对象序列化为字节序列输出。
> 3、如果此对象已经序列化过，则直接输出编号即可

### 序列化问题及解决方案

**java序列化算法存在的问题？**

由于java序利化算法不会重复序列化同一个对象，只会记录已序列化对象的编号。**如果序列化一个可变对象（对象内的内容可更改）后，更改了对象内容，再次序列化，并不会再次将此对象转换为字节序列，而只是保存序列化编号。**

所以在看Spring源码的时候，能看到很多属性都是被`transient`所修饰的。

**那么有什么更好的解决方案吗？**

1、使用transient修饰的属性，java序列化时，会忽略掉此字段，所以反序列化出的对象，被transient修饰的属性是默认值。对于引用类型，值是null；基本类型，值是0；boolean类型，值是false。

#### 自定义序列化

2、java提供了**可选的自定义序列化。**可以进行控制序列化的方式，或者对序列化数据进行编码加密等

在序列化和反序列化过程中需要特殊处理的类必须实现具有这些确切签名的特殊方法：

```java
/**
writeObject 方法负责为其特定类写入对象的状态，以便相应的 readObject 方法可以恢复它。 
可以通过调用 out.defaultWriteObject 来调用保存 Object 字段的默认机制。 
该方法不需要关注属于其超类或子类的状态。 
通过使用 writeObject 方法或使用 DataOutput 支持的原始数据类型的方法将各个字段写入 ObjectOutputStream 来保存状态。*/
private void writeObject(java.io.ObjectOutputStream out) throws IOException{

}
/**
readObject 方法负责从流中读取并恢复类字段。 
它可以调用 in.defaultReadObject 来调用用于恢复对象的非静态和非瞬态字段的默认机制。 
defaultReadObject 方法使用流中的信息将保存在流中的对象的字段分配给当前对象中相应命名的字段。 
这可以处理类已经演变为添加新字段的情况。 
该方法不需要关注属于其超类或子类的状态。 通过从 ObjectInputStream 读取各个字段的数据并对对象的适当字段进行分配来恢复状态。 DataInput 支持读取原始数据类型*/
private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{

}
/**
当序列化流不完整时，readObjectNoData()方法可以用来正确地初始化反序列化的对象。例如，使用不同类接收反序列化对象，或者序列化流被篡改时，系统都会调用readObjectNoData()方法来初始化反序列化的对象。*/
private void readObjectNoData() throws ObjectStreamException{

}
```

案例如下：将重要信息进行加密，将瞬态变量进行序列化

```java
@Data
@AllArgsConstructor
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient int age;// 瞬态变量不会序列化
    private LocalDateTime localDateTime;

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        MyAES myAES = new MyAES();
        out.writeObject(myAES.encryptHex(name));// 加密传输
        out.writeInt(age);// 将瞬态变量序列化
        out.defaultWriteObject();// 其他Object字段进行默认序列化
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        MyAES myAES = new MyAES();
        this.name = myAES.decryptStr(in.readObject().toString());// 解密
        this.age = in.readInt();// 解析瞬态变量
        in.defaultReadObject();
    }
}
```

#### 进一步自定义

```java
/**在将对象写入流时需要指定要使用的替代对象的可序列化类应使用精确签名实现此特殊方法：*/
private Object writeReplace() throws ObjectStreamException;
   
/**如果该方法存在并且可以从被序列化对象的类中定义的方法访问，则该 writeReplace 方法由序列化调用。 因此，该方法可以具有私有、受保护和包私有访问。 对该方法的子类访问遵循 java 可访问性规则。
当从流中读取实例时需要指定替换的类应该使用精确的签名实现这个特殊方法。*/
private Object readResolve() throws ObjectStreamException;
   
//此 readResolve 方法遵循与 writeReplace 相同的调用规则和可访问性规则。
```

writeReplace()的案例：

```java
@Data
@AllArgsConstructor
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient int age;// 瞬态变量不会序列化
    private LocalDateTime localDateTime;

    // 在序列化时，会先调用此方法，再调用writeObject方法。此方法可将任意对象代替目标序列化对象
    private Object writeReplace() throws ObjectStreamException {
        return List.of(name, age, localDateTime);
    }

    public static void main(String[] args) throws Exception {
        Path path = Paths.get("D:/test.txt");
        try (FileOutputStream out = new FileOutputStream(path.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {

            LocalDateTime localDateTime = LocalDateTime.now();
            Person person1 = new Person("fzk1", 20, localDateTime);
            oos.writeObject(person1);
        }

        try (InputStream in = new FileInputStream(path.toFile());
             ObjectInputStream oi = new ObjectInputStream(in)
        ) {
            List<Object> list = (List<Object>) oi.readObject();
            System.out.println(list);// 输出[fzk1, 20, 2022-01-11T18:31:39.027016]
        }
    }
}
```

readResolve()的案例：

```java
@Data
@AllArgsConstructor
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient int age;// 瞬态变量不会序列化
    private LocalDateTime localDateTime;

    // 在序列化时，会先调用此方法，再调用writeObject方法。此方法可将任意对象代替目标序列化对象
//    private Object writeReplace() throws ObjectStreamException {
//        return List.of(name, age, localDateTime);
//    }

    // 此方法在readeObject后调用。反序列化时替换反序列化出的对象，反序列化出来的对象被立即丢弃
    private Object readResolve() throws ObjectStreamException {
        Map<String, Object> map = new HashMap<>();
        map.put("name", this.name);
        map.put("age", this.age);
        map.put("localDateTime", this.localDateTime);
        return map;
    }

    public static void main(String[] args) throws Exception {
        Path path = Paths.get("D:/test.txt");
        try (FileOutputStream out = new FileOutputStream(path.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {

            LocalDateTime localDateTime = LocalDateTime.now();
            Person person1 = new Person("fzk1", 20, localDateTime);
            oos.writeObject(person1);
        }

        try (InputStream in = new FileInputStream(path.toFile());
             ObjectInputStream oi = new ObjectInputStream(in)
        ) {
            Map<String, Object> map = (Map<String, Object>) oi.readObject();
            System.out.println(map);//输出{localDateTime=2022-01-11T18:33:14.700559800, name=fzk1, age=0}
        }
    }
}
```

这里输出的是0，因为age是瞬变变量，默认情况下不会被序列化。

##  Externalizable

每个要存储的对象都针对 Externalizable 接口进行了测试。 如果对象支持 Externalizable，则调用 writeExternal 方法。 如果对象不支持 Externalizable 并且实现了 Serializable，则使用 ObjectOutputStream 保存对象。

重构 Externalizable 对象时，会使用public无参数构造函数创建实例，然后调用 readExternal 方法。 可序列化对象是通过从 ObjectInputStream 中读取来恢复的。 Externalizable 实例可以通过 Serializable 接口中记录的 writeReplace 和 readResolve 方法指定替换对象。

```java
@Data
public class Man implements Externalizable {
    private static final long serialVersionUID = 1L;// 序列化版本号
    private String name;
    private int age;

    @Override// 序列化
    public void writeExternal(ObjectOutput out) throws IOException {
        MyAES myAES=new MyAES();
        out.writeObject(myAES.encryptHex(name));// 加密
        out.writeInt(age);
    }

    @Override// 反序列化
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        MyAES myAES=new MyAES();
        this.name=myAES.decryptStr(in.readObject().toString());
        this.age=in.readInt();
    }

    public static void main(String[] args) throws Exception{
        Path path = Paths.get("D:/test.txt");
        try (FileOutputStream out = new FileOutputStream(path.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {

            LocalDateTime localDateTime = LocalDateTime.now();
            Person person1 = new Person("fzk1", 20, localDateTime);
            oos.writeObject(person1);
        }

        try (InputStream in = new FileInputStream(path.toFile());
             ObjectInputStream oi = new ObjectInputStream(in)
        ) {
            System.out.println(oi.readObject());
        }
    }
}
```

Externalizable接口和Serializable接口功能类似，不过此接口强制用户自定义序列化和反序列化方式。

注意：**必须提供public的无参构造器，因为在反序列化的时候需要反射创建对象**



























