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
// src/test/java/nl/knaw/dans/validatebagpack/core/rules/RuleSetsTest.java
package nl.knaw.dans.validatebagpack.core.rules;

import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class RuleSetsTest {
    private RuleSets ruleSets;

    @BeforeEach
    void setUp() {
        ruleSets = new RuleSets(null, null, "profile", null, false, Collections.emptyMap());
    }

    @Test
    void getCommonRules_should_not_throw() {
        var rules = ruleSets.getCommonRules();
    }
}
