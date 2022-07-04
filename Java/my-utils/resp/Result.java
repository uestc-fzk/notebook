package com.scrm.marketing.util.resp;


/**
 * @author fzk
 * @date 2021-10-14 19:29
 */
public class Result {
    private int code;
    private String msg;
    private Object data;

    public Result() {
    }

    public Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result success() {
        return ResultCache.SUCCESS;
    }

    public static Result success(Object data) {
        return new Result(CodeEum.SUCCESS.getCode(), CodeEum.SUCCESS.getMsg(), data);
    }

    public static Result error(CodeEum codeEum) {
        return new Result(codeEum.getCode(), codeEum.getMsg(), null);
    }

    public static Result error(CodeEum codeEum, String msg) {
        return new Result(codeEum.getCode(), codeEum.getMsg(), null).addMsg(msg);
    }

    public static Result init(int code, String msg, Object data) {
        return new Result(code, msg, data);
    }

    /*getter和setter方法*/
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Result addMsg(String addMsg) {
        this.msg = this.msg + ": " + addMsg;
        return this;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }

    /* 获取常量的方法 */
    public static Result PARAM_MISS() {
        return ResultCache.PARAM_MISS;
    }

    public static Result PARAM_ERROR() {
        return ResultCache.PARAM_ERROR;
    }

    public static Result FAIL() {
        return ResultCache.FAIL;
    }


    /* 缓存 */
    private static class ResultCache extends Result {
        public static final Result SUCCESS = new ResultCache(200, "ok", null);
        public static final Result PARAM_MISS = new ResultCache(CodeEum.PARAM_MISS.getCode(), CodeEum.PARAM_MISS.getMsg(), null);
        public static final Result PARAM_ERROR = new ResultCache(CodeEum.PARAM_ERROR.getCode(), CodeEum.PARAM_ERROR.getMsg(), null);
        public static final Result FAIL = new ResultCache(CodeEum.FAIL.getCode(), CodeEum.FAIL.getMsg(), null);

        private ResultCache(int code, String msg, Object data) {
            super(code, msg, data);
        }

        @Override
        public void setCode(int code) {
            throw new RuntimeException("常量不支持修改");
        }

        @Override
        public void setMsg(String msg) {
            throw new RuntimeException("常量不支持修改");
        }

        @Override
        public Result addMsg(String msg) {
            throw new RuntimeException("常量不支持修改");
        }

        @Override
        public void setData(Object data) {
            throw new RuntimeException("常量不支持修改");
        }
    }
}

