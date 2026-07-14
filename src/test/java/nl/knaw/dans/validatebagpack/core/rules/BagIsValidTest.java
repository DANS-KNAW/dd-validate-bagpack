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
import nl.knaw.dans.validatebagpack.config.HoleyBagsConfig;
import nl.knaw.dans.validatebagpack.core.service.BagItServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class BagIsValidTest extends AbstractTestFixture {
    private BagIsValid bagIsValid;

    @BeforeEach
    void createRule() {
        bagIsValid = new BagIsValid(new BagItServiceImpl(), new HoleyBagsConfig());
    }

    @Test
    void validate_should_return_success_for_valid_bag() throws Exception {
        var bagDir = testDir.resolve("validBag");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        var dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        var file1 = dataDir.resolve("file1.txt");
        Files.writeString(file1, "content1");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            "7e55db001d319a94b0b713529a756623  data/file1.txt\n");

        var result = bagIsValid.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }

    @Test
    void validate_should_return_success_for_holey_bag_if_allowed() throws Exception {
        var bagDir = testDir.resolve("holeyBag");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");

        var dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);

        // create a file that is "remotely" available
        var remoteFile = testDir.resolve("remoteFile.txt");
        Files.writeString(remoteFile, "content1");

        Files.writeString(bagDir.resolve("fetch.txt"), remoteFile.toUri().toURL() + " 8 data/file1.txt\n");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            "7e55db001d319a94b0b713529a756623  data/file1.txt\n");

        var holeyBagsConfig = new HoleyBagsConfig();
        holeyBagsConfig.setAllow(true);
        bagIsValid = new BagIsValid(new BagItServiceImpl(), holeyBagsConfig);
        var result = bagIsValid.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }

    @Test
    void validate_should_return_error_for_holey_bag_if_not_allowed() throws Exception {
        var bagDir = testDir.resolve("holeyBagNotAllowed");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        var dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        Files.writeString(bagDir.resolve("fetch.txt"), "http://example.com/file1.txt 8 data/file1.txt\n");
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            "7e55db001d319a94b0b713529a756623  data/file1.txt\n");

        var holeyBagsConfig = new HoleyBagsConfig();
        holeyBagsConfig.setAllow(false);
        bagIsValid = new BagIsValid(new BagItServiceImpl(), holeyBagsConfig);
        var result = bagIsValid.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).contains("Bag is not valid");
    }

    @Test
    void validate_should_return_error_for_missing_payload_directory() throws Exception {
        var bagDir = testDir.resolve("noPayload");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        Files.writeString(bagDir.resolve("manifest-md5.txt"), "");

        var result = bagIsValid.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertThat(result.getErrorMessages().get(0)).contains("Bag is not valid");
    }

    @Test
    void validate_should_return_error_for_missing_bagit_txt() throws Exception {
        var bagDir = testDir.resolve("noBagitTxt");
        Files.createDirectory(bagDir);

        var result = bagIsValid.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertThat(result.getErrorMessages().get(0)).contains("Bag is not valid");
    }

    @Test
    void validate_should_return_error_for_missing_manifest() throws Exception {
        var bagDir = testDir.resolve("noManifest");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        var dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);

        var result = bagIsValid.validate(bagDir);

        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().size()).isEqualTo(1);
        assertThat(result.getErrorMessages().get(0)).contains("Bag is not valid");
    }
}
