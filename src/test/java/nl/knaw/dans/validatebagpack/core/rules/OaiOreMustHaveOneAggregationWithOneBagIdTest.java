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

import java.nio.file.Files;
import java.util.Map;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class OaiOreMustHaveOneAggregationWithOneBagIdTest extends AbstractTestFixture {

    private @NonNull FileServiceImpl getFileService() throws Exception {
        var sparqlFile = testDir.resolve("findBagId.sparql");
        Files.writeString(sparqlFile, """
            PREFIX ore: <http://www.openarchives.org/ore/terms/>
            PREFIX dans: <https://dans.knaw.nl/ontologies/relations#>
            SELECT ?resource ?bagId WHERE {
              ?resource a ore:Aggregation .
              OPTIONAL { ?resource dans:hasDansBagId ?bagId }
            }
            """);
        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(Map.of("findBagId", sparqlFile));
        return fileService;
    }

    @Test
    void validate_returnsError_whenOaiOreFileMissing() throws Exception {
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("OAI-ORE JSON-LD file not found");
    }

    @Test
    void validate_returnsError_whenNoAggregation() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/"
              },
              "@type": "ResourceMap"
            }
            """);
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("Expected exactly one 'ore:Aggregation'");
    }

    @Test
    void validate_returnsError_whenMultipleAggregations() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "dans": "https://dans.knaw.nl/ontologies/relations#"
              },
              "@graph": [
                { "@id": "urn:agg:1", "@type": "ore:Aggregation", "dans:hasDansBagId": "urn:uuid:1234" },
                { "@id": "urn:agg:2", "@type": "ore:Aggregation", "dans:hasDansBagId": "urn:uuid:5678" }
              ]
            }
            """);
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("Expected exactly one 'ore:Aggregation'");
    }

    @Test
    void validate_returnsError_whenNoBagId() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/"
              },
              "@graph": [
                { "@id": "urn:agg:1", "@type": "ore:Aggregation" }
              ]
            }
            """);
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("Expected exactly one 'dansBagId'");
    }

    @Test
    void validate_returnsError_whenBagIdIsInvalidUrnUuid() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "dans": "https://dans.knaw.nl/ontologies/relations#"
              },
              "@graph": [
                { "@id": "urn:agg:1", "@type": "ore:Aggregation", "dans:hasDansBagId": "not-a-urn:uuid" }
              ]
            }
            """);
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("not a valid URN:UUID");
    }

    @Test
    void validate_returnsOk_whenValid() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "dans": "https://dans.knaw.nl/ontologies/relations#"
              },
              "@graph": [
                { "@id": "urn:agg:1", "@type": "ore:Aggregation", "dans:hasDansBagId": "urn:uuid:123e4567-e89b-12d3-a456-426614174000" }
              ]
            }
            """);
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }
}
