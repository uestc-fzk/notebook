1.GOPROXY的配置

在国内无法直接使用go get下载golang的各种包，但是，https://goproxy.io/zh/，可以让go get 正常使用！！！

```shell
go env -w GOPROXY=https://goproxy.io,direct
```

