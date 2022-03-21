## 简介

可以用来抓取网页中的标题，生成目录

> 注意的是，网页中的标题，必须有id，毕竟页内跳转依赖的是id。

## 安装

> GitHub地址：https://github.com/tscanlin/tocbot

1.CDN方式引入：

```html
<!--tocbot 的css-->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/tocbot/4.11.1/tocbot.css">
<!--tocbot 的js-->
<script src="https://cdnjs.cloudflare.com/ajax/libs/tocbot/4.11.1/tocbot.min.js"></script>
```

2.下载使用：可以去GitHub上学习并下载

```https
https://github.com/tscanlin/tocbot/releases/
```

```html
<!--tocbot 的css-->
<link rel="stylesheet" type="text/css" href="/lib/tocbot/tocbot.css">
<!--tocbot 的js-->
<script src="/lib/tocbot/tocbot.min.js"></script>
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


<!--目录-->
<div class="layui-card"
     style="position: fixed;z-index: 1000;margin-left: -20px;right: 10px;bottom: 10px;width: auto">
    <div class="layui-card-header">
        <a href="#"><h3 style="text-align: center">目录</h3></a>
    </div>
    <div class="layui-card-body">
        <ol class="js-toc">

        </ol>
        <ol>
            <a href="#comment">评论</a>
        </ol>
    </div>
</div>
```

> 其中，类js-toc是放置目录的，js-toc-content是抓取标题的，将抓取此标签内的所有标题。
>
> 自己尝试了一下，js-toc-content只能写一个，第二个js-toc-content是无效的
>
> 然后呢这个类名是可以改的，不过最好不要和其他插件或者框架产生冲突。

### 初始化

```js
/*捕获标题的函数*/
function grabHeader(){
    tocbot.init({
        // Where to render the table of contents.
        tocSelector: '.js-toc',
        // Where to grab the headings to build the table of contents.
        contentSelector: '.js-toc-content',
        // Which headings to grab inside of the contentSelector element.
        headingSelector: 'h1, h2, h3',
        // For headings inside relative or absolute positioned containers within content.
        hasInnerContainers: true,
    });
}
```

> 注意：确保正文是可滚动的，并且文档标题具有 **id 属性**，tocbot 和您的浏览器需要这些东西来使哈希跳转到正确的标题

### 刷新

如果标题发生了改变，可以刷新一下

```js
tocbot.refresh();
```



## API

### 函数

- init
  根据传入的参数选项初始化

```js
tocbot.init(options)
```

- destory
  删除tocbot并移除事件监听器

```js
tocbot.destroy()
```

- refresh
  如果文档发生变化并且需要重建，则刷新 tocbot。

```js
tocbot.refresh()
```

### Options

```js
// Where to render the table of contents.
tocSelector: '.js-toc',
// Where to grab the headings to build the table of contents.
contentSelector: '.js-toc-content',
// Which headings to grab inside of the contentSelector element.
headingSelector: 'h1, h2, h3',
// Headings that match the ignoreSelector will be skipped.
ignoreSelector: '.js-toc-ignore',
// For headings inside relative or absolute positioned containers within content
hasInnerContainers: false,
// Main class to add to links.
linkClass: 'toc-link',
// Extra classes to add to links.
extraLinkClasses: '',
// Class to add to active links,
// the link corresponding to the top most heading on the page.
activeLinkClass: 'is-active-link',
// Main class to add to lists.
listClass: 'toc-list',
// Extra classes to add to lists.
extraListClasses: '',
// Class that gets added when a list should be collapsed.
isCollapsedClass: 'is-collapsed',
// Class that gets added when a list should be able
// to be collapsed but isn't necessarily collapsed.
collapsibleClass: 'is-collapsible',
// Class to add to list items.
listItemClass: 'toc-list-item',
// Class to add to active list items.
activeListItemClass: 'is-active-li',
// How many heading levels should not be collapsed.
// For example, number 6 will show everything since
// there are only 6 heading levels and number 0 will collapse them all.
// The sections that are hidden will open
// and close as you scroll to headings within them.
collapseDepth: 0,
// Smooth scrolling enabled.
scrollSmooth: true,
// Smooth scroll duration.
scrollSmoothDuration: 420,
// Smooth scroll offset.
scrollSmoothOffset: 0,
// Callback for scroll end.
scrollEndCallback: function (e) {},
// Headings offset between the headings and the top of the document (this is meant for minor adjustments).
headingsOffset: 1,
// Timeout between events firing to make sure it's
// not too rapid (for performance reasons).
throttleTimeout: 50,
// Element to add the positionFixedClass to.
positionFixedSelector: null,
// Fixed position class to add to make sidebar fixed after scrolling
// down past the fixedSidebarOffset.
positionFixedClass: 'is-position-fixed',
// fixedSidebarOffset can be any number but by default is set
// to auto which sets the fixedSidebarOffset to the sidebar
// element's offsetTop from the top of the document on init.
fixedSidebarOffset: 'auto',
// includeHtml can be set to true to include the HTML markup from the
// heading node instead of just including the textContent.
includeHtml: false,
// includeTitleTags automatically sets the html title tag of the link
// to match the title. This can be useful for SEO purposes or
// when truncating titles.
includeTitleTags: false,
// onclick function to apply to all links in toc. will be called with
// the event as the first parameter, and this can be used to stop,
// propagation, prevent default or perform action
onClick: function (e) {},
// orderedList can be set to false to generate unordered lists (ul)
// instead of ordered lists (ol)
orderedList: true,
// If there is a fixed article scroll container, set to calculate titles' offset
scrollContainer: null,
// prevent ToC DOM rendering if it's already rendered by an external system
skipRendering: false,
// Optional callback to change heading labels.
// For example it can be used to cut down and put ellipses on multiline headings you deem too long.
// Called each time a heading is parsed. Expects a string in return, the modified label to display.
// function (string) => string
headingLabelCallback: false,
// ignore headings that are hidden in DOM
ignoreHiddenElements: false,
// Optional callback to modify properties of parsed headings.
// The heading element is passed in node parameter and information parsed by default parser is provided in obj parameter.
// Function has to return the same or modified obj.
// The heading will be excluded from TOC if nothing is returned.
// function (object, HTMLElement) => object | void
headingObjectCallback: null,
// Set the base path, useful if you use a `base` tag in `head`.
basePath: '',
// Only takes affect when `tocSelector` is scrolling,
// keep the toc scroll position in sync with the content.
disableTocScrollSync: false
```

