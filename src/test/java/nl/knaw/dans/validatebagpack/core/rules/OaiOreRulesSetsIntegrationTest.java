// src/test/java/nl/knaw/dans/validatebagpack/core/rules/OaiOreRulesRuleEngineIntegrationTest.java
package nl.knaw.dans.validatebagpack.core.rules;

import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.lib.util.ruleengine.RuleEngineImpl;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
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

    private static final String FIND_PROPS = """
        PREFIX ore: <http://www.openarchives.org/ore/terms/>
        PREFIX schemaOld: <http://schema.org/>
        PREFIX schema: <https://schema.org/>
        PREFIX dvcore: <https://dataverse.org/schema/core#>
        SELECT ?id ?restricted ?name
        WHERE {
          ?aggregation ore:aggregates ?res .
          BIND(str(?res) AS ?id)
          OPTIONAL { ?res dvcore:restricted ?restricted }
          OPTIONAL { ?res schema:name ?name }
          OPTIONAL { ?res schemaOld:name ?name }
        }
        """;
    private static final String FIND_BAG_ID = """
        PREFIX ore: <http://www.openarchives.org/ore/terms/>
        PREFIX dans: <https://dans.knaw.nl/ontologies/relations#>
        SELECT ?resource ?bagId WHERE {
          ?resource a ore:Aggregation .
          OPTIONAL { ?resource dans:hasDansBagId ?bagId }
        }
        """;

    private RuleEngineImpl ruleEngine;
    private List<NumberedRule> rules_2_4;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(Map.ofEntries(
                namedSparqlQuery("findAggregatedResourceProps", FIND_PROPS),
                namedSparqlQuery("findBagId", FIND_BAG_ID)
            )
        );
        var ruleSets = new RuleSets(null, null, "profile", fileService);
        rules_2_4 = ruleSets.getCommonRules().stream()
            .filter(r1 -> r1.getNumber().startsWith("2.4" ))
            .map(r -> r.getNumber().equals("2.4(a)" )
                ? new NumberedRule(r.getNumber(), r.getRule())
                : r
            ).toList();

        ruleEngine = new RuleEngineImpl();
    }

    private Map.Entry<String, Path> namedSparqlQuery(String name, String query) throws Exception {
        var sparqlFile = testDir.resolve(name + "sparql" );
        Files.writeString(sparqlFile, query);
        return Map.entry(name, sparqlFile);
    }

    public record TestCase(
        List<String> errorMessages, String oaiOreContent
    ) {
    }

    static Stream<TestCase> oaiOreTestCases() {
        return Stream.of(
            new TestCase(
                // note the difference with the unit test: the error message contains the file path here
                Arrays.asList("""
                    File is not valid JSON-LD: 'target/test/OaiOreRulesSetsIntegrationTest/bag/metadata/oai-ore.jsonld'. Error: Unexpected character ('i' (code 105)): was expecting double-quote to start field name
                     at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 3]""", null, null
                ), "{ invalid json }"
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
                    "dans": "https://dans.knaw.nl/ontologies/relations#"
                  },
                  "@graph": [
                    {
                      "@id": "urn:agg:1",
                      "@type": "ore:Aggregation",
                      "dans:hasDansBagId": "not-a-urn:uuid"
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
                    "dans": "https://dans.knaw.nl/ontologies/relations#"
                  },
                  "@graph": [
                    {
                      "@id": "urn:agg:1",
                      "@type": "ore:Aggregation",
                      "dans:hasDansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                    }
                  ]
                }
                """
            )
        );
    }

    @ParameterizedTest
    @MethodSource("oaiOreTestCases" )
    void validateBag_with_2_4_rules(TestCase testCase) throws Exception {
        var bagDir = testDir.resolve("bag" );
        Files.createDirectories(bagDir.resolve("metadata" ));

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
                assertThat(actual.split("\n" )).as("Rule %s", ruleNr)
                    .hasSameElementsAs(Arrays.asList(expected.split("\n" )));
            }
        }
    }
}
