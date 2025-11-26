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
import nl.knaw.dans.bagit.conformance.BagProfileChecker;
import nl.knaw.dans.bagit.exceptions.conformance.BagitVersionIsNotAcceptableException;
import nl.knaw.dans.bagit.exceptions.conformance.FetchFileNotAllowedException;
import nl.knaw.dans.bagit.exceptions.conformance.MetatdataValueIsNotAcceptableException;
import nl.knaw.dans.bagit.exceptions.conformance.MetatdataValueIsNotRepeatableException;
import nl.knaw.dans.bagit.exceptions.conformance.RequiredManifestNotPresentException;
import nl.knaw.dans.bagit.exceptions.conformance.RequiredMetadataFieldNotPresentException;
import nl.knaw.dans.bagit.exceptions.conformance.RequiredTagFileNotPresentException;
import nl.knaw.dans.bagit.reader.BagReader;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RequiredArgsConstructor
public class BagMustConformToDansBagPackBagItProfile implements BagValidatorRule {
    private final String profile;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var bag = new BagReader().read(path);
        try {
            BagProfileChecker.bagConformsToProfile(new ByteArrayInputStream(profile.getBytes(StandardCharsets.UTF_8)), bag);
            return RuleResult.ok();
        }
        catch (FetchFileNotAllowedException | RequiredMetadataFieldNotPresentException | MetatdataValueIsNotAcceptableException |
               RequiredManifestNotPresentException | BagitVersionIsNotAcceptableException | RequiredTagFileNotPresentException |
               MetatdataValueIsNotRepeatableException e) {
            // Unfortunately, the library makes it hard to report all problems in one go.
            return RuleResult.error(e.getMessage());
        }
        catch (Exception e) {
            throw new RuntimeException("Could not check bag against profile", e);
        }
    }
}
