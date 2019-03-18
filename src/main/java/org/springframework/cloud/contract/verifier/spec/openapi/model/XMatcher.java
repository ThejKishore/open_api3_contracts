package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  * 	Matcher
 *  * 		body
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

    List<XMatcherDetails> xbody;
    Set<XMatcherDetails> xheader;

    public void addHeaderMatcher(XMatcherDetails xMatcherDetails){
        if(xheader == null) {
            xheader = new HashSet<>();
        }
        xheader.add(xMatcherDetails);
    }

    public void addHeaderMatchers(Set<XMatcherDetails> xMatcherDetails){
        if(xheader == null) {
            xheader = new HashSet<>();
        }
        xheader.addAll(xMatcherDetails);
    }

}
