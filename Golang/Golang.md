# 一些技巧

## 环境配置

作为新手，一定一定不要一来就按照官网教程一步一步的走，因为国内特殊情况，最好还是去网上找那种`一套式保姆级教程`，可以少走很多配环境的弯路！！！比如：https://www.liwenzhou.com/posts/Go/golang-menu/

1.GOPROXY的配置

在国内无法直接使用go get下载golang的各种包，但是，https://goproxy.io/zh/，可以让go get 正常使用！！！

```shell
go env -w GOPROXY=https://goproxy.io,direct
# 或者也可以用国内的代理
go env -w GOPROXY=https://goproxy.cn,direct

go env -w GOSUMDB="sum.golang.google.cn"  # 校验包也用国内的代理
```

2.目前都是用go mod管理依赖

```shell
go env -w GO111MODULE=on # 开启mod依赖管理
```



## 交叉编译

在控制台下安装其他系统的运行时，比如下文是`linux x64`

```shell
SET CGO_ENABLED=0
SET GOOS=linux
SET GOARCH=amd64
go build 我的应用.go
```



# 软件安装

## protocolbuffers安装

其GitHub地址：https://github.com/protocolbuffers/protobuf

在其源代码的tag列表中找到需要的版本，如3.16.0，点进去之后选择符合电脑平台的压缩包：

![image-20220228195544340](Golang.assets\image-20220228195544340.png)

选择其中的win64版本的压缩包，解压后，在其bin目录有且仅有一个protoc.exe，这个玩意就是用来生成其他代码如go代码或者Java代码的命令，把它的目录如`D:\SoftWare\protoc-3.14.0-win64\bin`加入到环境变量中，就可以到处执行了(主要是给一些框架执行)

执行`protoc --version`，输出如下则成功了：

```shel
C:\Users\zhike.feng>protoc --version
libprotoc 3.14.0
```

在Linux上安装的话，步骤是一模一样的，下载上图中第二个压缩包，移动到`/usr/local`目录下，`unzip`解压，正常情况下会把`protoc`命令解压到`/usr/local/bin`目录中，此时就可以直接`protoc --version`验证是否成功了(因为/usr/local/bin一般都是在Linux的环境变量中的，不在的话可以自己配置一下)



## go-micro安装使用

这个玩意最好是在Linux上跑，先安装个protocolbuffers.

GitHub地址：https://github.com/asim/go-micro

在其文档处找到**Command Line Interface**，从这里开始：先下载go-micro

```go
go install go-micro.dev/v4/cmd/micro@master
```

需要下载和protocolbuffer相关的依赖：

```go
go get -u google.golang.org/protobuf/proto
go get github.com/golang/protobuf/protoc-gen-go@latest
go get go-micro.dev/v4/cmd/protoc-gen-micro@latest
```

在这3个下载之后，GOPATH/bin目录下会出现3个命令：`micro`， `protoc-gen-go`，  `protoc-gen-micro`。如果没有，则表明下载失败，重新再下载(可以将GOPATH的bin目录和pkg目录都删了)

然后其它的就和GitHub的引导一致了

需要注意的是如果要使用`micro`命令，需要使得此命令的目录在Linux的环境变量PATH下，需要配置一下。一般micro命令会安装于GOPATH/bin下，将此目录加入PATH变量即可。

如果出现问题，把防火墙关了试一下