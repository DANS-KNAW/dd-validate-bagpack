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
import nl.knaw.dans.validatebagpack.core.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class OaiOreMustHaveOneAggregationWithOneBagId implements BagValidatorRule {
    private final FileService fileService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var oaiOrePath = path.resolve(Constants.OAI_ORE_PATH);

        if (Files.notExists(oaiOrePath)) {
            log.warn("OAI-ORE JSON-LD file not found at expected location: {}", oaiOrePath);
            return RuleResult.error("OAI-ORE JSON-LD file not found at expected location: " + oaiOrePath);
        }

        var model = fileService.readJsonLdAsRdfModel(oaiOrePath);

        var aggregations = new HashSet<String>();
        var bagIds = new ArrayList<String>();

        try (var queryExecution = fileService.executeNamedQuery("findBagId", model)) {
            var results = queryExecution.execSelect();
            while (results.hasNext()) {
                var solution = results.next();
                var resource = solution.getResource("resource");
                if (resource != null) {
                    aggregations.add(resource.toString());
                }

                var node = solution.get("bagId");
                if (node != null && node.isLiteral()) {
                    bagIds.add(node.asLiteral().getString());
                }
            }
        }

        if (aggregations.size() != 1) {
            return RuleResult.error(String.format("Expected exactly one 'ore:Aggregation' resource, but found %d", aggregations.size()));
        }

        if (bagIds.size() != 1) {
            return RuleResult.error(String.format("Expected exactly one 'dansBagId' property on the aggregation, but found %d", bagIds.size()));
        }

        var bagId = bagIds.get(0);
        if (!isValidUrnUuid(bagId)) {
            return RuleResult.error("Value of 'dansBagId' is not a valid URN:UUID: " + bagId);
        }

        return RuleResult.ok();
    }

    private boolean isValidUrnUuid(String s) {
        if (s == null || !s.startsWith("urn:uuid:")) {
            return false;
        }
        try {
            UUID.fromString(s.substring("urn:uuid:".length()));
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

}
