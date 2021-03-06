#  资料

官网(有中文文档)：https://kubernetes.io/zh/

Kubernetes：https://www.orchome.com/1333

尚硅谷语雀笔记：https://www.yuque.com/leifengyang/oncloud

# 环境搭建(Kubeadm)

Kubeadm 是一个提供了 `kubeadm init` 和 `kubeadm join` 的工具， 作为创建 Kubernetes 集群的 “快捷途径” 的最佳实践。
这个工具能通过两条指令完成一个kubernetes集群的部署：

```shell
# 创建一个 Master 节点
kubeadm init --<args>

# 将一个 Node 节点加入到当前集群中
kubeadm join <Master节点的IP和端口 >
```

kubeadm 通过执行必要的操作来启动和运行最小可用集群。 按照设计，它只关注启动引导，而非配置机器。同样的， 安装各种 “锦上添花” 的扩展，例如 Kubernetes Dashboard、 监控方案、以及特定云平台的扩展，都不在讨论范围内

## 环境说明

这里我的系统环境和软件版本选择如下：

| 系统或软件    | 版本号     |
| ------------- | ---------- |
| 操作系统Linux | CentOS 7.6 |
| kubernetes    | 1.20.9     |
| docker-ce     | 18.06.1-ce |

对于版本说明：虽然说查看kubernetes在GitHub上的CHANGELOG可以看到最新的1.23.x已经支持到docker 20.10.7以下了，但是我特么跑出来有问题。

最终多次尝试，甚至将centos从8.2降到了7.6，终于在上面这个版本配置下成功了。

在3台轻量云服务器上进行环境配置：

| 服务器      | 角色       | 公网IP         |
| ----------- | ---------- | -------------- |
| 腾讯云4核8G | k8s-master | 124.223.192.8  |
| 腾讯云2核4G | k8s-node1  | 101.34.5.36    |
| 阿里云2核2G | k8s-node2  | 106.15.235.113 |

## 安装kubeadm

从官方文档的这里开始：https://kubernetes.io/zh/docs/reference/setup-tools/kubeadm/，按照这里一步一步的搭建，在其中的某些步骤需要更改到国内的阿里云镜像。

### 准备开始

首先有一些机器的要求如下：

