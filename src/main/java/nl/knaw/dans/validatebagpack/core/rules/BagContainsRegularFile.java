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
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;

import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public class BagContainsRegularFile implements BagValidatorRule {
    private final String relativeFilePath;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var target = path.resolve(relativeFilePath);

        if (!Files.exists(target)) {
            return RuleResult.error(String.format("Path '%s' does not exist", relativeFilePath));
        }

        if (!Files.isRegularFile(target)) {
            return RuleResult.error(String.format("Path '%s' is not a regular file", relativeFilePath));
        }

        return RuleResult.ok();
    }
}
