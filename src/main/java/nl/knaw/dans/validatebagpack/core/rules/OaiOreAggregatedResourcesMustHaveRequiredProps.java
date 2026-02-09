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

import com.fasterxml.jackson.core.JsonParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.core.service.FileService;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OaiOreAggregatedResourcesMustHaveRequiredProps implements BagValidatorRule {
    private final FileService fileService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var oaiOrePath = path.resolve(Constants.OAI_ORE_PATH);

        if (Files.notExists(oaiOrePath)) {
            log.warn("OAI-ORE JSON-LD file not found at expected location: {}", oaiOrePath);
            return RuleResult.error("OAI-ORE JSON-LD file not found at expected location: " + oaiOrePath);
        }

        Model model;
        try {
            model = fileService.readJsonLdAsRdfModel(path.resolve(Constants.OAI_ORE_PATH));
        } catch (JsonParseException e){
            return RuleResult.error(e.getMessage());
        }

        var errors = new ArrayList<String>();

        try (var queryExecution = fileService.executeNamedQuery("findAggregatedResourceProps", model)) {
            var results = queryExecution.execSelect();
            while (results.hasNext()) {
                var solution = results.next();
                checkId(solution.getLiteral("id"), errors);
                checkName(solution.get("name"), errors);
                checkRestricted(solution.get("restricted"), errors);
            }
        }

        if (!errors.isEmpty()) {
            return RuleResult.error(errors);
        }

        return RuleResult.ok();
    }

    private void checkId(Literal id, List<String> errors) {
        if (id == null || id.getString().isBlank()) {
            errors.add("(i) Aggregated resource has missing or blank 'id' property");
        }
        else {
            try {
                new URI(id.getString());
            }
            catch (URISyntaxException e) {
                errors.add(String.format("(i) Aggregated resource has invalid 'id' property: %s", id));
            }
        }
    }

    private void checkName(RDFNode name, List<String> errors) {
        if (name == null) {
            errors.add("(ii) Aggregated resource has missing 'name' property");
        }
    }

    private void checkRestricted(RDFNode restricted, List<String> errors) {
        if (restricted == null) {
            errors.add("(iii) Aggregated resource has missing 'restricted' property");
        }
        else if (!restricted.isLiteral()) {
            errors.add("(iii) Aggregated resource 'restricted' property is not a literal");
        }
        else {
            Literal literal = restricted.asLiteral();
            if (!literal.getDatatype().getURI().equals(Constants.XML_SCHEMA_BOOLEAN)) {
                errors.add("(iii) Aggregated resource 'restricted' property is not a boolean");
            }
        }
    }

}
