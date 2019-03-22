package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XRequestMatcher extends XMatcher{
    Set<XMatcherDetails> xRequestParams;
    Set<XMatcherDetails> xpathParams;

    public void addRequestParamMatchers(Collection<XMatcherDetails> xMatcherDetails){
        if(xRequestParams == null) {
            xRequestParams = new HashSet<>();
        }
        xRequestParams.addAll(xMatcherDetails);
    }

    public void addPathParamMatchers(Collection<XMatcherDetails> xMatcherDetails){
        if(xpathParams == null) {
            xpathParams = new HashSet<>();
        }
        xpathParams.addAll(xMatcherDetails);
    }
}
