package org.springframework.cloud.contract.verifier.spec.openapi.helper;

import org.springframework.cloud.contract.spec.internal.RegexPatterns;
import org.springframework.cloud.contract.spec.internal.XContractCommon;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.spec.openapi.model.XMatcherDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

public class JsonModelHelper {

    private JsonModelHelper(){}

    private static Random random = new Random();
    private static XContractCommon com = new XContractCommon();

    public static Map<String,Object> getResponseBodyDSL(Map<String, Object> dataJsonMap, Map<String, XMatcherDetails> responseMatchers,Set<String> ignoredAttributes) {
        LinkedHashMap<String,Object> data1= new LinkedHashMap<>();
        for(Map.Entry<String,Object> keyValue: dataJsonMap.entrySet()){
            if(!ignoredAttributes.contains(keyValue.getKey()))
                getResponseBody(responseMatchers, data1, keyValue,ignoredAttributes);
        }
        return data1;
    }


    public static Map<String,Object> getRequestBodyDSL(Map<String, Object> dataJsonMap, Map<String, XMatcherDetails> responseMatchers) {
        LinkedHashMap<String,Object> data1= new LinkedHashMap<>();
        for(Map.Entry<String,Object> keyValue: dataJsonMap.entrySet()){
            getRequestBody(responseMatchers, data1, keyValue);
        }
        return data1;
    }


    private static void getResponseBody(Map<String, XMatcherDetails> responseMatchers, LinkedHashMap<String, Object> data1, Map.Entry<String, Object> keyValue,Set<String> ignoredAttributes) {
        if (keyValue != null && !ignoredAttributes.contains(keyValue.getKey())) {
            String innerkey = keyValue.getKey();
            Object valueObj = keyValue.getValue();
            Optional<String> regexValue = regexForKey(responseMatchers, innerkey);
            if (valueObj instanceof Date) {
                data1.put(innerkey, com.value(
                        regexValue.map(regexData -> com.c(DataGeneratorHelper.randomValueGenerator(regexData))).orElseGet(() -> com.c(LocalDateTime.now())),
                        regexValue.map(regexData -> com.p(com.regex(regexData))).orElseGet(() -> com.p(com.regex(com.isoDateTime())))
                        )
                );
            } else if (valueObj instanceof Number) {
                data1.put(innerkey, com.value(
                        regexValue.map(regexData -> com.c(DataGeneratorHelper.randomValueGenerator(regexData))).orElseGet(() -> com.c(random.nextInt(3000))),
                        regexValue.map(regexData -> com.p(com.regex(regexData))).orElseGet(() -> com.p(com.regex(com.number())))
                        )
                );
            } else if (valueObj instanceof String) {
                data1.put(innerkey, com.value(
                        regexValue.map(regexData -> com.c(DataGeneratorHelper.randomValueGenerator(regexData))).orElseGet(() -> com.c("something")),
                        regexValue.map(regexData -> com.p(com.regex(regexData))).orElseGet(() -> com.p(com.regex(com.nonEmpty())))
                        )
                );
            } else if (valueObj instanceof Map) {
                data1.put(innerkey, extractResponseData(valueObj, responseMatchers,ignoredAttributes));
            }
        }
    }


