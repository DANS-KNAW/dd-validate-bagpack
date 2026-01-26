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
        var bagItTxtPath = testDir.resolve("bag").resolve("bagit.txt");
        Files.writeString(bagItTxtPath, """
            BagIt-Version: 0.97
            Tag-File-Character-Encoding: UTF-8
            """);
    }

    private void createPidMapping(String pidMappingFileContent) throws IOException {
        var metadataDir = testDir.resolve("bag").resolve("metadata");
        Files.createDirectory(metadataDir);
        Files.writeString(metadataDir.resolve("pid-mapping.txt"), pidMappingFileContent);
    }

    private Path createBagWithPidMapping(String pidMappingFileContent) throws IOException {
        Path bagDir = testDir.resolve("bag");
        Files.createDirectory(bagDir);
        createBagItTxt();
        createPidMapping(pidMappingFileContent);
        return bagDir;
    }

    @Test
    void validate_returns_ok_when_targets_and_payload_files_match() throws Exception {
        var bagDir = createBagWithPidMapping("""
            urn:1 data/file1.txt
            urn:2 data/file2.txt
            """);
        Path dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Files.writeString(dataDir.resolve("file1.txt"), "abc");
        Files.writeString(dataDir.resolve("file2.txt"), "def");
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
        var bagDir = createBagWithPidMapping("""
            urn:1 data/file1.txt
            urn:2 data/file2.txt
            """);
        Path dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Files.writeString(dataDir.resolve("file1.txt"), "abc");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            "900150983cd24fb0d6963f7d28e17f72  data/file1.txt");

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0))
            .isEqualTo("PID mapping targets and payload files do not match. Targets not in payload files: [data/file2.txt]");
    }

    @Test
    void validate_returns_error_when_payload_file_missing_pid_target() throws Exception {
        var bagDir = createBagWithPidMapping("urn:1 data/file1.txt");
        Path dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Files.writeString(dataDir.resolve("file1.txt"), "abc");
        Files.writeString(dataDir.resolve("file2.txt"), "def");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            """
                900150983cd24fb0d6963f7d28e17f72  data/file1.txt
                cb8379ac709b1b4c6e0cfc3e3b6c8e6c  data/file2.txt
                """);

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0))
            .isEqualTo("PID mapping targets and payload files do not match. Payload files not in targets: [data/file2.txt]");
    }

    @Test
    void validate_returns_error_when_pid_mapping_file_missing() throws Exception {
        Path bagDir = testDir.resolve("bag");
        Files.createDirectory(bagDir);

        var rule = new PidMappingTargetsMustEqualsPayloadFiles(
            new FileServiceImpl(), new BagItServiceImpl());
        RuleResult result = rule.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0))
            .startsWith("PID mapping file does not exist: ");
    }
}
