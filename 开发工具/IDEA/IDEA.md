

## 破解

2023版本破解：https://www.bilibili.com/read/cv26088967

安装及破解教程：https://www.exception.site/essay/how-to-free-use-intellij-idea-2020 未验证过，不知是否有效
还有一个更牛逼的激活网站：https://idea.medeming.com 缺点是要关注公众号，不过它的激活码确实是可以用的，非常方便。

更推荐：还有一种是**学生身份申请IDEA的免费教育许可证**，每年可以申请一次，每次有效期一年，这个虽然麻烦，但是学生账户可以登录使用所有Jetbrains系列产品如GoLand，Pycharm。申请地址：https://www.jetbrains.com.cn/community/education/#students

## debug测试

![debug1](IDEA.assets/debug1.bmp)
![debug2](IDEA.assets/debug2.bmp)
![debug3](IDEA.assets/debug3.bmp)
![debug4](IDEA.assets/debug4.bmp)
![debug5](IDEA.assets/debug5.bmp)

## 设置jsp模板
File-settings
![设置jsp模块](IDEA.assets/设置jsp模块.bmp)
```java
<%
    String basePath = request.getScheme() + "://" 
        + request.getServerName() + ":" + 
        request.getServerPort() + 
        request.getContextPath() + "/";
%>
<base href="<%=basePath%>">
```

## 批量操作

Alt 按住左键上滑或者下滑；
![IDEA批量操作](IDEA.assets/IDEA批量操作.png)

## 换行符问题

windows和Unix和macos换行符不一样，有时项目中的换行符是Windows的CRLF，但是被改成了LF这种情况，如果直接提交，会有很多文件提交到git上，分析大半天。

先点中当前项目，然后如下修改即可：

![image-20220408102741539](IDEA.assets/image-20220408102741539.png)

设置里面也有：

![image-20220408102834393](IDEA.assets/image-20220408102834393.png)