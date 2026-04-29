package org.ulpgc.dacd.marketfeeder.controller.exceptions;

public class FeederConnectionException extends RuntimeException {
    public FeederConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}