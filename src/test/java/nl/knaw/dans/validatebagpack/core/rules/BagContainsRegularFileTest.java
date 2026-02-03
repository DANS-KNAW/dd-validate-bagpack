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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BagContainsRegularFileTest extends AbstractTestFixture {

    @Test
    void validate_should_return_error_when_file_does_not_exist() throws Exception {
        var rule = new BagContainsRegularFile("missing.txt");
        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertEquals(List.of("Path 'missing.txt' does not exist"), result.getErrorMessages());
    }

    @Test
    void validate_should_return_error_when_path_is_a_directory() throws Exception {
        var dir = testDir.resolve("subdir");
        Files.createDirectory(dir);

        var rule = new BagContainsRegularFile("subdir");
        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertEquals(List.of("Path 'subdir' is not a regular file"), result.getErrorMessages());
    }

    @Test
    void validate_should_return_ok_when_regular_file_exists() throws Exception {
        var file = testDir.resolve("file.txt");
        Files.createFile(file);

        var rule = new BagContainsRegularFile("file.txt");
        var result = rule.validate(testDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }
}