    private static void getRequestBody(Map<String, XMatcherDetails> responseMatchers, LinkedHashMap<String, Object> data1, Map.Entry<String, Object> keyValue) {
        if (keyValue != null) {
            String innerkey = keyValue.getKey();
            Object valueObj = keyValue.getValue();
            Optional<String> regexValue = regexForKey(responseMatchers, innerkey);
            if (valueObj instanceof Date) {
                data1.put(innerkey, com.value(
                        regexValue.map(regexData -> com.c(com.regex(regexData))).orElseGet(() -> com.c(com.regex(com.isoDateTime()))),
                        regexValue.map(regexData -> com.p(DataGeneratorHelper.randomValueGenerator(regexData))).orElseGet(() -> com.p(LocalDateTime.now()))
                        )
                );
            } else if (valueObj instanceof Number) {
                data1.put(innerkey, com.value(
                        regexValue.map(regexData -> com.c(com.regex(regexData))).orElseGet(() -> com.c(com.regex(com.number()))),
                        regexValue.map(regexData -> com.p(DataGeneratorHelper.randomValueGenerator(regexData))).orElseGet(() -> com.p(random.nextInt(3000)))
                        )
                );
            } else if (valueObj instanceof String) {
                data1.put(innerkey, com.value(
                        regexValue.map(regexData -> com.c(com.regex(regexData))).orElseGet(() -> com.c(com.regex(com.nonEmpty()))),
                        regexValue.map(regexData -> com.p(DataGeneratorHelper.randomValueGenerator(regexData))).orElseGet(() -> com.p("something"))
                        )
                );
            } else if (valueObj instanceof Map) {
                data1.put(innerkey, extractRequestData(valueObj, responseMatchers));
            }
        }
    }


    private static Object extractResponseData(Object data,Map<String, XMatcherDetails> responseMatchers,Set<String> ignoredAttributes) {
        LinkedHashMap<String,Object> extractedMap = new LinkedHashMap<>();
        if (data instanceof Map) {
            for (Map.Entry<String, Object> keyValue : ((Map<String, Object>) data).entrySet()) {
                if(!ignoredAttributes.contains(keyValue.getKey()))
                    getResponseBody(responseMatchers, extractedMap, keyValue,ignoredAttributes);
            }
        }
        return extractedMap;
    }

    private static Object extractRequestData(Object data,Map<String, XMatcherDetails> responseMatchers) {
        LinkedHashMap<String,Object> extractedMap = new LinkedHashMap<>();
        if (data instanceof Map) {
            for (Map.Entry<String, Object> keyValue : ((Map<String, Object>) data).entrySet()) {
                getRequestBody(responseMatchers, extractedMap, keyValue);
            }
        }
        return extractedMap;
    }


    private static Optional<String> regexForKey(Map<String, XMatcherDetails> responseMatchers,String key){
        if(responseMatchers != null && !responseMatchers.isEmpty()){
            return responseMatchers.containsKey(key) ? Optional.ofNullable(getRegexFromXMatcher(responseMatchers.get(key))) : Optional.empty();
        }
        return Optional.empty();
    }


    private static String getRegexFromXMatcher(XMatcherDetails matcherDetails){
        if("by_regex".equals(matcherDetails.getType())) {
            return ( matcherDetails.getPredefined() !=null && !matcherDetails.getPredefined().isEmpty()) ? returnPattern(matcherDetails.getPredefined()) : matcherDetails.getValue();
        }
        return null;
    }


    private static String returnPattern(String predefinedValue){
        YamlContract.PredefinedRegex predefinedRegex = YamlContract.PredefinedRegex.valueOf(predefinedValue);
        return predefinedToPattern(predefinedRegex).pattern();

    }


    private static Pattern predefinedToPattern(YamlContract.PredefinedRegex predefinedRegex) {
        RegexPatterns patterns = new RegexPatterns();
        switch (predefinedRegex) {
            case only_alpha_unicode:
                return patterns.onlyAlphaUnicode();
            case number:
                return patterns.number();
            case any_double:
                return patterns.aDouble();
            case any_boolean:
                return patterns.anyBoolean();
            case ip_address:
                return patterns.ipAddress();
            case hostname:
                return patterns.hostname();
            case email:
                return patterns.email();
            case url:
                return patterns.url();
            case uuid:
                return patterns.uuid();
            case iso_date:
                return patterns.isoDate();
            case iso_date_time:
                return patterns.isoDateTime();
            case iso_time:
                return patterns.isoTime();
            case iso_8601_with_offset:
                return patterns.iso8601WithOffset();
            case non_empty:
                return patterns.nonEmpty();
            case non_blank:
                return patterns.nonBlank();
            default:
                return patterns.nonBlank();
        }
    }


}
