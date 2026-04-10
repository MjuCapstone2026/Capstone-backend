package com.mju.capstone_backend.global.exception;

import org.springframework.http.HttpStatus;

public record ErrorResponse(int status, String error, String message) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message);
    }
}
