package future

import (
	"fmt"
	"runtime"
	"sync/atomic"
	"time"
)

// GoroutinePool 协程池
type GoroutinePool struct {
	taskChan        chan Task
	closeSignal     chan int64    // 关闭信号通知通道
	corePoolSize    int64         // 核心协程数量
	maxPoolSize     int64         // 最大协程数量
	aliveTime       time.Duration // 协程最大空闲时间
	aliveCount      int64         // 活跃协程数量
	maxTaskChanSize int64         // 任务通道最大缓冲
}

// NewGoroutinePool 创建协程池
func NewGoroutinePool(corePoolSize int64, maxPoolSize int64, aliveTime time.Duration, maxTaskChanSize int64) *GoroutinePool {
	taskChan := make(chan Task, maxTaskChanSize)
	closeSignal := make(chan int64) // 无缓冲关闭信号通道
	pool := &GoroutinePool{
		taskChan:        taskChan,
		closeSignal:     closeSignal,
		corePoolSize:    corePoolSize,
		maxPoolSize:     maxPoolSize,
		aliveTime:       aliveTime,
		aliveCount:      0,
		maxTaskChanSize: maxTaskChanSize,
	}
	// 先创建核心协程
	for i := int64(0); i < pool.corePoolSize; i++ {
		go worker(pool)
	}
	return pool
}

func (pool *GoroutinePool) SubmitTask(task Task) {
	// 先判断协程数量够不够
	if int64(len(pool.taskChan)) > (pool.maxTaskChanSize>>1) &&
		atomic.LoadInt64(&pool.aliveCount) < pool.maxPoolSize {
		go worker(pool)
	}
	// 提交任务到通道
	pool.taskChan <- task
}

func (pool *GoroutinePool) Close() {
	close(pool.closeSignal) // 要先关这个
	close(pool.taskChan)
}

// Task 任务函数
type Task func()

// worker 工作协程
func worker(pool *GoroutinePool) {
	atomic.AddInt64(&pool.aliveCount, 1) // 原子+1
	fmt.Println("新协程启动")
	defer func() { // 捕获异常并减少活跃协程数量
		err := recover()
		atomic.AddInt64(&pool.aliveCount, -1) // 原子-1
		if err != nil {
			fmt.Printf("协程退出，捕获异常:%+v\n", err)
		} else {
			fmt.Println("协程安全退出")
		}
	}()
	sleepTime := time.Duration(0)
label:
	for {
		select {
		case task, ok := <-pool.taskChan: // 通道的comma,ok语法在通道关闭是ok为false
			if !ok { // 说明taskChan已经关闭
				fmt.Println("检测到taskChan已经关闭")
				break
			}
			// 任务处理
			task()
			// 刷新休眠不活跃时间
			sleepTime = time.Duration(0)
		case <-pool.closeSignal: // 监听到关闭信号
			break label
		default: // 将多余空闲协程关闭
			sleepTime += time.Millisecond * 100
			time.Sleep(time.Millisecond * 100)
			if sleepTime > pool.aliveTime &&
				atomic.LoadInt64(&pool.aliveCount) > pool.corePoolSize {
				break label
			}
		}
	}
	runtime.Goexit() // 关闭协程
}
