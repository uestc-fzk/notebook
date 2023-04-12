本文档记录HTTP的学习。

资料：https://developer.mozilla.org/zh-CN/docs/Web/HTTP

# 概述

概述仅记录一些重要的信息，其余的可见：https://developer.mozilla.org/zh-CN/docs/Web/HTTP。

HTTP是超文本传输协议，用于传输超媒体文件如HTML文件的应用层协议。设计用于web浏览器和服务器通信。

HTTP是**无状态协议**，多个连续的请求是独立的。但是可用Cookie携带状态。

HTTP请求的内容统称资源，由一个URL标识。

URL：统一资源定位符，标识资源的名称和位置。

```http
协议://主机:端口/路径?查询#片段
http://example.com:80/path/myfile.html?k1=v1&k2=v2#SomewhereInTheDocument
```

HTTP是client-server协议：客户端发送一个请求，服务端返回一个响应。

**代理Proxy**

在浏览器和服务器之间，有许多计算机和其他设备转发了 HTTP 消息。由于 Web 栈层次结构的原因，它们大多都出现在传输层、网络层和物理层上，对于 HTTP 应用层而言就是透明的，虽然它们可能会对应用层性能有重要影响。还有一部分是表现在应用层上的，被称为**代理**（Proxy）。代理既可以表现得透明，又可以不透明（“改变请求”会通过它们）。代理主要有如下几种作用：

- 缓存（可以是公开的也可以是私有的，像浏览器的缓存）
- 过滤（像反病毒扫描，家长控制...）
- 负载均衡（让多个服务器服务不同的请求）
- 认证（对不同资源进行权限管理）
- 日志记录（允许存储历史信息）

**连接**

HTTP并不需要其底层的传输层协议是面向连接的，只需要它是可靠的，或不丢失消息的（至少返回错误）。在互联网中，有两个最常用的传输层协议：TCP 是可靠的，而 UDP 不是。因此，HTTP 依赖于面向连接的 TCP 进行消息传递，但**面向连接并不是必须的**。

HTTP/1.0 默认为每一对 HTTP 请求/响应都打开一个单独的 TCP 连接，比较低效。

HTTP/1.1 引入了流水线（被证明难以实现）和持久连接的概念。

HTTP/2 则发展得更远，通过在一个连接复用消息的方式来让这个连接始终保持为暖连接。

**发展**

HTTP/1.1：1997年发布

- 连接复用
- 流水线（被证明难以实现）
- 响应分块
- 缓存控制
- 内容协商

HTTP/2：2015年发布

- 基于二进制协议而不是以前的文本协议，不可读
- 多路复用：并行的请求能在同一个链接中处理，移除了 HTTP/1.x 中顺序和阻塞的约束
- 压缩标头
- HTTP/2 帧机制是在 HTTP/1.x 语法和底层传输协议之间增加了一个新的中间层，而没有从根本上修改它，即它是建立在经过验证的机制之上。

HTTP/3：基于QUIC

