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

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OaiOreMustHaveOneAggregationWithOneBagIdTest extends AbstractTestFixture {

    private @NonNull FileServiceImpl getFileService() throws Exception {
        var fileService = new FileServiceImpl();
        fileService.loadNamedSparqlQueries(Map.of(
                "findBagId",
                Path.of("src/main/assembly/dist/cfg/find-bagId.sparql" )
            )
        );
        return fileService;
    }

    @Test
    void validate_should_return_error_when_OaiOre_file_is_missing() throws Exception {
        var rule = new OaiOreMustHaveOneAggregationWithOneBagId(getFileService());
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("OAI-ORE JSON-LD file not found" );
    }
}
