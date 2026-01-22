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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OaiOreAggregatedResourcesMustHaveRequiredPropsTest extends AbstractTestFixture {

    @Test
    void validate_returns_error_when_oai_ore_file_missing() throws Exception {
        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(new FileServiceImpl());
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessages().get(0).contains("OAI-ORE JSON-LD file not found"));
    }

    @Disabled("No query found with name: findAggregatedResourceProps")
    @Test
    void validate_returns_error_when_aggregated_resource_missing_required_props() throws Exception {
        Path oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        // Aggregated resource missing 'name' and 'restricted'
        String jsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "ResourceMap",
              "ore:aggregates": [
                {
                  "id": "urn:uuid:1234"
                }
              ]
            }
            """;
        Files.writeString(oaiOre, jsonLd);

        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(new FileServiceImpl());
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("missing 'name'")));
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("missing 'restricted'")));
    }

    @Disabled("No query found with name: findAggregatedResourceProps")
    @Test
    void validate_returns_error_when_id_is_invalid_uri() throws Exception {
        Path oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        String jsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "ResourceMap",
              "ore:aggregates": [
                {
                  "id": "not a uri",
                  "name": "Test",
                  "restricted": false
                }
              ]
            }
            """;
        Files.writeString(oaiOre, jsonLd);

        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(new FileServiceImpl());
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("invalid 'id'")));
    }

    @Disabled("No query found with name: findAggregatedResourceProps")
    @Test
    void validate_returns_ok_when_all_required_props_present_and_valid() throws Exception {
        Path oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        String jsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "ResourceMap",
              "ore:aggregates": [
                {
                  "id": "urn:uuid:1234",
                  "name": "Test",
                  "restricted": false
                }
              ]
            }
            """;
        Files.writeString(oaiOre, jsonLd);

        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(new FileServiceImpl());
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }
}
