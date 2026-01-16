package com.atlantic.esocial.configuration.filter;

public class TransactionIdHolder {
 
    private static final ThreadLocal<String> TRANSACTION_ID = new ThreadLocal<>();
 
    public static void setTransactionId(String transactionId) {
        TRANSACTION_ID.set(transactionId);
    }
 
    public static String getTransactionId() {
        return TRANSACTION_ID.get();
    }
 
    public static void clear() {
        TRANSACTION_ID.remove();
    }    
}
