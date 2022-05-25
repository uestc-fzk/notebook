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



# Java NIO

## 资料

尚硅谷的PDF课件

Java17源码nio包

## 概述

### IO概述

IO操作分为：

1、BIO，同步阻塞

2、**NIO，同步非阻塞**

Java NIO(New IO 或 Non  Blocking IO)是Java1.4支持的API。NIO支持面向缓冲区、基于通道的IO操作。NIO基于**Reactor模式**，IO调用不会阻塞。

NIO中实现非阻塞IO的核心对象是Selector(多路复用器)，它可以注册各种IO事件，当某个事件发生的时候，它就会通知我们。一个连接器线程可以同时处理成千上万个连接，而不用创建大量线程，大大减小了系统开销。

3、**AIO，异步非阻塞IO**

AIO是NIO 2，Java7引入改进版NIO 2，异步非阻塞模型。

AIO是基于事件和回调机制实现的，即AIO不需要Selector操作，而是事件驱动形式。Java AIO其实是**Proactor模式**的应用，和Reactor模式类似。

- Reactor与Proactor的区别
  主要区别就是**真正的读取和写入操作是有谁来完成的**， Reactor 中需要应用程序自己读取或者写入数据，而 Proactor 模式中，应用程序不需要进行实际的读写过程，它只需要**从缓存区读取或者写入即可**，操作系统会读取缓存区或者写入缓存区到真正的 IO 设备。

### NIO核心组件

Java NIO核心组件类有：Channels、Buffers、Selectors。其它组件如Pipe和FileLock只是与这3个核心组件共同使用的工具类。

1、Channel，通道

与IO中的Stream(流)是差不多一个等级的。Stream是单向的，如InputStream、OutputStream。Channel是双向的，可读可写。

主要实现有FileChannel、DatagramChannel、SocketChannel和ServerSocketChannel，分别对应文件IO、UDP、TCP(Server和Client)

2、Buffer

3、Selector，多路复用连接器

运行单个线程处理多个channel。如果打开了多个通道，但每个通道流量都不高，就可以用这个。比如聊天服务器。

4、3者关系

Channel像流，读数据到Buffer，Buffer写数据到channel

![image-20220519143815960](JavaSE.assets/image-20220519143815960.png)

Selector使用一个线程处理多个channel

![image-20220519143900113](JavaSE.assets/image-20220519143900113.png)

### nio包

![java.nio](JavaSE.assets/java.nio.png)

## Buffer

Java NIO 中的 Buffer 用于和 NIO 通道进行交互。数据是从通道读入缓冲区，从缓冲区写入到通道中的。

缓冲区本质上是一块可以写入数据和读取数据的内存。这块内存被包装成 NIO Buffer 对象，并提供了一组方法方便操作该内存。缓冲区实际上是一个容器对象，更直接的说，**其实就是一个数组**。
在 NIO 库中，所有数据都是用缓冲区处理的。在读取数据时，直接读到缓冲区中的； 在写入数据时，从缓冲区中写入的；任何时候访问 NIO 中的数据，都是将它放到缓冲区中。而在面向流 I/O 系统中，所有数据都是直接写入或者直接将数据读取到 Stream 对象中。

![image-20220522214617519](JavaSE.assets/image-20220522214617519.png)

最常用的是`ByteBuffer`。

```java
/**
 * 缓冲区是特定原始类型元素的线性、有限序列
 * 线程安全：多个并发线程使用缓冲区是不安全的。如果一个缓冲区要被多个线程使用，那么对缓冲区的访问应该由适当的同步控制 
 */
public abstract class Buffer {
    // Invariants: mark <= position <= limit <= capacity
    private int mark = -1; // 标记位
    // 缓冲区的位置position是要读取或写入的下一个元素的索引
    private int position = 0;
    // 缓冲区的限制是不应读取或写入的第一个元素的索引
    private int limit;
    // 缓冲区的容量capacity是它包含的元素数，即数组长度
    private int capacity;
    
    // 将此时的position设置为标记位mark
   	public Buffer mark() {
        mark = position;
        return this;
    }
    
  	/** 重置position为之前标记的mark */
    public Buffer reset() {
        int m = mark;
        if (m < 0) throw new InvalidMarkException();
        position = m;
        return this;
    }
    
    /**
     * 翻转缓冲区. 设置limit=position, 重置position=0, 清除mark=-1
     * 一般是把数据写入缓冲区后准备从缓存区读数据之前调用，如：管道写、get()
     * 如：
     * buf.put(magic);    // Prepend header
     * in.read(buf);      // Read data into rest of buffer
     * buf.flip();        // Flip buffer
     * out.write(buf);    // Write header + data to channel
     *
     * 在将数据从一个地方传输到另一个地方时，此方法通常与compact方法结合使用。
     */
    public Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }
    
    /**
     * 回退缓冲区. 重置position=0，清除mark=-1。多次从缓冲区读数据的时候适用
     * 如：管道写、get()
     * out.write(buf);    // Write remaining data
     * buf.rewind();      // Rewind buffer
     * buf.get(array);    // Copy data into array
     */
    public Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }
    
    /** 在position和limit间是否还有数据 */
    public final boolean hasRemaining() {
        return position < limit;
    }
    
    /**
     * 清空缓冲区.  重置position=0，重置limit=capacity，清除mark=-1
     * 一般在准备向缓冲区写入数据之前调用，如：管道读、put()
     * buf.clear();     // Prepare buffer for reading
     * in.read(buf);    // Read data
     * 此方法实际上没有擦除缓冲区中的数据，因为在结合position和limit使用时，没必要将数据重置为0
     */
    public Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }
}

```

> 注意：有两种方式能清空缓冲区：clear()或 compact()方法。clear()方法会清空整个缓冲区。compact()方法只会清除已经读过的数据。任何未读的数据都被移到缓冲区的起始处，新写入的数据将放到缓冲区未读数据的后面。
>
> 在上面给出的几个控制position等的方法一般是常用情况，也可以直接用 position(int) 和 limit(int) 方法控制这两个变量以实现业务逻辑。

关于Buffer的使用案例这里不给出，因为下面的通道使用案例会用到ByteBuffer。

### ByteBuffer

```java

public abstract class ByteBuffer extends Buffer 
    implements Comparable<ByteBuffer> {
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    // These fields are declared here rather than in Heap-X-Buffer in order to
    // reduce the number of virtual method invocations needed to access these
    // values, which is especially costly when coding small buffers.
    //
    final byte[] hb;                  // Non-null only for heap buffers
    final int offset;
    boolean isReadOnly;
    
    /**
     * 创建一个字节缓冲区切片，其内容是此缓冲区内容的共享子序列。
     * 新缓冲区的内容将从该缓冲区中的index位置开始，并将包含length元素。此缓冲区内容的更改将在新缓冲区中可见，反之亦然；两个缓冲区的position、limit和mark值将是独立的。
     * 新缓冲区的position为零，其capacity和limit为length ，其mark=-1，其字节顺序将为BIG_ENDIAN
     * 当且仅当此缓冲区是直接的时，新缓冲区将是直接的，并且当且仅当此缓冲区是只读的时，它将是只读的
     * @since 13
     */
    @Override
    public abstract ByteBuffer slice(int index, int length);
    
    /**
     * 创建一个共享此缓冲区内容的只读缓冲区
     * 此缓冲区内容的更改将在新缓冲区中可见；但是，新缓冲区本身将是只读的，并且不允许修改共享内容。
     * 两个缓冲区的位置、限制和标记值将是独立的。
     * 如果此缓冲区本身是只读的，则此方法的行为方式与duplicate方法完全相同
     */
    public abstract ByteBuffer asReadOnlyBuffer();
}
```

#### 直接缓冲区

