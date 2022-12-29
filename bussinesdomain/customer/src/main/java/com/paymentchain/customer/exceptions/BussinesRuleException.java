package com.paymentchain.customer.exceptions;

import org.springframework.http.HttpStatus;

public class BussinesRuleException extends Exception {
    private long id;
    private String code;
    private HttpStatus httpStatus;

    public BussinesRuleException(long id, String code, String message,HttpStatus httpStatus) {
        super(message);
        this.id = id;
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BussinesRuleException(String code, String message,HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BussinesRuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCode() {
        return code;
    }
}
