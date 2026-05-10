package com.homehn.backend.exception;


public class AppException extends RuntimeException {
    private final int status;
    public AppException(String message)             { super(message); this.status = 400; }
    public AppException(String message, int status) { super(message); this.status = status; }
    public int getStatus() { return status; }
}

