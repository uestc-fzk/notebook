package future

import (
	"fmt"
	"runtime"
	"sync/atomic"
	"time"
)

// GoroutinePoolFuture 协程池
type GoroutinePoolFuture struct {
	taskChan        chan Future
	closeSignal     chan int64    // 关闭信号通知通道
	corePoolSize    int64         // 核心协程数量
	maxPoolSize     int64         // 最大协程数量
	aliveTime       time.Duration // 协程最大空闲时间
	aliveCount      int64         // 活跃协程数量
	maxTaskChanSize int64         // 任务通道最大缓冲
}

// NewGoroutinePoolFuture 创建协程池
func NewGoroutinePoolFuture(corePoolSize int64, maxPoolSize int64, aliveTime time.Duration, maxTaskChanSize int64) *GoroutinePoolFuture {
	taskChan := make(chan Future, maxTaskChanSize)
	closeSignal := make(chan int64) // 无缓冲关闭信号通道
	pool := &GoroutinePoolFuture{
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
		go workerF(pool)
	}
	return pool
}

func (pool *GoroutinePoolFuture) SubmitTask(task TaskF) Future {
	// 先判断协程数量够不够
	if int64(len(pool.taskChan)) > (pool.maxTaskChanSize>>1) &&
		atomic.LoadInt64(&pool.aliveCount) < pool.maxPoolSize {
		go workerF(pool)
	}
	// 提交任务到通道
	future := &taskFuture{
		task:       task,
		errChan:    make(chan error, 1),
		resultChan: make(chan interface{}, 1),
	}
	pool.taskChan <- future
	return future
}

func (pool *GoroutinePoolFuture) Close() {
	close(pool.closeSignal) // 要先关这个
	close(pool.taskChan)
}

// TaskF 任务函数
type TaskF func() (interface{}, error)

type Future interface {
	Get() (interface{}, error)
}

type taskFuture struct {
	task       TaskF
	errChan    chan error       // 放错误信息
	resultChan chan interface{} // 放结果
}

func (f *taskFuture) Get() (interface{}, error) {
	for {
		select {
		case r, ok := <-f.resultChan:
			if !ok {
				// result管道关闭了且无数据，说明有err
				return nil, <-f.errChan
			}
			return r, nil
		case err, ok := <-f.errChan:
			if !ok { // err管道关闭了且无数据，说明有result
				return <-f.resultChan, nil
			}
			return nil, err
		}
	}
}

func (f *taskFuture) execute() {
	defer func() {
		e := recover()
		if e != nil {
			// 将执行任务造成的panic转为error返给Future对象
			f.errChan <- fmt.Errorf("%+v", e) // 这里必须这么转异常，因为e的类型可能是各种各样的
			close(f.errChan)
			close(f.resultChan)
			panic(e) // 继续向上抛panic
		}
	}()
	// 1.真正执行任务
	result, err := f.task()
	// 2.结果处理
	if err != nil {
		f.errChan <- err
	} else {
		f.resultChan <- result
	}
	// 3.关闭管道
	close(f.errChan)
	close(f.resultChan)
}

// workerF 工作协程
func workerF(pool *GoroutinePoolFuture) {
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
			task.(*taskFuture).execute()
			// 刷新休眠不活跃时间
			sleepTime = time.Duration(0)
			break
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
