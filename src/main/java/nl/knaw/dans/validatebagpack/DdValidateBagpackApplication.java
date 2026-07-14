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

package nl.knaw.dans.validatebagpack;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import nl.knaw.dans.lib.util.XmlSchemaValidator;
import nl.knaw.dans.lib.util.ruleengine.RuleEngineImpl;
import nl.knaw.dans.validatebagpack.config.DdValidateBagpackConfig;
import nl.knaw.dans.validatebagpack.core.rules.RuleSets;
import nl.knaw.dans.validatebagpack.core.service.BagItServiceImpl;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import nl.knaw.dans.validatebagpack.core.service.RuleEngineServiceImpl;
import nl.knaw.dans.validatebagpack.core.service.ValidationTaskManagerImpl;
import nl.knaw.dans.validatebagpack.resources.ValidateApiResource;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class DdValidateBagpackApplication extends Application<DdValidateBagpackConfig> {

    public static void main(final String[] args) throws Exception {
        new DdValidateBagpackApplication().run(args);
    }

    @Override
    public String getName() {
        return "DD Validate BagPack";
    }

    @Override
    public void initialize(final Bootstrap<DdValidateBagpackConfig> bootstrap) {

    }

    @Override
    public void run(final DdValidateBagpackConfig config, final Environment environment) {
        var xmlSchemaValidator = new XmlSchemaValidator(Map.of(
            "DataCite", config.getValidation().getDataCiteSchema()
        ));

        var bagItService = new BagItServiceImpl();
        var fileService = new FileServiceImpl();
        try {
            fileService.loadNamedSparqlQueries(config.getValidation().getSparqlQueries());
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load SPARQL queries", e);
        }
        var validationConfig = config.getValidation();
        var holeyBagsConfig = validationConfig.getHoleyBags();

        var ruleSets = new RuleSets(bagItService, xmlSchemaValidator, getContentsAsString(validationConfig.getDansBagPackBagItProfile(), StandardCharsets.UTF_8),
            fileService, holeyBagsConfig);
        var ruleEngineService = new RuleEngineServiceImpl(new RuleEngineImpl(), ruleSets.getCommonRules(), bagItService, validationConfig.getBaseFolder());
        var validationTaskFactory = new ValidationTaskManagerImpl(
            ruleEngineService, validationConfig.getTaskRetentionTime().toJavaDuration(),
            validationConfig.getMaxNumberOfTasks());
        environment.jersey().register(new ValidateApiResource(validationTaskFactory, validationConfig.getTaskQueue().build(environment),
            appendEndSlashIfMissing(validationConfig.getBaseUrl())));
    }

    private URI appendEndSlashIfMissing(URI uri) {
        if (uri.toString().endsWith("/")) {
            return uri;
        }
        return URI.create(uri + "/");
    }

    private String getContentsAsString(URI uri, Charset charset) {
        try (var is = uri.toURL().openStream()) {
            return StringUtils.toEncodedString(is.readAllBytes(), charset);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
