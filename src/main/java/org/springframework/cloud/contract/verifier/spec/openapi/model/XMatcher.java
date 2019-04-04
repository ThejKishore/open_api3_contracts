package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 *  * 	Matcher
 *  * 		body
 *              matcher-ref: "#/components/schemas/accessToken/x-matchers/default/matchers/body"
 *              ignore:
 *                 - access_token
 *             - path: $.['city']
 *               type: by_regex
 *               value: "[a-zA-Z0-9 .]+"
 *               predefined: enums
 *  *
 *  * 		header
 *  *          - name: Authorization
 *               in: header
 *               type: by_regex
 *               value: "[a-zA-Z0-9 .]+"
 *               predefined: enums
 *
 *  * 		parameters
 *  *          - name: zipcode
 *  *            in: query / path
 *  *            type: by_regex
 *  *            value: "[a-zA-Z0-9 .]+"
 *               predefined: enums
 *
 *
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XMatcher {

    String bodyMatchRefer;
    Set<String> bodyIgnoredAttributes = new HashSet<>();

    String headerMatchRefer;
    Set<String> headerIgnoredAttributes = new HashSet<>();

    Map<String,XMatcherDetails> xbody;
    Map<String,XMatcherDetails> xheader;


    public void addHeaderMatchers(Map<String,XMatcherDetails> xMatcherDetails){
        if(xheader == null) {
            xheader = new HashMap<>();
        }
        xheader.putAll(xMatcherDetails);
    }

    public void addBodyMatchers(Map<String,XMatcherDetails> xMatcherDetails){
        if(xbody == null) {
            xbody = new HashMap<>();
        }
        xbody.putAll(xMatcherDetails);
    }


    public void addHeaderIgnoredAttributes(Collection<String> headerIgnoredAttributes){
        if(this.headerIgnoredAttributes == null) {
            this.headerIgnoredAttributes = new HashSet<>();
        }
        this.headerIgnoredAttributes.addAll(headerIgnoredAttributes);
    }

    public void addBodyIgnoredAttributes(Collection<String> bodyIgnoredAttributes){
        if(this.bodyIgnoredAttributes == null) {
            this.bodyIgnoredAttributes = new HashSet<>();
        }
        this.bodyIgnoredAttributes.addAll(bodyIgnoredAttributes);
    }


}
