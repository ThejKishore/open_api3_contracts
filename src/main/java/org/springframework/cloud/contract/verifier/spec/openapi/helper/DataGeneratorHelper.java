package org.springframework.cloud.contract.verifier.spec.openapi.helper;

import com.mifmif.common.regex.Generex;

import java.util.Base64;

public class DataGeneratorHelper {

    public static final String BASIC_AUTH = "Basic %s";
    public static final String BEARER_AUTH = "Bearer %s";
    public static final String USR_PWD = "%s:%s";

    public static String generateBasicAuthCode(){
        String base64EncodedValue = Base64.getEncoder().encode(String.format(USR_PWD, "user", "password").getBytes()).toString();
        return concatenatedValue(BASIC_AUTH,base64EncodedValue);

    }

    public static String generateBearerAuthCode(){
        String base64EncodedValue = Base64.getEncoder().encode(String.format(USR_PWD, "user", "password").getBytes()).toString();
        return concatenatedValue(BEARER_AUTH,base64EncodedValue);

    }

    private static String concatenatedValue(String formater,String value){
        return String.format(formater,value);
    }

    public static String randomValueGenerator(String regexVal){
        Generex generex = new Generex(regexVal);
        return  generex.random(5,10);
    }
}
