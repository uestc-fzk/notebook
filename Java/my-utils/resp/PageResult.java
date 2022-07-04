package com.scrm.marketing.util.resp;


/**
 * @author fzk
 * @date 2021-10-14 19:51
 */
public class PageResult extends Result {
    // 数据总条目数
    private int total;
    // 当前页码
    private int pageNum;

    public PageResult() {
        super();
    }

    public PageResult(int code, String msg, Object data, int total, int pageNum) {
        super(code, msg, data);
        this.total = total;
        this.pageNum = pageNum;
    }

    public static PageResult init(CodeEum codeEum, Object data, int total, int pageNum) {
        return init(codeEum.getCode(), codeEum.getMsg(), data, total, pageNum);
    }

    public static PageResult init(int code, String msg, Object data, int total, int pageNum) {
        return new PageResult(code, msg, data, total, pageNum);
    }

    public static PageResult success(Object data, int total, int pageNum) {
        return init(CodeEum.SUCCESS, data, total, pageNum);
    }

    public static PageResult error(CodeEum codeEum) {
        return init(codeEum, null, -1, -1);
    }

    /**
     * getter和setter方法
     */
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "total=" + total +
                ", pageNum=" + pageNum +
                '}';
    }
}
