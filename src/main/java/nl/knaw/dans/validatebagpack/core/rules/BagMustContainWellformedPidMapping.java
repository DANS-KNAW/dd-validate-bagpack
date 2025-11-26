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

import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;

import java.nio.file.Files;
import java.nio.file.Path;

public class BagMustContainWellformedPidMapping implements BagValidatorRule {
    @Override
    public RuleResult validate(Path path) throws Exception {
        try {
            var pidMapping = path.resolve(Constants.PID_MAPPING_PATH);
            if (Files.notExists(pidMapping)) {
                return RuleResult.error("PID mapping file is missing: " + Constants.PID_MAPPING_PATH);
            }

            var fileService = new nl.knaw.dans.validatebagpack.core.service.FileServiceImpl();
            fileService.parsePidMapping(pidMapping);

            return RuleResult.ok();
        } catch (IllegalArgumentException e) {
            return RuleResult.error(e.getMessage());
        }

    }

}
