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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BagMustConformToDansBagPackBagitProfileTest extends AbstractTestFixture {

    static BagValidatorRule rule;

    @BeforeAll
    static void createRule() {
        var uri = URI.create("https://dans-knaw.github.io/dans-bagpack-profile/versions/dans-bagpack-profile-1.0.0.json");
        HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
        try {
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            rule = new BagMustConformToDansBagPackBagItProfile(response.body());
        }
        catch (Exception e) {
            assumeTrue(false, "Skipping tests, could not read profile: " + e.getMessage());
        }
    }

    @Test
    void validate_returnsError_when_when_bagit_txt_is_empty() throws Exception {
        Files.createFile(testDir.resolve("bagit.txt"));
        assertThatThrownBy(() -> rule.validate(testDir))
            .isInstanceOf(InvalidBagitFileFormatException.class)
            .hasMessage("bagit.txt MUST contain 'BagIt-Version' AND 'Tag-File-Character-Encoding' entries!");

    }

    @Test
    void validate_returnsOk_when_version_is_invalid() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 97
            Tag-File-Character-Encoding: UTF-8
            """);
        assertThatThrownBy(() -> rule.validate(testDir))
            .isInstanceOf(UnparsableVersionException.class)
            .hasMessage("Version must be in format MAJOR.MINOR but was [97]!");
    }

    @Test
    void validate_returnsOk_when_encoding_is_invalid() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 0.97
            Tag-File-Character-Encoding: xx
            """);
        assertThatThrownBy(() -> rule.validate(testDir))
            .isInstanceOf(UnsupportedCharsetException.class)
            .hasMessage("xx");
    }

    @Test
    void validate_returnsOk_whenBagConforms() throws Exception {
        Files.writeString(testDir.resolve("bagit.txt"), """
            BagIt-Version: 0.97
            Tag-File-Character-Encoding: UTF-8
            """);
        var result = rule.validate(testDir);
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }
}
