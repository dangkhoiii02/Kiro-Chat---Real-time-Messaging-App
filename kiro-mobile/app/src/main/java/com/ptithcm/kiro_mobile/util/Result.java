package com.ptithcm.kiro_mobile.util;

/**
 * Generic result wrapper for repository calls.
 */
public final class Result<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    private final Status status;
    private final T data;
    private final String message;
    private final int code;

    private Result(Status status, T data, String message, int code) {
        this.status  = status;
        this.data    = data;
        this.message = message;
        this.code    = code;
    }

    // ── Factories ────────────────────────────────────────────────────────────

    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null, 200);
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null, 0);
    }

    public static <T> Result<T> error(String message, int code) {
        return new Result<>(Status.ERROR, null, message, code);
    }

    public static <T> Result<T> networkError() {
        return error("Không có kết nối mạng. Vui lòng thử lại.", 0);
    }

    public static <T> Result<T> unauthorized() {
        return error("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", 401);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Status getStatus()  { return status; }
    public T      getData()    { return data; }
    public String getMessage() { return message; }
    public int    getCode()    { return code; }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isError()   { return status == Status.ERROR; }
}
