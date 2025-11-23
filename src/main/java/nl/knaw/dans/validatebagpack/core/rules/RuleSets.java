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
import nl.knaw.dans.lib.util.XmlSchemaValidator;
import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.validatebagpack.core.service.BagItService;
import nl.knaw.dans.validatebagpack.core.service.FileService;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;

import java.util.List;
import java.util.regex.Pattern;

@AllArgsConstructor
public class RuleSets {
    public static final String PROFILE_VERSION = "1.0.0";
    public static final String KEY_DATACITE_SCHEMA = "DataCite";

    private final BagItService bagItService;
    private final XmlSchemaValidator xmlSchemaValidator;
    private final String dansBagPackBagItProfile;
    private final FileService fileService = new FileServiceImpl();

    public List<NumberedRule> getCommonRules() {
        return List.of(
            new NumberedRule("1.1", new BagIsValid(bagItService)),
            new NumberedRule("1.2(a)", new BagContainsRegularFile("metadata/datacite.xml"), List.of("1.1")),
            new NumberedRule("1.2(b)", new BagFileConformsToXmlSchema("metadata/datacite.xml",
                fileService, KEY_DATACITE_SCHEMA, xmlSchemaValidator,
                List.of(
                    Pattern.compile("^cvc-pattern-valid: Value ':none' is not facet-valid with respect to pattern '.*' for type 'doiType'.*$"),
                    Pattern.compile("^cvc-complex-type.*Element 'identifier' must have no element \\[children], and the value must be valid.*$"))),
                List.of("1.2(a)")),
            new NumberedRule("2(a)", new BagMustConformToDansBagPackBagItProfile(dansBagPackBagItProfile), List.of("1.1"))
        );
    }
}
