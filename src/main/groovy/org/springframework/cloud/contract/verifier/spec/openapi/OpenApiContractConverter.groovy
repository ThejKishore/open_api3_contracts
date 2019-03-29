package org.springframework.cloud.contract.verifier.spec.openapi

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.parser.OpenAPIV3Parser
import org.apache.commons.lang3.StringUtils
import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.spec.ContractConverter
import org.springframework.cloud.contract.spec.internal.*
import org.springframework.cloud.contract.verifier.converter.YamlContract
import org.springframework.cloud.contract.verifier.spec.openapi.helper.DataGeneratorHelper
import org.springframework.cloud.contract.verifier.spec.openapi.helper.JsonModelHelper
import org.springframework.cloud.contract.verifier.spec.openapi.model.XContract

import java.util.regex.Pattern

import static org.apache.commons.lang3.StringUtils.isNumeric

/**
 * Created by John Thompson on 5/24/18.
 */
@Slf4j
class OpenApiContractConverter implements ContractConverter<Collection<PathItem>> {

    public static final OpenApiContractConverter INSTANCE = new OpenApiContractConverter()
    public static final OpenApiConverterHelper helper = new OpenApiConverterHelper()
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ObjectWriter jsonObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();

    private static final String REGEX_SCHEME = "[A-Za-z][+-.\\w^_]*:"

    private static final String HTTPS_REGEX_SCHEME = "https:"

    // Example: "//".
    private static final String REGEX_AUTHORATIVE_DECLARATION = "/{2}"

    // Optional component. Example: "suzie:abc123@". The use of the format "user:password" is deprecated.
    private static final String REGEX_USERINFO = "(?:\\S+(?::\\S*)?@)?"

    // Examples: "fitbit.com", "22.231.113.64".
    private static final String REGEX_HOST = "(?:" +
            // @Author = http://www.regular-expressions.info/examples.html
            // IP address
            "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
            "|" +
            // host name
            "(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)" +
            // domain name
            "(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*" +
            // TLD identifier must have >= 2 characters
            "(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))"

    // Example: ":8042".
    private static final String REGEX_PORT = "(?::\\d{2,5})?"

    //Example: "/user/heartrate?foo=bar#element1".
    private static final String REGEX_RESOURCE_PATH = "(?:/\\S*)?"

    protected static final Pattern HTTPS_URL = Pattern.compile("^(?:" + HTTPS_REGEX_SCHEME + REGEX_AUTHORATIVE_DECLARATION +
            REGEX_USERINFO + REGEX_HOST + REGEX_PORT + REGEX_RESOURCE_PATH + ")\$")

    protected static final Pattern URL = Pattern.compile("^(?:(?:" + REGEX_SCHEME + REGEX_AUTHORATIVE_DECLARATION + ")?" +
            REGEX_USERINFO + REGEX_HOST + REGEX_PORT + REGEX_RESOURCE_PATH + ")\$")

