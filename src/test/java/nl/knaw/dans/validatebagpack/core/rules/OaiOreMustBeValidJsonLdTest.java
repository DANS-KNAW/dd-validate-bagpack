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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OaiOreMustBeValidJsonLdTest extends AbstractTestFixture {

    @Test
    void validate_returns_error_when_oai_ore_file_missing() throws Exception {
        var rule = new OaiOreMustBeValidJsonLd();
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessages().get(0).contains("File does not exist"));
    }

    @Test
    void validate_returns_error_when_oai_ore_file_is_invalid_json() throws Exception {
        Path oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, "{ invalid json }");

        var rule = new OaiOreMustBeValidJsonLd();
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessages().get(0).contains("File is not valid JSON-LD"));
    }

    @Test
    void validate_returns_ok_when_oai_ore_file_is_valid_jsonld() throws Exception {
        Path oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        // Minimal valid JSON-LD
        String validJsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "Dataset",
              "name": "Test dataset"
            }
            """;
        Files.writeString(oaiOre, validJsonLd);

        var rule = new OaiOreMustBeValidJsonLd();
        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }
}
