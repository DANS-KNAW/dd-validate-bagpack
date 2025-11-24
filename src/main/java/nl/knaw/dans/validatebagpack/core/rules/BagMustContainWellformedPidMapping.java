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
    private static final String PID_MAPPING_FILENAME = "metadata/pid-mapping.txt";

    @Override
    public RuleResult validate(Path path) throws Exception {
        try {
            var pidMappingPath = path.resolve(PID_MAPPING_FILENAME);
            if (Files.notExists(pidMappingPath)) {
                return RuleResult.error("PID mapping file is missing: " + PID_MAPPING_FILENAME);
            }

            var fileService = new nl.knaw.dans.validatebagpack.core.service.FileServiceImpl();
            fileService.parsePidMapping(pidMappingPath);

            return RuleResult.ok();
        } catch (IllegalArgumentException e) {
            return RuleResult.error(e.getMessage());
        }

    }

}
