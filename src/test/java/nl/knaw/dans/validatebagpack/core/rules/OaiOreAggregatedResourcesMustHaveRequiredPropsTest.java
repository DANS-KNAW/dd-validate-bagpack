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
import java.util.List;
import java.util.Map;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class OaiOreAggregatedResourcesMustHaveRequiredPropsTest extends AbstractTestFixture {

    @Test
    void validate_should_return_error_when_oai_ore_file_missing() throws Exception {
        var rule = new OaiOreAggregatedResourcesMustHaveRequiredProps(new FileServiceImpl());
        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertThat(result.getErrorMessages().get(0)).startsWith(
            "OAI-ORE JSON-LD file not found at expected location: " + testDir.resolve("metadata/oai-ore.jsonld" )
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
            "(iii) Aggregated resource has missing 'restricted' property" )
        );
    }


    @Test
    void validate_should_not_throw() throws Exception {
        var oaiOre = testDir.resolve(OAI_ORE_PATH);
        Files.createDirectories(oaiOre.getParent());
        Files.writeString(oaiOre, """
            {
              "@context": {
                "ore": "http://www.openarchives.org/ore/terms/",
                "schema": "https://schema.org/",
                "dvcore": "<https://dataverse.org/schema/core#>"
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
        assertThat(result.getErrorMessages()).hasSameElementsAs(List.of(
            "Jena parser threw RiotException while reading JSON-LD file target/test/OaiOreAggregatedResourcesMustHaveRequiredPropsTest/metadata/oai-ore.jsonld: JsonLdError[code=A local context contains a term that has an invalid or missing IRI mapping [code=INVALID_IRI_MAPPING]., message=A local context contains a term that has an invalid or missing IRI mapping [code=INVALID_IRI_MAPPING].]" )
        );
    }

    private @NonNull FileServiceImpl getFileService() throws IOException {
        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(Map.of(
            "findAggregatedResourceProps",
            Path.of("src/main/assembly/dist/cfg/find-aggregated-resource-props.sparql")
        ));
        return fileService;
    }
}
