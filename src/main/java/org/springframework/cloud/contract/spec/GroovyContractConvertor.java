package org.springframework.cloud.contract.spec;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mifmif.common.regex.Generex;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.contract.spec.internal.*;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.spec.openapi.model.Tuple;
import org.springframework.cloud.contract.verifier.spec.openapi.model.XContract;
import org.springframework.cloud.contract.verifier.spec.openapi.model.XMatcherDetails;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Contract.make {
 *     description("Get zipCode")
 *     request {
 *         headers {
 *             contentType(applicationJson())
 *             header( authorization(),anyNonBlankString())
 *         }
 *         url value(consumer(regex('/v1/zipCodes/[0-9]{5}')),producer('/v1/zipCodes/55401'))
 *         method GET()
 *     }
 *     response {
 *         status 200
 * //        body(file('response.json'))
 *         body(
 *                 city: "asdsddsds",
 *                 state: "asdsdsdsds",
 *                 zipCode: "asdsadssadsd",
 * //                zipCode:$(producer(anyNonEmptyString())),
 *                 directShipStoreNumber:"asdsdasdsadasd",
 *                 primeDistributionCenterId:"asdsdsdsd",
 *                 citySuspendOrderIndicator:"asdsadsdsd"
 *         )
 *
 *         testMatchers {
 *             jsonPath('$.city' , byType())
 *             jsonPath('$.state' , byType())
 *             jsonPath('$.zipCode' , byType())
 *             jsonPath('$.directShipStoreNumber' , byType())
 *             jsonPath('$.primeDistributionCenterId' , byType())
 *             jsonPath('$.citySuspendOrderIndicator' , byType())
 *         }
 *
 *         headers {
 *             contentType(applicationJson())
 *         }
 *     }
 */

@Slf4j
public class GroovyContractConvertor {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ObjectWriter jsonObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    private static XContractCommon $ = new XContractCommon();

    @SneakyThrows
    public static Contract convertXContractToGroovyContract(XContract contract, OpenAPI openApi){
        Contract groovyContract = new Contract();
        groovyContract.priority(contract.getPriority());
        groovyContract.setName(contract.getName());
        groovyContract.setDescription(contract.getDescription());

        //[start::create::request]
        Request request = new Request();
        //Add the request method..
        request.method(createTheMethod(contract.getXRequestPath().getMethod().name()));

        //Add the url path
        request.url(urlPathCreation(contract.getXRequestPath().getUrlToHit(),contract.getXRequestMatcher().getXpathParams()));

        //add the request header

        Headers headers = new Headers();
        contract.getXRequestMatcher().getXheader().entrySet().stream()
                .forEach(headerData -> createRequestHeader(headerData.getValue(),headers));
        request.setHeaders(headers);


        //Add the request body....
        Map<String,Object> data = new LinkedHashMap<>();


        request.body(data);

        groovyContract.setRequest(request);
        //[end::create::request]


        //[start::create::response]
        Response response = new Response();


        Headers responseHeaders = new Headers();
        contract.getXResponseMatcher().getXheader().entrySet().stream()
                .forEach(headerData -> createResponseHeader(headerData.getValue(),responseHeaders));
        response.setHeaders(responseHeaders);





        groovyContract.setResponse(response);


        //[end::create::response]


        log.info("{} ",jsonObjectWriter.writeValueAsString(groovyContract));
        return groovyContract;
    }

    static Pattern urlValue= Pattern.compile("(\\{[a-z,A-Z]+\\})");

    private static DslProperty urlPathCreation(String urlPath , Map<String,XMatcherDetails> xMatcherDetails){
        if(urlPath.contains("{")&&urlPath.contains("}")){
            //create Server side
            String serverUrlPath = replaceUrlPathVariablesWithValues(createValueAndSet(xMatcherDetails),urlPath);
            //create client side
            String clientUrlPath = replaceUrlPathVariablesWithValues(createValueAndSetRegex(xMatcherDetails),urlPath);
            //value
            return $.value($.c($.regex(clientUrlPath)),$.p(serverUrlPath));

        }else{
            return $.toDslProperty(urlPath);
        }
    }


    private static  Map<String,Object> createRequestBody(List<XMatcherDetails> bodyConstructDetails){

        return null;
    }

    private static String replaceUrlPathVariablesWithValues(Map<String,String> keyValuePair,String urlPath){
        String url = urlPath;
        for (Map.Entry<String,String> pair: keyValuePair.entrySet()){
           url = url.replace( String.format("{%s}",pair.getKey()),pair.getValue());
        }
        return url;
    }

    private static Map<String,String> createValueAndSet(Map<String,XMatcherDetails> xMatcherDetails){
        return xMatcherDetails.entrySet().stream()
                .map(GroovyContractConvertor::createValueForRegex)
                .collect(Collectors.toMap(d -> d.getA(),d-> d.getB()));
    }

    private static Map<String,String> createValueAndSetRegex(Map<String,XMatcherDetails> xMatcherDetails){
        return xMatcherDetails.entrySet().stream()
                .map(GroovyContractConvertor::assignRegexToKey)
                .collect(Collectors.toMap(d -> d.getA(),d-> d.getB()));
    }

    private static final Predicate<XMatcherDetails> isRegex = xMatcherDetails -> "by_regex".equals(xMatcherDetails.getType());
    private static final Predicate<XMatcherDetails> isPredefined = xMatcherDetails -> xMatcherDetails.getPredefined() !=null && !xMatcherDetails.getPredefined().trim().isEmpty();
    private static final Predicate<XMatcherDetails> isJsonPath = xMatcherDetails -> xMatcherDetails.getJsonPath() !=null && !xMatcherDetails.getJsonPath().trim().isEmpty();



    private static Tuple<String,String> assignRegexToKey(Map.Entry<String,XMatcherDetails> xMatcherDetails){
        String key = isJsonPath.test(xMatcherDetails.getValue()) ? xMatcherDetails.getValue().getJsonPath() : xMatcherDetails.getValue().getName();
        return Tuple.<String,String>builder().a(key).b(xMatcherDetails.getValue().getValue()).build();
    }

    private static Tuple<String,String> createValueForRegex(Map.Entry<String,XMatcherDetails> xMatcherDetails){
        String key = isJsonPath.test(xMatcherDetails.getValue()) ? xMatcherDetails.getValue().getJsonPath() : xMatcherDetails.getValue().getName();
        if(isRegex.test(xMatcherDetails.getValue()) ) {
            String regex = isPredefined.test(xMatcherDetails.getValue()) ? returnPattern(xMatcherDetails.getValue().getPredefined()) : xMatcherDetails.getValue().getValue();
            Generex generex = new Generex(regex);
            return Tuple.<String,String>builder().a(key).b(generex.random(5,10)).build();
        }else{
            return Tuple.<String,String>builder().a(key).b(xMatcherDetails.getValue().getValue()).build();
        }

    }

    private static HttpMethods.HttpMethod createTheMethod(String requestMethod){
        HttpMethods.HttpMethod httpMethod = null;

        switch (requestMethod.toUpperCase()){
            case "GET":
                httpMethod = HttpMethods.HttpMethod.GET;
                break;
            case "POST":
                httpMethod = HttpMethods.HttpMethod.POST;
                break;
            case "DELETE":
                httpMethod = HttpMethods.HttpMethod.DELETE;
                break;
            case "PUT":
                httpMethod = HttpMethods.HttpMethod.PUT;
                break;
            default:
                httpMethod = HttpMethods.HttpMethod.GET;
                break;

        }
        return httpMethod;

    }

    //If content type is not specified then application/json is taken.
    private static void createRequestHeader(XMatcherDetails xMatcherDetails,Headers headers){
        log.info("--->{} ",xMatcherDetails);
        DslProperty dslProperty = null ;
        if(isRegex.test(xMatcherDetails)) {
            String pattern = isPredefined.test(xMatcherDetails) ? returnPattern(xMatcherDetails.getPredefined()) : xMatcherDetails.getValue();
            Generex generex = new Generex(pattern);
            dslProperty = $.value($.c($.regex(xMatcherDetails.getValue())),$.p(generex.random(5,10)));
        }
        if(dslProperty !=null) {
            headers.header(xMatcherDetails.getName(), dslProperty);
        }

    }

    //If content type is not specified then application/json is taken.
    private static void createResponseHeader(XMatcherDetails xMatcherDetails,Headers headers){
        log.info("--->{} ",xMatcherDetails);
        DslProperty dslProperty = null ;
        if(isRegex.test(xMatcherDetails)) {
            String pattern = isPredefined.test(xMatcherDetails) ? returnPattern(xMatcherDetails.getPredefined()) : xMatcherDetails.getValue();
            Generex generex = new Generex(pattern);
            dslProperty = $.value($.c(generex.random(5,10)),$.p($.regex(xMatcherDetails.getValue())));
        }
        if(dslProperty !=null) {
            headers.header(xMatcherDetails.getName(), dslProperty);
        }

    }


    private static String returnPattern(String predefinedValue){
        YamlContract.PredefinedRegex predefinedRegex = YamlContract.PredefinedRegex.valueOf(predefinedValue);
        return predefinedToPattern(predefinedRegex).pattern();

    }


    public static Pattern predefinedToPattern(YamlContract.PredefinedRegex predefinedRegex) {
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
