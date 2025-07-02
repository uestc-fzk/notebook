# GCC安装

windows要使用make命令，需要安装GCC并配置到环境变量。Linux和MacOS都自带GCC。

mingw-w64 项目是 gcc 的完整运行时环境，用于支持 Windows 64 位和 32 位操作系统原生的二进制文件。

因为需要的仅仅是GCC，所以先到mingw源代码托管网站找到Windows的下载页面：https://sourceforge.net/projects/mingw-w64/files，但是这个页面下载的是源代码需要再编译还挺麻烦。

可以去github上直接下载编译好的二进制gcc包：https://github.com/niXman/mingw-builds-binaries/releases，可以选择这个版本下载：x86_64-14.2.0-release-posix-seh-ucrt-rt_v12-rev1.7z。

1、下载之后解压即可，再将其bin目录添加到系统变量path中，此时在命令行中输入`gcc -v`即可查看是否成功。

2、然后将bin目录中的mingw32-make.exe文件复制粘贴并重命名为make.exe，这么做的目的是方便直接调用make命令

3、IDEA/GOLAND等开发IDE需要配置指定make命令所在目录才能在IDE中使用。



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





# 常用makefile

```makefile
jarfile="target/distribute_file_service-1.0-SNAPSHOT.jar"
p:
	mvn package
run:
	java -jar ${jarfile}
run-ea: # 开启断言检查
	java -ea -jar  ${jarfile}

git-proxy: # 开git代理
	git config --global http.proxy http://127.0.0.1:7890
	git config --global https.proxy https://127.0.0.1:7890
	git config --global -l

git-unproxy: # 关闭git代理
	git config --global --unset http.proxy
	git config --global --unset https.proxy
	git config --global -l

git-protocol-https:# 将本地 Git 仓库从 git 协议切换到 https 协议
	git remote -v
	git remote set-url origin https://gitee.com/uestc-fzk/distribute_file_service.git
	git remote -v

git-protocol-git:# 将本地 Git 仓库从 https 协议切换到 git 协议
	git remote -v
	git remote set-url origin git@gitee.com:uestc-fzk/distribute_file_service.git
	git remote -v

# git提交信息
msg=auto
git-push:
	git add .
	git commit -m "${msg}"
	git push origin main
```

