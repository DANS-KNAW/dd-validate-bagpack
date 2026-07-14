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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.bagit.exceptions.CorruptChecksumException;
import nl.knaw.dans.bagit.exceptions.FileNotInManifestException;
import nl.knaw.dans.bagit.exceptions.FileNotInPayloadDirectoryException;
import nl.knaw.dans.bagit.exceptions.InvalidBagitFileFormatException;
import nl.knaw.dans.bagit.exceptions.MissingBagitFileException;
import nl.knaw.dans.bagit.exceptions.MissingPayloadDirectoryException;
import nl.knaw.dans.bagit.exceptions.MissingPayloadManifestException;
import nl.knaw.dans.bagit.exceptions.VerificationException;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.config.HoleyBagsConfig;
import nl.knaw.dans.validatebagpack.core.service.BagItService;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class BagIsValid implements BagValidatorRule {
    private final BagItService bagItService;
    private final HoleyBagsConfig holeyBagsConfig;

    @Override
    public RuleResult validate(Path path) throws Exception {
        try {
            log.debug("Verifying bag {}", path);
            bagItService.verifyBag(path, holeyBagsConfig);
            log.debug("Bag {} is valid", path);
            return RuleResult.ok();
        }
        // only catch exceptions that have to do with the bag verification;
        // other exceptions such as IOException should be propagated to the rule engine
        // sadly FileNotInManifestException bubbles up as an IOException
        catch (FileNotInManifestException | InvalidBagitFileFormatException | MissingPayloadManifestException |
               MissingPayloadDirectoryException | FileNotInPayloadDirectoryException | MissingBagitFileException |
               CorruptChecksumException | VerificationException | NoSuchFileException e) {

            return RuleResult.error(String.format(
                "Bag is not valid: %s", e.getMessage()
            ), e);
        }
    }
}
