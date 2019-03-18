package org.springframework.cloud.contract.verifier.spec.openapi.model;

public enum XMethod {
    GET("get"),
    POST("post"),
    PUT("put"),
    DELETE("delete");

    private String methodName;

    XMethod(String methodName){
        this.methodName = methodName;
    }
}
