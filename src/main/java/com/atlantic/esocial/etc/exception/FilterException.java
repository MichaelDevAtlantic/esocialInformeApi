package com.atlantic.esocial.etc.exception;

public class FilterException extends Exception{ // TODO definir estrutura de exceptions e pasta.

    //TODO classificar em números de códigos padrões??
    public FilterException(String msg){
        super(msg);
    }

    public FilterException(String msg, Throwable cause){
        super(msg, cause);
    }
}