字节缓冲区有直接的与非直接的。

```java
 ByteBuffer buffer = ByteBuffer.allocateDirect(128);
```

直接字节缓冲区，Java 虚拟机将尽最大努力直接在其上执行本机 I/O 操作。也就是说，它将尝试避免在每次调用底层操作系统的本机 I/O 操作之一之前（或之后）将缓冲区的内容复制到（或从）中间缓冲区。

特点：

1、具有**更高的分配和释放成本**。
2、其内容可能**驻留在正常的垃圾收集堆之外**，因此它们对应用程序内存占用的影响可能并不明显。

因此，建议将直接缓冲区主要分配给受底层系统的本机 I/O 操作影响的大型、长期存在的缓冲区。通常，最好**仅在直接缓冲区对程序性能产生可衡量的增益时才分配它们**。

也可以通过将文件的区域直接mapping到内存来创建直接字节缓冲区。 Java 平台的实现可以选择支持通过 JNI 从本机代码创建直接字节缓冲区。如果其中一种缓冲区的实例引用了不可访问的内存区域，则访问该区域的尝试不会更改缓冲区的内容，并且会在访问时或稍后引发未指定的异常时间。

## Channel

### FileChannel

用于读取，写入，映射和操作文件的通道。文件通道**可以安全使用多个并发线程**。

在FileChannelImpl中，默认的锁实现是synchronized (positionLock) 

```java
// Lock for operations involving position and size
private final Object positionLock = new Object();
```

`FileInputStream`, `FileOutputStream`,`RandomAccessFile`对象的`getChannel()`方法返回被连接到相同的基本文件的文件信道。还可以直接调FileChannel.open静态方法开文件通道。

#### 常用API

1、简单使用：读文件写文件管道

```java
public static void main(String[] args) throws IOException {
    // 1.打开文件
    RandomAccessFile accessFile = new RandomAccessFile("D:/test.txt", "rw");
    // 2.获取FileChannel
    FileChannel fileChannel = accessFile.getChannel();
    // 3.分配buffer
    // buffer中有一个byte数组，position表示下个操作的索引，limit表示最大索引(exclusive)，capacity表数组长度
    ByteBuffer buf = ByteBuffer.allocate(64); 
    // 4.从文件管道读到buffer
    int count = fileChannel.read(buf);
    while (count != -1) {
        System.out.printf("读取字节数：%d \n", count);
        // 将buffer的limit设为当前position，然后将position置0，准备读数据
        buf.flip();
        while (buf.hasRemaining()) {
            System.out.printf("%c", (char) buf.get());
        }
        buf.clear();
        count = fileChannel.read(buf);
    }
    System.out.println();
    // 读完不用了记得clear
    buf.clear();
    buf.put("New String".getBytes(StandardCharsets.UTF_8));
    // 将limit设为当前position，然后将position置0，准备读数据
    buf.flip();
    while (buf.hasRemaining()) {
        fileChannel.write(buf);
    }
    // 读完不用了记得clear，将position置0，limit=capacity，并设置mark=-1
    buf.clear();
    fileChannel.force(true);// 强制落盘
    // 关闭channel
    accessFile.close();// 会先去关闭channel
}
```

2、文件管道互相读写：可用于文件复制

