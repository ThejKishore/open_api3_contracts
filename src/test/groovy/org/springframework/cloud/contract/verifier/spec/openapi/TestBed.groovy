package org.springframework.cloud.contract.verifier.spec.openapi

import org.apache.commons.io.FileUtils
import org.springframework.cloud.contract.verifier.spec.openapi.model.XMatcherDetails
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

class TestBed extends Specification {

    def "parse yml to class object "(){
        when:
            URL petstoreUrl = TestBed.class.getResource("/openapi/access_token_body_matcher.yml")
            File petstoreFile = new File(petstoreUrl.toURI())
            String yamlString = FileUtils.readFileToString(petstoreFile)
            Yaml yml = new Yaml()
            List< XMatcherDetails> xMatcherDetails = yml.load(yamlString)

        then:
            xMatcherDetails?.each { xmatch -> println(xmatch) }


    }

}
