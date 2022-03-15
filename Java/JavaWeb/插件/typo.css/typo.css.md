## 简介
中文网页重设与排版：

目标：一致化浏览器排版效果，构建最适合中文阅读的网页排版。包括桌面和移动平台。

预览：[typo.css](http://typo.sofi.sh/)

## 安装

> GitHub地址：https://github.com/sofish/typo.css
>
> 这个只能下载下来使用

下载包的目录结构

```text
.
├── README.md           --- 使用帮助
├── license.txt         --- 许可证
├── typo.css            --- 将应用于你的项目
└── typo.html           --- Demo/预览
```

只需要引入css文件即可。

```html
    <!--typo 的css-->
   <link rel="stylesheet" href="/lib/typo/typo.css"/>
```



## 快速开始

### 准备HTML

```html
<!--博客内容-->
<div class="layui-card-body js-toc-content">
    <div id="wrapper" class="typo typo-selection">

        <div id="content-markdown-view" style="width: 95%">
            <!-- Server-side output Markdown text -->
            <textarea style="display:none;">### Hello world!</textarea>
        </div>

    </div>
</div>
```

给需要加样式的内容的外层容器div加上`class="typo typo-selection"`就可以啦。

### 引入css文件

由于只有css文件，所有其引入是时机就显得很重要了，毕竟一旦引入，就开始渲染了。

```js
/*引入typo.css的函数*/
function getTypo(){
    $("head").append('<link rel="stylesheet" href="/lib/typo/typo.css"/>');
}
```

> 动态引入，可以控制其在我们的博客内容加载完成之后再引入css样式。

> 更多细节看GitHub，虽然也没什么细节了...