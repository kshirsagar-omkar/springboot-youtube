package com.tca.exception;

public class SalaryLessException extends RuntimeException {
    public SalaryLessException(String message) {
        super(message);
    }
}
