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
import nl.knaw.dans.validatebagpack.core.service.BagItServiceImpl;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PidMappingTargetsMustEqualsPayloadFilesTest extends AbstractTestFixture {

    private void createBagItTxt() throws IOException {
        Files.writeString(testDir.resolve("bag").resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
    }

    private void createPidMapping(String s) throws IOException {
        var metadataDir = testDir.resolve("bag").resolve("metadata");
        Files.createDirectory(metadataDir);
        Files.writeString(metadataDir.resolve("pid-mapping.txt"), s);
    }

    private Path createBagWithPidMapping(String s) throws IOException {
        Path bagDir = testDir.resolve("bag");
        Files.createDirectory(bagDir);
        createBagItTxt();
        createPidMapping(s);
        return bagDir;
    }

    @Test
    void validate_returns_ok_when_targets_and_payload_files_match() throws Exception {
        var s = "urn:1 data/file1.txt\nurn:2 data/file2.txt\n";
        var bagDir = createBagWithPidMapping(s);
        Path dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Path file1 = dataDir.resolve("file1.txt");
        Path file2 = dataDir.resolve("file2.txt");
        Files.writeString(file1, "abc");
        Files.writeString(file2, "def");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            """
                900150983cd24fb0d6963f7d28e17f72  data/file1.txt
                cb8379ac709b1b4c6e0cfc3e3b6c8e6c  data/file2.txt
                """);

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }

    @Test
    void validate_returns_error_when_pid_targets_missing_payload_file() throws Exception {
        var bagDir = createBagWithPidMapping("urn:1 data/file1.txt\nurn:2 data/file2.txt\n");
        Path dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Path file1 = dataDir.resolve("file1.txt");
        Files.writeString(file1, "abc");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            "900150983cd24fb0d6963f7d28e17f72  data/file1.txt\n");

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("Targets not in payload files");
    }

    @Test
    void validate_returns_error_when_payload_file_missing_pid_target() throws Exception {
        var bagDir = createBagWithPidMapping("urn:1 data/file1.txt\n");
        Path dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Path file1 = dataDir.resolve("file1.txt");
        Path file2 = dataDir.resolve("file2.txt");
        Files.writeString(file1, "abc");
        Files.writeString(file2, "def");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            """
                900150983cd24fb0d6963f7d28e17f72  data/file1.txt
                cb8379ac709b1b4c6e0cfc3e3b6c8e6c  data/file2.txt
                """);

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("Payload files not in targets");
    }

    @Test
    void validate_returns_error_when_pid_mapping_file_missing() throws Exception {
        Path bagDir = testDir.resolve("bag");
        Files.createDirectory(bagDir);

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("PID mapping file does not exist");
    }
}
