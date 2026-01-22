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

import static org.junit.jupiter.api.Assertions.assertEquals;

class BagMustContainWellformedPidMappingTest extends AbstractTestFixture {

    @Test
    void should_return_ok_when_pid_mapping_is_valid() throws Exception {
        Path pidMapping = testDir.resolve(Constants.PID_MAPPING_PATH);
        Files.createDirectories(pidMapping.getParent());
        Files.writeString(pidMapping, "1234 urn:nbn:nl:ui:13-1234\n");
        BagMustContainWellformedPidMapping rule = new BagMustContainWellformedPidMapping();

        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_error_when_pid_mapping_is_missing() throws Exception {
        BagMustContainWellformedPidMapping rule = new BagMustContainWellformedPidMapping();

        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertEquals(List.of("PID mapping file is missing: metadata/pid-mapping.txt"), result.getErrorMessages());
    }

    @Test
    void returns_ok_when_pid_mapping_is_empty() throws Exception {
        Path pidMapping = testDir.resolve(Constants.PID_MAPPING_PATH);
        Files.createDirectories(pidMapping.getParent());
        Files.writeString(pidMapping, "");
        BagMustContainWellformedPidMapping rule = new BagMustContainWellformedPidMapping();

        RuleResult result = rule.validate(testDir);

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }
}
