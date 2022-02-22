## 简介

 是一款开源的、可嵌入的 Markdown 在线编辑器（组件），基于 CodeMirror、jQuery 和 Marked 构建。

支持：

- markdown在线编辑
- markdown实时预览
- markdown转HTML
- 支持实时预览、图片（跨域）上传、预格式文本/代码/表格插入、代码折叠、跳转到行、搜索替换、只读模式、自定义样式主题和多语言语法高亮等功能；
- 支持 [ToC（Table of Contents）](https://pandao.github.io/editor.md/examples/toc.html)、[Emoji表情](https://pandao.github.io/editor.md/examples/emoji.html)、[Task lists](https://pandao.github.io/editor.md/examples/task-lists.html)、[@链接](https://pandao.github.io/editor.md/examples/@links.html)等 Markdown 扩展语法；
- 支持 TeX 科学公式（基于 [KaTeX](https://pandao.github.io/editor.md/examples/katex.html)）、流程图 [Flowchart](https://pandao.github.io/editor.md/examples/flowchart.html) 和 [时序图 Sequence Diagram](https://pandao.github.io/editor.md/examples/sequence-diagram.html);
- 支持[识别和解析 HTML 标签，并且支持自定义过滤标签及属性解析](https://pandao.github.io/editor.md/examples/html-tags-decode.html)，具有可靠的安全性和几乎无限的扩展性；
- 支持 AMD / CMD 模块化加载（支持 [Require.js](https://pandao.github.io/editor.md/examples/use-requirejs.html) & [Sea.js](https://pandao.github.io/editor.md/examples/use-seajs.html)），并且支持[自定义扩展插件](https://pandao.github.io/editor.md/examples/define-plugin.html)；
- 兼容主流的浏览器（IE8+）和 [Zepto.js](https://pandao.github.io/editor.md/examples/use-zepto.html)，且支持 iPad 等平板设备；

## 安装

> GitHub地址：https://gitee.com/pandao/editor.md

只能下载使用



## markdown编辑器

在网页中嵌入一个markdown编辑器

1.引入css和js

```html
<!--editor 的css-->
<link rel="stylesheet" href="/lib/editormd/css/editormd.css"/>
<link rel="shortcut icon" href="https://pandao.github.io/editor.md/favicon.ico" type="image/x-icon"/>
<!--jQuery-->
<script src="https://ajax.aspnetcdn.com/ajax/jQuery/jquery-3.6.0.min.js"></script>
<!--editor 的js-->
<script src="/lib/editormd/editormd.min.js"></script>
```

2.准备容器

```html
<div id="md-content">
    <textarea name="content" placeholder="请输入内容" required lay-verify="required"
              style="display:none;" class="layui-textarea"></textarea>
</div>
```

3.加载插件的函数

```js
/*加载editor.md插件的函数*/
function load_editor() {
    let testEditor = editormd("md-content", {
        // markdown: "xxxx",     // 动态设置内容，如从ajax获取
        width: "100%",
        height: "640px",
        syncScrolling: "single",
        path: "/lib/editormd/lib/" // Autoload modules mode, codemirror, marked... dependents libs path
    });

    /*
    // or
    testEditor = editormd({
        id      : "test-editormd",
        width   : "90%",
        height  : 640,
        path    : "../lib/"
    });
    */
}
```

> 在使用的时候，再调用这个函数进行初始化哦



## markdown转HTML
### 1.安装editor的预览插件
```html
<!--editor的预览css-->
<link rel="stylesheet" href="/lib/editormd/css/editormd.preview.min.css"/>
<!--jQuery-->
<script src="https://ajax.aspnetcdn.com/ajax/jQuery/jquery-3.6.0.min.js"></script>
<!--editor 的预览js-->
<script src="/lib/editormd/editormd.min.js"></script>
<script src="/lib/editormd/lib/marked.min.js"></script>
<script src="/lib/editormd/lib/prettify.min.js"></script>
```

### 2.准备一个div
```html
<div id="content-markdown-view">
    <!-- Server-side output Markdown text -->
    <textarea style="display:none;">### Hello world!</textarea>
</div>
```
其中，textarea吧，目前来说建议不使用

### 3.渲染markdown
首先需要获取markdown内容，可以通过ajax获取。
```js
/*渲染markdown到html 的函数*/
function editor_markdownToHTML(markdown) {
    testEditormdView = editormd.markdownToHTML("content-markdown-view", {
        markdown        : markdown ,//+ "\r\n" + $("#append-test").text(),
        //htmlDecode      : true,       // 开启 HTML 标签解析，为了安全性，默认不开启
        htmlDecode      : "style,script,iframe",  // you can filter tags decode
        //toc             : false,
        //tocm            : true,    // Using [TOCM]
        //tocContainer    : "#custom-toc-container", // 自定义 ToC 容器层
        //gfm             : false,
        //tocDropdown     : true,
        // markdownSourceCode : true, // 是否保留 Markdown 源码，即是否删除保存源码的 Textarea 标签
        //emoji           : true,
        //taskList        : true,
        //tex             : true,  // 默认不解析
        //flowChart       : true,  // 默认不解析
        //sequenceDiagram : true,  // 默认不解析
    });
}
```
获取到该markdown内容后，即可调用此函数进行渲染。


>注意：
>如果是将markdown内容赋值到textarea标签中，虽然也能渲染，但是markdown里的HTML内容将自动被textarea解析，达不到预览效果。
>因此，最建议的做法就是直接把markdown内容传给渲染函数，这样，HTML内容不会被转义。