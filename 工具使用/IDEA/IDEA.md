

## debug测试
![debug1](pictures/debug1.bmp)
![debug2](pictures/debug2.bmp)
![debug3](pictures/debug3.bmp)
![debug4](pictures/debug4.bmp)
![debug5](pictures/debug5.bmp)

## 设置jsp模板
File-settings
![设置jsp模块](pictures/设置jsp模块.bmp)
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
![IDEA批量操作](pictures/IDEA批量操作.png)