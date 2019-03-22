package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XRequestMatcher extends XMatcher{
    Map<String,XMatcherDetails> xRequestParams;
    Map<String,XMatcherDetails> xpathParams;

    public void addRequestParamMatchers(Map<String,XMatcherDetails> xMatcherDetails){
        if(xRequestParams == null) {
            xRequestParams = new HashMap<>();
        }
        xRequestParams.putAll(xMatcherDetails);
    }

    public void addPathParamMatchers(Map<String,XMatcherDetails> xMatcherDetails){
        if(xpathParams == null) {
            xpathParams = new HashMap<>();
        }
        xpathParams.putAll(xMatcherDetails);
    }
}
