package org.springframework.cloud.contract.verifier.spec.openapi.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XContract {
    private String contractId;
    private String name;
    private String description;
    private int priority;
    private XRequestPath xRequestPath;
    private Map<String,Object> xRequestBody;
    private Map<String,Object> xResponseBody;
    private Map<String,String> xRequestHeader;
    private Map<String,String> xResponseHeader;
    private XRequestMatcher xRequestMatcher;
    private XResponseMatcher xResponseMatcher;


}
