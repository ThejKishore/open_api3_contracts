package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XRequestMatcher extends XMatcher{
    Set<XMatcherDetails> xRequestParams;
    Set<XMatcherDetails> xpathParams;


    public void addRequestParamMatcher(XMatcherDetails xMatcherDetails){
        if(xRequestParams == null) {
            xRequestParams = new HashSet<>();
        }
        xRequestParams.add(xMatcherDetails);
    }

    public void addRequestParamMatchers(Set<XMatcherDetails> xMatcherDetails){
        if(xRequestParams == null) {
            xRequestParams = new HashSet<>();
        }
        xRequestParams.addAll(xMatcherDetails);
    }

    public void addPathParamMatcher(XMatcherDetails xMatcherDetails){
        if(xpathParams == null) {
            xpathParams = new HashSet<>();
        }
        xpathParams.add(xMatcherDetails);
    }

    public void addPathParamMatchers(Set<XMatcherDetails> xMatcherDetails){
        if(xpathParams == null) {
            xpathParams = new HashSet<>();
        }
        xpathParams.addAll(xMatcherDetails);
    }
}
