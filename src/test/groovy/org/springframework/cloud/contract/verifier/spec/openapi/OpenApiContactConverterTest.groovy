package org.springframework.cloud.contract.verifier.spec.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.mifmif.common.regex.Generex
import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.verifier.builder.SingleTestGenerator
import org.springframework.cloud.contract.verifier.converter.YamlContractConverter
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Created by jt on 5/24/18.
 */
class OpenApiContactConverterTest extends Specification {

    URL contractUrl = OpenApiContactConverterTest.getResource("/yml/contract.yml")
    File contractFile = new File(contractUrl.toURI())
    URL contractOA3Url = OpenApiContactConverterTest.getResource("/openapi/contract_OA3.yml")
    File contractOA3File = new File(contractOA3Url.toURI())

    URL contractOA3UrlPath = OpenApiContactConverterTest.getResource("/openapi/contract_OA3_contractPath.yml")
    File contractOA3FilePath = new File(contractOA3UrlPath.toURI())

    URL fruadApiUrl = OpenApiContactConverterTest.getResource("/openapi/openapi.yml")
    File fraudApiFile = new File(fruadApiUrl.toURI())

    URL payorApiUrl = OpenApiContactConverterTest.getResource("/openapi/payor_example.yml")
    File payorApiFile = new File(payorApiUrl.toURI())

    URL veloApiUrl = OpenApiContactConverterTest.getResource("/openapi/velooa3.yaml")
    File veloApiFile = new File(veloApiUrl.toURI())

    URL addressApiUrl = OpenApiContactConverterTest.getResource("/openapi/openapi_address.yml")
    File addressApiFile = new File(addressApiUrl.toURI())

    URL securityUrl = OpenApiContactConverterTest.getResource("/openapi/openapi_security.yaml")
    File securityApiFile = new File(securityUrl.toURI())

    OpenApiContractConverter contactConverter
    YamlContractConverter yamlContractConverter

    ObjectMapper objectMapper

    void setup() {
        contactConverter = new OpenApiContractConverter()
        yamlContractConverter = new YamlContractConverter()
        objectMapper = new ObjectMapper()
    }

    def "IsAccepted True"() {
        given:
        File file = new File('src/test/resources/openapi/openapi_petstore.yaml')
        when:

        def result = contactConverter.isAccepted(file)

        then:
        result
    }

    def "IsAccepted True 2"() {
        given:
        File file = new File('src/test/resources/openapi/openapi.yml')
        when:

        def result = contactConverter.isAccepted(file)

        then:
        result
    }

    def "IsAccepted False"() {
        given:
        File file = new File('foo')
        when:

        def result = contactConverter.isAccepted(file)

        then:
        !result

    }

    def "ConvertFrom - should not go boom"() {
        given:
        File file = new File('src/test/resources/openapi/openapi.yml')
        when:

        def result = contactConverter.convertFrom(file)

        println result

        then:
        result != null


    }


   /* def "Test Yaml Contract"() {
        given:
        Contract yamlContract = yamlContractConverter.convertFrom(contractFile).first()
        Collection<Contract> oa3Contract = contactConverter.convertFrom(contractOA3File)

        when:

        Contract openApiContract = oa3Contract.find { it.name.equalsIgnoreCase("some name") }

        then:
        openApiContract
        yamlContract.request.url == openApiContract.request.url
        yamlContract.request.method == openApiContract.request.method
        yamlContract.request.cookies == openApiContract.request.cookies
        yamlContract.request.headers == openApiContract.request.headers
        yamlContract.request.body == openApiContract.request.body
        yamlContract.request.bodyMatchers == openApiContract.request.bodyMatchers
        yamlContract.response.status == openApiContract.response.status
        yamlContract.response.headers == openApiContract.response.headers
        yamlContract.response.bodyMatchers == openApiContract.response.bodyMatchers
        yamlContract == openApiContract

    }*/

    def "test OA3 Fraud Yml"() {
        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(fraudApiFile)

        when:
        Contract contract = oa3Contract.getAt(0)

        then:
        contract
        oa3Contract.size() == 6

    }

    def "Test parse of test path"() {
        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(contractOA3FilePath)

        when:
        Contract contract = oa3Contract.getAt(0)

        then:
        contract
        contract.getRequest().url.clientValue.equals("/foo1")
    }

