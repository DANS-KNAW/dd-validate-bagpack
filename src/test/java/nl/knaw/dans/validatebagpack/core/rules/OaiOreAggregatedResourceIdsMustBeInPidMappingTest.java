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

import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static nl.knaw.dans.validatebagpack.core.rules.Constants.PID_MAPPING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class OaiOreAggregatedResourceIdsMustBeInPidMappingTest extends AbstractTestFixture {

    private @NonNull FileServiceImpl getFileService() throws IOException {
        var sparqlFile = testDir.resolve("findAggregatedResourceIds.sparql");
        Files.writeString(sparqlFile, """
            PREFIX ore: <http://www.openarchives.org/ore/terms/>
            SELECT ?id WHERE {
              ?aggregation ore:aggregates ?res .
              BIND(str(?res) AS ?id)
            }
            """);

        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(Map.of("findAggregatedResourceIds", sparqlFile));
        return fileService;
    }

    @Test
    void validate_should_return_error_when_aggregated_resource_id_not_in_pid_mapping() throws Exception {
        // Write minimal OAI-ORE file with one aggregated resource
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());

        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "schema": "https://schema.org/"
              },
              "@type": "ResourceMap",
              "ore:aggregates": [ { "@id": "urn:example:missing" } ],
              "@graph": [
                {
                  "@id": "urn:example:missing",
                  "schema:name": "Valid Name"
                }
              ]
            }
            """);

        // Write empty pid-mapping.txt
        var pidMapping = testDir.resolve(PID_MAPPING_PATH);
        Files.createDirectories(pidMapping.getParent());
        Files.writeString(pidMapping, "");
        var rule = new OaiOreAggregatedResourceIdsMustBeInPidMapping(getFileService());

        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages()).anyMatch(msg ->
            msg.contains("Aggregated resource ID 'urn:example:missing' not found in PID mapping")
        );
    }

    @Test
    void validate__should_return_success_when_all_aggregated_resource_ids_in_pid_mapping() throws Exception {
        // Write OAI-ORE file with one aggregated resource
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        var jsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "ResourceMap",
              "ore:aggregates": [
                { "id": "urn:uuid:1234" }
              ]
            }
            """;
        Files.writeString(oaiOre, jsonLd);

        // Write pid-mapping.txt with matching ID
        var pidMapping = testDir.resolve(PID_MAPPING_PATH);
        Files.createDirectories(pidMapping.getParent());
        Files.writeString(pidMapping, "urn:uuid:1234 urn:nbn:nl:ui:13-1234\n");

        var rule = new OaiOreAggregatedResourceIdsMustBeInPidMapping(getFileService());

        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }

    @Test
    void validate_should_return_success_when_no_aggregated_resources() throws Exception {
        // Write OAI-ORE file with no aggregates
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        var jsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "ResourceMap"
            }
            """;
        Files.writeString(oaiOre, jsonLd);

        // Write empty pid-mapping.txt
        var pidMapping = testDir.resolve(PID_MAPPING_PATH);
        Files.createDirectories(pidMapping.getParent());
        Files.writeString(pidMapping, "");

        // Provide required SPARQL query
        var fileService = getFileService();
        var rule = new OaiOreAggregatedResourceIdsMustBeInPidMapping(fileService);

        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }
}
