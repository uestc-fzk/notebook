package future

import (
	"errors"
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
func NewGoroutinePool(corePoolSize int64, maxPoolSize int64, aliveTime time.Duration, maxTaskChanSize int64) (*GoroutinePool, error) {
	// 1.检查参数
	if corePoolSize < 1 || maxPoolSize > 100 || aliveTime < time.Millisecond || maxTaskChanSize < 1 || maxTaskChanSize > 100 {
		return nil, errors.New("param error")
	}
	// 2.构建协程池
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
	// 3.创建核心协程
	for i := int64(0); i < pool.corePoolSize; i++ {
		go worker(pool)
	}
	return pool, nil
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
	// 1.活跃协程数+1
	atomic.AddInt64(&pool.aliveCount, 1)
	fmt.Println("new goroutine start")
	defer func() {
		// 2.捕获异常并减少活跃协程数量
		err := recover()
		// 活跃协程数-1
		if atomic.AddInt64(&pool.aliveCount, -1) == 0 {
			// 防止因panic导致协程池无活跃协程
			go worker(pool)
		}
		if err != nil {
			fmt.Printf("协程异常退出: %+v\n", err)
		}
	}()
	// 3.监听任务管道
	sleepChan := time.After(pool.aliveTime)
label:
	for {
		select {
		case task, ok := <-pool.taskChan: // 通道的comma,ok语法在通道关闭是ok为false
			if !ok {
				fmt.Println("检测到taskChan已经关闭")
				break
			}
			// 任务处理
			task()
			// 刷新空闲时间
			sleepChan = time.After(pool.aliveTime)
			break
		case <-pool.closeSignal: // 监听到关闭信号
			fmt.Println("receive close signal, close the goroutine")
			break label
		case <-sleepChan: // 空闲时间超时
			if atomic.LoadInt64(&pool.aliveCount) > pool.corePoolSize {
				fmt.Println("close of too free")
				break label
			}
			sleepChan = time.After(pool.aliveTime >> 1)
		}
	}
	runtime.Goexit() // 关闭协程
}
