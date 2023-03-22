# GCC安装

windows要使用make命令，需要安装GCC并配置到环境变量。

Linux和MacOS一般都自带GCC。

# 自定义参数

```makefile
# git提交信息
msg="auto"
git-push:
	git add .
	git commit -m "$(msg)"
	git push origin master
```

然后命令行中如下输入即可：

```shell
make git-push msg="hello world"
```

