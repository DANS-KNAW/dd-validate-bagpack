// src/test/java/nl/knaw/dans/validatebagpack/core/rules/OaiOreRulesRuleEngineIntegrationTest.java
package nl.knaw.dans.validatebagpack.core.rules;

import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.lib.util.ruleengine.RuleEngineImpl;
import nl.knaw.dans.lib.util.ruleengine.RuleValidationResult;
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

class OaiOreRulesIntegrationTest extends AbstractTestFixture {

    private RuleEngineImpl ruleEngine;
    List<NumberedRule> rules_2_4;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        var findProps = """
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
        var findBagId = """
            PREFIX ore: <http://www.openarchives.org/ore/terms/>
            PREFIX dans: <https://dans.knaw.nl/ontologies/relations#>
            SELECT ?resource ?bagId WHERE {
              ?resource a ore:Aggregation .
              OPTIONAL { ?resource dans:hasDansBagId ?bagId }
            }
            """;
        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(Map.ofEntries(
                namedSparqlQuery("findAggregatedResourceProps", findProps),
                namedSparqlQuery("findBagId", findBagId)
            )
        );
        var ruleSets = new RuleSets(null, null, "profile", fileService);
        rules_2_4 = ruleSets.getCommonRules().stream()
            .filter(r1 -> r1.getNumber().startsWith("2.4"))
            .map(r -> r.getNumber().equals("2.4(a)")
                ? new NumberedRule(r.getNumber(), r.getRule())
                : r
            ).toList();

        ruleEngine = new RuleEngineImpl();
    }

    private Map.Entry<String, Path> namedSparqlQuery(String name, String query) throws Exception {
        var sparqlFile = testDir.resolve(name + "sparql");
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
                // 2.4(a) valid
                Arrays.asList(null, "Expected exactly one 'ore:Aggregation' resource, but found 0", null
                ), """
                {
                  "@context": { "ore": "http://www.openarchives.org/ore/terms/" },
                  "@type": "ore:ResourceMap"
                }
                """
            ),
            new TestCase(
                // 2.4(c) invalid
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
                // all valid
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
                // all valid
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
        assertThat(results.stream().map(RuleValidationResult::getErrorMessage))
            .containsExactlyElementsOf(testCase.errorMessages);
    }
}
