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
import org.apache.jena.rdf.model.Model;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class OaiOreAggregatedResourceIdsMustBeInPidMapping implements BagValidatorRule {
    private final FileService fileService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var pidMappingFile = path.resolve(Constants.PID_MAPPING_PATH);
        var pidMap = fileService.parsePidMapping(pidMappingFile);

        Model model = null;
        try {
            model = fileService.readJsonLdAsRdfModel(path.resolve(Constants.OAI_ORE_PATH));
        } catch (JsonParseException e){
            return RuleResult.error(e.getMessage());
        }
        var uris = getAggregatedResourceIds(model);

        List<String> errors = new ArrayList<>();
        for (URI uri : uris) {
            if (!pidMap.containsKey(uri)) {
                errors.add(String.format("Aggregated resource ID '%s' not found in PID mapping", uri));
            }
        }

        if (!errors.isEmpty()) {
            return RuleResult.error(errors);
        }

        return RuleResult.ok();
    }

    private @NonNull List<URI> getAggregatedResourceIds(Model model) {
        List<URI> uris = new ArrayList<>();
        try (var exec = fileService.executeNamedQuery("findAggregatedResourceIds", model)) {
            var results = exec.execSelect();
            while (results.hasNext()) {
                var solution = results.next();
                uris.add(URI.create(solution.getLiteral("id").getString()));
            }
        }
        return uris;
    }
}
