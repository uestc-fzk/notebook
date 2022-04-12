# 资料

> JWT官网：https://jwt.io/introduction
>
> JWT编码查看：https://jwt.io/#debugger-io
>
> Jwt使用教程：https://github.com/auth0/java-jwt

# jwt

## 简介

JWT---Json Web Token	JSON 网络令牌


### 应用场景

以下是 JSON Web Tokens 有用的一些场景：

- **授权**：这是使用 JWT 最常见的场景。用户登录后，每个后续请求都将包含 JWT，允许用户访问该令牌允许的路由、服务和资源。**单点登录**是当今广泛使用 JWT 的一项功能，因为它的**开销很小**，并且能够轻松跨不同域使用。

- **信息交换**：JSON Web Tokens 是一种在各方之间安全传输信息的好方法。因为 JWT 可以被签名——例如，使用公钥/私钥对——你可以确定发件人就是他们所说的那样。此外，由于使用标头和有效负载计算签名，因此您还可以验证内容是否未被篡改。

### 传统Cookie+Session与JWT对比

① 在传统的用户登录认证中，因为http是无状态的，所以都是采用session方式。用户登录成功，服务端会保证一个session，当然会给客户端一个sessionId，客户端会把sessionId保存在cookie中，每次请求都会携带这个sessionId。

cookie+session这种模式通常是保存在内存中，而且服务从单服务到多服务会面临的session共享问题，随着用户量的增多，开销就会越大。而JWT不是这样的，只需要服务端生成token，客户端保存这个token，每次请求携带这个token，服务端认证解析就可。

② JWT方式校验方式更加简单便捷化，无需通过redis缓存，而是直接根据token取出保存的用户信息，以及对token可用性校验，单点登录，验证token更为简单。


### 组成结构

在其紧凑形式中，JSON Web Tokens 由用点 ( `.`)分隔的三个部分组成，它们是：

- 标题
- 有效载荷
- 签名

因此，JWT 通常如下所示。

> `xxxxx.yyyyy.zzzzz`

#### 头部

HEADER

标头*通常*由两部分组成：令牌的类型，即 JWT，以及正在使用的签名算法，例如 HMAC SHA256 或 RSA。

例如：

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

然后，这个 JSON 被**Base64Url**编码以形成 JWT 的第一部分。
注意：因为默认令牌类型就是jwt，所以第二个参数其实是可以不用的。

#### 有效载荷

PAYLOAD

令牌的第二部分是负载，其中包含声明。声明是关于实体（通常是用户）和附加数据的声明。共有三种类型的声明：*注册声明*、*公共*声明和*私人*声明。

