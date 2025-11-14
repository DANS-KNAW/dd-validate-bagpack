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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.XmlSchemaValidator;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.core.service.FileService;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class BagFileConformsToXmlSchema implements BagValidatorRule {
    protected final String file;
    protected final FileService fileService;
    protected final String schema;
    private final XmlSchemaValidator xmlSchemaValidator;
    private final List<Pattern> ignorePatterns;

    @Override
    public RuleResult validate(Path path) throws Exception {
        try {
            var fileName = path.resolve(file);
            log.debug("Validating {} against schema {}", fileName, schema);
            var errors = validateXmlFile(fileName, schema);

            if (!errors.isEmpty()) {
                var msg = String.format("%s does not conform to %s: \n%s",
                    file, schema, String.join("\n", errors));

                return RuleResult.error(msg);
            }
        }
        catch (SAXException e) {
            return RuleResult.error(e.getMessage(), e);
        }

        return RuleResult.ok();
    }

    private List<String> validateXmlFile(Path file, String schema) throws ParserConfigurationException, IOException, SAXException {
        var xml = fileService.readFileContents(file);
        var results = xmlSchemaValidator.validateDocument(new StreamSource(new ByteArrayInputStream(xml)), schema);

        // If "cvc-pattern-valid: Value ':none' is not facet-valid with respect to pattern '10\\..+/.+' for type 'doiType'." was returned,
        // remove it, as BagPack makes an exception for :none DOIs.
        for (var pattern : ignorePatterns) {
            results.removeIf(t -> pattern.matcher(t.getMessage()).matches());
        }

        return results.stream()
            .map(Throwable::getLocalizedMessage)
            .map(e -> String.format(" - %s", e))
            .collect(Collectors.toList());
    }

}
