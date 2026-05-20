package com.att.tdp.issueflow.common;

import org.springframework.http.HttpStatus;

public final class Require {
    private Require() {
    }

    public static <T> T found(java.util.Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, message));
    }

    public static void valid(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    public static void conflict(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(HttpStatus.CONFLICT, message);
        }
    }
}
