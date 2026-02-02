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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import org.assertj.core.api.AssertionsForClassTypes;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OaiOreAggregatedResourcesMustHaveRequiredPropsTest extends AbstractTestFixture {
    private ListAppender<ILoggingEvent> listAppender;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream errContent;

    @BeforeEach
    public void setup() throws Exception {
        super.setUp();
        errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void tearDown() {
        System.setErr(originalErr);
    }

    @Test
    void validate_should_return_error_when_oai_ore_file_missing() throws Exception {
        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(new FileServiceImpl());
        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertThat(result.getErrorMessages().get(0)).startsWith(
            "OAI-ORE JSON-LD file not found at expected location: " + testDir.resolve("metadata/oai-ore.jsonld")
        );
    }

    @Test
    void validate_should_return_error_when_missing_required_props() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "schema": "https://schema.org/",
                "dvcore": "https://dataverse.org/schema/core#"
              },
              "@type": "ore:ResourceMap",
              "ore:aggregates": [ {
                "@id": "urn:example:xx"
              } ]
            }
            """);

        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(getFileService());
        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(2);
        assertThat(result.getErrorMessages()).hasSameElementsAs(List.of(
            "(ii) Aggregated resource has missing 'name' property",
            "(iii) Aggregated resource has missing 'restricted' property")
        );
    }

    @Test
    void validate_ignores_aggregate_when_id_is_blank() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "schema": "https://schema.org/",
                "dvcore": "https://dataverse.org/schema/core#"
              },
              "@type": "ore:ResourceMap",
              "ore:aggregates": [ {
                    "@id": "   ",
                    "schema:name": "Example Resource",
                    "dvcore:restricted": false
              } ]
            }
            """);
        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(getFileService());

        var result = rule.validate(testDir);

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());

        // Check the error output
        assertThat(errContent.toString()).contains("WARNING: Non well-formed subject [   ] has been skipped.");
    }

    @Test
    void validate_should_throw_when_restricted_is_not_boolean() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
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
            """);
        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(getFileService());

        var result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertThat(result.getErrorMessages()).hasSameElementsAs(List.of(
            "(iii) Aggregated resource 'restricted' property is not a boolean",
            "(iii) Aggregated resource 'restricted' property is not a literal")
        );
    }

    @Test
    void validate_should_return_ok_when_all_required_props_present_and_valid() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());

        Files.writeString(oaiOre, """
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
                    "dvcore:restricted": false
                }
              ]
            }
            """);

        var fileService = getFileService();
        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(fileService);
        var result = rule.validate(testDir);

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    private @NonNull FileServiceImpl getFileService() throws IOException {
        var fileService = new FileServiceImpl();
        var sparqlFile = testDir.resolve("findAggregatedResourceProps.sparql");
        Files.writeString(sparqlFile, """
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
            """);
        fileService.loadNamedSparqlQueries(Map.of("findAggregatedResourceProps", sparqlFile));
        return fileService;
    }
}