    protected static final Pattern TRUE_OR_FALSE = Pattern.compile(/(true|false)/)
    protected static final Pattern ALPHA_NUMERIC = Pattern.compile('[a-zA-Z0-9]+')
    protected static final Pattern ONLY_ALPHA_UNICODE = Pattern.compile(/[\p{L} ]*/)
    protected static final Pattern NUMBER = Pattern.compile('-?(\\d*\\.\\d+|\\d+)')
    protected static final Pattern INTEGER = Pattern.compile('-?(\\d+)')
    protected static final Pattern POSITIVE_INT = Pattern.compile('([1-9]\\d*)')
    protected static final Pattern DOUBLE = Pattern.compile('-?(\\d*\\.\\d+)')
    protected static final Pattern HEX = Pattern.compile('[a-fA-F0-9]+')
    protected static final Pattern IP_ADDRESS = Pattern.compile('([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])')
    protected static final Pattern HOSTNAME_PATTERN = Pattern.compile('((http[s]?|ftp):/)/?([^:/\\s]+)(:[0-9]{1,5})?')
    protected static final Pattern EMAIL = Pattern.compile('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}')
    protected static final Pattern UUID = Pattern.compile('[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
    protected static final Pattern ANY_DATE = Pattern.compile('(\\d\\d\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])')
    protected static final Pattern ANY_DATE_TIME = Pattern.compile('([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])')
    protected static final Pattern ANY_TIME = Pattern.compile('(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])')
    protected static final Pattern NON_EMPTY = Pattern.compile(/[\S\s]+/)
    protected static final Pattern NON_BLANK = Pattern.compile(/^\s*\S[\S\s]*/)
    protected static final Pattern ISO8601_WITH_OFFSET = Pattern.compile(/([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\.\d{3})?(Z|[+-][01]\d:[0-5]\d)/)


    @Override
    boolean isAccepted(File file) {
        try {
            def spec = new OpenAPIV3Parser().read(file.path)

            if (spec == null) {
                log.debug("Spec Not Found")
                throw new RuntimeException("Spec not found")
            }

            if (spec.paths.size() == 0) { // could toss NPE, which is also invalid spec
                log.debug("No Paths Found")
                throw new RuntimeException("No paths found")
            }

            def contractsFound = false
            //check spec for contracts
            spec.paths.each { k, v ->
                if (!contractsFound) {
                    v.readOperations().each { operation ->
                        if (operation.extensions) {
                            def contracts = operation.extensions."x-contracts"
                            if (contracts != null && contracts.size > 0) {
                                contractsFound = true
                            }
                        }
                    }
                }
            }

            return contractsFound
        } catch (Exception e) {
            log.error("Unexpected error in reading contract file")
            log.error(e.message)
            return false
        }
    }

    @Override
    Collection<Contract> convertFrom(File file) {


        def sccContracts = []

        def spec = new OpenAPIV3Parser().read(file.path)
        Map<String, XContract> map = helper.getStringXContractHashMap(spec)

        spec?.paths?.each { path, pathItem ->
            pathItem.readOperations().each { operation ->
                if (operation?.extensions?."x-contracts") {
                    operation.extensions."x-contracts".each { openApiContract ->

                        def contractId = openApiContract.contractId
                        log.info(" {} {} ",contractId ,contractId.getClass().name)

                        XContract xContract = map[contractId.toString()]

                        log.info(" xcontract {} ",jsonObjectWriter.writeValueAsString(xContract))

                        def contractPath = (StringUtils.isEmpty(openApiContract.contractPath)) ? path : openApiContract.contractPath

                        sccContracts.add(
                                Contract.make {
                                    if (openApiContract?.name != null) {
                                        log.info("Creating Contract for: {} " , openApiContract?.name)
                                    }

                                    println "Creating Contract for: ${openApiContract?.name}"

                                    name = openApiContract?.name
                                    description = openApiContract?.description
                                    priority = openApiContract?.priority

                                    if(openApiContract.ignored) {
                                        ignored = openApiContract.ignored
                                    } else {
                                        ignored = false
                                    }

                                    request {
                                        //set method
                                        if (pathItem?.get?.is(operation)) {
                                            method("GET")
                                        } else if (pathItem?.put.is(operation)) {
                                            method("PUT")
                                        } else if (pathItem?.post.is(operation)) {
                                            method("POST")
                                        } else if (pathItem?.delete.is(operation)) {
                                            method("DELETE")
                                        } else if (pathItem?.patch.is(operation)) {
                                            method("PATCH")
                                        }
                                        def headerList=[]
                                        if (operation?.parameters) {
                                            boolean isPathBoolean = false
                                            String consumerExposedPath = new String(contractPath)
                                            String produceExposedPath = new String(contractPath)
                                            operation?.parameters?.each { openApiParam ->
                                                openApiParam?.extensions?."x-contracts".each { contractParam ->
                                                    if (contractParam.contractId == contractId) {
                                                        if (openApiParam.in == 'path') {
                                                            def key = String.format("{%s}", openApiParam.name)
                                                            produceExposedPath = produceExposedPath.replace(key, contractParam.default)
                                                            consumerExposedPath = consumerExposedPath.replace(key, contractParam.matchers.paramaters.value[0])
                                                            isPathBoolean = true
                                                        }
                                                    }
                                                }
                                            }
                                            if(isPathBoolean) {
                                                url($(c(regex(consumerExposedPath)), p(produceExposedPath))) {
                                                    queryParameters {
                                                        operation?.parameters?.each { openApiParam ->
                                                            if (openApiParam.in == 'header') {
//                                                                headers {
                                                                log.info(" --- inside ---param --- header ")
                                                                log.info(" -header --{} ",xContract.XRequestMatcher.xheader[openApiParam.name])
                                                                def xheader = xContract.XRequestMatcher.xheader[openApiParam.name]
                                                                String regexval = xheader?.value?:"[0-9a-zA-Z]{10}"
                                                                if(openApiParam.name == "Authorization") {
                                                                    headerList.add(new Header(openApiParam.name, value(c(regex(nonEmpty())), p(DataGeneratorHelper.generateBasicAuthCode()))))
                                                                } else {
                                                                    headerList.add(new Header(openApiParam.name, value(c(regex(nonEmpty())),p(DataGeneratorHelper.randomValueGenerator(regexval)))))
                                                                }
//                                                                }
                                                            }
                                                            if (openApiParam.in == 'query') {
//                                                                log.info(" --- inside ---param --- query ")
                                                                def xquery = xContract.XRequestMatcher.XRequestParams[openApiParam.name]
//                                                                log.info(" -query --{} ",query)
                                                                String regexval = xquery?.value?:"[0-9a-zA-Z]{10}"
                                                                parameter(openApiParam.name, value(c(regex(nonEmpty())),p(DataGeneratorHelper.randomValueGenerator(regexval))))
                                                            }
                                                        }
                                                    }
                                                }
                                            } else{

                                                url(contractPath) {
                                                    queryParameters {
                                                        operation?.parameters?.each { openApiParam ->
                                                            if (openApiParam.in == 'header') {
                                                                log.info(" --- inside ---param --- header ")
                                                                log.info(" -header --{} ",xContract.XRequestMatcher.xheader[openApiParam.name])
                                                                def xheader = xContract.XRequestMatcher.xheader[openApiParam.name]
                                                                def regexval = xheader?.value?:"[0-9a-zA-Z]{10}"
                                                                log.info(" regexval {} ",regexval)
//                                                                headers {
                                                                if(openApiParam.name == "Authorization") {
                                                                    headerList.add(new Header(openApiParam.name, value(c(regex(nonEmpty())), p(DataGeneratorHelper.generateBasicAuthCode()))))
                                                                } else {
                                                                    headerList.add(new Header(openApiParam.name, value(c(regex(nonEmpty())),p(DataGeneratorHelper.randomValueGenerator(regexval)))))
                                                                }
//                                                                }
                                                            }
                                                            if (openApiParam.in == 'query') {
                                                                log.info(" --- inside ---param --- query ")
                                                                def xquery = xContract.XRequestMatcher.XRequestParams[openApiParam.name]
                                                                log.info(" -query --{} ",query)
                                                                def regexval = xquery?.value?:"[0-9a-zA-Z]{10}"
                                                                parameter(openApiParam.name, value(c(regex(nonEmpty())),p(DataGeneratorHelper.randomValueGenerator(regexval))))
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                        } else {
                                            url(contractPath)
                                        }
                                        headers {
                                            if (openApiContract?.requestHeaders) {
                                                openApiContract?.requestHeaders?.each { xheader ->
                                                    xheader.each { k, v ->
                                                        header(k, v)
                                                    }
                                                }
                                            }

                                            headerList?.each { d ->
                                                entries << d

                                            }

                                            if(operation?.requestBody?.content){
                                                LinkedHashMap<String, MediaType> content = operation?.requestBody?.content
                                                //todo - not sure if there is a use case for more than one map entry here
                                                header(contentType(), containing(content.entrySet().getAt(0).key))
                                            }
                                        }

                                        if (operation?.requestBody?.extensions?."x-contracts") {
                                            operation?.requestBody?.extensions?."x-contracts"?.each { contractBody ->
                                                if (contractBody.contractId == contractId) {
//                                                    log.debug ("-------contract body ${contractBody.body} -----------")
//                                                    body(toDslProperty(contractBody.body))
                                                    def data = new HashMap<String,DslProperty>()
                                                    contractBody?.body?.each{ entry ->
//                                                        log.debug( "---- ${entry}")
//                                                        log.debug(" ----${contractBody?.matchers?.body[0].getClass().name}")
                                                        if(operation?.requestBody?.content.entrySet().getAt(0).key != 'application/x-www-form-urlencoded') {
                                                            if (contractBody.matchers?.body != null) {
                                                                Pattern regexVal = regexifFound(contractBody.matchers.body, entry.key)
                                                                log.debug("-----------> {} ", regexVal)
                                                                data.put(entry.key, value(c(regex(regexVal)), p(entry.value)))
//                                                            data.put(entry.key, value(c(entry.value), p(entry.value)))
                                                            } else {
                                                                data.put(entry.key, entry.value)
                                                            }
                                                        } else {
                                                            data.put(entry.key, entry.value)
                                                        }
                                                    }

                                                    String JSON_STRING = xContract.XRequestBody['default_body']
                                                    if(JSON_STRING){
                                                        Map<String, Object> requestBodyMap = objectMapper.readValue(JSON_STRING, new TypeReference<Map<String, Object>>(){})
//                                                                body(responseContract.body)
                                                        //todo construct the Map<String,DslProperty>
                                                        def data1 = JsonModelHelper.getRequestBodyDSL(requestBodyMap,xContract.XRequestBody.xbody)
                                                        body(data1)
                                                    } else {
                                                        body(data)
                                                    }

                                                    bodyMatchers {
                                                        contractBody.matchers?.body?.each { matcher ->
                                                            MatchingTypeValue value = null
                                                            log.debug(" ----------${matcher.type}---------")
                                                            switch (matcher.type) {
                                                                case 'by_date':
                                                                    value = byDate()
                                                                    break
                                                                case 'by_time':
                                                                    value = byTime()
                                                                    break
                                                                case 'by_timestamp':
                                                                    value = byTimestamp()
                                                                    break
                                                                case 'by_regex':
                                                                    String regex = matcher.value
                                                                    if (matcher.predefined) {
                                                                        YamlContract.PredefinedRegex  pdRegx = YamlContract.PredefinedRegex.valueOf(matcher.predefined)
                                                                        regex = predefinedToPattern(pdRegx).pattern()
                                                                    }
                                                                    value = byRegex(regex)
                                                                    break
                                                                case 'by_equality':
                                                                    value = byEquality()
                                                                    break
                                                            }
                                                            jsonPath(matcher.path, value)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    response {
                                        if (operation?.responses) {
                                            operation.responses.each { openApiResponse ->
                                                if (openApiResponse?.value?.extensions?.'x-contracts') {
                                                    openApiResponse?.value?.extensions?.'x-contracts'?.each { responseContract ->
                                                        if (responseContract.contractId == contractId) {

                                                            def httpResponse = openApiResponse.key.replaceAll("[^a-zA-Z0-9 ]+","")

                                                            if (isNumeric(httpResponse)) {
                                                                status(httpResponse as Integer)
                                                            }

                                                            def contentTypeSB = new StringBuffer()

                                                            openApiResponse.getValue()?.content?.keySet()?.each { val ->
                                                                contentTypeSB.append(val)
                                                                contentTypeSB.append(';')
                                                            }

                                                            headers {

                                                                responseContract.headers.each { String headerKey, Object headerValue ->
                                                                    def matcher = responseContract?.matchers?.headers?.find { it.key == headerKey }
                                                                    if (headerValue instanceof List) {
                                                                        ((List) headerValue).each {
                                                                            Object serverValue = serverValue(it, matcher, headerKey)
                                                                            header(headerKey, new DslProperty(it, serverValue))
                                                                        }
                                                                    } else {
                                                                        Object serverValue = serverValue(headerValue, matcher, headerKey)
                                                                        header(headerKey, new DslProperty(headerValue, serverValue))
                                                                    }
                                                                }
                                                            }

                                                            if(responseContract.cookies){
                                                                cookies {
                                                                    responseContract.cookies.each { responseCookie ->
                                                                        def matcher =responseContract.matchers.cookies.find { it.key == responseCookie.key }
                                                                        Object serverValue = serverCookieValue(responseCookie.value, matcher, responseCookie.key)
                                                                        cookie(responseCookie.key, new DslProperty(responseCookie.value, serverValue))
                                                                    }
                                                                }
                                                            }

                                                            if (responseContract.body) {
                                                                //todo
                                                                log.debug("-------- ${responseContract.body}")
                                                                def data = new HashMap<String,DslProperty>()
                                                                responseContract?.body?.each { entry ->
                                                                    if (responseContract.matchers?.body != null) {
                                                                        Pattern regexVal = regexifFound(responseContract.matchers.body, entry.key)
                                                                        String generateRegexBaseValue =DataGeneratorHelper.randomValueGenerator(regexVal.pattern())
                                                                        log.debug("generated random value --- ${generateRegexBaseValue}")
                                                                        if (entry.value) {
//                                                                            log.debug("----------- {} ", regexVal)
//                                                                            data.put(entry.key, value(c(entry.value), p(regex(regexVal))))
                                                                            data.put(entry.key, value(c(generateRegexBaseValue), p(regex(regexVal))))
//                                                                      data.put(entry.key, value(c(entry.value), p(entry.value)))
                                                                        } else{
                                                                            //if entry value is null an empty string
                                                                            data.put(entry.key,generateRegexBaseValue)

                                                                        }
                                                                    } else {
                                                                        data.put(entry.key, entry.value)
                                                                    }
                                                                }

                                                                //todo if example available.
                                                                String JSON_STRING = xContract.XResponseBody['default_body']
                                                                if(JSON_STRING){
                                                                    Map<String, Object> responseMap = objectMapper.readValue(JSON_STRING, new TypeReference<Map<String, Object>>(){})
//                                                                body(responseContract.body)
                                                                    println ( " $responseMap")
                                                                    //todo construct the Map<String,DslProperty>
                                                                    def data1 = JsonModelHelper.getResponseBodyDSL(responseMap,xContract.XResponseMatcher.xbody)
                                                                    body(data1)
                                                                } else{
                                                                    body(data)
                                                                }



                                                            }

                                                            if (responseContract.bodyFromFile) body(file(responseContract.bodyFromFile))

                                                            if (responseContract.async) async()
                                                            //used in producer test case to validate the value...
                                                            bodyMatchers{
                                                                responseContract.matchers?.body?.each { matcher ->
                                                                    MatchingTypeValue value = null
                                                                    switch (matcher.type) {
                                                                        case 'by_date':
                                                                            value = byDate()
                                                                            break
                                                                        case 'by_time':
                                                                            value = byTime()
                                                                            break
                                                                        case 'by_timestamp':
                                                                            value = byTimestamp()
                                                                            break
                                                                        case 'by_regex':
                                                                            String regex = matcher.value
                                                                            if (matcher.predefined) {
                                                                                regex = predefinedToPattern(YamlContract.PredefinedRegex.valueOf(matcher.predefined)).pattern()
                                                                            }
                                                                            value = byRegex(regex)
                                                                            break
                                                                        case 'by_equality':
                                                                            value = byEquality()
                                                                            break
                                                                        case 'by_type':
                                                                            value = byType() {
                                                                                if (matcher.minOccurrence != null) minOccurrence(matcher.minOccurrence)
                                                                                if (matcher.maxOccurrence != null) maxOccurrence(matcher.maxOccurrence)
                                                                            }
                                                                            break
                                                                        case 'by_command':
                                                                            value = byCommand(matcher.value)
                                                                            break
                                                                        case 'by_null':
                                                                            value = byNull()
                                                                            break
                                                                    }
                                                                    jsonPath(matcher.path, value)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                })
                    }
                }
            }
        }
        log.debug("constact json {}",jsonObjectWriter.writeValueAsString(sccContracts[0]))
        return sccContracts
    }


    private Pattern regexifFound(ArrayList data,String key){
        Pattern toBeReturned = NON_EMPTY
        data.any { keyValue ->
            if( (keyValue['path'] ==  String.format('\$.[\'%s\']',key) || keyValue['path'] ==  String.format('\$.%s',key) ) && (keyValue['type'] == 'by_regex') ) {
                log.debug("inside the condition ${keyValue.predefined}")
                if(keyValue.predefined != null){
                    YamlContract.PredefinedRegex  pdRegx = YamlContract.PredefinedRegex.valueOf(keyValue.predefined)
                    return  toBeReturned = predefinedToPattern(pdRegx)

                } else {
                    return toBeReturned = Pattern.compile(keyValue.value)
                }
            }
        }
        return toBeReturned

    }

    //only_alpha_unicode, number, any_boolean, ip_address, hostname, email, url, uuid, iso_date, iso_date_time, iso_time, iso_8601_with_offset, non_empty, non_blank

    //todo - extend from yaml converter?
    protected Pattern predefinedToPattern(YamlContract.PredefinedRegex predefinedRegex) {
        RegexPatterns patterns = new RegexPatterns()
        switch (predefinedRegex) {
            case YamlContract.PredefinedRegex.only_alpha_unicode:
                return ONLY_ALPHA_UNICODE
            case YamlContract.PredefinedRegex.number:
                return NUMBER
            case YamlContract.PredefinedRegex.any_double:
                return patterns.aDouble()
            case YamlContract.PredefinedRegex.any_boolean:
                return TRUE_OR_FALSE
            case YamlContract.PredefinedRegex.ip_address:
                return IP_ADDRESS
            case YamlContract.PredefinedRegex.hostname:
                return HOSTNAME_PATTERN
            case YamlContract.PredefinedRegex.email:
                return EMAIL
            case YamlContract.PredefinedRegex.url:
                return URL
            case YamlContract.PredefinedRegex.uuid:
                return UUID
            case YamlContract.PredefinedRegex.iso_date:
                return ANY_DATE
            case YamlContract.PredefinedRegex.iso_date_time:
                return ANY_DATE_TIME
            case YamlContract.PredefinedRegex.iso_time:
                return ANY_TIME
            case YamlContract.PredefinedRegex.iso_8601_with_offset:
                return ISO8601_WITH_OFFSET
            case YamlContract.PredefinedRegex.non_empty:
                return NON_EMPTY
            case YamlContract.PredefinedRegex.non_blank:
                return NON_BLANK
        }
    }

//todo - extend from yaml converter?
    protected Object serverValue(Object value, def matcher, String key) {
        Object serverValue = value
        if (matcher?.regex) {
            serverValue = Pattern.compile(matcher.regex)
            Pattern pattern = (Pattern) serverValue
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.predefined) {
            Pattern pattern = predefinedToPattern(matcher.predefined)
            serverValue = pattern
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.command) {
            serverValue = new ExecutionProperty(matcher.command)
        }
        return serverValue
    }

//todo - extend from yaml converter?
    protected Object serverCookieValue(Object value, def matcher, String key) {
        Object serverValue = value
        if (matcher?.regex) {
            serverValue = Pattern.compile(matcher.regex)
            Pattern pattern = (Pattern) serverValue
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.predefined) {
            Pattern pattern = predefinedToPattern(matcher.predefined)
            serverValue = pattern
            assertPatternMatched(pattern, value, key)
        }
        return serverValue
    }

//todo - extend from yaml converter?
    protected Object clientValue(Object value, YamlContract.KeyValueMatcher matcher, String key) {
        Object clientValue = value
        if (matcher?.regex) {
            clientValue = Pattern.compile(matcher.regex)
            Pattern pattern = (Pattern) clientValue
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.predefined) {
            Pattern pattern = predefinedToPattern(matcher.predefined)
            clientValue = pattern
            assertPatternMatched(pattern, value, key)
        }
        return clientValue
    }

//todo - extend from yaml converter?
    private void assertPatternMatched(Pattern pattern, value, String key) {
        boolean matches = pattern.matcher(value.toString()).matches()
        if (!matches) throw new IllegalStateException("Broken headers! A header with " +
                "key [${key}] with value [${value}] is not matched by regex [${pattern.pattern()}]")
    }
    @Override
    Collection<PathItem> convertTo(Collection<Contract> contract) {

        throw new RuntimeException("Not Implemented")

    }
}