1. 注册声明
   预定义声明，非强制性，推荐性
   其中一些是： **iss**（发行者）、 **exp**（到期时间）、 **sub**（主题）、 **aud**（受众）[等](https://tools.ietf.org/html/rfc7519#section-4.1)。

   > 请注意，声明名称只有三个字符，因为 JWT 是紧凑的。

2. 公共声明
   可随意定义
   但是为了避免冲突，它们应该在[IANA JSON Web Token Registry](https://www.iana.org/assignments/jwt/jwt.xhtml)中定义或定义为包含抗冲突命名空间的 URI。

3. 私人声明
   这些是为在同意使用它们的各方之间**共享信息**而创建的**自定义声明**，既不是注册声明，也不是公共声明。

例如：

```json
{
  "sub": "15088880000",
  "name": "John Doe",
  "admin": true
}
```

然后对有效负载进行**Base64Url**编码以形成 JSON Web 令牌的第二部分。

> 请注意，对于已签名的令牌，此信息虽然受到防篡改保护，但**任何人都可以读取**。除非加密，**否则不要将 *机密信息* 放在 JWT 的负载或标头元素中。**



#### 验证签名

VERIFY SIGNATURE：用于防止篡改payload中的数据

要创建签名部分，必须获取编码的标头、编码的有效载荷、秘密、标头中指定的算法，并对其进行签名。

例如，如果要使用 HMAC SHA256 算法，则签名将通过以下方式创建：

```json
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret)
```

签名用于验证消息在此过程中没有更改，并且在使用私钥签名的令牌的情况下，它还可以验证 JWT 的发件人是它所说的人。

#### 输出格式

输出是三个由`.`分隔的 Base64-URL 字符串，可以在 HTML 和 HTTP 环境中轻松传递，同时与基于 XML 的标准（如 SAML）相比更加紧凑。

![jwt输出格式](https://uestc-fzk.gitee.io/notebook/权限认证/JWT/pictures/jwt输出格式.png)

### base64

Base64是网络上最常见的用于传输8Bit[字节码](https://baike.baidu.com/item/字节码/9953683)的**编码**方式之一，Base64就是一种基于64个可打印字符来表示[二进制](https://baike.baidu.com/item/二进制/361457)数据的方法。

Base64编码是从二进制到字符的过程，可用于在[HTTP](https://baike.baidu.com/item/HTTP)环境下传递较长的标识信息。采用Base64编码具有不可读性，需要解码后才能阅读。

> 注意：base64是**编码方式**，不是加密算法。

### jwt验证流程和特点

**验证流程：**

① 在头部信息中声明加密算法和常量， 然后把header使用json转化为字符串
② 在载荷中声明用户信息，同时还有一些其他的内容；再次使用json 把载荷部分进行转化，转化为字符串
③ 使用在header中声明的加密算法和每个项目随机生成的**secret**来进行加密， 把第一步分字符串和第二部分的字符串进行加密， 生成新的字符串。词字符串是独一无二的。
④ 解密的时候，只要客户端带着JWT来发起请求，服务端就直接使用**secret**进行解密。

**特点：**

① 三部分组成，每一部分都进行字符串的转化
② 解密的时候没有使用数据库，仅仅使用的是secret进行解密
③ **JWT的secret千万不能泄密！**

### jwt优缺点

**优点**：
>1. 不用在服务器存放用户数据，减轻服务器压力
>应用程序分布式部署的情况下，Session需要做多机数据共享，通常可以存在数据库或者Redis里面。而JWT不需要。
>2. 轻量级，json风格，简单
>3. 无状态：JWT不在服务端存储任何状态。

**缺点**
>1. 一次性：无状态是JWT的特点，但也导致了这个问题，JWT是一次性的。想修改里面的内容，就必须签发一个新的JWT。即缺陷是一旦下发，**服务后台无法拒绝携带该jwt的请求（如踢除用户）**
>	>- 续签问题
>      如果你使用jwt做会话管理，传统的Cookie续签方案一般都是框架自带的，Session有效期30分钟，30分钟内如果有访问，有效期被刷新至30分钟。
>	>  一样的道理，要改变JWT的有效时间，就要签发新的JWT。
>  最简单的一种方式是每次请求刷新JWT，即每个HTTP请求都返回一个新的JWT。这个方法不仅暴力不优雅，而且每次请求都要做JWT的加密解密，会带来性能问题。另一种方法是在Redis中单独为每个JWT设置过期时间，每次访问时刷新JWT的过期时间。
>	  - 无法销毁
>	    通过JWT的验证机制可以看出来，一旦签发一个JWT，在到期之前就会始终有效，无法中途废弃。
>	    例如你在payload中存储了一些信息，当信息需要更新时，则重新签发一个JWT，但是由于旧的jwt还没过期，拿着这个旧的JWT依旧可以登录，那登录后服务端从JWT中拿到的信息就是过时的。
>	    为了解决这个问题，我们就需要在服务端部署额外的逻辑，例如设置一个黑名单，一旦签发了新的JWT，那么旧的就加入黑名单（比如存到redis里面），避免被再次使用。
>	  - 可以看出想要破解JWT一次性的特性，就需要在服务端存储jwt的状态。但是引入 redis 之后，就把无状态的jwt硬生生变成了有状态了，**违背了JWT的初衷**。而且这个方案和Session都差不多了。
>	
>2. 安全性：payload(载荷)是base64编码的，没有加密，不安全。session存在服务端，更安全。
>
>3. 性能：JWT太长。
>     由于是无状态使用JWT，所有的数据都被放到JWT里，如果还要进行一些数据交换，那载荷会更大，经过编码之后导致JWT非常长，Cookie的限制大小一般是4k，cookie很可能放不下，所以**JWT一般放在LocalStorage里面**。
>     并且用户在系统中的每一次Http请求都会把JWT携带在Header里面，Http请求的Header可能比Body还要大。
>     而**SessionId只是很短的一个字符串**，因此使用JWT的Http请求比使用Session的开销大得多。

**JWT和token的区别**

token依赖于Redis查询数据信息，token存放value数据比较安全。
jwt不依赖与Redis，数据存放在客户端(浏览器)。



## 模拟jwt加密与验证

```java
    /**
     * 模拟jwt案例
     * <p>
     * 头部
     * payload载荷
     * 验证签名
     */
    @Test
    public void jwtEncode() throws JSONException {
        // header
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        // payload
        JSONObject payLoad = new JSONObject();
        payLoad.put("username", "fzk");
        payLoad.put("userId", "1");

        // base64对数据进行编码
        String jwtHeader = Base64.encodeBase64String(header.toString().getBytes());
        String jwtPayLoad = Base64.encodeBase64String(payLoad.toString().getBytes());

        // 验证签名 后面加盐值 一般盐值存放在服务器端
        // 防止暴力破解、撞库
        String signKey = "myKey1";
        String sign = DigestUtils.md5DigestAsHex((jwtPayLoad.toString() + signKey).getBytes());

        // 合成jwt
        String jwt = jwtHeader + "." + jwtPayLoad + "." + sign;
        System.out.println(jwt);
    }

    /**
     * 验证jwt签名
     * 验证数据没有被修改
     */
    @Test
    public void checkJWT() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxIiwidXNlcm5hbWUiOiJmemsifQ==.7c7fde9d2fca720aa75aeac8d9cdbd5a";
        String[] splits = jwt.split("\\.");

        // 取出头部和负载
        String jwtHeader = splits[0];
        String jwtPayLoad = splits[1];

        // 验证签名
        String signKey = "myKey1";
        String sign = DigestUtils.md5DigestAsHex((jwtPayLoad.toString() + signKey).getBytes());
        if (StringUtils.isEmpty(sign))
            return;

        System.out.println(sign.equals(splits[2]));
    }
```

## 快速开始

### 1. Maven依赖

```xml
<!-- JWT依赖 -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>3.18.1</version>
</dependency>

<!--自动配置类提示-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

<!--不知道为什么报错：加入这个就不报错了-->
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>2.3.1</version>
</dependency>
```

### 2.yaml配置

```yaml
config:
  jwt:
    # 加密密钥
    secret: "myKey"
    # token有效时长：1天
    expire: 3600
    # header 名称
    header: "token"
```

### 3.JWTConfig

```java
/**
 * JWT的token，区分大小写
 */
@ConfigurationProperties(prefix = "config.jwt")
@Component
public class JwtConfig {
 
    private String secret;
    private long expire;
    private String header;
 
    /**
     * 生成token
     * @param subject
     * @return
     */
    public String createToken (String subject){
        Date nowDate = new Date();
        Date expireDate = new Date(nowDate.getTime() + expire * 1000);//过期时间
 
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(subject)
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    /**
     * 获取token中注册信息
     * @param token
     * @return
     */
    public Claims getTokenClaim (String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        }catch (Exception e){
//            e.printStackTrace();
            return null;
        }
    }
    /**
     * 验证token是否过期失效
     * @param expirationTime
     * @return
     */
    public boolean isTokenExpired (Date expirationTime) {
        return expirationTime.before(new Date());
    }
 
    /**
     * 获取token失效时间
     * @param token
     * @return
     */
    public Date getExpirationDateFromToken(String token) {
        return getTokenClaim(token).getExpiration();
    }
    /**
     * 获取用户名从token中
     */
    public String getUsernameFromToken(String token) {
        return getTokenClaim(token).getSubject();
    }
 
    /**
     * 获取jwt发布时间
     */
    public Date getIssuedAtDateFromToken(String token) {
        return getTokenClaim(token).getIssuedAt();
    }
 
    // --------------------- getter & setter ---------------------
 
    public String getSecret() {
        return secret;
    }
    public void setSecret(String secret) {
        this.secret = secret;
    }
    public long getExpire() {
        return expire;
    }
    public void setExpire(long expire) {
        this.expire = expire;
    }
    public String getHeader() {
        return header;
    }
    public void setHeader(String header) {
        this.header = header;
    }
}
```

### 4.配置拦截器

```java
@Component
public class TokenInterceptor extends HandlerInterceptorAdapter {
 
    @Resource
    private JwtConfig jwtConfig ;
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws SignatureException {
        /** 地址过滤 */
        String uri = request.getRequestURI() ;
        if (uri.contains("/login")){
            return true ;
        }
        /** Token 验证 */
        String token = request.getHeader(jwtConfig.getHeader());
        if(StringUtils.isEmpty(token)){
            token = request.getParameter(jwtConfig.getHeader());
        }
        if(StringUtils.isEmpty(token)){
            throw new SignatureException(jwtConfig.getHeader()+ "不能为空");
        }
 
        Claims claims = null;
        try{
            claims = jwtConfig.getTokenClaim(token);
            if(claims == null || jwtConfig.isTokenExpired(claims.getExpiration())){
                throw new SignatureException(jwtConfig.getHeader() + "失效，请重新登录。");
            }
        }catch (Exception e){
            throw new SignatureException(jwtConfig.getHeader() + "失效，请重新登录。");
        }
 
        /** 设置 identityId 用户身份ID */
        request.setAttribute("identityId", claims.getSubject());
        return true;
    }
}
```

注册拦截器到SpringMVC

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Resource
    private TokenInterceptor tokenInterceptor ;
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/**");
    }
}
```



## 用法

### 选择算法

该算法定义了令牌的签名和验证方式。在 HMAC 算法、密钥对或`KeyProvider`RSA 和 ECDSA 算法的情况下，它可以使用秘密的原始值进行实例化。创建后，该实例可重用于令牌签名和验证操作。

当使用 RSA 或 ECDSA 算法并且您只需要**签署**JWT 时，您可以通过传递`null`值来避免指定公钥。当您只需要**验证**JWT时，可以使用私钥完成相同的操作。
**Using static secrets or keys:**

```java
//HMAC
Algorithm algorithmHS = Algorithm.HMAC256("secret");

//RSA
RSAPublicKey publicKey = //Get the key instance
RSAPrivateKey privateKey = //Get the key instance
Algorithm algorithmRS = Algorithm.RSA256(publicKey, privateKey);
```

>**HMAC 密钥长度和安全性**
>
>使用基于哈希的消息验证码时，例如 HS256 或 HS512，为了符合 JSON Web 算法 (JWA) 规范 (RFC7518) 的严格要求，您**必须**使用具有相同（或更大）的密钥位长度作为输出哈希的大小。这是为了避免削弱身份验证代码的安全强度（请参阅 NIST 建议 NIST SP 800-117）。例如，使用 HMAC256 时，密钥长度必须至少为 256 位。
>
>至于KeyProvider的算法RSA和ECDSA，暂时不去了解。



### 创建并签署令牌

首先调用`JWT.create()`创建一个`JWTCreator`实例。

使用此构建器定义令牌具有的自定义声明。

最后获取 String 令牌，调用`sign()`并传递`Algorithm`实例。

1. **Example using `HS256`**

   ```java
   @Test
   void testCreateToken() {
       try {
           Date nowDate = new Date();
           // 1.选择签名算法
           Algorithm algorithm = Algorithm.HMAC256("myKey");
           // 2.创建JWT并签署令牌
           //Map<String, Object> map = new HashMap<>();
           //map.put("typ", "JWT");
           //map.put("alg", "HS256");
           String token = JWT.create()
               //2.1添加头部.
               //.withHeader(map) 
   
               // 2.2添加payload
               // 2.2.1.建议添加的声明
               .withIssuer("auth0")    // iss：发行者
               .withAudience("userId")//"aud" claim. 受众 可以用来放userId
               .withClaim("username", "fzk")   //自己的声明.可以放其他的数据
               .withIssuedAt(nowDate)//"iat" claim . 发行时间
               .withExpiresAt(new Date(nowDate.getTime() + 3600 * 1000)) // "exp" claim. 到期时间
               .withNotBefore(nowDate)    // 设置令牌在什么时间之前是不能使用的
               // 2.2.2.可以不要的声明(建议不加)
               .withSubject("xxx")     //"sub" claim.主题
               .withJWTId("jwt01") //"jti" claim. JWT的id
   
               // 2.3签署令牌并返回JWT的Json String
               .sign(algorithm);
           System.out.println(token);
       } catch (JWTCreationException exception) {
           //如果无法将 Claim 转换为 JSON 或签名过程中使用的密钥无效，
           // JWTCreationException则将引发。
           System.out.println("令牌生成失败");
       }
   }
   ```

    > 对于头部header信息：
    >
    > 经过测试发现：
    >
    > - 头部的alg标签的值只由选择的加密算法确定，typ的值可以自定义。
    >
    > - 而如果不加头部，会自动加上：typ=JWT，alg=具体签名算法
    >
    > 因此，只需要加上payload的声明即可，再选择签名的加密算法。
    >
    > 尤其是时间那3个声明，建议都加上，这样在验证签名的时候，时间校验也会自动进行。

2. **Example using `RS256`**

    ```java
    RSAPublicKey publicKey = //Get the key instance
    RSAPrivateKey privateKey = //Get the key instance
    try {
        Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
        String token = JWT.create()
            .withIssuer("auth0")
            .sign(algorithm);
    } catch (JWTCreationException exception){
        //Invalid Signing configuration / Couldn't convert Claims.
    }
    ```
    
    > 如果有需要，再仔细研究公钥与私钥算法的使用。

### 验证

#### 令牌验证

首先调用`JWT.require()`并传递算法实例来创建`JWTVerifier`实例。

如果需要令牌具有特定的声明值，请使用生成器来定义它们。

方法build()返回的实例是可重用的，因此可以定义一次并使用它验证不同的令牌。

最后调用verifier.verify()传递令牌。

如果令牌具有无效签名或不满足声明要求，`JWTVerificationException`则将引发。

```java
@Test
void testVerify(){
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJ1c2VySWQiLCJzdWIiOiJ4eHgiLCJuYmYiOjE2MjYxNDUzMDksImlzcyI6ImF1dGgwIiwiZXhwIjoxNjI2MTQ4OTA5LCJpYXQiOjE2MjYxNDUzMDksImp0aSI6Imp3dDAxIiwidXNlcm5hbWUiOiJmemsifQ.JrpJCfAxMNjSMSWWHQCPz8RsxM0-PdFR1hWdA7lvnGs";
    try {
        Algorithm algorithm = Algorithm.HMAC256("myKey2");
        JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer("auth0")
            .build(); //Reusable verifier instance
        DecodedJWT jwt = verifier.verify(token);
        System.out.println("有效令牌");
    } catch (JWTVerificationException exception){
        //Invalid signature/claims
        System.out.println("无效令牌：可能是时间验证不通过，可能是别人修改了，可能是秘钥不对");
    }
}
```

> 在验证的时候呢，秘钥要一致，加密算法要一致
>
> 令牌无法解析或者签名无效或者过期了，都将抛异常

#### 时间验证

JWT 令牌可能包含可用于验证的 DateNumber 字段：

- 令牌是在过去的日期发行的 `"iat" < TODAY`
- 令牌尚未过期`"exp" > TODAY`
- 令牌已经可以使用。 `"nbf" < TODAY`

在验证令牌时，**时间验证会自动发生**，`JWTVerificationException`当值无效时会导致抛出。
**如果缺少任何先前的字段，则不会在此验证中考虑它们。**

所以建议在token中加上3个时间声明。

### 解码令牌

```java
@Test
void testDecode() {
    String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJ1c2VySWQiLCJzdWIiOiJ4eHgiLCJuYmYiOjE2MjYxNDUzMDksImlzcyI6ImF1dGgwIiwiZXhwIjoxNjI2MTQ4OTA5LCJpYXQiOjE2MjYxNDUzMDksImp0aSI6Imp3dDAxIiwidXNlcm5hbWUiOiJmemsifQ.JrpJCfAxMNjSMSWWHQCPz8RsxM0-PdFR1hWdA7lvnGs";
    try {
        // 1. 解码
        DecodedJWT jwt = JWT.decode(token);

        // 2. header claims
        String algorithm = jwt.getAlgorithm();//HS256
        String type = jwt.getType();// JWT
        String alg = jwt.getHeaderClaim("alg").asString();//HS256

        // 3. payload claims
        // 3.1获取公共声明
        String issuer = jwt.getIssuer(); //"iss" auth0
        List<String> audienceList = jwt.getAudience();// [userId] 说明受众可以加多个
        Date expiresAt = jwt.getExpiresAt();//"exp" 过期时间 Tue Jul 13 12:01:49 CST 2021
        Date notBefore = jwt.getNotBefore();//"nbf" 在Tue Jul 13 11:01:49 CST 2021之前不能使用
        Date issuedAt = jwt.getIssuedAt();//"iat" 签发时间 Tue Jul 13 11:01:49 CST 2021
        // 3.2获取私人声明
        String username = jwt.getClaim("username").asString();//fzk

    } catch (JWTDecodeException exception) {
        // 如果令牌的语法无效或标头或有效负载不是 JSON，JWTDecodeException将引发。
    }
}
```



# Sa-token整合JWT

这个可以结合Sa-token的码云项目里的jwt-demo进行整合。不过也许是JWT选择的不一样，我这个JWT整合与其存在一定区别，不过过程是一样的。

将其放入spring的容器中即可。

## 配置在类中

```java
@Component
public class SaTokenJwtUtil {

    /**
     * 秘钥 (随便手打几个字母就好了)
     */
    public static String secret = "myKey";

    /**
     * token有效期 (单位: 秒)
     */
    public static long TIMEOUT = 60 * 60 * 24;// 1天


    public static String LOGIN_ID_KEY = "loginId";


    /**
     * 根据userId生成token
     *
     * @param loginId 账号id
     * @return jwt-token
     */
    public static String createToken(Object loginId) {
        Date nowDate = new Date();
        return JWT.create()
                //2.1添加头部.
                //.withHeader(map)

                // 2.2添加payload
                // 2.2.1.建议添加的声明
                .withIssuer("auth0")    // iss：发行者
                .withClaim(LOGIN_ID_KEY, loginId.toString())
                .withIssuedAt(nowDate)//"iat" claim . 发行时间
                .withExpiresAt(new Date(nowDate.getTime() + TIMEOUT * 1000)) // "exp" claim. 到期时间
                .withNotBefore(nowDate)    // 设置令牌在什么时间之前是不能使用的

                // 2.3签署令牌并返回JWT的Json String
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 从一个jwt里面解析出Claims
     *
     * @param tokenValue token值
     *                   //     * @param base64Security 秘钥
     * @return Claims对象
     */
    public static DecodedJWT getClaims(String tokenValue) {
//     System.out.println(tokenValue);
        DecodedJWT decodedJWT = null;

        // 先解码 看看能不能解码：不能将抛出JWTDecodeException异常
        decodedJWT = JWT.decode(tokenValue);

        // 再进行校验token：失败将抛出JWTVerificationException
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer("auth0")
                .build(); //Reusable verifier instance
        verifier.verify(tokenValue);

        return decodedJWT;
    }

    /**
     * 从一个jwt里面解析loginId
     *
     * @param tokenValue token值
     *                   //     * @param base64Security 秘钥
     * @return loginId
     */
    public static String getLoginId(String tokenValue) {
        try {
            Claim loginId = getClaims(tokenValue).getClaim(LOGIN_ID_KEY);
            if (loginId == null) {
                return null;
            }
            return loginId.asString();
        } catch (JWTDecodeException e) {// 不能解码：可能是秘钥改了
//         throw NotLoginException.newInstance(StpUtil.TYPE, NotLoginException.TOKEN_TIMEOUT);
//            System.out.println("无效令牌：可能是时间验证不通过，可能是别人修改了，可能是秘钥不对");
            throw NotLoginException.newInstance(StpUtil.stpLogic.loginType, NotLoginException.INVALID_TOKEN);
        } catch (JWTVerificationException e) {// 解码成功，但是有异常，说明校验失败，应该是失效了
            return NotLoginException.TOKEN_TIMEOUT;
//            throw NotLoginException.newInstance(StpUtil.stpLogic.loginType, NotLoginException.INVALID_TOKEN);
        } catch (Exception e) {
            throw new SaTokenException(e);
        }
    }


    static {

        // 修改默认实现
        StpUtil.stpLogic = new StpLogic("login") {

            // 重写 (随机生成一个tokenValue)
            @Override
            public String createTokenValue(Object loginId) {
                return SaTokenJwtUtil.createToken(loginId);
            }

            // 重写 (在当前会话上登录id )
            @Override
            public void login(Object loginId, SaLoginModel loginModel) {
                // ------ 1、获取相应对象
                SaStorage storage = SaManager.getSaTokenContext().getStorage();
                SaTokenConfig config = getConfig();
                // ------ 2、生成一个token
                String tokenValue = createTokenValue(loginId);
                storage.set(splicingKeyJustCreatedSave(), tokenValue);    // 将token保存到本次request里

                if (config.getIsReadCookie()) {    // cookie注入
                    SaManager.getSaTokenContext().getResponse().addCookie(getTokenName(), tokenValue, "/", config.getCookieDomain(), (int) config.getTimeout());
                }
            }

            // 重写 (获取指定token对应的登录id)
            @Override
            public String getLoginIdNotHandle(String tokenValue) {
                try {
                    return SaTokenJwtUtil.getLoginId(tokenValue);
                } catch (Exception e) {
                    return null;
                }
            }

            // 重写 (当前会话注销登录)
            @Override
            public void logout() {
                // 如果连token都没有，那么无需执行任何操作
                String tokenValue = getTokenValue();
                if (tokenValue == null) {
                    return;
                }
                // 如果打开了cookie模式，把cookie清除掉
                if (getConfig().getIsReadCookie() == true) {
                    SaManager.getSaTokenContext().getResponse().deleteCookie(getTokenName());
                }
            }

            // 重写 (获取指定key的session)
            @Override
            public SaSession getSessionBySessionId(String sessionId, boolean isCreate) {
                throw new SaTokenException("jwt has not session");
            }

            // 重写 (获取当前登录者的token剩余有效时间 (单位: 秒))
            @Override
            public long getTokenTimeout() {
                // 如果没有token
                String tokenValue = getTokenValue();
                if (tokenValue == null) {
                    return SaTokenDao.NOT_VALUE_EXPIRE;
                }
                // 开始取值
                DecodedJWT decodedJWT = null;
                try {
                    decodedJWT = SaTokenJwtUtil.getClaims(tokenValue);
                } catch (Exception e) {
                    return SaTokenDao.NOT_VALUE_EXPIRE;
                }
//                if (decodedJWT == null) {
//                    return SaTokenDao.NOT_VALUE_EXPIRE;
//                }
                Date expiration = decodedJWT.getExpiresAt();
                if (expiration == null) {
                    return SaTokenDao.NOT_VALUE_EXPIRE;
                }
                return (expiration.getTime() - System.currentTimeMillis()) / 1000;
            }

            // 重写 (返回当前token的登录设备)
            @Override
            public String getLoginDevice() {
                return SaTokenConsts.DEFAULT_LOGIN_DEVICE;
            }

            // 重写 (获取当前会话的token信息)
            @Override
            public SaTokenInfo getTokenInfo() {
                SaTokenInfo info = new SaTokenInfo();
                info.tokenName = getTokenName();
                info.tokenValue = getTokenValue();
                info.isLogin = isLogin();
                info.loginId = getLoginIdDefaultNull();
                info.loginType = getLoginType();
                info.tokenTimeout = getTokenTimeout();
//           info.sessionTimeout = getSessionTimeout();
//           info.tokenSessionTimeout = getTokenSessionTimeout();
//           info.tokenActivityTimeout = getTokenActivityTimeout();
                info.loginDevice = getLoginDevice();
                return info;
            }
        };
    }
}
```

由于这些变量和方法都是静态的，类加载的时候便已经完成初始化，因此不能通过加`@ConfigurationProperties(value = "satoken")`进而通过配置文件配置其私钥与过期时间。即写死在类中了。



## 配置在yaml中

虽然这些变量和方法都是静态的，而且修改` StpUtil.stpLogic`也需要在静态代码块中，但是可以自己去读取yaml文件，将yaml文件的值配置到静态变量里。

注意：静态变量赋值一次便无法修改，所以在定义的时候，不能初始化。

```java
@Component//放入容器中
public class SaTokenJwtUtil {
    static {
        try {
            Yaml yaml = new Yaml();
            URL url = SaTokenJwtUtil.class.getClassLoader().getResource("application.yaml");
            if (url != null) {
                //获取yaml文件中的配置数据，然后转换为obj，
                //也可以将值转换为Map
                Map map = (Map) yaml.load(new FileInputStream(url.getFile()));
                System.out.println(map);
                //通过map我们取值就可以了.
                Map my = (Map) map.get("my");
                Map jwt = (Map) my.get("jwt");
                // 这里写死了要求my.jwt下所需的3个属性
                Object secret = jwt.get("secret");
                Object timeout = jwt.get("timeout");
                Object loginIdKey = jwt.get("loginIdKey");
                if (secret == null || timeout == null || loginIdKey == null)
                    throw new Exception("请配置完整my.jwt下的3个属性");
                System.out.println(secret);
                System.out.println(timeout);
                System.out.println(loginIdKey);
                SaTokenJwtUtil.secret = secret.toString();
                SaTokenJwtUtil.TIMEOUT = Long.parseLong(timeout.toString());
                SaTokenJwtUtil.LOGIN_ID_KEY = loginIdKey.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 秘钥 (随便手打几个字母就好了)
     */
//    public static String secret = "myKey";
    public static String secret;

    /**
     * token有效期 (单位: 秒)
     */
//    public static long TIMEOUT = 60 * 60 * 24 * 7;// 7天
    public static long TIMEOUT;// 7天


    //public static String LOGIN_ID_KEY = "loginId";
    public static String LOGIN_ID_KEY;

    // 其他代码的和上面一致
}
```

注意：由于这里只向`application.yaml`文件去获取值，所以一定要配置在这个文件里。

```yaml
my:
  jwt:
    secret: xxxxxx
    timeout: 604800 # 单位：s
    loginIdKey: loginId
```



## satoken异常处理

satoken认证失败之后，会抛出异常，需要处理一下。
对于satoken的认证鉴权，请看文档，之所以要记录异常处理，是因为文档没有，在demo源码中找到。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 自定义的异常的处理
     *
     * @param me 自定义的异常
     * @return 返回Result
     */
    @ExceptionHandler({MyException.class})
    public Result handle(MyException me) {
        return Result.init(me.getCode(), me.getMessage(), null);
    }

    /**
     * 拦截来自Sa-token抛出的异常
     *
     * @param e        SaTokenException
     * @param request  请求
     * @param response 响应
     * @return 返回Result
     */
    @ExceptionHandler({SaTokenException.class})
    public Result handlerException(SaTokenException e, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // 打印堆栈，以供调试，上线之前需要注释掉
        System.out.println("全局异常，上线之前需要注释掉打印信息---------------");
        e.printStackTrace();

        // 不同异常返回不同状态码
        Result result = null;
        if (e instanceof NotLoginException) {    // 如果是未登录异常
            NotLoginException ee = (NotLoginException) e;
            result = Result.error(CodeEum.NOT_LOGIN);
            result.setMsg(ee.getMessage());
        } else if (e instanceof NotRoleException) {        // 如果是角色异常
            NotRoleException ee = (NotRoleException) e;
            result = Result.error(CodeEum.AUTH_FAIL);
            result.setMsg("无此角色：" + ee.getRole());
        } else if (e instanceof NotPermissionException) {    // 如果是权限异常
            NotPermissionException ee = (NotPermissionException) e;
            result = Result.error(CodeEum.AUTH_FAIL);
            result.setMsg("无此权限：" + ee.getCode());//权限码
        } else if (e instanceof DisableLoginException) {    // 如果是被封禁异常
            DisableLoginException ee = (DisableLoginException) e;
            result = Result.error(CodeEum.AUTH_FAIL);
            result.setMsg("账号被封禁：" + ee.getDisableTime() + "秒后解封");
        } else {    // 普通异常, 输出：500 + 异常信息
            result = Result.error(CodeEum.ERROR);
            result.setMsg(e.getMessage());
        }

        // 返回给前端
        return result;
    }
}
```