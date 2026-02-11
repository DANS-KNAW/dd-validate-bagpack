/*
 * Copyright (C) 2025 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.validatebagpack.core.rules;

import nl.knaw.dans.lib.util.XmlSchemaValidator;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class BagFileConformsToXmlSchemaTest extends AbstractTestFixture {

    @Test
    void validate_should_fail_on_missing_datacite_elements() throws Exception {

        var invalidXml = """
        <resource xmlns="http://datacite.org/schema/kernel-4"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-4 metadata.xsd">
             <identifier identifierType="DOI">:::</identifier>
        </resource>
        """;
        Files.writeString(testDir.resolve("datacite.xml"), invalidXml);

        var namespace = "http://datacite.org/schema/kernel-4";
        var validator = new XmlSchemaValidator(
            Map.of(namespace, Path.of("src/main/assembly/dist/cfg/datacite/v4.4/metadata.xsd").toUri())
        );
        var rule = new BagFileConformsToXmlSchema(
            "datacite.xml", new FileServiceImpl(), namespace, validator, List.of()
        );

        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).containsOnlyOnce("does not conform to");
        assertThat(result.getErrorMessages().get(0)).containsOnlyOnce("cvc-complex-type.2.4.b: The content of element 'resource' is not complete. One of");
        assertThat(result.getErrorMessages().get(0)).containsOnlyOnce("http://datacite.org/schema/kernel-4\":creators");
        assertThat(result.getErrorMessages().get(0)).containsOnlyOnce("http://datacite.org/schema/kernel-4\":title");
        // TODO note that the invalid DOI is not reported
    }

    @Test
    void validate_should_succeed_with_valid_xml() throws Exception {
        var dataciteXsd = Path.of("src/main/assembly/dist/cfg/datacite/v4.4/metadata.xsd");

        var xml = """
        <resource xmlns="http://datacite.org/schema/kernel-4"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-4 metadata.xsd">
            <identifier identifierType="DOI">10.1234/example.doi</identifier>
            <creators>
                <creator>
                    <creatorName>Smith, John</creatorName>
                </creator>
            </creators>
            <titles>
                <title>Example Title</title>
            </titles>
            <publisher>Example Publisher</publisher>
            <publicationYear>2024</publicationYear>
            <resourceType resourceTypeGeneral="Text">Dataset</resourceType>
        </resource>
        """;
        Files.writeString(testDir.resolve("datacite.xml"), xml);

        var namespace = "http://datacite.org/schema/kernel-4";
        var validator = new XmlSchemaValidator(Map.of(namespace, dataciteXsd.toUri()));
        var rule = new BagFileConformsToXmlSchema(
            "datacite.xml", new FileServiceImpl(), namespace, validator, List.of()
        );

        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }

    @Test
    void validate_should_not_report_the_ignored_pattern() throws Exception {
        var xsdPath = testDir.resolve("some.xsd");
        Files.writeString(xsdPath, """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                       targetNamespace="http://example.com/schema"
                       xmlns="http://example.com/schema"
                       elementFormDefault="qualified">
              <xs:element name="root">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="value">
                      <xs:simpleType>
                        <xs:restriction base="xs:string">
                          <xs:pattern value="10\\..+/.+"/>
                        </xs:restriction>
                      </xs:simpleType>
                    </xs:element>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """);
        Files.writeString(testDir.resolve("test.xml"), """
            <root xmlns="http://example.com/schema">
                <value>none</value>
            </root>
            """);
        var validator = new XmlSchemaValidator(Map.of("http://example.com/schema", xsdPath.toUri()));

        // test without ignore pattern

        var rule2 = new BagFileConformsToXmlSchema(
            "test.xml", new FileServiceImpl(), "http://example.com/schema", validator, List.of()
        );
        var result2 = rule2.validate(testDir);
        assertThat(result2.getErrorMessages().get(0)).contains("cvc-type.3.1.3: The value ");
        assertThat(result2.getErrorMessages().get(0)).contains("not facet-valid with respect to pattern");

        // test with ignore pattern

        var ignorePattern = Pattern.compile("(?s).*cvc-pattern-valid: Value .* is not facet-valid with respect to pattern.*");
        var rule = new BagFileConformsToXmlSchema(
            "test.xml", new FileServiceImpl(), "http://example.com/schema", validator, List.of(ignorePattern)
        );
        var result = rule.validate(testDir);
        assertThat(result.getErrorMessages().get(0)).contains("cvc-type.3.1.3: The value ");
        assertThat(result.getErrorMessages().get(0)).doesNotContain("not facet-valid with respect to pattern");
    }
}
