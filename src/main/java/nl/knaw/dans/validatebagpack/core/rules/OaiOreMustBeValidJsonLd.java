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

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static nl.knaw.dans.validatebagpack.core.rules.Constants.OAI_ORE_PATH;

@RequiredArgsConstructor
@Slf4j
public class OaiOreMustBeValidJsonLd implements BagValidatorRule {
    private final boolean enableJsonLdValidation;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var oaiOrePath = path.resolve(OAI_ORE_PATH);
        if (Files.notExists(oaiOrePath)) {
            return RuleResult.error("File does not exist: " + path);
        }

        String error = getJsonLdValidationError(oaiOrePath);
        if (error != null) {
            if (enableJsonLdValidation) {
                return RuleResult.error(String.format("File is not valid JSON-LD: '%s'. Error: %s", oaiOrePath, error));
            }
            else {
                log.warn("File is not valid JSON-LD, but validation is disabled: '{}'. Error: {}", oaiOrePath, error);
            }
        }

        return RuleResult.ok();
    }

    private String getJsonLdValidationError(Path filePath) {
        try (var reader = Files.newBufferedReader(filePath)) {
            Object jsonObject = JsonUtils.fromReader(reader);
            JsonLdProcessor.expand(jsonObject);
            return null;
        }
        catch (JsonLdError | IOException e) {
            return e.getMessage();
        }
    }
}