QUIC 旨在为 HTTP 连接设计更低的延迟。类似于 HTTP/2，它是一个多路复用协议，但是 HTTP/2 通过单个 TCP 连接运行，所以在 TCP 层处理的数据包丢失检测和重传可以阻止所有流。QUIC 通过 [UDP](https://developer.mozilla.org/zh-CN/docs/Glossary/UDP) 运行多个流，并为每个流独立实现数据包丢失检测和重传，因此如果发生错误，只有该数据包中包含数据的流才会被阻止。

**HTTP流水线**

默认情况下HTTP请求发出直到收到响应才会发下一个请求，往返时延为RTT。

流水线是在同一条长连接上发出连续请求，而不是等待应答，避免延迟。正确实现流水线是复杂的，重要的消息可能被延迟到不重要的消息后面。这个重要性的概念甚至会演变为影响到页面布局！因此 HTTP 流水线在大多数情况下带来的改善并不明显。

流水线受制于[队头阻塞（HOL）](https://zh.wikipedia.org/wiki/队头阻塞)问题。

由于这些原因，流水线已被 HTTP/2 中更好的算法——*多路复用*（multiplexing）所取代。

所有遵循HTTP/1.1的代理和服务器都应该支持流水线，但是基本上浏览器都默认关闭了这个特性。

**协议升级机制**

HTTP/1.1提供了了一种使用 [Upgrade](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade) 标头字段的特殊机制，这一机制允许将一个已建立的连接升级成新的、不相容的协议。这种机制主要用于引导WebSocket连接。

> 注意：HTTP/2 明确禁止使用此机制；这个机制只属于 HTTP/1.1。

升级请求：

```http
GET /index.html HTTP/1.1
Host: www.example.com
Connection: upgrade
Upgrade: websocket
```

如果服务器接受升级，返回101状态码，和一个要切换到的协议的标头字段 Upgrade。

如果服务器没有或不能升级，会忽略`Upgrade`，当做常规HTTP请求处理。

一旦这次升级完成了，连接就变成了双向管道。并且可以通过新协议完成启动升级的请求。

# 安全

## CSP

**内容安全策略**（[CSP](https://developer.mozilla.org/zh-CN/docs/Glossary/CSP)）是一个额外的安全层，用于检测并削弱某些特定类型的攻击，包括跨站脚本（[XSS](https://developer.mozilla.org/zh-CN/docs/Glossary/Cross-site_scripting)）和数据注入攻击等。无论是数据盗取、网站内容污染还是恶意软件分发，这些攻击都是主要的手段。

开启CSP需响应头增加`Content-Security-Policy`或响应的HTML文件配置`<meta>`元素来配置该策略：

```html
<meta
  http-equiv="Content-Security-Policy"
  content="default-src 'self'; img-src https://*; child-src 'none';" />
```

CSP的目标是减少XSS攻击，避免恶意脚本在浏览器执行。CSP 通过指定有效域——即浏览器认可的可执行脚本的有效来源——使服务器管理者有能力减少或消除 XSS 攻击所依赖的载体。

常见安全策略：

1. 所有内容来自同一个源，不包括子域名

```http
Content-Security-Policy: default-src 'self'
```

2. 允许内容来自信任的域名及其子域名

```http
Content-Security-Policy: default-src 'self' *.example.com
```

3. 允许来自任何源的图片，限制音频和视频多媒体文件来源，限制脚本来源

```http
Content-Security-Policy: default-src 'self'; img-src *; media-src media1.com media2.com; script-src userscripts.example.com
```

- 图片可以从任何地方加载 (注意`*`通配符)
- 多媒体文件仅允许从 media1.com 和 media2.com 加载（不允许从这些站点的子域名）
- 可运行脚本仅允许来自于 userscripts.example.com

4. 银行网站想要所有内容必须以SSL方式获取

```http
Content-Security-Policy: default-src https://onlinebanking.jumbobank.com
```

5. 邮箱网站允许邮件包含HTML，图片可从任意地方加载，禁止脚本

```http
Content-Security-Policy: default-src 'self' *.mailsite.com; img-src *
```

虽然此例子未指定`script-src`，但是`default-src`已经限制脚本只能从原始服务器来源获取。

## X-XSS_Protection

HTTP **`X-XSS-Protection`** 响应头是 Internet Explorer，Chrome 和 Safari 的一个特性，当检测到跨站脚本攻击 ([XSS (en-US)](https://developer.mozilla.org/en-US/docs/Glossary/Cross-site_scripting)) 时，浏览器将停止加载页面。若网站设置了良好的 [`Content-Security-Policy`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Content-Security-Policy) 来禁用内联 JavaScript (`'unsafe-inline'`)，现代浏览器不太需要这些保护，但其仍然可以为尚不支持 [CSP](https://developer.mozilla.org/zh-CN/docs/Glossary/CSP) 的旧版浏览器的用户提供保护。

```htpp
X-XSS-Protection: 0
X-XSS-Protection: 1
X-XSS-Protection: 1; mode=block
X-XSS-Protection: 1; report=<reporting-uri>
```

- 0：禁止xss过滤
- 1：启用xss过滤，默认。如果检测到跨站脚本攻击，浏览器将清除页面（删除不安全的部分）。

- `1; mode=block`：启用 XSS 过滤。如果检测到攻击，浏览器将不会清除页面，而是阻止页面加载。
- `1; report=<reporting-URI> (Chromium only)`：启用 XSS 过滤。如果检测到跨站脚本攻击，浏览器将清除页面并使用 CSP [`report-uri` (en-US)](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/report-uri)指令的功能发送违规报告。

# cookies

**cookies是服务器发给浏览器保存的一小块数据**。浏览器存储 cookie 之后发请求都会将cookie放在请求头中。通常用于保存用户登录状态。

## HTTP会话交互

Cookie 曾一度用于客户端数据的存储，因当时并没有其他合适的存储办法而作为唯一的存储手段。但现在推荐使用现代存储 API，如[Web storage API](https://developer.mozilla.org/zh-CN/docs/Web/API/Web_Storage_API)（`localStorage` 和 `sessionStorage`）或[IndexedDB](https://developer.mozilla.org/zh-CN/docs/Web/API/IndexedDB_API) 。因为cookie每次都会自动发给服务器，而localStorage等则可以自行选择部分传输，在移动数据情况下比较省流量。

HTTP连接是无状态的，为了让HTTP会话有状态，必须让浏览器发的HTTP请求带有一些会话状态信息，**必须在浏览器存一些状态信息**如自动发的cookie或localstorage等。

当然服务器也可以存一些会话状态信息，称之为session，因为服务器无法单纯从HTTP连接直接找到其session信息，因此必须让请求带有1个唯一id来找到这个存储在服务器的会话信息。如Tomcat服务器会在cookie中设置sessionId来唯一对应其存在服务器的会话状态信息。

> 注意：**浏览器必须存储会话状态信息**，而服务器会话状态存储是不必要的。

Cookie和session的区别？`多个无状态HTTP请求-->有会话状态的交互`

- **Session可以指浏览器与服务器之间的HTTP交互会话，此时Cookie是一种维护会话的手段**，即由浏览器发请求时带上状态信息使得服务器能够识别浏览器，以此维持会话。
- Session也可以指服务器存储在内存或磁盘的会话状态信息，此时cookie指的是浏览器存储在本地的状态信息。
- 总之cookie让多个无状态的HTTP请求组合成浏览器与服务器的有会话状态的交互。

注意：

- 浏览器存储的数据可被用户直接看到，**可将重要的会话状态信息存在服务器内存或磁盘**，浏览器存储其唯一id如用户id。
- 用户甚至可以直接修改浏览器存储的数据，如果cookie中存储用户id，那用户手动改了岂不是可以访问他人信息，**可用JWT解决修改问题**。

## 生命周期

1、设置cookie

服务器指示浏览器存储cookie需在响应头如下指定：

```http
HTTP/1.0 200 OK
Content-type: application/json
Set-Cookie: name1=val1
Set-Cookie: name2=val2
```

浏览器将存储两对cookie，之后发请求都会带上它们：

```http
GET /sample_page.html HTTP/1.1
Host: www.example.org
Cookie: name1=val1; name2=val2
```

2、设置cookie过期时间，不设置则为会话期cookie，会话结束浏览器自动清除。

```http
Set-Cookie: name1=val1; Expires=Wed, 21 Oct 2015 07:28:00 GMT;
```

3、设置cookie ttl：`Max-Age=<number>`，单位秒

秒数为 0 或 -1 将会使 cookie 直接过期。假如 `Expires` 和 `Max-Age` 属性均存在，那么 `Max-Age` 的优先级更高。

## 属性

```http
Set-Cookie: id=a3fWa; Expires=Wed, 21 Oct 2015 07:28:00 GMT; Secure; HttpOnly
```

1、`Secure`属性：表示只能cookie只能在HTTPS协议下发给服务器，可防止被中间人访问。

2、`HttpOnly`属性：可防止js脚本访问cookie，js的`Document.cookie`API就无法访问和修改该cookie了。可缓解XSS攻击。

3、`Domain`属性：指定哪些主机可接受cookie，默认为同一host下设置cookie。浏览器发请求时将只会发送对应域名下的cookie。

如果指定了domain，会自动包含子域名，因此指定domain相比于省略限制更少。

如果设置 `Domain=mozilla.org`，则 Cookie 也包含在子域名中（如 `developer.mozilla.org`），当子域需要共享有关用户的信息时，**如单点登录可能有用**。

4、`Path`属性：指定URL路径，只有满足该前缀路径的请求才会自动携带cookie。如`Path=/docs`，则以下地址都会匹配：

- `/docs`
- `/docs/`
- `/docs/Web/`
- `/docs/Web/HTTP`

但是这些请求路径不会匹配以下地址：

- `/`
- `/docsets`
- `/fr/docs`

5、`SameSite`，没看懂

## 跟踪与隐私

`Domain`属性可设置域名，

第一方cookie：cookie和该页面来自同一主机。

第三方cookie：cookie由其它站点主机设置。如网站请求了第三方服务器，如图片或广告等，第三方服务器可能会设置第三方cookie用于广告和跟踪。

Firefox 默认情况下会阻止已知包含跟踪器的第三方 cookie。第三方 cookie（或仅跟踪 cookie）也可能被其他浏览器设置或扩展程序阻止。阻止 Cookie 会导致某些第三方组件（例如社交媒体窗口小部件）无法正常运行。

服务器设置第3方cookie时应该设置`SameSite`属性以指定是否可以将 cookie 发送到第三方站点。

注意法律规定允许用户在不接收 Cookie 的情况下使用大部分服务。

## 浏览器存储方式

Cookie本身是浏览器存储数据的一种方式，可由服务器设置，浏览器自动存储并每次请求自动发送。

其它存储方式如localstorage或sessionstorage存储限制比 cookie 大，并且永远不会发送到服务器。

可以使用 [IndexedDB API](https://developer.mozilla.org/zh-CN/docs/Web/API/IndexedDB_API) 或基于它构建的库来存储更多结构化的数据。

# 跨域资源共享(CORS)

CORS是一种基于HTTP头的机制，允许服务器标示除了它自己以外的其他[源](https://developer.mozilla.org/zh-CN/docs/Glossary/Origin)（域、协议或端口），使得浏览器允许这些源访问加载自己的资源。跨源资源共享还通过一种机制来检查服务器是否会允许要发送的真实请求，该机制通过浏览器发起一个到服务器托管的跨源资源的“预检”请求。在预检中，浏览器发送的头中标示有 HTTP 方法和真实请求中会用到的头。
