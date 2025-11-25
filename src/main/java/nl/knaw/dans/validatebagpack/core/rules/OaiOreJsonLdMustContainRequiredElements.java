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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.core.oaiore.OaiOreMetadataReader;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
@Slf4j
public class OaiOreJsonLdMustContainRequiredElements implements BagValidatorRule {
    private final OaiOreMetadataReader oaiOreMetadataReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var oaiOrePath = path.resolve(Constants.OAI_ORE_PATH);
        var s = Files.readString(oaiOrePath, StandardCharsets.UTF_8);
        var m = oaiOreMetadataReader.readMetadata(s);

        var bagIdString = m.getBagId();
        if (bagIdString == null) {
            return RuleResult.error("OAI-ORE JSON-LD is missing required element: bagId");
        }
        else {
            return checkBagId(bagIdString);
        }
    }

    private RuleResult checkBagId(String bagId) {
        if (bagId == null) {
            return RuleResult.error("OAI-ORE JSON-LD is missing required element: bagId");
        }
        else {
            try {
                var bagIdUri = URI.create(bagId);
                if (!bagIdUri.getScheme().equals("urn")) {
                    return RuleResult.error("OAI-ORE JSON-LD has invalid URI scheme for bagId: " + bagId);
                }
                if (!bagIdUri.getSchemeSpecificPart().startsWith("uuid:")) {
                    return RuleResult.error("OAI-ORE JSON-LD has invalid bagId, expected urn:uuid:<UUID>: " + bagId);
                }
                var uuidPart = bagIdUri.getSchemeSpecificPart().substring("uuid:".length());
                try {
                    java.util.UUID.fromString(uuidPart);
                }
                catch (IllegalArgumentException e) {
                    return RuleResult.error("OAI-ORE JSON-LD has invalid UUID in bagId: " + bagId);
                }
            }
            catch (IllegalArgumentException e) {
                return RuleResult.error("OAI-ORE JSON-LD has invalid URI for bagId: " + bagId);
            }
        }
        return RuleResult.ok();
    }
}
