package com.scrm.marketing.util.resp;

/**
 * @author fzk
 * @date 2021-10-14 19:34
 */
public enum CodeEum {
    SUCCESS(200, "ok"),
    TIMEOUT(408, "请求超时"),
    ERROR(1000, "系统错误"),
    NOT_LOGIN(1001, "未登录"),
    AUTH_FAIL(1003, "无权限"),
    NOT_EXIST(1004, "未找到资源"),
    FAIL(1005, "操作失败"),
    PARAM_MISS(1009, "缺少参数"),
    PARAM_ERROR(1010, "参数错误");

    public static int CODE_SUCCESS=200;
    public static int CODE_TIMEOUT=408;
    public static int CODE_ERROR=1000;
    public static int CODE_NOT_LOGIN=1001;
    public static int CODE_AUTH_FAIL=1003;
    public static int CODE_NOT_EXIST=1004;
    public static int CODE_FAIL=1005;
    public static int CODE_PARAM_MISS=1009;
    public static int CODE_PARAM_ERROR=1010;

    private int code;
    private String msg;

    private CodeEum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
