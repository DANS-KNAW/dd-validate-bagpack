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

import nl.knaw.dans.bagit.exceptions.InvalidBagitFileFormatException;
import nl.knaw.dans.bagit.exceptions.UnparsableVersionException;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BagMustConformToDansBagPackBagitProfileTest extends AbstractTestFixture {

    static BagValidatorRule rule;

    @BeforeAll
    static void createRule() {
        var uri = URI.create("https://dans-knaw.github.io/dans-bagpack-profile/versions/dans-bagpack-profile-1.0.0.json");
        var request = HttpRequest.newBuilder().uri(uri).build();
        try {
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            rule = new BagMustConformToDansBagPackBagItProfile(response.body());
        }
        catch (Exception e) {
            assumeTrue(false, "Skipping tests, could not read profile: " + e.getMessage());
        }
    }

    @Test
    void validate_should_throw_when_when_bagit_txt_is_empty() throws Exception {
        Files.createFile(testDir.resolve("bagit.txt"));
        assertThatThrownBy(() -> rule.validate(testDir))
            .isInstanceOf(InvalidBagitFileFormatException.class)
            .hasMessage("bagit.txt MUST contain 'BagIt-Version' AND 'Tag-File-Character-Encoding' entries!");

    }

    @Test
    void validate_should_throw_when_version_is_invalid() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 97
            Tag-File-Character-Encoding: UTF-8
            """);
        assertThatThrownBy(() -> rule.validate(testDir))
            .isInstanceOf(UnparsableVersionException.class)
            .hasMessage("Version must be in format MAJOR.MINOR but was [97]!");
    }

    @Test
    void validate_should_return_error_with_missing_bag_info_txt() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 0.97
            Tag-File-Character-Encoding: UTF-8
            """);
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo( RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages().get(0)).isEqualTo("Profile specifies metadata field [Internal-Sender-Identifier] is required but was not found!");
    }

    @Test
    void validate_should_throw_when_profile_is_missing() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 0.97
            Tag-File-Character-Encoding: UTF-8
            """);
        var ruleWithMissingProfile = new BagMustConformToDansBagPackBagItProfile(null);

        assertThatThrownBy(() -> ruleWithMissingProfile.validate(testDir))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Could not check bag against profile");
    }

    @Test
    void validate_should_return_success_with_a_bag_conform_profile() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 0.97
            Tag-File-Character-Encoding: UTF-8
            """);
        Files.writeString(testDir.resolve("bag-info.txt"), """
            Source-Organization: DANS
            Contact-Name: Employee At Dans
            Contact-Email: doeas.not.exist@dans.knaw.nl
            External-Description: test files
            Internal-Sender-Identifier: do_not_care
            """);
        Files.writeString(testDir.resolve("readme.txt"), "");
        Files.writeString(testDir.resolve("manifest-sha1.txt"), "");
        Files.writeString(testDir.resolve("tagmanifest-sha1.txt"), "");
        var metadata = testDir.resolve("metadata");
        Files.createDirectory(metadata);
        Files.writeString(metadata.resolve("datacite.xml"), "");
        Files.writeString(metadata.resolve("pid-mapping.txt"), "");
        Files.writeString(metadata.resolve("oai-ore.jsonld"), "");
        var result = rule.validate(testDir);
        assertThat(result.getStatus()).isEqualTo( RuleResult.Status.SUCCESS);
    }
}
