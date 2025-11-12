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
import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.validatebagpack.core.service.BagItService;

import java.util.List;

@AllArgsConstructor
public class RuleSets {
    public static final String PROFILE_VERSION = "1.0.0";

    private final BagItService bagItService;

    public List<NumberedRule> getCommonRules() {
        return List.of(
            new NumberedRule("1.1", new BagIsValid(bagItService))
        );
    }
}