```java
public static void main(String[] args) {
    try (
        FileChannel fromChannel = FileChannel.open(Path.of("D:/test1.txt"), StandardOpenOption.READ);
        FileChannel toChannel = FileChannel.open(Path.of("D:/test2.txt"), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    ) {
        long position = toChannel.size();
        long count = fromChannel.size();
        // position参数表示从何处开始写数据,count 表示最多写多少字节
        toChannel.transferFrom(fromChannel, position, count);
        // 这里的第一个参数position指从何处开始读数据，并读count字节，然后写入到目标管道
        fromChannel.transferTo(0L, count, toChannel);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

3、**Scatter/Gather** 

scatter/gather 用于描述从 Channel 中读取或 者写入到 Channel 的操作。

分散指将从 Channel 中读取的数据`分散(scatter)`到多个 Buffer 中。 

聚集将多个 Buffer 中的数据`聚集(gather)`后发送到 Channel。

scatter / gather 经常用于需要将传输的数据分开处理的场合，例如传输一个由消息头和消息体组成的消息，你可能会将消息体和消息头分散到不同的 buffer 中，这样你可以方便的处理消息头和消息体

3.1 分散读

缺点：不适应动态消息，这要求消息头长度固定。

```java
public static void main(String[] args) {
    try (FileChannel fileChannel = FileChannel.open(Path.of("D:/test2.txt"), StandardOpenOption.READ)) {
        ByteBuffer header = ByteBuffer.allocate(64); // 64字节头部信息
        ByteBuffer body = ByteBuffer.allocate(1024); // 消息体
        ByteBuffer[] arrayBuf = {header, body};

        fileChannel.read(arrayBuf); // 按顺序首先会写入header，写满后再写下一个buffer

        header.flip(); // 将limit设为当前position，然后将position置0，准备读数据
        System.out.println("header:");
        while (header.hasRemaining()) {
            System.out.printf("%c", (char) header.get());
        }
        System.out.println("\nbody:");
        body.flip();
        while (body.hasRemaining()) {
            System.out.printf("%c", (char) body.get());
        }
    } catch (IOException e) {
        e.printStackTrace();
    }

}
```

3.2 聚集写

```java
static void testGather() {
    try (FileChannel fileChannel = FileChannel.open(Path.of("D:/test2.txt"), StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
        ByteBuffer header = ByteBuffer.wrap("i am the header".getBytes(StandardCharsets.UTF_8));
        ByteBuffer body = ByteBuffer.wrap("i am the body".getBytes(StandardCharsets.UTF_8));
        ByteBuffer[] arrayBuf = {header, body};

        fileChannel.write(arrayBuf);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

这个聚集和分散感觉有点...

#### force()落盘

FileChannel.force()方法将通道里尚未写入磁盘的数据强制写到磁盘上。出于性能方面的考虑，操作系统会将数据缓存在内存中，所以无法保证写入到 FileChannel 里的数据一定会即时写到磁盘上。要保证这一点，需要调用 force()方法。 

force()方法有一个 boolean 类型的参数，指明是否同时将文件元数据（权限信息等） 写到磁盘上。

> 如RocketMQ中，消息接受是将生产者发的消息读入ByteBuffer，消息提交commit只是将消息写入FileChannel，而落盘flush操作则是调用FileChannel.force(false)方法进行强制写盘。

#### 内存映射文件I/O

使用案例：RocketMQ保存消息的日志文件就是用的此内存映射文件I/O。

内存映射文件 I/O 是一种读和写文件数据的方法，它可以比常规的基于流或者基于通道的 I/O 快的多。内存映射文件 I/O 是通过使文件中的数据出现为 内存数组的内容来完成的，这其初听起来似乎不过就是将整个文件读到内存中，但是事实上并不是这样。 

一般来说，只有文件中**实际读取或者写入的部分才会映射到内存中**。

```java
/**
 * @author fzk
 * @datetime 2022-05-22 22:49
 */
public class FileChannelTest {
    @Test
    void FileMapIOTest() {
        try (RandomAccessFile file = new RandomAccessFile("D:/test.txt", "rw")) {
            FileChannel fileChannel = file.getChannel();
            int start = 0, size = 1024;
            MappedByteBuffer mbb = fileChannel.map(FileChannel.MapMode.READ_WRITE, start, size);
            mbb.put(2, (byte) 'a');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

#### FileLock文件锁

文件锁在 OS 中很常见，如果多个程序同时访问、修改同一个文件，很容易因为文件数据不同步而出现问题。给文件加一个锁，同一时间，只能有一个程序修改此文件，或者程序都只能读此文件，这就解决了同步问题。 

**文件锁是进程级别的**，不是线程级别的。文件锁可以解决多个进程并发访问、修改同一个文件的问题，但不能解决多线程并发访问、修改同一文件的问题。使用文件锁时，同一进程内的多个线程，可以同时访问、修改此文件。

**文件锁是不可重入**的。

```java
public abstract class FileChannel extends AbstractInterruptibleChannel
    implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel
{
	/**
     * 获取对该通道文件的 给定区域 的锁定。
     * 此方法的调用将阻塞，直到可以锁定区域，或关闭此通道或中断调用线程
	 * 由position和size参数指定的区域不需要包含在实际的底层文件中，甚至不需要重叠。
	 * 锁定区域的大小是固定的；如果锁定区域最初包含文件的结尾，并且文件超出该区域，则文件的新部分将不会被锁定覆盖。
	 * 如果预期文件的大小会增长并且需要锁定整个文件，则应锁定从零开始且不小于文件预期最大大小的区域。零参数lock()方法只是锁定一个大小为Long.MAX_VALUE的区域。
	 * 某些操作系统不支持共享锁，在这种情况下，共享锁请求会自动转换为排他锁请求。
	 * 新获取的锁是共享的还是独占的，可以通过调用生成的锁对象的isShared方法来测试。
	 * 文件锁代表整个 Java 虚拟机持有。它们不适用于控制同一虚拟机内的多个线程对文件的访问。
	 * @param shared true 共享锁，false 排它锁
     */
    public abstract FileLock lock(long position, long size, boolean shared)
        throws IOException;
    public abstract FileLock tryLock(long position, long size, boolean shared)
        throws IOException;
}
```

例子：

```java
/**
 * @author fzk
 * @date 2022-05-23 22:55
 */
public class FileLockTest {
    public static void main(String[] args) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap("hello fileLock\n".getBytes());

        FileChannel channel = FileChannel.open(Path.of("D:/test.txt"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        channel.position(channel.size()); // 将position定位到末尾
        // 获取整个文件排它锁
        FileLock lock = channel.tryLock(0, Long.MAX_VALUE, false);
        System.out.println("是共享锁吗: " + lock.isShared());

        channel.write(buf);
        channel.close();
        System.out.println("写操作完成.");
        //读取数据
        readPrint("D:/test.txt");
    }

    public static void readPrint(String path) throws IOException {
        FileReader filereader = new FileReader(path);
        BufferedReader bufferedreader = new BufferedReader(filereader);
        String tr = bufferedreader.readLine();
        System.out.println("读取内容: ");
        while (tr != null) {
            System.out.println(" " + tr);
            tr = bufferedreader.readLine();
        }
        filereader.close();
        bufferedreader.close();
    }
}
```

### AsynchronousFileChannel(待续)

异步文件管道。

### NetworkChannel

这个接口是通用的对应于套接字的通道。它是服务器端套接字通道和客户端套接字通道的父接口。

```java
/**
 * 网络套接字的通道
 * @since 1.7
 */
public interface NetworkChannel extends Channel {
    /** 将通道的套接字绑定到本地地址 */
    NetworkChannel bind(SocketAddress local) throws IOException;

    /**
     * 返回此通道的套接字绑定到的套接字地址
     * 如果通道bind到 Internet 协议套接字地址，则此方法的返回值是java.net.InetSocketAddress类型
     */
    SocketAddress getLocalAddress() throws IOException;
}
```

#### ServerSocketChannel

是一个基于通道的 socket 服务端监听器。它增加了通道语义，因此能够在非阻塞模式下运行。

```java
/**
 * 监听socket的面向流的selectable通道.
 * 调用open()方法可以创建server-socket channel(服务器套接字通道)
 * 新创建的server-socket channel 打开了但还没有绑定端口. 可以bind()方法绑定
 * 使用setOption方法配置套接字选项。 Internet 协议套接字的服务器套接字通道支持以下选项：
 * SO_RCVBUF 套接字接收缓冲区的大小
 * SO_REUSEADDR 重用地址
 * @since 1.4
 */
public abstract class ServerSocketChannel extends AbstractSelectableChannel implements NetworkChannel
{
    /**
     * 打开Internet协议套接字的服务器套接字通道
     * 新通道是通过调用系统范围默认SelectorProvider对象的openServerSocketChannel方法创建的
     * 新通道的套接字最初是未绑定的；在接受连接之前，它必须通过其套接字的bind方法之一绑定到特定地址
     */
    public static ServerSocketChannel open() throws IOException {
        return SelectorProvider.provider().openServerSocketChannel();
    }
    
    /**
     * 将通道的套接字绑定到本地地址并配置套接字以侦听连接
     * 它会调用ServerSocketChannelImpl.netBind()方法
     */
    public final ServerSocketChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }
    
    /** 调整此通道的阻塞模式 */
    public final SelectableChannel configureBlocking(boolean block) throws IOException
    {
        synchronized (regLock) {
            if (!isOpen())
                throw new ClosedChannelException();
            boolean blocking = !nonBlocking;
            if (block != blocking) {
                if (block && haveValidKeys())
                    throw new IllegalBlockingModeException();
                implConfigureBlocking(block);
                nonBlocking = !block;
            }
        }
        return this;
    }
    
    /**
     * 接受与此通道的套接字建立的连接。
     * 非阻塞模式：如果没有挂起的连接，此方法将立即返回null
     * 阻塞模式：将无限期地阻塞，直到有新的连接可用或发生 I/O 错误
     */
    public abstract SocketChannel accept() throws IOException;
}
```

![ServerSocketChannelImpl](JavaSE.assets/ServerSocketChannelImpl.png)

#### SocketChannel

是一个连接到 TCP 网络套接字的selectable通道，可以被多路复用。

```java

/**
 * 面向流的连接套接字的selectable通道。
 * 支持非阻塞连接：可以创建一个套接字通道，并且可以通过connect方法启动建立到远程套接字的链接的过程，以便稍后通过finishConnect方法完成。
 * 支持异步关闭，类似于Channel类中指定的异步关闭操作。
 * 如果套接字的输入端被一个线程关闭，而另一个线程在套接字通道上的读取操作中被阻塞，则阻塞线程中的读取操作将在不读取任何字节的情况下完成，并将返回-1 。
 * 如果套接字的输出端被一个线程关闭，而另一个线程在套接字通道上的写操作中被阻塞，则被阻塞的线程将收到AsynchronousCloseException 。
 * 可以使用setOption方法配置套接字选项，有哪些选项直接看源码注释吧，这里不列出。
 * @since 1.4
 */
public abstract class SocketChannel extends AbstractSelectableChannel
    implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel
{
    /**
     * 打开一个套接字通道并将其连接到一个远程地址   
     * @param remote 可以传入一个IP套接字地址或UNIX域套接字地址
     */
    public static SocketChannel open(SocketAddress remote) throws IOException
    {
        SocketChannel sc;
        requireNonNull(remote);
        if (remote instanceof InetSocketAddress)
            sc = open();
        else if (remote instanceof UnixDomainSocketAddress)
            sc = open(StandardProtocolFamily.UNIX);
        else
            throw new UnsupportedAddressTypeException();

        try {
            sc.connect(remote);
        } catch (Throwable x) {
            try {
                sc.close();
            } catch (Throwable suppressed) {
                x.addSuppressed(suppressed);
            }
            throw x;
        }
        assert sc.isConnected();
        return sc;
    }
    
    public abstract int write(ByteBuffer src) throws IOException;
}
```

![SocketChannelImpl](JavaSE.assets/SocketChannelImpl.png)

#### 简单使用

下面案例简单的用服务端接受来自客户端的消息。

```java
/**
 * @author fzk
 * @date 2022-05-22 17:35
 */
public class NetworkChannelTest {
    // 服务端
    @Test
    void serverTest() {
        ByteBuffer buffer = ByteBuffer.allocate(128);
        // 1.打开服务套接字通道，并监听8080端口
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(8080));
            ssc.configureBlocking(false); // 设置为非阻塞模式

            while (true) {
                System.out.println("waiting for connection...");
                /*
                2.异步等待连接
                如果此通道处于非阻塞模式，则没有挂起的连接将立即返回null;
                否则它将无限期地阻塞，直到有新的连接可用或发生I/O 错误
                */
                SocketChannel sc = ssc.accept();
                if (sc == null) {
                    Thread.sleep(5000L);
                } else {
                    // 3.获取到连接接受数据
                    System.out.printf("收到来自%s的请求 \n", sc.getRemoteAddress());
                    buffer.clear();// 清空缓存区
                    // 从连接中读数据到缓冲区
                    System.out.print("收到数据：");
                    int count = 0;
                    while ((count = sc.read(buffer)) != -1) {
                        buffer.flip(); // 翻转缓冲区，limit=position，position=0
                        System.out.printf("\n收到%d字节数据: ", count);
                        while (buffer.hasRemaining()) {
                            System.out.printf("%c", (char) buffer.get());
                        }
                        buffer.clear();
                    }
                    System.out.println();
                    buffer.clear();

                    sc.close(); // 关闭连接
                }
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 客户端
    @Test
    void clientTest() {
        try (
                // 1.以IP协议打开并连接到8080端口
                SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8080))
        ) {
            // 设置为非阻塞模式
            socketChannel.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(128);
            // 2.向服务端发数据
            buffer.put("hello, i am client.".getBytes(StandardCharsets.UTF_8));
            buffer.flip();// 翻转缓冲区，limit=position, position=0, 准备从缓存区读数据
            for (int i = 0; i < 50; i++) {
                buffer.rewind(); // 回退缓冲区，position=0，用于从缓冲区重读
                socketChannel.write(buffer);
            }
            buffer.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

#### DatagramChannel(待续)

待续...

## Selector

Selector 一般称为**选择器** ，也可以翻译为**多路复用器** 。它是 Java NIO 核心组件中的一个，用于检查一个或多个 NIO Channel（通道）的状态是否处于可读、可写。如 此可以实现单线程管理多个 channels,也就是可以管理多个网络链接。

使用 Selector 的好处在于： 使用更少的线程来就可以来处理通道了， 相比使用多个线程，避免了线程上下文切换带来的开销。

```java
/**
 * SelectableChannel可选管道的多路复用器
 * 可以通过open方法来创建选择器，该方法将使用系统的默认值selector provider创建一个新的选择器。 还可以通过调用自定义选择器提供程序的openSelector方法来创建选择器。
 * SelectionKey对象代表SelectableChannel在Selector的注册，selector保留selection key的3种集合：
 * 1.key set：包含此选择器当前注册的SelectionKey。管道调用register方法注册到选择器中，会把相关的key放入key set。
 * 2.selected-key set：该集合中的SelectionKey的管道至少有一个位于兴趣集中的操作已经准备就绪。总是key set 的子集。
 * 3.cancelled-key set：该集合中的选择键都取消了，但是其中的管道还没有从此选择器注销。总是key set 的子集。注销的key在下次选择操作时会从所有key集合中移除
 * <h2>Selection</h2>
 * 选择操作查询底层操作系统，以更新每个已注册通道执行其key interest set兴趣集标识的任何操作的准备情况。有两种形式的选择操作：
 * 		1.select()、select(long)和selectNow()方法将准备执行操作的通道的键添加到selected-key set中，或更新已在selected-key set中的键的ready-operation set
 * 		2.select(Consumer)、select(Consumer，long)和selectNow(Consumer)方法对准备执行操作的每个通道的键执行操作。这些方法不会添加到selected-key set
 * <h3>Selection operations that add to the selected-key set</h3>
 * 在每次选择操作期间，可以向选择器的selected-key set添加key，也可以从其selected-key set和cancelled-key set删除key。
 * 选择由select(),select(long),selectNow() 方法执行, 包含3步：
 * 		1.cancelled-key set中的每个key都将从其所属的每个key set中删除，并注销其管道。此步骤将cancelled-key set保留为空。
 *		2.查询底层操作系统，以更新每个剩余通道在选择操作开始时执行其key兴趣集中操作的准备情况。对于准备好的管道将执行以下两个操作之一：
 *			a.如果通道的键不在seleted-key set中，则将其添加到该键集中，并修改其就绪操作集
 *			b.否则，通道的键已在seleted-key set中，其就绪操作集将被修改，以标识通道已就绪的任何新操作。保留之前记录在就绪集中的任何就绪信息；换句话说，底层系统返回的就绪集按位分离到键的当前就绪集。
 *		3.如果在执行步骤（2）时向cancelled-key set中添加了任何key，则将按照步骤（1）处理这些密钥。
 *
 * <h2>并发性</h2>
 * 	选择器本身和key set是线程安全的，但是它的seleted-key set和cancelled-key set不是
 */
public abstract class Selector implements Closeable {
    /**
     * 选择一组管道已经准备好I/O操作的key
     * 此方法执行上面描述的阻塞selection operation操作。只有在至少选择一个管道后，此选择器的wakeup()才会被调用，或当前线程中断
     * @return  返回就绪操作集更新的key的数量，可能为0
     */
    public abstract int select() throws IOException;
    
    /** 此方法同上，不过在超时后也会返回。此方法不提供实时保证: 就像调用Object.wait(long)方法一样 */
    public abstract int select(long timeout) throws IOException;
    
    /**
     * 选择一组管道已经准备好I/O操作的key
     * 此方法与上面不同，执行的是 非阻塞 selection operation，如果key set中没有管道可选择，则立即返回0
     * 调用此方法将清除任何先前调用wakeup方法的影响。
     */
    public abstract int selectNow() throws IOException;
    
    /**
     * 选择一组管道已经准备好I/O操作的key，并对它们执行给定的操作
     * 此方法执行上面描述的阻塞selection operation操作。只有在至少选择一个管道后，此选择器的wakeup()才会被调用，或当前线程中断
     * @since 11
     */
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout");
        return doSelect(Objects.requireNonNull(action), timeout);
    }
    
    /**
     * Default implementation of select(Consumer) and selectNow(Consumer).
     */
    private int doSelect(Consumer<SelectionKey> action, long timeout) throws IOException {
        synchronized (this) {
            Set<SelectionKey> selectedKeys = selectedKeys();
            synchronized (selectedKeys) {
                selectedKeys.clear();
                // 1.选择已经准备好的管道
                int numKeySelected;
                if (timeout < 0) {
                    numKeySelected = selectNow();
                } else {
                    numKeySelected = select(timeout);
                }

                // copy selected-key set as action may remove keys
                Set<SelectionKey> keysToConsume = Set.copyOf(selectedKeys);
                assert keysToConsume.size() == numKeySelected;
                selectedKeys.clear();

                // 2.对每个key执行操作
                keysToConsume.forEach(k -> {
                    action.accept(k);
                    if (!isOpen())
                        throw new ClosedSelectorException();
                });

                return numKeySelected;
            }
        }
    }
}
```

通过 Selector 的 `select()`方法，可以查询出已经就绪的通道操作。

### SelectableChannel

**可选管道**。不是所有Channel都是多路复用的，如FileChannel。只有继承了`SelectableChannel`抽象类的才能被Selector复用。

```java
/**
 * 通过Selector实现多路复用的管道
 * 注册：管道要先注册到Selector中才能被复用，返回一个新的SelectionKey对象，表示通道与选择器的注册
 * 取消注册：管道不能直接取消注册，代表它的注册的key必须取消才行，取消key会使得复用器在下次复用此管道操作时对其取消注册
 * close：管道close的时候此管道的所有注册key都会被取消
 *
 * selectable管道是线程安全的
 *
 * 新创建的selectable管道总是阻塞模式的，非阻塞模式和多路复用器结合会很有用；复用管道在注册到复用器的时候必须是非阻塞模式的
 */
public abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {
    /**
     * 使用给定的选择器注册此管道，返回一个代表注册的SelectionKey
     * 
     * @param sel 注册到哪个选择器
     * @param ops 指定选择器对该管道的操作；有4种操作，用 位或 运算可以选择多种操作
     * 			 SelectionKey.OP_READ = 1 << 0; 可读
     *   		 SelectionKey.OP_WRITE = 1 << 2; 可写
     * 			 SelectionKey.OP_CONNECT = 1 << 3; 连接
     *   		 SelectionKey.OP_ACCEPT = 1 << 4; 接收
     * @param att The attachment for the resulting key; 可为null
     * @return  表示该管道与选择器注册的key
     */
    public abstract SelectionKey register(Selector sel, int ops, Object att)
        throws ClosedChannelException;
    /**
     * 取回代表此管道注册在给定选择器的key
     * @return key 或 null
     */
    public abstract SelectionKey keyFor(Selector sel);
}
```

选择器查询的不是管道的操作，而是通道的某个操作的一种就绪状态。

什么是操作的就绪状态？一旦通道具备完成某个操作的条件，表示该通道的某个操作已经就绪，就可以被 Selector 查询到，程序可以对通道进行对应的操作。如 SocketChannel 通道可以连接到一个服务器，则处于`连接就绪OP_CONNECT`。 一个 ServerSocketChannel 服务器通道准备好接收新进入的连接，则处于 `接收就绪OP_ACCEPT`状态。一个有数据可读的通道，可以说是 `读就绪OP_READ`。一个等待写数据的通道可以说是`写就绪OP_WRITE`。

> 注意：一个通道，并没有一定要支持所有的四种操作。比如服务器通道 ServerSocketChannel 支持 Accept 接受操作，而 SocketChannel 客户端通道则不支持。 可以通过通道上的 validOps()方法，来获取特定通道下所有支持的操作集合。

### SelectionKey

**选择键**。选择键的概念，和事件的概念比较相似。一个选择键**类似监听器模式里边的一个事件**。由于 Selector 不是事件触发的模式，而是主动去查询的模式，所以不叫事件 Event，而是叫 SelectionKey 选择键。

```java
/**
 * 一个代表一个SelectableChannel与Selector的注册的令牌token
 * 每当通道被选择器注册时，都会创建一个选择键；取消键不会立即将其从选择器中删除，而是在下一个选择操作期间添加到选择器的cancelled-key set中以进行删除
 * 选择键包含两个表示为整数值的操作集，操作集的每一位表示由通道支持的可选择操作的类别
 * 1.interest set，兴趣集，决定哪些操作会被selector选择器测试是否处于就绪状态
 * 2.ready set，就绪集，管道哪些操作已经被选择器标记为就绪
 * Selection keys 是线程安全的
 */

public abstract class SelectionKey {
    /** 读操作位 */
    public static final int OP_READ = 1 << 0;

    /** 写操作位 */
    public static final int OP_WRITE = 1 << 2;

    /** 用于套接字连接操作的操作集位 */
    public static final int OP_CONNECT = 1 << 3;

    /** socket-accept的操作集位 */
    public static final int OP_ACCEPT = 1 << 4;
    /**
     * 取消此key的通道与其选择器的注册.
     * 返回时，此key将无效，并将被添加到其选择器的cancelled-key set中，
     * 在下一次选择操作期间，key将从所有选择器的key sets中移除。
     */
    public abstract void cancel();
    
}
```

### NIO编程示例

```java
/**
 * @author fzk
 * @date 2022-05-23 20:35
 */
public class SelectorTest {
    @Test
    // 服务端启动这一个就行，它可以处理多个客户端连接
    void serverTest() {
        try (
                // 1.创建选择器selector
                Selector selector = Selector.open();
                // 2.创建服务端socket管道并设置为非阻塞
                ServerSocketChannel ssc = ServerSocketChannel.open();
        ) {
            ssc.configureBlocking(false); // 设置为非阻塞
            ssc.bind(new InetSocketAddress("localhost", 8080));// 监听端口

            // 3.管道注册到选择器中, 指定兴趣集为接收事件
            // 与 Selector 一起使用时，Channel 必须处于非阻塞模式下
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            ByteBuffer readBuf = ByteBuffer.allocate(1024);
            ByteBuffer writeBuf = ByteBuffer.allocate(128);
            writeBuf.put("received".getBytes(StandardCharsets.UTF_8));
            writeBuf.flip();

            // 4.选择器查询就绪管道
            while (selector.select() > 0) {
                System.out.println("准备开始处理，selectedKey数量为：" + selector.selectedKeys().size());
                // 5.取出selected-key set的迭代器对其进行操作
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        // 创建新的连接，并且把连接注册到 selector 上
                        // 声明这个 channel 只对读操作感兴趣
                        SocketChannel socketChannel = ssc.accept();
                        socketChannel.configureBlocking(false);// 设置为非阻塞
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        if (!socketChannel.isConnected()) {
                            key.cancel();// 取消key，从selector中注销
                            System.out.println(key + "取消了");
                            continue;
                        }
                        readBuf.clear();
                        socketChannel.read(readBuf);
                        readBuf.flip();
                        System.out.println("服务端收到消息: " + new String(Arrays.copyOf(readBuf.array(), readBuf.limit())));
                        // 将此键的兴趣值改为写操作
                        key.interestOps(SelectionKey.OP_WRITE);
                    } else if (key.isWritable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        if (!socketChannel.isConnected()) {
                            key.cancel();// 取消key，从selector中注销
                            System.out.println(key + "取消了");
                            continue;
                        }
                        writeBuf.rewind();
                        socketChannel.write(writeBuf);
                        // 将此键的兴趣值改为读操作
                        key.interestOps(SelectionKey.OP_READ);
                    } else {
                        System.out.println(key + "取消了");
                        key.cancel();
                    }
                    // 6.必须将key从selected-key set中移除，否则下次处理又会有这个key
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    // 这个客户端可以启动很多个相同的
    void clientTest() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress("localhost", 8080));
            ByteBuffer writeBuf = ByteBuffer.allocate(32);
            ByteBuffer readBuf = ByteBuffer.allocate(32);

            for (int i = 0; i < 5; i++) {
                // 发送消息
                writeBuf.clear();
                writeBuf.put(("hello i am client " + i).getBytes(StandardCharsets.UTF_8));
                writeBuf.flip();
                socketChannel.write(writeBuf);

                // 接受消息
                readBuf.clear();
                socketChannel.read(readBuf);
                readBuf.flip();
                System.out.println("客户端收到消息: " + new String(Arrays.copyOf(readBuf.array(), readBuf.limit())));
                Thread.sleep(1000);
            }
            socketChannel.finishConnect();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

## Pipe

Java NIO 管道是 2 个线程之间的单向数据连接。Pipe 有一个 source 通道和一个 sink 通道。数据会被写到 sink 通道，从 source 通道读取。

![Pipe](JavaSE.assets/Pipe.png)

```java
/**
 * 一对实现单向管道的通道。
 * 管道由一对通道组成：一个可写的接收sink通道和一个可读的source通道。一旦一些字节被写入接收器通道，它们就可以完全按照它们被写入的顺序从源通道中读取。
 * 将字节写入管道的线程是否会阻塞，直到另一个线程从管道读取这些字节或一些先前写入的字节是系统相关的，因此未指定。许多管道实现将在接收器和源通道之间缓冲多达一定数量的字节，但不应假定这种缓冲
 */
public abstract class Pipe {
    /** 可读取source管道 */
    public abstract SourceChannel source();

    /** 可写入sink管道 */
    public abstract SinkChannel sink();

    public static Pipe open() throws IOException {
        return SelectorProvider.provider().openPipe();
    }
}
```

注意：从这里可以看出`Pipe`是没有继承SelectableChannel抽象类的，即不能多路复用，这与Go中的`chan`管道是不同的。

测试代码

```java
/**
 * @author fzk
 * @date 2022-05-23 21:54
 */
public class PipeTest {
    @Test
    void pipeTest() {
        Pipe.SinkChannel sinkChannel = null;
        try {
            // 1.打开管道
            Pipe pipe = Pipe.open();
            // 2.获取sinkChannel
            sinkChannel = pipe.sink();

            ByteBuffer writeBuf = ByteBuffer.allocate(64);
            writeBuf.put(("hello pipe" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            writeBuf.flip();
            // 3.往sinkChannel写消息
            while (writeBuf.hasRemaining())
                sinkChannel.write(writeBuf);

            // 4.新建线程并从sourceChannel读消息
            Thread t2 = new Thread(() -> {
                Pipe.SourceChannel sourceChannel = null;
                try {
                    sourceChannel = pipe.source();
                    ByteBuffer readBuf = ByteBuffer.allocate(16);
                    while ((sourceChannel.read(readBuf) != -1)) {
                        readBuf.flip();
                        System.out.println("收到消息: " + new String(Arrays.copyOf(readBuf.array(), readBuf.limit())));
                        readBuf.clear();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fastClose(sourceChannel);// sink管道和source管道都要关闭
                }
            });
            t2.start();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fastClose(sinkChannel);// sink管道和source管道都要关闭
        }
    }

    void fastClose(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## file

这里的file指的是java.nio.file包，这里面定义Java虚拟机访问文件，文件属性和文件系统的接口和类。

### Path

`java.nio.file.Path` 实例表示文件系统中的路径。一个路径可以指向一个文件或一个目录。路径可以是绝对路径，也可以是相对路径。

`Path` 接口类似于`java.io.File`类，但是有一些差别。不过，在许多情况下，可以使用 Path 接口来替换 File 类的使用。

```java
// 此接口的实现是不可变的，并且可以安全地供多个并发线程使用
public interface Path extends Comparable<Path>, Iterable<Path>, Watchable {
    /** 返回一个路径，该路径是消除了冗余名称元素(.或..)的路径 */
    Path normalize();
    /**
     * Returns a {@code Path} by converting a URI.
     * @since 11
     */
    public static Path of(URI uri) {
        String scheme =  uri.getScheme();
        if (scheme == null)
            throw new IllegalArgumentException("Missing scheme");

        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file"))
            return FileSystems.getDefault().provider().getPath(uri);

        // try to find provider
        for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }
        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
    }
}
```

normalize()方法和of()方法的区别在于前者会先把路径中的`.`和`..`清除再进行文件系统的查找，而后者不会。只要路劲正确，两者都能找到文件，只是toString()方法返回的路劲不同而已。

### Files

java.nio.file.Files

```java
/**
 * 该类只包含对文件，目录或其他类型文件进行操作的静态方法。
 * 在大多数情况下，这里定义的方法将委托给相关的文件系统提供程序来执行文件操作
 * @since 1.7
 */
public final class Files {
    // buffer size used for reading and writing
    private static final int BUFFER_SIZE = 8192;
    // 它的各个静态方法在下面逐一说明
}
```

#### 创建目录

创建目录有两个方法: `createDirectory()`和`createDirectories()`，后者与前者的区别在于会级联创建不存在的父目录。

```java
/** 创建所有不存在的父目录来创建目录，当目录早已存在时不会像createDirectory方法那样报错 */
public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
    // 1.尝试直接创建目录，这里会调用createDirectory()方法
    try {
        createAndCheckIsDirectory(dir, attrs);
        return dir;
    } catch (FileAlreadyExistsException x) {
        // file exists and is not a directory
        throw x;
    } catch (IOException x) {
        // parent may not exist or other reason
    }
    SecurityException se = null;
    try {
        dir = dir.toAbsolutePath();
    } catch (SecurityException x) {
        // don't have permission to get absolute path
        se = x;
    }
    // 2.寻找存在的父目录
    Path parent = dir.getParent();
    while (parent != null) {
        try {
            provider(parent).checkAccess(parent);
            break;
        } catch (NoSuchFileException x) {
            // does not exist
        }
        parent = parent.getParent();
    }
    if (parent == null) {
        // unable to find existing parent
        if (se == null) {
            throw new FileSystemException(dir.toString(), null,
                                          "Unable to determine if root directory exists");
        } else {
            throw se;
        }
    }

    // 3.层层创建目录
    Path child = parent;
    for (Path name: parent.relativize(dir)) {
        child = child.resolve(name);
        createAndCheckIsDirectory(child, attrs);
    }
    return dir;
}
```

可以看到`createDirectories()`方法是对目录各个不存在的父目录进行层层调用`createDirectory()`进行创建的。

#### copy、move、delete

```java
/**
 * 将文件复制到目标文件
 * 如果支持符号链接，并且文件是符号链接，那么链接的最终目标将被复制。 
 * 如果文件是目录，那将在目标位置创建一个空目录.该方法可以使用walkFileTree方法复制目录和目录中的所有条目，或者需要的整个文件树
 *
 * 复制文件不是原子操作。如果抛出一个IOException ，则目标文件可能不完整，或者某些文件属性尚未从源文件复制。当指定了REPLACE_EXISTING选项并且目标文件存在时，将替换目标文件。 对于其他文件系统活动，检查文件的存在和创建新文件可能不是原子的。
 */
public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
    FileSystemProvider provider = provider(source);
    if (provider(target) == provider) {
        // same provider
        provider.copy(source, target, options);
    } else {
        // different providers
        CopyMoveHelper.copyToForeignTarget(source, target, options);
    }
    return target;
}

/**
 * 移动或重命名文件
 * 默认情况下，此方法尝试将文件移动到目标文件，如果目标文件存在，则失败
 * 如果文件是符号链接，则移动符号链接本身，而不是链接的目标被移动。
 * 可以调用此方法来移动空目录。移动文件树可能涉及复制而不是移动目录，并且可以使用copy方法与Files.walkFileTree实用程序方法结合使用。
 * 当作为非原子操作执行移动，并且抛出IOException时，则不会定义文件的状态。 原始文件和目标文件都可能存在，目标文件可能不完整，或者某些文件属性可能未被复制到原始文件中。
 */
public static Path move(Path source, Path target, CopyOption... options) throws IOException {
    FileSystemProvider provider = provider(source);
    if (provider(target) == provider) {
        // same provider
        provider.move(source, target, options);
    } else {
        // different providers
        CopyMoveHelper.moveToForeignTarget(source, target, options);
    }
    return target;
}

public enum StandardCopyOption implements CopyOption {
    /** 如果存在则替换，不指定的情况下呢，如果存在会报错FileAlreadyExistsException */
    REPLACE_EXISTING,
    /** 复制文件属性 */
    COPY_ATTRIBUTES,
    /** 以原子方式移动文件 */
    ATOMIC_MOVE;
}
/**
 * 删除文件
 * 如果文件是符号链接，那么符号链接本身而不是链接的最终目标被删除。
 * 如果文件是目录，那么该目录必须为空。
 * 该方法可以使用walkFileTree方法来删除目录和目录中的所有条目，或者需要的整个文件树。
 * 在某些操作系统上，当文件打开并被该Java虚拟机或其他程序使用时，可能无法删除文件。
 */
public static void delete(Path path) throws IOException {
    provider(path).delete(path);
}
```

#### 遍历文件树

在上面的几个方法，都不能对非空目录进行操作，因为目录需要对每个条目都进行处理。Files提供了一些遍历目录的方法，如`walk()`、`walkFileTree()`

```java
/**
 * 遍历文件树
 * 将以一个给定的起始文件为根。文件树遍历深度优先于给定FileVisitor调用所遇到的每个文件。
 
 * @param options 如果options参数包含FOLLOW_LINKS选项，则遵循符号链接，且此时会检查是否出现循环目录，循环检测是通过记录目录的file-key，或者如果file-key不可用，则通过调用isSameFile方法来测试目录是否与祖先相同的文件来完成。 当检测到一个循环时，它被视为I/O错误，向visitFileFailed方法传入一个FileSystemLoopException的异常。
 * @param maxDepth 是要访问的目录的最大级别数。0即只有起始文件被访问，当访问层数超过maxDepth，会调用visitFileFailed方法
 */
public static Path walkFileTree(Path start,
                                Set<FileVisitOption> options,
                                int maxDepth,
                                FileVisitor<? super Path> visitor) throws IOException {
    /**
         * Create a FileTreeWalker to walk the file tree, invoking the visitor
         * for each event.
         */
    try (FileTreeWalker walker = new FileTreeWalker(options, maxDepth)) {
        FileTreeWalker.Event ev = walker.walk(start);
        do {
            FileVisitResult result = switch (ev.type()) {
                case ENTRY -> {
                    IOException ioe = ev.ioeException();
                    if (ioe == null) {
                        assert ev.attributes() != null;
                        yield visitor.visitFile(ev.file(), ev.attributes());
                    } else {
                        yield visitor.visitFileFailed(ev.file(), ioe);
                    }
                }
                case START_DIRECTORY -> {
                    var res = visitor.preVisitDirectory(ev.file(), ev.attributes());

                    // if SKIP_SIBLINGS and SKIP_SUBTREE is returned then
                    // there shouldn't be any more events for the current
                    // directory.
                    if (res == FileVisitResult.SKIP_SUBTREE ||
                        res == FileVisitResult.SKIP_SIBLINGS)
                        walker.pop();
                    yield res;
                }
                case END_DIRECTORY -> {
                    var res = visitor.postVisitDirectory(ev.file(), ev.ioeException());

                    // SKIP_SIBLINGS is a no-op for postVisitDirectory
                    if (res == FileVisitResult.SKIP_SIBLINGS)
                        res = FileVisitResult.CONTINUE;
                    yield res;
                }
                default -> throw new AssertionError("Should not get here");
            };

            if (Objects.requireNonNull(result) != FileVisitResult.CONTINUE) {
                if (result == FileVisitResult.TERMINATE) {
                    break;
                } else if (result == FileVisitResult.SKIP_SIBLINGS) {
                    walker.skipRemainingSiblings();
                }
            }
            ev = walker.next();
        } while (ev != null);
    }

    return start;
}
```

遍历文件树`walkFileTree()`需要传入一个文件访问接口`FileVisitor`，可以自己实现，也可以继承`SimpleFileVisitor`类。

```java
public interface FileVisitor<T> {
    /**
     * 进入目录前调用
     * 如果返回FileVisitResult.CONTINUE，则进入目录访问
     * 如果返回FileVisitResult.SKIP_SUBTREE或SKIP_SIBLINGS，则进入目录
     */
    FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException;

	// 访问文件时调用
    FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException;

    // 当文件不能访问时调用，即目录不能打开时调用；文件不可读时调用
    FileVisitResult visitFileFailed(T file, IOException exc) throws IOException;

    /**
     * 访问完目录及其所有子条目时调用 
     * @param exc 遍历目录内的子内容时出现的错误
     */
    FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException;
}

public enum FileVisitResult {
    // 继续访问
    CONTINUE,
    // 终止访问
    TERMINATE,
    /**
     * 跳过子树结构，即不访问目录内的子条目。
     * 只有在FileVisitor.preVisitDirectory()方法返回才有意义，其它方法返回效果同 CONTINUE
     */
    SKIP_SUBTREE,
    /**
     * 跳过该文件或目录的兄弟节点
     * 如果是FileVisitor.preVisitDirectory()方法调用，则会跳过其目录内容访问，
     * 且该目录的FileVisitor.postVisitDirectory()不会调用
     */
    SKIP_SIBLINGS;
}
```

简单使用：

```java
/**
 * @author fzk
 * @date 2022-05-24 10:22
 */
public class FilesTest {
    public static void main(String[] args) throws IOException {
        Files.walkFileTree(Path.of("D:/testDir"), Set.of(FileVisitOption.FOLLOW_LINKS), 10, new MyFileVisitor());
    }

    private static class MyFileVisitor implements FileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            System.out.printf("进入目录%s \n", dir.getFileName());
            if (dir.startsWith("D:/testDir/skipDir")) {
                System.out.printf("跳过目录%s \n", dir.getFileName());
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            System.out.printf("访问到文件%s\n", file.getFileName());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            System.out.printf("访问文件%s失败:%s \n", file.getFileName(), exc.toString());
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
                System.out.printf("访问目录%s中的内容出现错误:%s \n", dir.getFileName(), exc.toString());
                return FileVisitResult.TERMINATE;
            }
            System.out.printf("退出目录%s", dir.getFileName());
            return FileVisitResult.CONTINUE;
        }
    }
}
```

## Charset

字符集

java.nio包最后一部分是字符集java.nio.charset，这一部分就简略带过。

```java
/**
 * @author fzk
 * @datetime 2022-05-25 22:00
 */
public class CharsetTest {
    public static void main(String[] args) {
        // 所有支持的字符集
        //SortedMap<String, Charset> charsets = Charset.availableCharsets();
        //charsets.forEach((key, value) -> System.out.println(key));
        
        // 默认的字符集UTF-8
        System.out.printf("默认字符集: UTF-8? %b \n", Charset.defaultCharset() == StandardCharsets.UTF_8);


        // 编码方法：encode(String)，返回一个字节缓冲区
        ByteBuffer encode = Charset.forName("GBK").encode("这个世界很残酷");
        while (encode.hasRemaining()) {
            System.out.printf("%d", (int) encode.get());
        }
        encode.rewind();// 回退缓冲区
        // 解码方法：decode(ByteBuffer)返回一个字符缓冲区
        CharBuffer decode = Charset.forName("GBK").decode(encode);
        System.out.println("\n" + decode);
    }
}
// 执行结果如下：
/*
默认字符集: UTF-8? true 
-43-30-72-10-54-64-67-25-70-36-78-48-65-31
这个世界很残酷
*/
```

因为`encode(String)`方法会返回一个ByteBuffer，所以以后将String转换为ByteBuffer，也可以用这个编码方法。

## NIO 网络编程

### 编程步骤

![编程步骤总结](JavaSE.assets/编程步骤总结.png)

### 服务端

大致步骤如下：

1、服务端启动一个`ServerSocketChannel`通道并注册到选择器`Selector`上；

2、选择器监听服务器通道的可接受连接状态`SelectionKey.OP_ACCEPT`，每次发生都意味着新到来一个连接；

3、将新到来的连接注册到选择器`Selector`上并监听其可读状态`SelectionKey.OP_READ`；

4、当客户端发来消息，选择器监听到状态变化，就读取消息内容，将其发送给其它注册到此服务器的客户端。

代码实现：

```java
/**
 * 服务端
 * @author fzk
 * @date 2022-05-25 12:23
 */
public class ChatServer {
    public static void main(String[] args) {
        try {
            new ChatServer().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() throws IOException {
        try (
                // 1.新建selector选择器
                Selector selector = Selector.open();
                // 2.新建服务端通道并绑定到8080端口
                ServerSocketChannel ssc = ServerSocketChannel.open();
        ) {
            ssc.bind(new InetSocketAddress("localhost", 8080));
            ssc.configureBlocking(false);// 设置为非阻塞模式

            // 3.注册channel通道到选择器上
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务端启动成功...");

            // 4.监听通道
            while (selector.select() > 0) {
                // 5.获取到已经就绪的通道
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    iterator.remove();// 必须从选择器选中selected-key集合中移除已经处理的key

                    // 6.监听到有新客户端连接
                    if (next.isAcceptable()) {
                        SocketChannel socketChannel = ssc.accept();
                        socketChannel.configureBlocking(false);// 设置为非阻塞模式
                        // 注册到选择器
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        // 回复客户端
                        socketChannel.write(StandardCharsets.UTF_8.encode("欢迎加入聊天室..."));
                    }
                    // 7.监听到客户端发新消息
                    else if (next.isReadable()) {
                        // 7.1.取出发送者通道
                        SocketChannel sendChan = (SocketChannel) next.channel();

                        // 7.2.读取消息
                    /* 这里可以改为读一批字节就发一批字节，
                    下面这种把字节数组解码变字符串又编码回字节数组发送给客户端的做法只是为了展示消息 */
                        StringBuilder sb = new StringBuilder(128);
                        ByteBuffer buf = ByteBuffer.allocate(128);
                        while (sendChan.read(buf) > 0) {
                            buf.flip();
                            sb.append(StandardCharsets.UTF_8.decode(buf));
                            buf.clear();
                        }
                        String message = sb.toString();
                        System.out.printf("服务端收到来自%s的消息：%s\n", sendChan.getRemoteAddress(), message);

                        // 7.3.广播消息到其它客户端
                        Set<SelectionKey> keys = selector.keys();
                        ByteBuffer msgBuf = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
                        for (SelectionKey key : keys) {
                            SelectableChannel tarChan = key.channel();
                            // 跳过服务器
                            if (tarChan instanceof ServerSocketChannel) continue;
                            // 跳过发送者
                            if (tarChan == sendChan) continue;

                            msgBuf.rewind();// 回退缓冲区
                            ((SocketChannel) tarChan).write(msgBuf);// 发送消息
                        }
                    }
                }
            }
        }
    }
}
```

因为广播消息I/O是比较费时的，所以可以新建Sender线程来专门发消息，从而让主线程一直监听selector选择器：

```java
/**
 * 服务端
 * 发送者线程
 *
 * @author fzk
 * @datetime 2022-05-25 21:13
 */
public class AsyncChatServer {
    public static void main(String[] args) {
        try {
            new AsyncChatServer(10).startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 阻塞队列：放入有消息到来的套接字通道
    private final BlockingQueue<MessageHolder> blockingQueue;

    public AsyncChatServer(int capacity) {
        blockingQueue = new ArrayBlockingQueue<>(capacity);
    }

    public void startServer() throws IOException {
        try (
                // 1.新建selector选择器
                Selector selector = Selector.open();
                // 2.新建服务端通道并绑定到8080端口
                ServerSocketChannel ssc = ServerSocketChannel.open();
        ) {
            ssc.bind(new InetSocketAddress("localhost", 8080));
            ssc.configureBlocking(false);// 设置为非阻塞模式

            // 3.注册channel通道到选择器上
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务端启动成功...");

            // 4.启动发送者线程
            Thread sender = new Thread(new Sender(selector, blockingQueue));
            sender.start();

            // 5.主线程监听通道
            while (selector.select() > 0) {
                // 6.获取到已经就绪的通道
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    iterator.remove();// 必须从选择器选中selected-key集合中移除已经处理的key

                    // 7.监听到有新客户端连接
                    if (next.isAcceptable()) {
                        SocketChannel socketChannel = ssc.accept();
                        socketChannel.configureBlocking(false);// 设置为非阻塞模式
                        // 注册到选择器
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        // 回复客户端
                        socketChannel.write(StandardCharsets.UTF_8.encode("欢迎加入聊天室..."));
                    }
                    // 8.监听到客户端发新消息
                    else if (next.isReadable()) {
                        // 8.1 取出发送者通道并交给发送者线程去处理
                        SocketChannel sendChan = (SocketChannel) next.channel();

                        // 8.2 读取消息
                        /* 这里可以改为读一批字节就发一批字节，
                        下面这种把字节数组解码变字符串又编码回字节数组发送给客户端的做法只是为了展示消息 */
                        StringBuilder sb = new StringBuilder(128);
                        ByteBuffer buf = ByteBuffer.allocate(128);
                        while (sendChan.read(buf) > 0) {
                            buf.flip();
                            sb.append(StandardCharsets.UTF_8.decode(buf));
                            buf.clear();
                        }
                        String message = sb.toString();
                        System.out.printf("服务端收到来自%s的消息：%s\n", sendChan.getRemoteAddress(), message);
                        // 8.3 把消息放入队列
                        blockingQueue.put(new MessageHolder(message, sendChan));
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class Sender implements Runnable {
        private final Selector selector;
        private final BlockingQueue<MessageHolder> queue;

        public Sender(Selector selector, BlockingQueue<MessageHolder> queue) {
            this.selector = selector;
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // 1.取出消息
                    MessageHolder msgHolder = queue.take();

                    // 2.广播消息到其它客户端
                    Set<SelectionKey> keys = selector.keys();
                    ByteBuffer msgBuf = ByteBuffer.wrap(msgHolder.message.getBytes(StandardCharsets.UTF_8));
                    for (SelectionKey key : keys) {
                        SelectableChannel tarChan = key.channel();
                        // 跳过服务器
                        if (tarChan instanceof ServerSocketChannel) continue;
                        // 跳过发送者
                        if (tarChan == msgHolder.sendChan) continue;

                        msgBuf.rewind();// 回退缓冲区
                        ((SocketChannel) tarChan).write(msgBuf);// 发送消息
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static class MessageHolder {
        private final String message;
        private final SocketChannel sendChan;

        public MessageHolder(String message, SocketChannel sendChan) {
            this.message = message;
            this.sendChan = sendChan;
        }
    }
}
```

### 客户端

客户端相对于服务端就比较简单，只需要

1、新建客户端`SocketChannel`套接字通道并注册到选择器`Selector`；

2、由于客户端一直处于可写状态`SelectionKey.OP_WRITE`，所以只需要监听其可读状态`SelectionKey.OP_READ`，新建读线程监听可读状态并读取消息；

3、发消息的内容可以选择以控制台发送，这就要求必须以主线程运行Scanner扫描控制台。

代码实现：

```java
/**
 * 客户端
 * @author fzk
 * @date 2022-05-25 12:23
 */
public class ChatClient {
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

### 测试结果

测试的时候起两个客户端，并且由于客户端监听了Console，所以不要在一个类中以线程形式启动两个客户端，两个客户端要分开启动：

客户端A：

```java
/**
 * @author fzk
 * @datetime 2022-05-25 21:07
 */
public class ClientA {
    public static void main(String[] args) {
        new ChatClient().startClient("ClientA");
    }
}
```

客户端B：

```java
/**
 * @author fzk
 * @datetime 2022-05-25 21:08
 */
public class ClientB {
    public static void main(String[] args) {
        new ChatClient().startClient("ClientB");
    }
}
```

先启动服务端，再分别启动两个客户端，然后发送和接受消息的结果如下：

![image-20220525215447713](JavaSE.assets/image-20220525215447713.png)
