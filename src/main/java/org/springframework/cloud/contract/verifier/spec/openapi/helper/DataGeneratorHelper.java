package org.springframework.cloud.contract.verifier.spec.openapi.helper;

import com.mifmif.common.regex.Generex;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DataGeneratorHelper {

    public static final String BASIC_AUTH = "Basic %s";
    public static final String BEARER_AUTH = "Bearer %s";
    public static final String USR_PWD = "%s:%s";

    public static String generateBasicAuthCode(){
        return concatenatedValue(BASIC_AUTH,createBase64("user", "password"));

    }

    private static String createBase64(String username, String password) {
        String credentials = String.format(USR_PWD,username,password);
        return new String(Base64.getEncoder().encode(credentials.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8);
    }

    public static String generateBearerAuthCode(){
        return concatenatedValue(BEARER_AUTH,createBase64("user", "password"));

    }

    private static String concatenatedValue(String formater,String value){
        return String.format(formater,value);
    }

    public static String randomValueGenerator(String regexVal){
        Generex generex = new Generex(regexVal);
        return  generex.random(5,10);
    }
}