    def "Test Parse of Payor example contracts"() {

        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(payorApiFile)

        when:
        Contract contract = oa3Contract.getAt(0)

        then:
        contract
    }

    def "Test Parse of Velo Contracts"() {

        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(payorApiFile)
        Collection<Contract> veloContracts = contactConverter.convertFrom(veloApiFile)

        when:
        Contract contract = oa3Contract.getAt(0)
        Contract veloContract = veloContracts.getAt(0)

        then:
        contract
        contactConverter.isAccepted(veloApiFile)
    }


    def "Address contract"(){

        given:
             Collection<Contract> addressContracts = contactConverter.convertFrom(addressApiFile)

        when:
             Contract contract = addressContracts.getAt(0)
             def result = contactConverter.isAccepted(addressApiFile)
             println(result)


        then:
            result
            contract
    }


    def "Security contract"(){

        given:
        Collection<Contract> addressContracts = contactConverter.convertFrom(securityApiFile)

        when:
        Contract contract = addressContracts.getAt(0)
        def result = contactConverter.isAccepted(securityApiFile)
        println(result)


        then:
        result
        contract
    }


    def "actual complexContract"() {
        when:
        Contract contract = Contract.make {
            priority(1)
            description("Get zipCode")
            request {
                headers {
                    contentType(applicationJson())
                    header(authorization(), anyNonBlankString())
                }
                url value(consumer(regex('/v1/zipCodes/[0-9]{5}')), producer('/v1/zipCodes/55401'))
                method GET()
            }
            response {
                status 200
                body(
                        city: $(c("testCity"),p(regex(alphaNumeric()))),
                        state: $(c("testState"),p(regex(alphaNumeric()))),
                        zipCode: $(c("75056"),p(regex(number()))),
                        directShipStoreNumber: $(c("03"),p(regex(alphaNumeric()))),
                        primeDistributionCenterId: $(c("04"),p(regex(alphaNumeric()))),
                        citySuspendOrderIndicator: $(c("05"),p(regex(alphaNumeric())))
                )


                headers {
                    contentType(applicationJson())
                }
            }
        }

        then:
        println( objectMapper.writeValueAsString( contract))

    }


//    @Ignore
    def "actual security contract "() {
        given:
        Contract contract = Contract.make {
            priority(1)
            name("get access token for posted form data")
            request {
                headers {
                    header(contentType() , containing(applicationFormUrlencoded()))
//                    header(authorization(), "something.somethign")
//                    header("X-CLIENT-MODE", "something.somethign")
//                    header("X-GUEST-ACCOUNT-ID", "something.somethign")
                }
                url "/token"
                method POST()
                body([
                        grant_type:"client_credentials",
                        username:"user",
                        password:"password"

                ])
                bodyMatchers {
                    jsonPath('$.grant_type',byRegex(nonEmpty()))
                    jsonPath('$.username',byRegex(nonEmpty()))
                    jsonPath('$.password',byRegex(nonEmpty()))
                }
            }
            response {
                status 200
                body(
                        access_token: value(c("access_token"),p(regex(nonEmpty()))),
                        token_type: value(c("token_type"),p(regex(nonEmpty()))),
                        expires_in: value(c(10000),p(regex(number()))),
                        refresh_token: value(c("refresh_token"),p(regex(nonEmpty()))),
                        account_id: value(c("account_id"),p(regex(nonEmpty()))),
                        account_href: value(c("account_href"),p(regex(nonEmpty())))
                )

                headers {
                    contentType(applicationJson())
                }
            }
        }
        Collection<Contract> addressContracts = contactConverter.convertFrom(securityApiFile)

        when:
        def groovyContract =   objectMapper.writeValueAsString( contract)
        def yamlContract =  objectMapper.writeValueAsString( addressContracts[0] )

        then:
        groovyContract == yamlContract
    }


    def "validate if random regexbased value is correct"(){
        when:
        Generex generex = new Generex(/[a-zA-Z0-9]+/)
        String generateRegexBaseValue = generex.random(5,10)

        then:
        println("generated random value ---> ${generateRegexBaseValue}")

    }

}