> - 一台兼容的 Linux 主机。Kubernetes 项目为基于 Debian 和 Red Hat 的 Linux 发行版以及一些不提供包管理器的发行版提供通用的指令
> - 每台机器 2 GB 或更多的 RAM （如果少于这个数字将会影响你应用的运行内存)
> - 2 CPU 核或更多
> - 集群中的所有机器的网络**彼此均能相互连接**(公网和内网都可以)
> - **节点之中不可以有重复的主机名**、MAC 地址或 product_uuid。请参见[这里](https://kubernetes.io/zh/docs/setup/production-environment/tools/kubeadm/install-kubeadm/#verify-mac-address)了解更多详细信息。
> - **开启机器上的某些端口**。请参见[这里](https://kubernetes.io/zh/docs/setup/production-environment/tools/kubeadm/install-kubeadm/#check-required-ports) 了解更多详细信息。
> - 禁用交换分区。为了保证 kubelet 正常工作，你 **必须禁用交换分区**。

我的3台服务器均满足前4点，接下从剩下来的一条条配置：

#### 设置不同主机名

```shell
#各个机器设置自己的主机名
hostnamectl set-hostname k8s-xxx
```

分别设置k8s-master，k8s-node1，k8s-node2

然后修改3台服务器的/etc/hosts文件，将这几个主机名增加到域名映射规则，结果如下：

```shell
[root@k8s-master ~]# cat /etc/hosts
127.0.0.1 VM-4-15-centos VM-4-15-centos
127.0.0.1 localhost.localdomain localhost
127.0.0.1 localhost4.localdomain4 localhost4

::1 VM-4-15-centos VM-4-15-centos
::1 localhost.localdomain localhost
::1 localhost6.localdomain6 localhost6

124.223.192.8 k8s-master
101.34.5.36 k8s-node1
106.15.235.113 k8s-node2
```

此时，这3台服务器互相ping主机名要求能ping通，命令`ping -c 2 k8s-master`等

#### 检查所需端口

启用这些[必要的端口](https://kubernetes.io/zh/docs/reference/ports-and-protocols/)后才能使 Kubernetes 的各组件相互通信：

![image-20220318153121559](Kubernetes.assets/image-20220318153121559.png)

这个必要的端口，有点多啊，为了方便，直接关掉3台服务器的防火墙(这里需要注意的是，像阿里云腾讯云这种云服务器，不仅仅有机器本身的防火墙，云厂商那边还有一个防火墙，云厂商的防火墙需要配置开放规则)

```shell
systemctl stop firewalld # 停止防火墙
systemctl disable firewalld # 关闭开机自启动
```

#### 禁用swap分区

Swap分区在系统的物理内存不够用的时候，把硬盘内存中的一部分空间释放出来，以供当前运行的程序使用。那些被释放的空间可能来自一些很长时间没有什么操作的程序，这些被释放的空间被临时保存到Swap分区中，等到那些程序要运行时，再从Swap分区中恢复保存的数据到内存中。

```shell
swapoff -a   # 临时关闭
sed -ri 's/.*swap.*/#&/' /etc/fstab  # 永久关闭
```

可以使用 free命令查看

```shell
[root@k8s-master ~]# free -m
              total        used        free      shared  buff/cache   available
Mem:           7820        1137        3656           2        3027        6374
Swap:             0           0           0
```

可以看到swap部分全是0。

#### 时间同步

```shell
yum install ntpdate -y
# 都同步到阿里云时间服务器
ntpdate ntp1.aliyun.com
```

### 允许 iptables 检查桥接流量

确保 `br_netfilter` 模块被加载。这一操作可以通过运行 `lsmod | grep br_netfilter` 来完成。若要显式加载该模块，可执行 `sudo modprobe br_netfilter`。

为了让你的 Linux 节点上的 iptables 能够正确地查看桥接流量，你需要确保在你的 `sysctl` 配置中将 `net.bridge.bridge-nf-call-iptables` 设置为 1。例如：

```bash
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
sudo sysctl --system
```

更多的相关细节可查看[网络插件需求](https://kubernetes.io/zh/docs/concepts/extend-kubernetes/compute-storage-net/network-plugins/#network-plugin-requirements)页面。

### 安装runtime

为了在 Pod 中运行容器，Kubernetes 使用 [容器运行时（Container Runtime）](https://kubernetes.io/zh/docs/setup/production-environment/container-runtimes)。
默认情况下，Kubernetes 使用 [容器运行时接口（Container Runtime Interface，CRI）](https://kubernetes.io/zh/docs/concepts/overview/components/#container-runtime) 来与你所选择的容器运行时交互。

说白了，就是要安装像docker这样的容器。

#### 安装docker

k8s关于安装容器的文档：https://kubernetes.io/zh/docs/setup/production-environment/container-runtimes/
但是呢，安装docker这部分的话，还是得去参考阿里云的docker镜像进行安装https://developer.aliyun.com/mirror/docker-ce，因为国内需要配置阿里云的docker镜像进行加速才能正常下载容器镜像。

在阿里云这边进行安装的时候需要注意，k8s对于docker版本很严格(或者说兼容性很差)，这里可以去k8s的GitHub找到CHANGELOG，
查看k8s某个版本支持的docker版本。在确定k8s版本是1.20的情况下，我选择的docker版本是18.06.1-ce。

#### 配置cgroup 驱动

配置 Docker 守护程序，尤其是使用 systemd 来管理容器的 cgroup。这里顺便再配置个阿里云的镜像加速器：

```shell
sudo mkdir /etc/docker
cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "registry-mirrors": ["https://b9pmyelo.mirror.aliyuncs.com"],
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF



sudo systemctl enable docker
sudo systemctl daemon-reload
sudo systemctl restart docker
```

这里第一个是加速器，其它的是配置守护程序的。

> **警告：**
> 你需要确保容器运行时和 kubelet 所使用的是相同的 cgroup 驱动，否则 kubelet 进程会失败。相关细节可参见[配置 cgroup 驱动](https://kubernetes.io/zh/docs/tasks/administer-cluster/kubeadm/configure-cgroup-driver/)。

### 安装 kubeadm、kubelet 和 kubectl

你需要在每台机器上安装以下的软件包：

- `kubeadm`：用来初始化集群的指令。
- `kubelet`：在集群中的每个节点上用来启动 Pod 和容器等。
- `kubectl`：用来与集群通信的命令行工具。

kubeadm **不能** 帮你安装或者管理 `kubelet` 或 `kubectl`，所以你需要 确保它们与通过 kubeadm 安装的控制平面的版本相匹配。 如果不这样做，则存在发生版本偏差的风险，可能会导致一些预料之外的错误和问题。

由于官方配置的yum源地址在国外，所以又去阿里云找到了kubernetes的镜像：https://developer.aliyun.com/mirror/kubernetes，然后按照阿里云教程进行下载如下：

```shell
cat <<EOF | sudo tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=http://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=http://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg
   http://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
EOF

# 将 SELinux 设置为 permissive 模式（相当于将其禁用）
sudo setenforce 0
sudo sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config
# 这里一定要指定下载版本为1.20.9
sudo yum install -y kubelet-1.20.9 kubeadm-1.20.9 kubectl-1.20.9

# 设置开启自启动并立刻启动kubelet
sudo systemctl enable kubelet && systemctl start kubelet
```

kubelet 现在每隔几秒就会重启，因为它陷入了一个等待 kubeadm 指令的死循环。

### 配置 cgroup 驱动程序

> **警告：**你需要确保容器运行时和 kubelet 所使用的是相同的 cgroup 驱动，否则 kubelet 进程会失败。相关细节可参见[配置 cgroup 驱动](https://kubernetes.io/zh/docs/tasks/administer-cluster/kubeadm/configure-cgroup-driver/)。

只要docker那边配好了，kubelet这边可以不用配置，因为默认就是以systemd作为cgroup驱动。

那么至此，环境就算是安装好了，接下来是配置环境。

## kubeadm init

文档地址：https://kubernetes.io/zh/docs/reference/setup-tools/kubeadm/kubeadm-init/#config-file

此命令初始化一个 Kubernetes 控制平面节点。

"init" 命令执行很多个阶段，其中第一个阶段为`preflight       Run pre-flight checks`，这个阶段特别容易出错，几乎只要前面有任何地方配置错误什么的，这里必warning甚至error。

在init命令执行的时候，要时刻注意出现的什么warning和error，然后上网找到解决方法，也可以去k8s的GitHub的issue页面搜索这些问题，90%情况有。

参数选项常用的：

```shell
--apiserver-advertise-address string # API 服务器所公布的其正在监听的 IP 地址。如果未设置，则使用默认网络接口。
--apiserver-bind-port int32    # API 服务器绑定的端口，默认值：6443
--config string	# kubeadm 配置文件的路径。
--control-plane-endpoint string   # 为控制平面指定一个稳定的 IP 地址或 DNS 名称
--image-repository string    # 选择用于拉取控制平面镜像的容器仓库，默认值："k8s.gcr.io"
--kubernetes-version string  # 为控制平面选择一个特定的 Kubernetes 版本。   默认值："stable-1"
--pod-network-cidr string # pod 网络可以使用的 IP 地址段。如果设置了这个参数，控制平面将会为每一个节点自动分配 CIDRs
--service-cidr string     默认值："10.96.0.0/12"
```

### 初始化主节点

对于目前这个案例，初始化一个控制平面结点，即初始化主节点：**只对主节点执行以下命令**

```shell
kubeadm init \
--image-repository registry.aliyuncs.com/google_containers \
--kubernetes-version v1.20.9
# 这里必须指定下载的镜像地址，不然默认是`k8s.gcr.io`，下载半天都不动的，k8s的版本也要指定，这将用于选择下载的k8s的组件的版本。
```

如果出现了任何warning或者error，可以查查资料解决。
如果在这一步卡住很久，直至它提示超时：则去看看docker日志

```shell
[wait-control-plane] Waiting for the kubelet to boot up the control plane as static Pods from directory "/etc/kubernetes/manifests". This can take up to 4m0s
```

> 因为这一步是启动docker容器的，如果说一直超时，说明有一些容器启动失败了，则会一直重启尝试到超时，这个时候可以用命令`docker logs 容器id`查看这个容器启动失败的原因，一般都是网络配置问题，kubeadm init目录似乎在指定`--apiserver-advertise-address master的公网ip`的时候，容器启动会一直报连接不上2379端口？

如果顺利的话，会看到这个页面：

![kubeadm成功init](Kubernetes.assets/kubeadm成功init.png)

从图中可以看到control-plane初始化成功，然后需要使用集群还需要经过一些步骤的。接下来就按照这些步骤机芯kubectl工具的配置，pod network网络组件的安装，以及node的加入。

现在立刻先把这些内容复制下来保存着：因为这些内容对后面的步骤很重要

```shell
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

Alternatively, if you are the root user, you can run:

  export KUBECONFIG=/etc/kubernetes/admin.conf

You should now deploy a pod network to the cluster.
Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
  https://kubernetes.io/docs/concepts/cluster-administration/addons/

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join 10.0.4.15:6443 --token 7q37gw.bazsiukli3dzpdjs \
    --discovery-token-ca-cert-hash sha256:928a0596140de3a90041e6756ff2ba626f7472b8dd78acf9d714c325adfa7e09 
```

#### 使用kubectl工具

kubectl在未进行如下配置的时候，是无法使用的，按照上面的提示继续在主节点进行如下配置：

```shell
mkdir -p $HOME/.kube 
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

#### 安装pod网络插件

在上面图中，还需要在主节点安装一个网络插件，在找到给出的网页后，选择Calico插件

```shell
curl https://docs.projectcalico.org/manifests/calico.yaml -O

kubectl apply -f calico.yaml
```

在安装了网络插件后，使用`kubectl get nodes`命令可以看到当前集群中有哪些结点，当前只有k8s-master；用`kubectl get pods -A`可以查看集群中部署了哪些应用

```shell
[root@VM-4-15-centos ~]# kubectl get nodes
NAME         STATUS     ROLES                  AGE    VERSION
k8s-master   NotReady   control-plane,master   2m9s   v1.20.9
[root@VM-4-15-centos ~]# kubectl get pods -A
NAMESPACE     NAME                                       READY   STATUS    RESTARTS   AGE
kube-system   calico-kube-controllers-858c9597c8-qkkbl   1/1     Running   0          9m7s
kube-system   calico-node-tb87m                          1/1     Running   0          9m7s
kube-system   coredns-7f89b7bc75-q7snq                   1/1     Running   0          10m
kube-system   coredns-7f89b7bc75-s42x7                   1/1     Running   0          10m
kube-system   etcd-k8s-master                            1/1     Running   0          10m
kube-system   kube-apiserver-k8s-master                  1/1     Running   0          10m
kube-system   kube-controller-manager-k8s-master         1/1     Running   0          10m
kube-system   kube-proxy-fjcwx                           1/1     Running   0          11m
kube-system   kube-scheduler-k8s-master                  1/1     Running   0          10m
```

### join 工作节点

按照上面的提示，在非主节点的从节点中执行下面这一行即可加入集群：

```shell
kubeadm join 10.0.4.15:6443 --token 7q37gw.bazsiukli3dzpdjs \
    --discovery-token-ca-cert-hash sha256:928a0596140de3a90041e6756ff2ba626f7472b8dd78acf9d714c325adfa7e09
```

现在腾讯云2核4G的从节点k8s-node1执行如下：

```shell
[root@k8s-node1 ~]# kubeadm join 10.0.4.15:6443 --token 7q37gw.bazsiukli3dzpdjs \
>     --discovery-token-ca-cert-hash sha256:928a0596140de3a90041e6756ff2ba626f7472b8dd78acf9d714c325adfa7e09
[preflight] Running pre-flight checks
[preflight] Reading configuration from the cluster...
[preflight] FYI: You can look at this config file with 'kubectl -n kube-system get cm kubeadm-config -o yaml'
[kubelet-start] Writing kubelet configuration to file "/var/lib/kubelet/config.yaml"
[kubelet-start] Writing kubelet environment file with flags to file "/var/lib/kubelet/kubeadm-flags.env"
[kubelet-start] Starting the kubelet
[kubelet-start] Waiting for the kubelet to perform the TLS Bootstrap...

This node has joined the cluster:
* Certificate signing request was sent to apiserver and a response was received.
* The Kubelet was informed of the new secure connection details.

Run 'kubectl get nodes' on the control-plane to see this node join the cluster.
```

可以看到加入成功。最后一行提示可以去control-plane即主节点查看集群情况，那么去看看情况如下：

```shell
[root@VM-4-15-centos ~]# kubectl get nodes
NAME         STATUS   ROLES                  AGE     VERSION
k8s-master   Ready    control-plane,master   26m     v1.20.9
k8s-node1    Ready    <none>                 4m51s   v1.20.9
```

但是！！！**这个10.0.4.15是内网地址**，master节点是腾讯云，因此这个腾讯云的从节点可以根据内网地址加入成功，这个阿里云的怎么加入啊？

试了一下直接加入，果然不行。那么再试试直接用公网ip或者说之前修改过主机映射规则的主机名k8s-master能不能行呢？

```shell
[root@k8s-node2 ~]# kubeadm join k8s-master:6443 --token 7q37gw.bazsiukli3dzpdjs     --discovery-token-ca-cert-hash sha256:928a0596140de3a90041e6756ff2ba626f7472b8dd78acf9d714c325adfa7e09
[preflight] Running pre-flight checks
[preflight] Reading configuration from the cluster...
[preflight] FYI: You can look at this config file with 'kubectl -n kube-system get cm kubeadm-config -o yaml'
error execution phase preflight: unable to fetch the kubeadm-config ConfigMap: failed to get config map: Get "https://10.0.4.15:6443/api/v1/namespaces/kube-system/configmaps/kubeadm-config?timeout=10s": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)
To see the stack trace of this error execute with --v=5 or higher
[root@k8s-node2 ~]# kubeadm join 124.223.192.8:6443 --token 7q37gw.bazsiukli3dzpdjs     --discovery-token-ca-cert-hash sha256:928a0596140de3a90041e6756ff2ba626f7472b8dd78acf9d714c325adfa7e09
[preflight] Running pre-flight checks
error execution phase preflight: couldn't validate the identity of the API Server: Get "https://124.223.192.8:6443/api/v1/namespaces/kube-public/configmaps/cluster-info?timeout=10s": x509: certificate is valid for 10.96.0.1, 10.0.4.15, not 124.223.192.8
To see the stack trace of this error execute with --v=5 or higher
```

可以看到都不行。而且最后提示了证书只对10.96.0.1和 10.0.4.15有效。

不过这个问题可以解决！看下面。

### 指定控制平面初始化

其实在kubeadm init指令中有个参数`--control-plane-endpoint string`可以指定控制平面的IP或者DNS名称。

这里只要在主节点初始化的时候指定控制平面的DNS名称即可：把主节点的名称作为控制平面名称

```shell
kubeadm reset # 先把集群reset了
rm -rf $HOME/.kube   # 这里必须删掉这个目录，否则kubectl报错
# 再重新init
kubeadm init \
--image-repository registry.aliyuncs.com/google_containers \
--kubernetes-version v1.20.9 \
--control-plane-endpoint=k8s-master
```

在初始化成功之后，依旧会出现那个提示信息，不过此时的关于kubeadm join的部分发生了一些变化，从内网ip变成了指定的k8s-master这个主机名称了：

```shell
Then you can join any number of worker nodes by running the following on each as root:

kubeadm join k8s-master:6443 --token 593cdf.z241rw97gh363165 \
    --discovery-token-ca-cert-hash sha256:395d56bbd609e6cafff2f96f79ac43d550923f05a336a34f304e77a539e7cbbe
```

再次将这两个从节点加入到集群中，只要两个从节点在/etc/hosts配置了k8s-master到主节点的ip的DNS解析，那么从节点就能访问到主节点，且证书也不会报错了。

主节点在完成了对kubectl的配置之后，使用命令查看当前集群：

```shell
[root@VM-4-15-centos ~]# kubectl get nodes
NAME         STATUS     ROLES                  AGE     VERSION
k8s-master   Ready      control-plane,master   6m25s   v1.20.9
k8s-node1    Ready      <none>                 2m49s   v1.20.9
k8s-node2    NotReady   <none>                 2m21s   v1.20.9
```

啊，好像还是有问题，可以看到3个节点状态只有k8s-node2的status是NotReady，此时用`kubectl get pods -A`查看：

```shell
[root@VM-4-15-centos ~]# kubectl get pods -A
NAMESPACE     NAME                                       READY   STATUS     RESTARTS   AGE
kube-system   calico-kube-controllers-858c9597c8-phbdm   1/1     Running    0          7m27s
kube-system   calico-node-68pbd                          1/1     Running    0          7m27s
kube-system   calico-node-s5l56                          0/1     Init:0/3   0          7m27s
kube-system   calico-node-xjlmb                          1/1     Running    0          7m27s
kube-system   coredns-7f89b7bc75-6kjg2                   1/1     Running    0          11m
kube-system   coredns-7f89b7bc75-xh2vp                   1/1     Running    0          11m
kube-system   etcd-k8s-master                            1/1     Running    0          11m
kube-system   kube-apiserver-k8s-master                  1/1     Running    0          11m
kube-system   kube-controller-manager-k8s-master         1/1     Running    0          11m
kube-system   kube-proxy-hkjcj                           1/1     Running    0          11m
kube-system   kube-proxy-q9r87                           1/1     Running    0          7m28s
kube-system   kube-proxy-vzqdw                           1/1     Running    0          7m56s
kube-system   kube-scheduler-k8s-master                  1/1     Running    0          11m
```

可以看到里面有个玩意一直init步骤处于0的状态啊。当我正在寻找是不是又在哪用了内网ip的时候，它已经通了：

```shell
[root@VM-4-15-centos ~]# kubectl get nodes
NAME         STATUS   ROLES                  AGE   VERSION
k8s-master   Ready    control-plane,master   19m   v1.20.9
k8s-node1    Ready    <none>                 15m   v1.20.9
k8s-node2    Ready    <none>                 15m   v1.20.9
```

所以可以多等一会，看上面的AGE栏大概需要10分钟？

### 验证集群自恢复能力

前提：3台服务器都已经设置过开机自启动docker和kubelet了

将3台服务器都reboot重启，查看恢复情况。

先看从节点：docker ps -a ，是可以看到很多容器exit，也有很多容器刚刚up。

然后主节点：docker ps -a也能看到容器新up。并用kubectl命令查看pod情况：

```shell
[root@k8s-master ~]# kubectl get pod -A
NAMESPACE     NAME                                       READY   STATUS    RESTARTS   AGE
kube-system   calico-kube-controllers-858c9597c8-phbdm   0/1     Error     1          25m
kube-system   calico-node-68pbd                          0/1     Running   1          25m
kube-system   calico-node-s5l56                          0/1     Running   3          25m
kube-system   calico-node-xjlmb                          0/1     Running   2          25m
kube-system   coredns-7f89b7bc75-6kjg2                   1/1     Running   1          29m
kube-system   coredns-7f89b7bc75-xh2vp                   1/1     Running   1          29m
kube-system   etcd-k8s-master                            1/1     Running   1          29m
kube-system   kube-apiserver-k8s-master                  1/1     Running   1          29m
kube-system   kube-controller-manager-k8s-master         1/1     Running   1          29m
kube-system   kube-proxy-hkjcj                           1/1     Running   1          29m
kube-system   kube-proxy-q9r87                           1/1     Running   2          25m
kube-system   kube-proxy-vzqdw                           0/1     Error     1          25m
kube-system   kube-scheduler-k8s-master                  1/1     Running   1          29m
```

可以看到有些pod已经error了，但是没关系，等一等...大概几分钟就能看到全部恢复running

```shell
[root@k8s-master ~]# kubectl get pod -A
NAMESPACE     NAME                                       READY   STATUS    RESTARTS   AGE
kube-system   calico-kube-controllers-858c9597c8-phbdm   1/1     Running   2          26m
kube-system   calico-node-68pbd                          1/1     Running   1          26m
kube-system   calico-node-s5l56                          0/1     Running   3          26m
kube-system   calico-node-xjlmb                          1/1     Running   2          26m
kube-system   coredns-7f89b7bc75-6kjg2                   1/1     Running   1          29m
kube-system   coredns-7f89b7bc75-xh2vp                   1/1     Running   1          29m
kube-system   etcd-k8s-master                            1/1     Running   1          29m
kube-system   kube-apiserver-k8s-master                  1/1     Running   1          29m
kube-system   kube-controller-manager-k8s-master         1/1     Running   1          29m
kube-system   kube-proxy-hkjcj                           1/1     Running   1          29m
kube-system   kube-proxy-q9r87                           1/1     Running   2          26m
kube-system   kube-proxy-vzqdw                           1/1     Running   2          26m
kube-system   kube-scheduler-k8s-master                  1/1     Running   1          29m
```

## kubeadm 命令

在上面中，已经出现了kubeadm init和kubeadm join和kubeadm reset命令了，接下来详细介绍一下其他命令

### kubeadm reset

在init失败了的情况下，直接又进行init是会失败的，需要使用reset命令尽力还原由 `kubeadm init` 或 `kubeadm join` 所做的更改。

在init完成后，还需要进行的步骤中创建了一些关于kubectl的配置，这个也需要删除，即` rm -rf  $HOME/.kube `，否则kubectl命令执行会报错。

### kubeadm token

如[使用引导令牌进行身份验证](https://kubernetes.io/zh/docs/reference/access-authn-authz/bootstrap-tokens/)所描述的，引导令牌用于在即将加入集群的节点和主节点间建立双向认证。

`kubeadm init` 创建了一个有效期为 24 小时的令牌，下面的命令允许你管理令牌，也可以创建和管理新的令牌

> 生成新令牌：kubeadm token create --print-join-command
>
> 查看所有令牌：kubeadm token list

### kubeadm join

待续...



## Dashboard

在官网找到关于Dashboard这类其它工具的安装教程：https://kubernetes.io/zh/docs/reference/tools/、

Dashboard 是基于网页的 Kubernetes 用户界面，可以使用kubectl命令管理k8s集群，也能使用dashboard，那必然是用dashboard了。

其部署步骤如下：

1、应用dashboard的yaml文件来部署

```shell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.3.1/aio/deploy/recommended.yaml
```

2、设置访问端口

```shell
kubectl edit svc kubernetes-dashboard -n kubernetes-dashboard
```

在文件中找到`type=ClusterIP`，将其改为`type=NodePort`；然后执行`kubectl get svc -A |grep kubernetes-dashboard`，如下：

```shell
[root@k8s-master ~]# kubectl edit svc kubernetes-dashboard -n kubernetes-dashboard
service/kubernetes-dashboard edited
[root@k8s-master ~]# kubectl get svc -A |grep kubernetes-dashboard
kubernetes-dashboard   dashboard-metrics-scraper   ClusterIP   10.104.160.128   <none>        8000/TCP                 11m
kubernetes-dashboard   kubernetes-dashboard        NodePort    10.99.165.218    <none>        443:31982/TCP            11m
```

此时就可以访问这个随机分配的端口了，访问集群中除了主节点外的：https://非maste的ip:31982，此时还需要登录验证：

![image-20220319001148092](Kubernetes.assets/image-20220319001148092.png)

3、创建访问账号

新建一个yaml文件：

```shell
vim dash.yaml
# 然后将以下内容复制进去

apiVersion: v1
kind:           
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
```

应用此yaml配置

```shell
kubectl apply -f dash.yaml
```

4、获取访问Token令牌

```shell
#获取访问令牌
kubectl -n kubernetes-dashboard get secret $(kubectl -n kubernetes-dashboard get sa/admin-user -o jsonpath="{.secrets[0].name}") -o go-template="{{.data.token | base64decode}}"
```

结果如下：此处一定要非常注意这整个token，**复制的话，一定要复制到最末尾，即`[root@k8s-master ~]#`的前面**

```shell
[root@k8s-master ~]# kubectl -n kubernetes-dashboard get secret $(kubectl -n kubernetes-dashboard get sa/admin-user -o jsonpath="{.secrets[0].name}") -o go-template="{{.data.token | base64decode}}"
eyJhbGciOiJSUzI1NiIsImtpZCI6ImhsNXczQUpxWl8xMFgxS09CSmdER040eW9ZZ25LY19UUGc5bnBTMzFBWEEifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlcm5ldGVzLWRhc2hib2FyZCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLXhzcGY1Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiI5Y2M0M2FlYi0zY2FlLTQzYzItOTVjOS01MTg1M2Q3ZDAxMzMiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZXJuZXRlcy1kYXNoYm9hcmQ6YWRtaW4tdXNlciJ9.XC-VMXYzWy5wHQHRbjLj5Q4DPtc7P8k8sWVBSWPDO4HuXhlEUdr7MC1TDR1U_rnKPcsTvbpWhDT8uTf40jhDQEKN7WFRe00ug-ouL4b5j8-NAUTuXXP0Bs1sP9TVZQ0kmK8r75TZ_WLcDh0lfN--piFZplVcYSCkD3wfUUkacUalT709wyy6LnY42MvuwevGJmUdfINdCUrKHwRzNbIQ47WvaTyWB5ir6PxFdC2rB5H12CvVFjtUCkiYVORGosQKJl8RfP4XHSGWH9mgbpaxrN9LaNCuUFC58vOyz5W_rlv4-H9AKXj7kwA5ULoAaZRivgadcAIr1Ku0UASW6eWLdA[root@k8s-master ~]# 
```

拿着这个token去登录：

![dashboard1](Kubernetes.assets/dashboard1.png)



# kubectl 命令

## 常用

```shell
# 使用 example-service.yaml 中的定义创建服务。
kubectl apply -f example-service.yaml


########### kubectl get ###############
kubectl get pods -owide # 以纯文本输出格式列出所有 pod，并包含附加信息(如节点名IP等)。
kubectl get ns # 查出所有名称空间

########### kubectl describe ###############
kubectl describe nodes <node-name> # 显示名称为 <node-name> 的节点的详细信息。
kubectl describe pod名 # 显示名为 <pod-name> 的 pod 的详细信息。

########### kubectl delete ###############
kubectl delete -f example-service.yaml  # 删除又yaml文件定义的资源
# 删除所有带有 '<label-key>=<label-value>' 标签的 Pod 和服务
kubectl delete pods,services -l <label-key>=<label-value>
kubectl delete pods --all # 删除所有 pod，包括未初始化的 pod
```

# Kubernetes核心概念

## 名称空间

Kubernetes 支持多个虚拟集群，它们底层依赖于同一个物理集群。 这些虚拟集群被称为名称空间。 在一些文档里名称空间也称为命名空间。
名字空间为名称提供了一个范围。**资源的名称需要在名字空间内是唯一的**，但不能跨名字空间。 名字空间不能相互嵌套，每个 Kubernetes 资源只能在一个名字空间中。不必使用多个名字空间来分隔仅仅轻微不同的资源，例如同一软件的不同版本： 应该使用[标签](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/labels/) 来区分同一名字空间中的不同资源。

> 注意：避免使用前缀 `kube-` 创建名字空间，因为它是为 Kubernetes 系统名字空间保留的。

Kubernetes 会创建四个初始名字空间：

- `default` 没有指明使用其它名字空间的对象所使用的默认名字空间
- `kube-system` Kubernetes 系统创建对象所使用的名字空间
- `kube-public` 这个名字空间是自动创建的，所有用户（包括未经过身份验证的用户）都可以读取它。 这个名字空间主要用于集群使用，以防某些资源在整个集群中应该是可见和可读的。 这个名字空间的公共方面只是一种约定，而不是要求。
- `kube-node-lease` 此名字空间用于与各个节点相关的 [租约（Lease）](https://kubernetes.io/docs/reference/kubernetes-api/cluster-resources/lease-v1/)对象。 节点租期允许 kubelet 发送[心跳](https://kubernetes.io/zh/docs/concepts/architecture/nodes/#heartbeats)，由此控制面能够检测到节点故障

```shell
kubectl create ns my-namespace  # 直接创建namespace
kubectl delete ns my-namespace  # 直接删除namespace
# 获取namespace，在命令中，可以把namespace简写为ns
[root@k8s-master ~]# kubectl get ns
NAME                   STATUS   AGE
default                Active   2d18h
kube-node-lease        Active   2d18h
kube-public            Active   2d18h
kube-system            Active   2d18h
kubernetes-dashboard   Active   2d15h

# 用文件配置进行名称空间相关操作
cat <<EOF | sudo tee myNamespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: hello
EOF

kubectl apply -f myNamespace.yaml  # 以文件方式定义名称空间，-f指以文件方式
kubectl delete -f myNamespace.yaml # 最好也已文件方式删除
```

在默认情况下，所有请求都是访问的default这个名称空间，可以通过`--namespace`设置请求的名称空间

```shell
kubectl get pod --namespace=<名称空间名字>
kubectl run nginx --image=nginx --namespace=<名字空间名称>
```

需要注意的是，并不是所有资源都在名称空间中，有很多资源没有名称空间这个概念，如名称空间本身，节点，持久化卷等

```shell
# 查看位于名字空间中的资源
kubectl api-resources --namespaced=true
# 查看不在名字空间中的资源
kubectl api-resources --namespaced=false
```



## 工作负载

工作负载是在 Kubernetes 上运行的应用程序。

无论你的负载是单一组件还是由多个一同工作的组件构成，在 Kubernetes 中你 可以在一组 [Pods](https://kubernetes.io/zh/docs/concepts/workloads/pods) 中运行它。 在 Kubernetes 中，Pod 代表的是集群上处于运行状态的一组 [容器](https://kubernetes.io/zh/docs/concepts/overview/what-is-kubernetes/#why-containers)。

不需要直接管理每个 Pod。 相反，可以使用 *负载资源* 来管理一组 Pods。 这些资源配置 [控制器](https://kubernetes.io/zh/docs/concepts/architecture/controller/) 来确保合适类型的、处于运行状态的 Pod 个数是正确的，与你所指定的状态相一致。

Kubernetes 提供若干种内置的工作负载资源：

- [Deployment](https://kubernetes.io/zh/docs/concepts/workloads/controllers/deployment/) 和 [ReplicaSet](https://kubernetes.io/zh/docs/concepts/workloads/controllers/replicaset/) （替换原来的资源 [ReplicationController](https://kubernetes.io/zh/docs/reference/glossary/?all=true#term-replication-controller)）。 `Deployment` 适合用来管理集群上的**无状态应用**，`Deployment` 中的所有 `Pod` 都是相互等价的，并且在需要的时候被换掉。
- [StatefulSet](https://kubernetes.io/zh/docs/concepts/workloads/controllers/statefulset/) 让你能够运行一个或者多个以某种方式跟踪应用状态的 Pods。 例如，如果你的负载会**将数据作持久存储**，你可以运行一个 `StatefulSet`，将每个 `Pod` 与某个 [`PersistentVolume`](https://kubernetes.io/zh/docs/concepts/storage/persistent-volumes/) 对应起来。你在 `StatefulSet` 中各个 `Pod` 内运行的代码可以将数据复制到同一 `StatefulSet` 中的其它 `Pod` 中以提高整体的服务可靠性。

- [DaemonSet](https://kubernetes.io/zh/docs/concepts/workloads/controllers/daemonset/) 定义提供**节点本地支撑设施**的 `Pods`。这些 Pods 可能对于你的集群的运维是 非常重要的，例如作为网络链接的辅助工具或者作为网络 [插件](https://kubernetes.io/zh/docs/concepts/cluster-administration/addons/) 的一部分等等。每次你向集群中添加一个新节点时，如果该节点与某 `DaemonSet` 的规约匹配，则控制面会为该 `DaemonSet` 调度一个 `Pod` 到该新节点上运行。
- [Job](https://kubernetes.io/zh/docs/concepts/workloads/controllers/job/) 和 [CronJob](https://kubernetes.io/zh/docs/concepts/workloads/controllers/cron-jobs/)。 定义一些一直运行到结束并停止的任务。`Job` 用来表达的是一次性的任务，而 `CronJob` 会根据其时间规划反复运行。

![image-20220319172635956](Kubernetes.assets/image-20220319172635956.png)

### Pod

Pod 是 k8s 系统中可以**创建和管理的最小单元**，其他的资源对象都是用来支撑或者扩展 Pod 对象功能的，比如控制器对象是用来管控 Pod 对象的，Service 或者 Ingress 资源对象是用来暴露 Pod 引用对象的，PersistentVolume 资源对象是用来为 Pod 提供存储等等，k8s 不会直接处理容器，而是 Pod，Pod 是由一个或多个 container 组成 

Pod 是 Kubernetes 的最重要概念，每一个 Pod 都有一个特殊的被称为**”根容器“的 Pause 容器**。Pause 容器对应的镜像属于 Kubernetes 平台的一部分，除了 Pause 容器，每个 Pod 还包含一个或多个紧密相关的用户业务容器。

#### pod联网

每个 Pod 都在每个地址族中获得一个唯一的 IP 地址。 Pod 中的每个容器共享网络名字空间，包括 IP 地址和网络端口。 ***Pod 内* 的容器可以使用 `localhost` 互相通信**。 当 Pod 中的容器与 *Pod 之外* 的实体通信时，它们必须协调如何使用共享的网络资源 （例如端口）。

在同一个 Pod 内，所有容器共享一个 IP 地址和端口空间，并且可以通过 `localhost` 发现对方。 他们也能通过如 SystemV 信号量或 POSIX 共享内存这类标准的进程间通信方式互相通信。 不同 Pod 中的容器的 IP 地址互不相同，没有 [特殊配置](https://kubernetes.io/zh/docs/concepts/policy/pod-security-policy/) 就不能使用 IPC 进行通信。 如果某容器希望与运行于其他 Pod 中的容器通信，可以通过 IP 联网的方式实现。

Pod 中的容器所看到的系统主机名与为 Pod 配置的 `name` 属性值相同。 [网络](https://kubernetes.io/zh/docs/concepts/cluster-administration/networking/)部分提供了更多有关此内容的信息。

**Pod分类**：

1、普通pod

普通 Pod 一旦被创建，就会被放入到 etcd 中存储，随后会被 Kubernetes Master 调度到某个具体的 Node 上并进行绑定，随后该 Pod 对应的 Node 上的 kubelet 进程实例化成一组相关的 Docker 容器并启动起来。在默认情 况下，当 Pod 里某个容器停止时，Kubernetes 会自动检测到这个问题并且重新启动这个 Pod 里某所有容器， 如果 Pod 所在的 Node 宕机，则会将这个 Node 上的所有 Pod 重新调度到其它节点上。 

2、静态pod

静态 Pod 是由 kubelet 进行管理的仅**存在于特定 Node** 上的 Pod,它们不能通过 API Server 进行管理，无法与 ReplicationController、Deployment 或 DaemonSet 进行关联，并且kubelet 也无法对它们进行健康检查。

#### pod详细配置

用yaml定义pod：

```yaml
apiVersion: v1 #版本号
kind: Pod #类型
metadata:
  name: string, #pod的名称
  namespace: string, #pod所属的命名空间,默认为default
  labels: #自定义标签列表
    - name: string
  annotations: #自定义注解列表
    - name: string
spec: #开始详细定义
  containers: #定义容器列表
  - name: string #容器名称
    image: string #镜像名称
    imagePullPolicy: [Always | Never | IfNotPresent] #镜像拉取策略(Always:每次都尝试重新拉取镜像;IfNotPresent:如果本地有该镜像就用本地的,本地不存在就下载镜像;Never:仅使用本地镜像)
    command: [string] #容器的启动命令列表,如果不指定,则使用镜像打包时使用的启动命令
    args: [string] #容器启动命令参数列表
    workingDir: string #容器的工作目录
    volumeMounts: #挂载到容器内部的存储卷配置
    - name: string #引用pod定义的共享存储卷的名称,需要使用volumes[]部分中定义的共享存储卷名称
      mountPath: string #存储卷在容器内Mount的绝对路径,应少于512个字符
      readOnly: boolean #是否为只读模式,默认为读写模式
    ports: #容器需要暴露的端口号列表
    - name: string #端口的名称
      containerPort: int #容器需要监听的端口号
      hostPort: int #容器所在主机需要监听的端口号,默认与ports[0].containerPort相同.设置hostPort时,同一台宿主机将无法启动该容器的第2份副本(端口冲突)
      protocol: string #端口协议,支持TCP和UDP,默认TCP
    env: #容器运行前需要设置的环境变量列表
    - name: string #环境变量名称
      value: string #环境变量的值
    resources: #资源限制和资源请求的设置
      limits: #资源限制设置
        cpu: string #CPU限制,单位为core数,将用于docker run --cpu-shares 参数
        memory: string #内存限制,单位可以为MiB/GiB等,将用于docker run --memory 参数
      requests: #资源请求限制
        cpu: string #CPU请求,单位为core数,容器启动的初始可用数量
        memory: string #内存请求,单位可以为MiB/GiB等,容器启动的初始可用大小
  volumes: #在该pod上定义的共享存储卷列表
    - name: string #共享存储卷名称,容器定义部分的containers[].volumeMounts[].name将应用该共享存储卷的名称.而volume的类型包括(emptyDir,hostPath,gitRepo,nfs,glusterfs,persistentVolumeClaim,flocker,configMap,rdb,gcePersistentDisk,awsElasticBlockStore等),可以定义多个volume,每个volume的name必须唯一
      emptyDir: {} #此为类型是emptyDir的存储卷,与pod同生命周期的一个临时目录,值是空对象
      hostPath: #类型为hostPath的存储卷,标识挂载pod所在宿主机的目录,通过volumes[].hostPath.path指定
        path: string #pod所在主机的目录,将被用于容器中mount的目录
      secret: #类型为secret的存储卷,标识挂载集群预定义的secret对象到容器内部
        secretName: string 
        items:
        - key: string
          path: string
      configMap: #类型为configMap的存储卷,标识挂载集群预定义的configMap对象到容器内部
        name: string #configMap的名称
        items:
        - key: string
          path: string
    livenessProbe: #对pod内各个容器健康检查的设置,如果探测无响应几次后,系统将自动重启该容器.主要方式有三种(exec,httpGet,tcpSocket)
      exec: #以exec的方式检查pod内各个容器的健康状况
        command: [string] #exec方式需要指定的命令或脚本
      httpGet: #对pod内各个容器健康检查的设置,使用httpGet的方式
        path: string #get的路径
        port: number #get的端口号
        host: string #get的主机ip
        scheme: string #get的协议 http/https
        httpHeaders: #get的请求头
        - name: string #get请求头的key
          value: string #get请求头的值
      tcpSocket: #对pod内各个容器健康检查的设置,使用tcpSocket的方式
        port: number #检测number端口是否在使用
      initialDelaySeconds: 0 #容器启动完成后进行首次探测的时间,单位是秒
      timeoutSeconds: 1 #对容器健康检查的探测等待响应的超时时间设置,单位是秒,默认1秒.超过该超时时间时,将认为该容器不健康,并重启该容器.
      periodSeconds: 10 #对容器健康检查的定期探测时间设置,单位是秒,默认10秒探测一次
      successThreshold: 1 #探测成功的阈值,默认1次,达到该次数时,表示容器正常/健康
      failureThreshold: 3 #探测失败的阈值,达到该次数时,表示容器异常/不健康
      securityContext: #安全上下文,用于定义Pod或Container的权限和访问控制,用的少
        privileged: false
  restartPolicy: [Always | Never | onFailure] #pod的重启策略(Always:pod一旦终止运行,无论pod中的容器是如何终止的,kubelet都将重启它;OnFailure:只有pod以非0退出码终止时,kubelet才会重启该容器,如果容器正常结束,即退出码为0,kubelet则不会重启它
  nodeSelector: object #设置nodeSelector,表示将pod调度到包涵这些label的node节点上,以key:value格式指定{status:dev}
  imagePullSecrets: #拉取镜像时使用secret名称,以name:secretkey的格式指定
  - name: string
  hostNetwork: false #是否使用主机网络模式,默认false.如果设置为true,则表示容器使用宿主机网络,不在使用Docker网桥,该pod将无法在同一台宿主机上启动第二个相同副本
```

#### pod的生命周期和重启策略

**Pod的status**

Pod 的阶段（Phase）是 Pod 在其生命周期中所处位置的简单宏观概述。

| 取值                | 描述                                                         |
| :------------------ | :----------------------------------------------------------- |
| `Pending`（悬决）   | Pod 已被 Kubernetes 系统接受，但有一个或者多个容器尚未创建亦未运行。此阶段包括等待 Pod 被调度的时间和通过网络下载镜像的时间。 |
| `Running`（运行中） | Pod 已经绑定到了某个节点，Pod 中所有的容器都已被创建。至少有一个容器仍在运行，或者正处于启动或重启状态。 |
| `Succeeded`（成功） | Pod 中的所有容器都已成功终止，并且不会再重启。               |
| `Failed`（失败）    | Pod 中的所有容器都已终止，并且至少有一个容器是因为失败终止。也就是说，容器以非 0 状态退出或者被系统终止。 |
| `Unknown`（未知）   | 因为某些原因无法取得 Pod 的状态。这种情况通常是因为与 Pod 所在主机通信失败。 |

如果某节点死掉或者与集群中其他节点失联，Kubernetes 会实施一种策略，将失去的节点上运行的所有 Pod 的 `phase` 设置为 `Failed`

**pod重启策略**

Pod 的 `spec` 中包含一个 `restartPolicy` 字段，其可能取值包括 Always、OnFailure 和 Never。默认值是 Always。

| 重启策略  | 说明                                             |
| --------- | ------------------------------------------------ |
| Always    | 容器失效时，kubelet自动重启该pod的容器           |
| OnFailure | 容器终止运行且退出码不为0，kubelet自动重启该容器 |
| Never     | 容器终止，kubelet不会重启该容器                  |

#### 例子

```shell
kubectl run mynginx --image=nginx # 创建并运行一个 image in a pod

kubectl get pod  # 查看default名称空间的Pod
kubectl describe pod 你自己的Pod名字 # 显示名为 <pod-name> 的 pod 的详细信息
kubectl delete pod Pod名字 # 删除pod
kubectl logs Pod名字 # 查看Pod的运行日志

# 每个Pod - k8s都会分配一个ip
kubectl get pod -owide
curl 172.16.36.73 # 使用Pod的ip+pod里面运行容器的端口，如果访问不了，说明这个内网ip只能部署它的机器访问(公网不方便的地方)

# 获取一个交互 TTY 并运行 /bin/bash <pod-name >。默认情况下，输出来自第一个容器。
kubectl exec -it <pod-name> -- /bin/bash
```

使用yaml文件创建pod

```shell
cat <<EOF | sudo tee pod-mynginx.yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    run: mynginx
  name: mynginx
  namespace: default
spec:
  containers:
  - image: nginx
    name: mynginx
EOF

kubectl apply -f pod-mynginx.yaml  # 应用yaml文件，创建pod
kubectl delete -f pod-mynginx.yaml # 删除此yaml创建的pod
```



### Deployment

一个 *Deployment* 为 [Pods](https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/) 和 [ReplicaSets](https://kubernetes.io/zh/docs/concepts/workloads/controllers/replicaset/) 提供声明式的更新能力。

#### 多副本

一个pod的deploy

```shell
kubectl create deployment mytomcat --image=tomcat:8.5.68 # 创建一个deploy
# 这个命令执行后会启动一个pod，即使用命令将这个pod删除，很快又会启动一个新的pod

kubectl get deploy # 获取当前的deploy
kubectl delete deploy mytomcat # 删除这个mytomcat的deploy
```

创建多份副本pod的deploy，以下示例一次将会创建3个pod

```shell
kubectl create deployment mynginx3 --image=nginx --replicas=3 # 3份副本pod
```

以yaml文件创建，这里要注意apiVersion需要选择`apps/v1`

```shell
cat <<EOF | sudo tee deploy-mynginx3.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: mynginx3
  name: mynginx3
spec:
  replicas: 3  # 启动3个副本
  selector: # 这里的过滤选择标签 必须 匹配下面的template的标签
    matchLabels:
      app: mynginx3
  template:
    metadata:
      labels:
        app: mynginx3
    spec:
      containers: # 配置每个pod中的容器列表
      - image: nginx:1.14.2
        name: nginx
EOF

kubectl apply -f deploy-mynginx3.yaml # 部署
```

#### 扩缩容

通过kubectl scale命令指定副本数量

```shell
kubectl scale --replicas=3 deployment/mynginx3  # 这里缩容为3个副本
```

还可以通过kubectl edit命令修改replica数量实现扩容缩容

```shell
kubectl edit deployment mynginx3 # 这命令会返回该的deploy的yaml配置，直接修改即可
```

#### 更新Deploy

```shell
# 将deploy中的镜像替换成其它版本的镜像，pod将会杀死一个旧的，启动一个新的image的pod
kubectl set image deployment/mynginx3 nginx=nginx:1.16.1 --record

# 也可以直接修改配置资源，这里可以改的就很多了
kubectl edit deployment/mynginx3
```

可以在触发一个或多个更新之前暂停 Deployment，然后再恢复其执行。 这样做使得你能够在暂停和恢复执行之间应用多个修补程序，而不会触发不必要的上线操作。

暂停的 Deployment 和未暂停的 Deployment 的唯一区别是，Deployment 处于暂停状态时， PodTemplateSpec 的任何修改都不会触发新的上线。 Deployment 在创建时是默认不会处于暂停状态。

```shell
kubectl rollout pause deployment/mynginx3   # 暂停运行：只暂停新容器上线
# 此时进行镜像替换操作以及其他更新操作都不会触发上线
kubectl set image deploy/mynginx3 nginx=nginx:1.16.1 
kubectl set resources deploy/mynginx3 -c=nginx --limits=cpu=200m,memory=512Mi

#最终，恢复 Deployment 执行并观察新的 ReplicaSet 的创建过程，其中包含了所应用的所有更新：
kubectl rollout resume deploy/mynginx3
```

#### 版本回退

同时也具有版本回退功能

```shell
#历史记录
kubectl rollout history deploy/mynginx3
#查看某个历史详情
kubectl rollout history deployment/mynginx3 --revision=2
#回滚(回到上次)
kubectl rollout undo deployment/mynginx3
#回滚(回到指定版本)
kubectl rollout undo deployment/mynginx3 --to-revision=2
```

### ReplicaSet

ReplicaSet 的目的是**维护一组在任何时候都处于运行状态的 Pod 副本的稳定集合**。 因此，它通常用来保证给定数量的、完全相同的 Pod 的可用性。

**ReplicaSet 确保任何时间都有指定数量的 Pod 副本在运行**。 然而，Deployment 是一个更高级的概念，它管理 ReplicaSet，并向 Pod 提供声明式的更新以及许多其他有用的功能。 这实际上意味着，你可能永远不需要操作 ReplicaSet 对象：而是使用 Deployment，并在 spec 部分定义你的应用

**ReplicaSet的替代方案**

1、Deployment

尽管 ReplicaSet 可以独立使用，目前它们的主要用途是提供给 Deployment 作为 编排 Pod 创建、删除和更新的一种机制。当使用 Deployment 时，你不必关心 如何管理它所创建的 ReplicaSet，Deployment 拥有并管理其 ReplicaSet。 因此，建议你在需要ReplicaSet 时使用 Deployment。

2、Job

3、DaemonSet

4、ReplicationController

ReplicaSet 是 [ReplicationController](https://kubernetes.io/zh/docs/concepts/workloads/controllers/replicationcontroller/) 的后继者。二者目的相同且行为类似，只是 ReplicationController 不支持 [标签用户指南](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/labels/#label-selectors) 中讨论的基于集合的选择算符需求。 因此，相比于 ReplicationController，应优先考虑 ReplicaSet。

### StatefulSet

StatefulSet 是用来管理有状态应用的工作负载 API 对象。

和 [Deployment](https://kubernetes.io/zh/docs/concepts/workloads/controllers/deployment/) 类似， StatefulSet 管理基于相同容器规约的一组 Pod。但和 Deployment 不同的是， StatefulSet **为每个 Pod 维护了一个有粘性的 ID**。这些 Pod 是基于相同的规约来创建的， 但是不能相互替换：无论怎么调度，每个 Pod 都有一个永久不变的 ID。

可以**使用存储卷为工作负载提供持久存储**。尽管 StatefulSet 中的单个 Pod 仍可能出现故障， 但持久的 Pod 标识符使得将现有卷与替换已失败 Pod 的新 Pod 相匹配变得更加容易







待续...



### Jobs

......

 

## Label

*标签（Labels）* 是附加到 Kubernetes 对象（比如 Pods）上的键值对。 标签旨在用于指定对用户有意义且相关的对象的标识属性，但不直接对核心系统有语义含义。 标签可以用于组织和选择对象的子集。

Label 的最常见的用法是使用 metadata.labels 字段，来为对象添加 Label，通过spec.selector 来引用对象。

一些常用等label示例如下：

> - 版本标签："release" : "stable" , "release" : "canary"...
> - 环境标签："environment" : "dev" , "environment" : "production"
> - 架构标签："tier" : "frontend" , "tier" : "backend" , "tier" : "middleware"
> - 分区标签："partition" : "customerA" , "partition" : "customerB"...
> - 质量管控标签："track" : "daily" , "track" : "weekly"



Label Selector在Kubernetes中重要的使用场景有以下几处：

- kube-controller进程通过资源对象RC上定义的Label Selector来筛选要监控的Pod副本的数量，从而实现Pod副本的数量始终符合预期设定。
- kube-proxy进程通过Service的Label Selector来选择对应的Pod，自动建立起每个Service到对应Pod的请求转发路由表，从而实现Service的智能负载均衡机制。
- 通过对某些Node定义特定的Label，并且在Pod定义文件中使用NodeSelector这种标签调度策略，kube-scheduler进程可以实现Pod“定向调度”的特性。

### 标签选择算符

通过 *标签选择算符*，客户端/用户可以识别一组对象。标签选择算符是 Kubernetes 中的核心分组原语。

API 目前支持两种类型的选择算符：***基于等值的*** 和 ***基于集合的***。 标签选择算符可以由逗号分隔的多个 *需求* 组成。 在多个需求的情况下，必须满足所有要求，因此逗号分隔符充当逻辑 *与*（`&&`）运算符。

空标签选择算符或者未指定的选择算符的语义取决于上下文， 支持使用选择算符的 API 类别应该将算符的合法性和含义用文档记录下来。

**1、基于等值的**

匹配对象必须满足所有指定的标签约束，可接受的运算符有`=`、`==` 和 `!=` 三种。 前两个表示 *相等*（并且只是同义词），而后者表示 *不相等*

**2、基于集合的**

*基于集合* 的标签需求允许你通过一组值来过滤键。 支持三种操作符：`in`、`notin` 和 `exists` (只可以用在键标识符上)。

```
environment in (production, qa)
tier notin (frontend, backend)
partition
!partition
```

- 第一个示例选择了所有键等于 `environment` 并且值等于 `production` 或者 `qa` 的资源。
- 第二个示例选择了所有键等于 `tier` 并且值不等于 `frontend` 或者 `backend` 的资源，以及所有没有 `tier` 键标签的资源。
- 第三个示例选择了所有包含了有 `partition` 标签的资源；没有校验它的值。
- 第四个示例选择了所有没有 `partition` 标签的资源；没有校验它的值。 类似地，逗号分隔符充当 *与* 运算符。因此，使用 `partition` 键（无论为何值）和 `environment` 不同于 `qa` 来过滤资源可以使用 `partition, environment notin（qa)` 来实现

Service和ReplicationController只支持 基于等值 的选择算符；而新的资源如Deployment、Job、DaemonSet和ReplicaSet都支持：

```yaml
......        
        selector:
          matchLabels:
            component: redis
          matchExpressions:
            - {key: tier, operator: In, values: [cache]}
            - {key: environment, operator: NotIn, values: [dev]}
```

`matchLabels` 是由 `{key,value}` 对组成的映射。`matchExpressions` 是 Pod 选择算符需求的列表。 有效的运算符包括 `In`、`NotIn`、`Exists` 和 `DoesNotExist`。 来自 `matchLabels` 和 `matchExpressions` 的所有要求都按逻辑与的关系组合到一起，它们必须都满足才能匹配

































## 服务

### Service

将运行在一组 [Pods](https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/) 上的应用程序公开为网络服务的抽象方法。

一般使用Deployment来运行应用程序，它会动态创建和销毁pod。每个pod有自己的ip，但是在 Deployment 中，在同一时刻运行的 Pod 集合可能与稍后运行该应用程序的 Pod 集合不同。

这导致了一个问题： 如果一组 Pod（称为“后端”）为集群内的其他 Pod（称为“前端”）提供功能， 那么前端如何找出并跟踪要连接的 IP 地址，以便前端可以使用提供工作负载的后端部分？

这就要引入service了。

Kubernetes Service 定义了这样一种抽象：逻辑上的一组 Pod，一种可以访问它们的策略 —— 通常称为微服务。 Service 所针对的 Pods 集合通常是通过[选择算符](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/labels/)来确定的。 当每个 Service 创建时，会被**分配一个唯一的 IP 地址（也称为 clusterIP）**。 这个 IP 地址与 Service 的生命周期绑定在一起，只要 Service 存在，它就不会改变。 可以配置 Pod 使它与 Service 进行通信，Pod 知道与 Service 通信将被自动地负载均衡到该 Service 中的某些 Pod 上。

Service 在 Kubernetes 中是一个 REST 对象，和 Pod 类似。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: myservice3
spec:
  type: NodePort  # 默认是clusterIP
  selector:
    app: mynginx3  # 选中此标签的pod
  ports:
      # 默认情况下，为了方便起见，`targetPort` 被设置为与 `port` 字段相同的值
    - port: 80
      targetPort: 80
      # 可选字段
      # 默认情况下，为了方便起见，Kubernetes 控制平面会从某个范围内分配一个端口号（默认：30000-32767）
      nodePort: 30007
```

Kubernetes `ServiceTypes` 允许指定你所需要的 Service 类型，默认是 `ClusterIP`。

`Type` 的取值以及行为如下：默认是`clusterIP`

- `ClusterIP`：通过集群的内部 IP 暴露服务，选择该值时服务只能够在集群内部访问。
- [`NodePort`](https://kubernetes.io/zh/docs/concepts/services-networking/service/#type-nodeport)：通过每个节点上的 IP 和静态端口（`NodePort`）暴露服务。 `NodePort` 服务会路由到自动创建的 `ClusterIP` 服务。 通过请求 `<节点 IP>:<节点端口>`，可以从集群的外部访问一个 `NodePort` 服务。
- [`LoadBalancer`](https://kubernetes.io/zh/docs/concepts/services-networking/service/#loadbalancer)：使用云提供商的负载均衡器向外部暴露服务。 外部负载均衡器可以将流量路由到自动创建的 `NodePort` 服务和 `ClusterIP` 服务上。
- [`ExternalName`](https://kubernetes.io/zh/docs/concepts/services-networking/service/#externalname)：通过返回 `CNAME` 和对应值，可以将服务映射到 `externalName` 字段的内容（例如，`foo.bar.example.com`）。 无需创建任何类型代理

上面的这些配置会把标签为`app: mynginx3`的一组pod暴露在外网，并映射节点的80端口到pod的80端口，此时可以通过部署有这些pod的节点的公网IP访问到这些pod内的服务。

### Ingress

文档：https://kubernetes.github.io/ingress-nginx/deploy/

Ingress 是对集群中服务的外部访问进行管理的 API 对象，典型的访问方式是 HTTP，Ingress 可以提供负载均衡、SSL 终结和基于名称的虚拟托管。
[Ingress](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.23/#ingress-v1beta1-networking-k8s-io) 公开了从集群外部到集群内[服务](https://kubernetes.io/zh/docs/concepts/services-networking/service/)的 HTTP 和 HTTPS 路由。 流量路由由 Ingress 资源上定义的规则控制。

下面是一个将所有流量都发送到同一 Service 的简单 Ingress 示例：

![image-20220328220030961](Kubernetes.assets/image-20220328220030961.png)

Ingress 可为 Service 提供外部可访问的 URL、负载均衡流量、终止 SSL/TLS，以及基于名称的虚拟托管。

Ingress 不会公开任意端口或协议。 将 HTTP 和 HTTPS 以外的服务公开到 Internet 时，通常使用 [Service.Type=NodePort](https://kubernetes.io/zh/docs/concepts/services-networking/service/#type-nodeport) 或 [Service.Type=LoadBalancer](https://kubernetes.io/zh/docs/concepts/services-networking/service/#loadbalancer) 类型的 Service

#### 环境准备

前提：必须下载ingress控制器，可以选择 [ingress-nginx](https://kubernetes.github.io/ingress-nginx/deploy/)。 也可以从许多 [Ingress 控制器](https://kubernetes.io/zh/docs/concepts/services-networking/ingress-controllers) 中进行选择。

```shell
wget https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.47.0/deploy/static/provider/baremetal/deploy.yaml

#修改镜像
vim deploy.yaml
#将image的值改为如下值：
registry.cn-hangzhou.aliyuncs.com/lfy_k8s_images/ingress-nginx-controller:v0.46.0

# 改个名
mv deploy.yaml ingress-nginx.yaml
# 部署安装
kubectl apply -f ingress-nginx.yaml
# 检查安装的结果
[root@k8s-master ~]# kubectl get pod,svc -n ingress-nginx
NAME                                            READY   STATUS      RESTARTS   AGE
pod/ingress-nginx-admission-create-pnscj        0/1     Completed   0          63m
pod/ingress-nginx-admission-patch-mt46m         0/1     Completed   2          63m
pod/ingress-nginx-controller-65bf56f7fc-5t778   1/1     Running     0          63m

NAME                                         TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)                      AGE
service/ingress-nginx-controller             NodePort    10.99.53.23    <none>        80:31590/TCP,443:31067/TCP   63m
service/ingress-nginx-controller-admission   ClusterIP   10.99.15.254   <none>        443/TCP                      63m
```

此时可以**根据上面查到的ip端口访问查看**是否能够成功访问到ingress-nginx内置的nginx服务。比如此时就可以访问`http://fzk-tx.top:31590`。

#### 测试

创建两个deploy，并分别创建两个service公开访问，yaml如下：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: hello-server
  template:
    metadata:
      labels:
        app: hello-server
    spec:
      containers:
      - name: hello-server
        image: registry.cn-hangzhou.aliyuncs.com/lfy_k8s_images/hello-server
        ports:
        - containerPort: 9000
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: nginx-demo
  name: nginx-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: nginx-demo
  template:
    metadata:
      labels:
        app: nginx-demo
    spec:
      containers:
      - image: nginx
        name: nginx
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: nginx-demo
  name: nginx-demo
spec:
  selector:
    app: nginx-demo
  ports:
  - port: 8000
    protocol: TCP
    targetPort: 80
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: hello-server
  name: hello-server
spec:
  selector:
    app: hello-server
  ports:
  - port: 8000
    protocol: TCP
    targetPort: 9000
```

1、域名访问

这里需要去服务器提供商的域名解析管理将域名解析设置为泛解析*和@解析，这样下面的前缀域名才能有效

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress  
metadata:
  name: ingress-host-bar
spec:
  ingressClassName: nginx
  rules:
  - host: "hello.fzk-tx.top"
    http:
      paths:
      - pathType: Prefix # 前缀匹配
        path: "/"
        backend:
          service:
            name: hello-server
            port:
              number: 8000
  - host: "demo.fzk-tx.top"
    http:
      paths:
      - pathType: Prefix
        path: "/"  # 把请求会转给下面的服务，下面的服务一定要能处理这个路径，不能处理就是404
        # path: "/demo" # 这里可以配置匹配前缀
        backend:
          service:
            name: nginx-demo
            port:
              number: 8000
```

如果出现了某个错误`Internal error occurred: failed calling webhook “validate.nginx.ingress.kubernetes.io`，可以参考[这个](https://stackoverflow.com/questions/61616203/nginx-ingress-controller-failed-calling-webhook)

在经过上面这个配置之后，访问hello.fzk-tx.top:31590和demo.fzk-tx.top:31590，会出现不同效果，如下图：

![ingress-nginx1](Kubernetes.assets/ingress-nginx1.png)

2、路劲重写

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress  
metadata:
  annotations:   # 这里得加个注解启动高级功能
    nginx.ingress.kubernetes.io/rewrite-target: /$2
  name: ingress-host-bar
spec:
  ingressClassName: nginx
  rules:
  - host: "hello.fzk-tx.top"
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: hello-server
            port:
              number: 8000
  - host: "demo.fzk-tx.top"
    http:
      paths:
      - pathType: Prefix
        path: "/demo(/|$)(.*)"  # 这里就是把前缀路劲去掉
        backend:
          service:
            name: nginx-demo 
            port:
              number: 8000
```

此时就需要访问`http://demo.fzk-tx.top:31590/demo`

3、流量限制

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-limit-rate
  annotations:  # 流量限制要引入这个注解
    nginx.ingress.kubernetes.io/limit-rps: "1"
spec:
  ingressClassName: nginx
  rules:
  - host: "haha.fzk-tx.top"
    http:
      paths:
      - pathType: Exact  # 精确匹配，这里只匹配跟路劲
        path: "/"
        backend:
          service:
            name: nginx-demo
            port:
              number: 8000
```

此时可以访问`http://haha.fzk-tx.top:31590/`，然后疯狂刷新，就能看见流控了：

![image-20220329011036734](Kubernetes.assets/image-20220329011036734.png)

## 存储

**背景**

Container 中的文件在磁盘上是临时存放的，这给 Container 中运行的较重要的应用程序带来一些问题。**问题之一是当容器崩溃时文件丢失，kubelet 会重新启动容器，但容器会以干净的状态重启。** 第二个问题会在同一 `Pod` 中运行多个容器并共享文件时出现。 Kubernetes [卷（Volume）](https://kubernetes.io/zh/docs/concepts/storage/volumes/) 这一抽象概念能够解决这两个问题。虽然docker也能挂载卷，但是在kubernetes中，pod崩溃后，新pod可能会换节点部署，那么原容器挂载的卷在新的pod就不可见啦。

卷的核心是一个目录，其中可能存有数据，Pod 中的容器可以访问该目录中的数据。

使用卷时, 在 `.spec.volumes` 字段中设置为 Pod 提供的卷，并在 `.spec.containers[*].volumeMounts` 字段中声明卷在容器中的挂载位置。

卷挂载在镜像中的[指定路径](https://kubernetes.io/zh/docs/concepts/storage/volumes/#using-subpath)下。 Pod 配置中的每个容器必须独立指定各个卷的挂载位置。卷不能挂载到其他卷之上（不过存在一种[使用 subPath](https://kubernetes.io/zh/docs/concepts/storage/volumes/#using-subpath) 的相关机制），也不能与其他卷有硬链接。

kubernetes支持的**卷类型**非常多，在这里做demo的话可以用**nfs**或local类型卷。

![image-20220404225141837](Kubernetes.assets/image-20220404225141837.png)

### Volume

Volume是**Pod中能够被多个容器访问的共享目录**。Kubernetes的Volume概念、用途和目的与Docker的Volume比较类似，但两者不能等价。首先，Kubernetes中的Volume定义在Pod上，然后被一个Pod里的多个容器挂载到具体的文件目录下；其次，Kubernetes中的Volume中的数据也不会丢失。最后，Kubernetes支持多种类型的Volume，例如Gluster、Ceph等先进的分布式文件系统。

Volume的使用也比较简单，在大多数情况下，我们先在Pod上声明一个Volume，然后在容器里引用该Volume并Mount到容器里的某个目录上。

### nfs

`nfs` 卷能将 NFS (网络文件系统) 挂载到你的 Pod 中。 不像 `emptyDir` 那样会在删除 Pod 的同时也会被删除，`nfs` 卷的内容在删除 Pod 时会被保存，卷只是被卸载。 这意味着 `nfs` 卷可以被预先填充数据，并且这些数据可以在 Pod 之间共享。

#### 环境准备

```shell
#所有机器安装
yum install -y nfs-utils
```

主节点

```shell
#nfs主节点
echo "/nfs/data/ *(insecure,rw,sync,no_root_squash)" > /etc/exports

mkdir -p /nfs/data  # 创建这个文件夹
systemctl enable rpcbind --now
systemctl enable nfs-server --now
#配置生效
exportfs -r
```

接下来就检查一下成功与否

```shell
[root@k8s-master ~]# exportfs
/nfs/data     	<world>
```

从节点

```shell
showmount -e 主节点ip地址   # 此命令可以在从节点查看主节点哪些目录可以挂载
# 将目录挂载到主节点的这个nfs目录
mkdir -p /nfs/data
mount -t nfs 主节点ip:/nfs/data /nfs/data
```

![image-20220405001335605](Kubernetes.assets/image-20220405001335605.png)

从此图可以看到，左边的master节点创建的文件，在slave节点能看到，那么nfs文件系统就搭建成功了。

#### 原生方式挂载数据

```yaml
cat <<EOF | sudo tee deploy-nginx-pv-demo.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: nginx-pv-demo
  name: nginx-pv-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: nginx-pv-demo
  template:
    metadata:
      labels:
        app: nginx-pv-demo
    spec:
      containers:
      - image: nginx
        name: nginx
        volumeMounts:
        - name: html # 名无所谓的
          mountPath: /usr/share/nginx/html # 将容器内的此目录挂载出去
      volumes: # 挂载数据卷
        - name: html # 名得和上面这个一样
          nfs: # 以nfs方式挂载数据
            server: 124.223.192.8
            path: /nfs/data/nginx-pv   # 挂载的节点的此目录
EOF

# 在主节点执行命令创建容器
[root@k8s-master data]# kubectl apply -f deploy-nginx-pv-demo.yaml 
```

此yaml文件将容器内的/usr/share/nginx/html目录挂载到节点的/nfs/data/nginx-pv

直接应用应该是会报错的，原因也很简单：No such file or directory；需要自己提前创建好挂载的目录才行`mkdir -p /nfs/data/nginx-pv`；创建好之后，再去dashboard看就部署成功了。

测试一下：

```shell
# 在这个挂载的目录下搞一个index.html页面
cd /nfs/data/nginx-pv
echo "hello nfs" >> index.html

# 为了外部访问，桥接一个service
cat <<EOF | sudo tee deploy-nginx-pv-demo-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-pv-demo-service
spec:
  type: NodePort  # 默认是clusterIP
  selector:
    app: nginx-pv-demo  # 选中此标签的pod
  ports:
      # 默认情况下，为了方便起见，`targetPort` 被设置为与 `port` 字段相同的值
      - port: 80
        targetPort: 80
        # 可选字段
        # 默认情况下，为了方便起见，Kubernetes 控制平面会从某个范围内分配一个端口号（默认：30000-32767）
        nodePort: 30008
EOF

kubectl apply -f deploy-nginx-pv-demo-service.yaml
```

部署成功的话，就能直接访问测试了，可以看到完全没得问题

```shell
# nodePort方式得从外网ip访问
[root@k8s-master data]# curl fzk-tx.top:30008
hello nfs
```

此时如果用kubectl命令将部署的这个deployment删除的话，其挂载的目录/nfs/data/nginx-pv还是会存在，并不受影响。所以在pod出现问题后，kubernetes重启pod，一切就都恢复正常。

### 持久卷PVC

#### 概述

PersistentVolume 子系统为用户 和管理员提供了一组 API，将存储如何供应的细节从其如何被使用中抽象出来。引入两个新概念：

PV：持久卷(Persistent Volume)，将应用需要持久化的数据保存到指定位置。
PVC：持久卷申领(Persistent Volume Claim)，申明需要使用的持久卷规格。
PV 卷是集群中的资源。PVC 申领是对这些资源的请求。

**PV卷的供应有两种方式：**

1、静态供应：集群管理员提前创建好若干PV卷，之后PVC申请只能从这些分配好大小的块中选合适的。

![image-20220406232653075](Kubernetes.assets/image-20220406232653075.png)

2、动态供应：如果事先创建的静态PV卷无法满足用户的PVC匹配，集群可以尝试为它动态供应一个存储卷。

**绑定**

创建的PVC对象会寻找与之匹配的PV卷，并将两者绑定到一起。绑定具有**排他性**，实现上使用 ClaimRef 来记述 PV 卷 与 PVC 申领间的双向绑定关系。

如果找不到匹配的PV卷，PVC申领会无限期处于未绑定状态，直至匹配的PV卷出现。

**pod使用PVC**

Pod将PVC申领当做存储卷使用，找到PVC绑定的卷，并将其挂载到pod。

**持久卷的类型**

PV持久卷是用插件的形式实现的。kubernetes支持的插件比较多，下面做的demo以[nfs](https://kubernetes.io/zh/docs/concepts/storage/volumes/#nfs) 作为存储卷类型。

#### 持久卷PV配置

每个 PV 对象都包含 `spec` 部分和 `status` 部分，分别对应卷的规约和状态。示例：

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv01-10m
spec:
  capacity:
    storage: 10Mi # 设置卷容量,常用的单位有Ki,Mi,Gi等
  accessModes: # 访问模式
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Recycle # 回收策略
  storageClassName: nfs # 类
  nfs:
    path: /nfs/data/01 # nfs目录
    server: 124.223.192.8 # nfs主节点IP
```

**容量**

`spec.capacity.storage`属性，可以接受的10进制单位有m(milli),k(小写),M,G，如1G=10<sup>3</sup>M=10<sup>6</sup>k=10<sup>9</sup>m。公认的二进制单位有Ki,Mi,Gi，如1Gi=2<sup>10</sup>Mi=2<sup>20</sup>Ki。

**访问模式**

`spec.accessModes`属性。

- `ReadWriteOnce`

  卷可以被一个节点以读写方式挂载。

- `ReadOnlyMany`

  卷可以被多个节点以只读方式挂载。

- `ReadWriteMany`

  卷可以被多个节点以读写方式挂载。

- `ReadWriteOncePod`

  卷可以被单个 Pod 以读写方式挂载。 如果你想确保整个集群中只有一个 Pod 可以读取或写入该 PVC， 请使用ReadWriteOncePod 访问模式。这只支持 CSI 卷以及需要 Kubernetes 1.22 以上版本。

> **重要提醒！** 每个卷同一时刻只能以一种访问模式挂载，即使该卷能够支持 多种访问模式。

还有一点需要注意，不同存储卷插件支持的访问模式是不同的。如NFS只支持前三种，更多支持情况需要看[文档](https://kubernetes.io/zh/docs/concepts/storage/persistent-volumes/#persistent-volumes)。

**类**

 `spec.storageClassName` 属性。每个 PV 可以属于某个类（Class），特定类的 PV 卷只能绑定到请求该类存储卷的 PVC 申领。 未设置 `storageClassName` 的 PV 卷没有类设定，只能绑定到那些没有指定特定存储类的 PVC 申领。

**回收策略**

目前的回收策略有：

- Retain -- 手动回收(默认)
- Recycle -- 基本擦除 (`rm -rf /thevolume/*`)
- Delete -- 诸如 AWS EBS、GCE PD、Azure Disk 或 OpenStack Cinder 卷这类关联存储资产也被删除

目前，仅 NFS 和 HostPath 支持回收（Recycle）。 AWS EBS、GCE PD、Azure Disk 和 Cinder 卷都支持删除（Delete）。更多细节：https://kubernetes.io/zh/docs/concepts/storage/persistent-volumes/#reclaiming

#### PVC配置

每个 PVC 对象都有 `spec` 和 `status` 部分，分别对应申领的规约和状态。示例：

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: nginx-pvc
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests: # 请求需要200M空间
      storage: 200Mi
  storageClassName: nfs # 这个类将会与PV的类为nfs的进行匹配绑定
  selector: # 选择算符
    matchLabels:
      release: "stable"
    matchExpressions:
      - {key: environment, operator: In, values: [dev]}
```

**访问模式**

`spec.accessModes`，同PV访问模式。

**资源**

`spec.resources.requests.storage`字段请求存储空间资源。单位同PV处的描述。

**选择算符**

PVC还能根据选择算符过滤PV，选择合适的PV卷。

**类**

`spec.storageClassName`属性，PVC可以指定类名， `storageClassName` 值与 PVC 设置相同的 PV 卷， 才能绑定到 PVC 申领。如果没有设置，即为`""`，则被视为要请求的是没有设置存储类的 PV 卷。

#### 创建静态PV池

在nfs主节点创建下面几个目录：

```shell
#nfs主节点
mkdir -p /nfs/data/01
mkdir -p /nfs/data/02
mkdir -p /nfs/data/03
```

然后创建yaml文件

```shell
cat <<EOF | sudo tee pv.yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv01-10m
spec:
  capacity:
    storage: 10Mi # 设置卷容量,常用的单位有Ki,Mi,Gi等
  accessModes:
    - ReadWriteMany
  storageClassName: nfs
  nfs:
    path: /nfs/data/01 # nfs目录
    server: 124.223.192.8 # nfs主节点IP
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv02-1gi
spec:
  capacity: 
    storage: 1Gi # 设置卷容量,常用的单位有Ki,Mi,Gi等
  accessModes:
    - ReadWriteMany
  storageClassName: nfs
  nfs:
    path: /nfs/data/02
    server: 124.223.192.8 # nfs主节点IP
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv03-3gi
spec:
  capacity:
    storage: 3Gi # 设置卷容量,常用的单位有Ki,Mi,Gi等
  accessModes:
    - ReadWriteMany
  storageClassName: nfs
  nfs:
    path: /nfs/data/03
    server: 124.223.192.8    # nfs主节点IP
EOF

# 应用一下
kubectl apply -f pv.yaml
# 没有错误的话就查看效果如下
[root@k8s-master data]# kubectl get pv
NAME       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS      CLAIM   STORAGECLASS   REASON   AGE
pv01-10m   10M        RWX            Retain           Available           nfs                     48s
pv02-1gi   1Gi        RWX            Retain           Available           nfs                     48s
pv03-3gi   3Gi        RWX            Retain           Available           nfs                     48s

```

#### PVC创建与绑定

1、PVC创建

```shell
cat <<EOF | sudo tee pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: nginx-pvc
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests: # 请求需要200M空间
      storage: 200Mi
  storageClassName: nfs # 这个类需要与PV的类相同
EOF
```

申请一下呢

![PV1](Kubernetes.assets/PV1.png)

从上图可以看到，申请的200M第一个pv不能满足，所以选择绑定最合适的第2个1GB容量的pv。

查看pvc：

```shell
[root@k8s-master data]# kubectl get pvc
NAME        STATUS   VOLUME     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
nginx-pvc   Bound    pv02-1gi   1Gi        RWX            nfs            5m3s
```

#### 使用PVC作为卷

Pod 将PVC作为卷来使用，并藉此访问存储资源。 PVC和pod必须在同一namespace。找到PVC之后，其绑定的PV卷会被挂载到宿主上并挂载到 Pod 中。

```shell
cat <<EOF | sudo tee nginx-pvc.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: nginx-deploy-pvc
  name: nginx-deploy-pvc
spec:
  replicas: 2
  selector:
    matchLabels:
      app: nginx-deploy-pvc
  template:
    metadata:
      labels:
        app: nginx-deploy-pvc
    spec:
      containers:
      - image: nginx
        name: nginx
        volumeMounts:
        - name: html # 名字无所谓
          mountPath: /usr/share/nginx/html # 将此目录挂载出去
      volumes:
        - name: html # 这个名得和上面匹配
          persistentVolumeClaim: # 挂载到这个PVC上
            claimName: nginx-pvc # 这里的名称得和上面PVC的名称一样
EOF

# 应用一下
kubectl apply -f nginx-pvc.yaml
```

这样，就把nginx的/usr/share/nginx/html目录挂载到名称为nginx-pvc的PVC了，即它所申请的PV，即/nfs/data/02目录。

测试一下：

```shell
# 在这个挂载的目录下搞一个index.html页面
cd /nfs/data/02
echo "hello this is pvc for index.html" >> index.html

# 为了外部访问，桥接一个service
cat <<EOF | sudo tee nginx-pvc-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-deploy-pvc-service
spec:
  type: NodePort  # 默认是clusterIP
  selector:
    app: nginx-deploy-pvc  # 选中此标签的pod
  ports:
      # 默认情况下，为了方便起见，`targetPort` 被设置为与 `port` 字段相同的值
      - port: 80
        targetPort: 80
        # 可选字段
        # 默认情况下，为了方便起见，Kubernetes 控制平面会从某个范围内分配一个端口号（默认：30000-32767）
        nodePort: 30009
EOF

kubectl apply -f nginx-pvc-service.yaml
```

部署成功的话，就能直接访问测试了，可以看到完全没得问题

```shell
[root@k8s-master 02]# kubectl get service
NAME                       TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
hello-server               ClusterIP   10.111.39.94   <none>        8000/TCP       8d
kubernetes                 ClusterIP   10.96.0.1      <none>        443/TCP        16d
nginx-demo                 ClusterIP   10.101.165.9   <none>        8000/TCP       8d
nginx-deploy-pvc-service   NodePort    10.103.36.77   <none>        80:30009/TCP   9s
# nodePort 方式得从外网IP访问
[root@k8s-master 02]# curl fzk-tx.top:30009
hello this is pvc for index.html
```

### 临时卷Ephemeral Volume

有些应用程序需要额外的存储，但并不关心数据在重启后仍然可用。 例如，缓存服务经常受限于内存大小，将不常用的数据转移到比内存慢、但对总体性能的影响很小的存储中。

另有些应用程序需要以文件形式注入的只读数据，比如配置数据或密钥。

*临时卷* 就是为此类用例设计的。因为卷会遵从 Pod 的生命周期，**与 Pod 一起创建和删除**， 所以停止和重新启动 Pod 时，不会受持久卷在何处可用的限制。

临时卷在 Pod 规范中以 *内联* 方式定义，这简化了应用程序的部署和管理。

**临时卷的类型**

Kubernetes 为了不同的目的，支持几种不同类型的临时卷：

- [emptyDir](https://kubernetes.io/zh/docs/concepts/storage/volumes/#emptydir)：Pod 启动时为空，存储空间来自本地的 kubelet 根目录（通常是根磁盘）或内存
- [configMap](https://kubernetes.io/zh/docs/concepts/storage/volumes/#configmap)、 [downwardAPI](https://kubernetes.io/zh/docs/concepts/storage/volumes/#downwardapi)、 [secret](https://kubernetes.io/zh/docs/concepts/storage/volumes/#secret)：将不同类型的 Kubernetes 数据注入到 Pod 中，是作为 [本地临时存储](https://kubernetes.io/zh/docs/concepts/configuration/manage-resources-containers/#local-ephemeral-storage) 提供的。它们由各个节点上的 kubelet 管理。
- [CSI 临时卷](https://kubernetes.io/zh/docs/concepts/storage/volumes/#csi-ephemeral-volumes)：由专门[支持此特性](https://kubernetes-csi.github.io/docs/drivers.html) 的 [CSI 驱动程序](https://github.com/container-storage-interface/spec/blob/master/spec.md)提供
- [通用临时卷](https://kubernetes.io/zh/docs/concepts/storage/ephemeral-volumes/#generic-ephemeral-volumes)：可以由所有支持持久卷的存储驱动程序提供



### ConfigMap

ConfigMap 是一种 API 对象，用来将非机密性的数据保存到键值对中。使用时， [Pods](https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/) 可以将其用作环境变量、命令行参数或者存储卷中的配置文件。

ConfigMap 将环境配置信息和 [容器镜像](https://kubernetes.io/zh/docs/reference/glossary/?all=true#term-image) 解耦，便于应用配置的修改。

ConfigMap 使用 `data` 和 `binaryData` 字段。`data` 字段设计用来保存 UTF-8 字符串，而 `binaryData` 则被设计用来保存二进制数据作为 base64 编码的字串。

#### redis示例

1、创建配置集

```shell
# 先创建一个redis.conf配置文件
cat <<EOF | sudo tee redis.conf
port 6379
requirepass !MyRedis123456
protected-mode no
# daemonize yes # 这里不能开启守护进程，开启之后，redis默认后台启动，但是docker容器会自动结束


pidfile "/tmp/redis_6379.pid"
dbfilename dump6379.rdb
# 工作目录
dir /tmp
logfile "6379.log"

# 缓存策略
maxmemory 100mb
maxmemory-policy allkeys-lru
EOF

# 创建配置，redis保存到k8s的etcd；
[root@k8s-master configTestDir]# kubectl create cm redis-conf --from-file=redis.conf
configmap/redis-conf created
[root@k8s-master configTestDir]# kubectl get cm
NAME               DATA   AGE
kube-root-ca.crt   1      20d
redis-conf         1      6s
```

此时可以看看这个配置集详情：

```yaml
# 以yaml格式输出
[root@k8s-master configTestDir]# kubectl get cm redis-conf -oyaml
apiVersion: v1
kind: ConfigMap
data:
  redis.conf: |
    port 6379
    requirepass !MyRedis123456
    protected-mode no
    # daemonize yes # 这里不能开启守护进程，开启之后，redis默认后台启动，但是docker容器会自动结束


    pidfile "/tmp/redis_6379.pid"
    dbfilename dump6379.rdb
    # 工作目录
    dir /tmp
    logfile "6379.log"

    # 缓存策略
    maxmemory 100mb
    maxmemory-policy allkeys-lru
metadata:
  creationTimestamp: "2022-04-07T15:44:31Z"
  managedFields:
  - apiVersion: v1
    fieldsType: FieldsV1
    fieldsV1:
      f:data:
        .: {}
        f:redis.conf: {}
    manager: kubectl-create
    operation: Update
    time: "2022-04-07T15:44:31Z"
  name: redis-conf
  namespace: default
  resourceVersion: "2490004"
  uid: e0e19829-d6fb-4ff1-90d9-65b8cc31ae82
```

如果要手写configMap配置文件的话，就是从上面这个样板简化简化如下：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-conf
  namespace: default
data: # data就是配置数据，key：默认是文件名  value是文件内容
  redis.conf: |
    port 6379
    requirepass !MyRedis123456
    protected-mode no
    # daemonize yes # 这里不能开启守护进程，开启之后，redis默认后台启动，但是docker容器会自动结束


    pidfile "/tmp/redis_6379.pid"
    dbfilename dump6379.rdb
    # 工作目录
    dir /tmp
    logfile "6379.log"

    # 缓存策略
    maxmemory 100mb
    maxmemory-policy allkeys-lru
```

2、创建deployment

```yaml
cat <<EOF | sudo tee cm-redis-demo.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: cm-redis-demo
  name: cm-redis-demo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cm-redis-demo
  template:
    metadata:
      labels:
        app: cm-redis-demo
    spec:
      containers:
      - image: redis
        name: redis
        command: # 这里配置启动命令
        - redis-server
        - "/redis-master/redis.conf"  
        volumeMounts:
        - name: data # 名字无所谓
          mountPath: /data # 将此目录挂载出去
        - name: config
          mountPath: /redis-master
      volumes:
        - name: data # 这个名得和上面匹配
          emptyDir: {}
        - name: config
          configMap:
            name: redis-conf # 指定配置集ConfigMap
            items:
            - key: redis.conf # 指定cm定义中data的键
              path: redis.conf # mountPath下的文件名，将键对应的值作为此文件内容
EOF

# 应用一下
kubectl apply -f cm-redis-demo.yaml 
```

上面这些配置大概如下图所示：(注：下图出现的configMap是不能通过的，只有小写字母、数字和`-`是允许的)

![configMap-redis-demo](Kubernetes.assets/configMap-redis-demo.png)

在dashboard中进入到上述启动的pod中，查看redis.conf是否正确：

```shell
root@cm-redis-demo-685564999c-drnqf:/data# cd /redis-master/
root@cm-redis-demo-685564999c-drnqf:/redis-master# ls
redis.conf
root@cm-redis-demo-685564999c-drnqf:/redis-master# cat redis.conf 
port 6379
requirepass !MyRedis123456
protected-mode no
# daemonize yes # 这里不能开启守护进程，开启之后，redis默认后台启动，但是docker容器会自动结束


pidfile "/tmp/redis_6379.pid"
dbfilename dump6379.rdb
# 工作目录
dir /tmp
logfile "6379.log"

# 缓存策略
maxmemory 100mb
maxmemory-policy allkeys-lru
```

可以看到，没得问题哦。这里还可以用redis-cli命令连接redis服务，不过，由于设置了密码，这里好像直接用`redis-cli -a "!MyRedis123456"`能连上，但是还是认证失败。在下面的修改配置集时注释掉密码就可以在dashboard中用redis-cli使用了。

3、修改配置集configMap

那么如何动态修改configMap呢？

```shell
kubectl edit cm 你的configMap名称
```

这里将configMap修改之后，pod中的挂载的相应配置文件很快也会跟着更改。

不过似乎，deploy并没有让pod重启，那只能手动删除pod，等待deploy让它重启，这样新的redis配置才会生效。

### Secret

Secret 是一种包含少量敏感信息例如密码、令牌或密钥的对象。将这些信息放在 secret 中比放在 [Pod](https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/) 的定义或者 [容器镜像](https://kubernetes.io/zh/docs/reference/glossary/?all=true#term-image) 中来说更加安全和灵活

由于创建 Secret 可以独立于使用它们的 Pod， 因此在创建、查看和编辑 Pod 的工作流程中暴露 Secret（及其数据）的风险较小。 Kubernetes 和在集群中运行的应用程序也可以对 Secret 采取额外的预防措施， 例如避免将机密数据写入非易失性存储。

Secret 类似于 [ConfigMap](https://kubernetes.io/zh/docs/tasks/configure-pod-container/configure-pod-configmap/) 但专门用于保存机密数据。

#### Secret 的使用

Pod有3种方式使用secret：

1. 作为挂在到容器上的卷中的文件
2. 作为容器环境变量
3. 由kubelet在为pod拉取镜像时使用

创建secret

```shell
# -n 标志确保生成的文件在文本末尾不包含额外的换行符。因为当 kubectl 读取文件并将内容编码为 base64 字符串时，多余的换行符也会被编码
echo -n 'root' > ./username.txt
echo -n '123456'> ./password.txt
# 可以选择使用 --from-file=[key=]source 来设置密钥名称。
kubectl create secret generic mysql-user-pass \
	--from-file=username=./username.txt \
	--from-file=password=./password.txt

# 还可以使用--from-literal=<key>=<value>直接提供secret数据，特殊字符需要手动转义，最简单的转义方法就是单引号括起来
kubectl create secret generic mysql-user-pass \
	--from-literal=username='root' \
	--from-literal=password='!MySQL123456'
```



```shell
kubectl create secret docker-registry uestcfzk-docker \
--docker-username=你的docker用户名 \
--docker-password=你的docker密码 \
--docker-email=你注册docker的邮箱地址

[root@k8s-master ~]# kubectl get  secret
NAME                  TYPE                                  DATA   AGE
default-token-8qxx6   kubernetes.io/service-account-token   3      21d
uestcfzk-docker       kubernetes.io/dockerconfigjson        1      4s
```

可以看看创建的secret的配置信息：下面展示的信息

```yaml
[root@k8s-master ~]# kubectl get secret uestcfzk-docker -oyaml
apiVersion: v1
kind: Secret
data:
  .dockerconfigjson: eyJhdXRocyI6eyJodHRwczovL2luZGV4LmRvY2tlci5pby92MS8iOnsidXNlcm5hbWUiOiJ1ZXN0Y2Z6ayIsInBhc3N3b3JkIjoiZnprMDEwMzI2IiwiZW1haWwiOiI3Njc3MTkyOTdAcXEuY29tIiwiYXV0aCI6ImRXVnpkR05tZW1zNlpucHJNREV3TXpJMiJ9fX0=
metadata:
  name: uestcfzk-docker
  namespace: default
type: kubernetes.io/dockerconfigjson
```

这里看到data中的信息不是明文的，但是并不是安全的，因为这仅仅只是一个简单的base64编码而已。

pod使用这个secret

```shell
[root@k8s-master ~]# docker pull uestcfzk/redis:v1
Error response from daemon: pull access denied for uestcfzk/redis, repository does not exist or may require 'docker login'
# 这里可以看到必须登录才能拉去这个redis镜像了，因为把这个redis设置为私有的了
cat <<EOF | sudo tee secret-redis-demo.yaml
apiVersion: v1
kind: Pod
metadata:
  name: private-redis
spec:
  containers:
  - name: private-redis
    image: uestcfzk/redis:v1  # 这个镜像已经在dockerhub上改为私有的了，必须提供账户密码才能获取
  imagePullSecrets:  # 拉去镜像用的secret
  - name: uestcfzk-docker
EOF

# 应用一下捏，可以看到能成功拉到镜像
kubectl apply -f secret-redis-demo.yaml
```





