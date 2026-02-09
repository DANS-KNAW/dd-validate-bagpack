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
// src/test/java/nl/knaw/dans/validatebagpack/core/rules/OaiOreRulesRuleEngineIntegrationTest.java
package nl.knaw.dans.validatebagpack.core.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.lib.util.ruleengine.RuleEngineImpl;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import nl.knaw.dans.validatebagpack.config.ValidationConfig;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class OaiOreRulesSetsIntegrationTest extends AbstractTestFixture {

    private RuleEngineImpl ruleEngine;
    private List<NumberedRule> rules_2_4;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        var configDir = Path.of("src/main/assembly/dist/cfg/");
        var mapper = new ObjectMapper(new YAMLFactory());
        var validationConfig = mapper.readTree(Files.readString(configDir.resolve("config.yml"))).get("validation");
        var namedQueries = mapper.treeToValue(validationConfig, ValidationConfig.class)
            .getSparqlQueries().entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> configDir.resolve(e.getValue().getFileName())
            ));

        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(namedQueries);
        var ruleSets = new RuleSets(null, null, "profile", fileService);
        ruleEngine = new RuleEngineImpl();
        rules_2_4 = ruleSets.getCommonRules().stream()
            .filter(r1 -> r1.getNumber().startsWith("2.4"))
            .map(r -> r.getNumber().equals("2.4(a)")
                ? new NumberedRule(r.getNumber(), r.getRule()) // strip dependency from first tested rule
                : r // with dependencies
            ).toList();
    }

    public record TestCase(List<String> errorMessages, String oaiOreContent) {
    }

    static Stream<TestCase> oaiOreTestCases() {
        return Stream.of(
            new TestCase(
                // note the difference with the unit test: the error message contains the file path here
                Arrays.asList("""
                        File is not valid JSON-LD: 'target/test/OaiOreRulesSetsIntegrationTest/bag/metadata/oai-ore.jsonld'. Error: Unexpected character ('i' (code 105)): was expecting double-quote to start field name
                         at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 3]""",
                    null,
                    null
                ), "{ invalid json }"
            ),
            new TestCase(
                Arrays.asList(
                    "File is not valid JSON-LD: 'target/test/OaiOreRulesSetsIntegrationTest/bag/metadata/oai-ore.jsonld'. Error: invalid local context: 123",
                    null,
                    null
                ), // valid json but Invalid JSON-LD
                """
                { "@context": 123 }
                """
            ),
            new TestCase(
                Arrays.asList(
                    null,
                    """
                        Jena parser logged warnings/errors while reading JSON-LD file target/test/OaiOreRulesSetsIntegrationTest/bag/metadata/oai-ore.jsonld:
                        WARNING: Non well-formed subject [urn:example:invalid iri with spaces] has been skipped.""",
                    null
                ), // causes "has been skipped" on stderr
                """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/"
                  },
                  "@type": "ore:ResourceMap",
                  "ore:aggregates": [
                    { "@id": "urn:example:invalid iri with spaces" }
                  ]
                }
                """
            ),
            new TestCase(
                Arrays.asList(
                    null,
                    null,
                    null
                ), // duplicate key (identical @id and BagId), silently the last one is used
                """
                    {
                      "@context": {
                        "ore": "http://www.openarchives.org/ore/terms/",
                        "vaultMd": "https://schemas.dans.knaw.nl/metadatablock/dansDataVaultMetadata#"
                      },
                      "@graph": [
                        { "@id": "urn:agg:1",
                          "@type": "ore:Aggregation",
                          "dvcore:restricted": false,
                           "dans:hasDansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                        },
                        {
                          "@id": "urn:agg:1",
                          "@type": "ore:Aggregation",
                          "dvcore:restricted": true,
                           "vaultMd:dansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                        }
                      ]
                    }
                    """
            ),
            new TestCase(
                Arrays.asList(null, "Expected exactly one 'ore:Aggregation' resource, but found 0", null
                ), """
                {
                  "@context": { "ore": "http://www.openarchives.org/ore/terms/" },
                  "@type": "ore:ResourceMap"
                }
                """
            ),
            new TestCase(
                // note that the multiple lines of the unit test are concatenated into a single string here,
                Arrays.asList(null,
                    "Expected exactly one 'ore:Aggregation' resource, but found 0",
                    """
                        (ii) Aggregated resource has missing 'name' property
                        (iii) Aggregated resource has missing 'restricted' property"""
                ), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "schema": "https://schema.org/",
                    "dvcore": "https://dataverse.org/schema/core#"
                  },
                  "@type": "ore:ResourceMap",
                  "ore:aggregates": [ { "@id": "urn:example:xx" } ]
                }
                """
            ),
            new TestCase(
                // note that the multiple lines of the unit test are concatenated into a single string here,
                Arrays.asList(null,
                    "Expected exactly one 'ore:Aggregation' resource, but found 0",
                    """
                        (iii) Aggregated resource 'restricted' property is not a literal
                        (iii) Aggregated resource 'restricted' property is not a boolean"""
                ), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "schema": "https://schema.org/",
                    "dvcore": "https://dataverse.org/schema/core#"
                  },
                  "@type": "ore:ResourceMap",
                  "ore:aggregates": [ {
                        "@id": "urn:example:xx",
                        "schema:name": "Example Resource",
                        "dvcore:restricted": "not a boolean"
                  },
                  {
                        "@id": "urn:example:yy",
                        "schema:name": "Example Resource",
                        "dvcore:restricted": {}
                  } ]
                }
                """
            ),
            new TestCase(
                Arrays.asList(null, "Expected exactly one 'ore:Aggregation' resource, but found 2", null), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "dans": "https://dans.knaw.nl/ontologies/relations#"
                  },
                  "@graph": [
                    { "@id": "urn:agg:1",
                      "@type": "ore:Aggregation",
                       "dans:hasDansBagId": "urn:uuid:1234"
                    },
                    {
                      "@id": "urn:agg:2",
                      "@type": "ore:Aggregation",
                       "dans:hasDansBagId": "urn:uuid:5678"
                    }
                  ]
                }
                """
            ),
            new TestCase(
                Arrays.asList(null, "Expected exactly one 'ore:Aggregation' resource, but found 0", null
                ), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "schema": "https://schema.org/",
                    "dvcore": "https://dataverse.org/schema/core#"
                  },
                  "@type": "ore:ResourceMap",
                  "ore:aggregates": [ {
                        "@id": "urn:example:xx",
                        "schema:name": "Example Resource",
                        "dvcore:restricted": false,
                        "dans:hasDansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                    }
                  ]
                }
                """
            ),
            new TestCase(
                Arrays.asList(null, "Expected exactly one 'dansBagId' property on the aggregation, but found 0", null
                ), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "schema": "https://schema.org/",
                    "dvcore": "https://dataverse.org/schema/core#"
                  },
                  "@type": [
                      "ore:Aggregation",
                      "schema:Dataset"
                  ],
                  "ore:aggregates": [ {
                        "@id": "urn:example:xx",
                        "schema:name": "Example Resource",
                        "dvcore:restricted": false,
                        "dans:hasDansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                    }
                  ]
                }
                """
            ),
            new TestCase(
                Arrays.asList(null, "Value of 'dansBagId' is not a valid URN:UUID: not-a-urn:uuid", null
                ), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "vaultMd": "https://schemas.dans.knaw.nl/metadatablock/dansDataVaultMetadata#"
                  },
                  "@graph": [
                    {
                      "@id": "urn:agg:1",
                      "@type": "ore:Aggregation",
                      "vaultMd:dansBagId": "not-a-urn:uuid"
                    }
                  ]
                }
                """
            ),
            new TestCase(
                Arrays.asList(null, null, null), """
                {
                  "@context": {
                    "ore": "http://www.openarchives.org/ore/terms/",
                    "vaultMd": "https://schemas.dans.knaw.nl/metadatablock/dansDataVaultMetadata#"
                  },
                  "@graph": [
                    {
                      "@id": "urn:agg:1",
                      "@type": "ore:Aggregation",
                      "vaultMd:dansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                    }
                  ]
                }
                """
            )
        );
    }

    @ParameterizedTest
    @MethodSource("oaiOreTestCases")
    void validateBag_with_2_4_rules(TestCase testCase) throws Exception {
        var bagDir = testDir.resolve("bag");
        Files.createDirectories(bagDir.resolve("metadata"));

        // Write OAI-ORE file for this test case
        var oaiOreFile = bagDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOreFile.getParent());
        Files.writeString(oaiOreFile, testCase.oaiOreContent());

        // Act
        var results = ruleEngine.validateBag(bagDir, rules_2_4);

        // Assert
        for (var i = 0; i < testCase.errorMessages.size(); i++) {
            var expected = testCase.errorMessages.get(i);
            var actual = results.get(i).getErrorMessage();
            var ruleNr = rules_2_4.get(i).getNumber();
            if (expected == null) {
                assertThat(actual).as("Rule %s", ruleNr)
                    .isEqualTo(expected);
            }
            else {
                assertThat(actual.split("\n")).as("Rule %s", ruleNr)
                    .hasSameElementsAs(Arrays.asList(expected.split("\n")));
            }
        }
    }
}
