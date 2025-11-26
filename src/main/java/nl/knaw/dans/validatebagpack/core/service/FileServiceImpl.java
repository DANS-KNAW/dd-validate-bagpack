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
package nl.knaw.dans.validatebagpack.core.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class FileServiceImpl implements FileService {
    private final Map<String, Query> queryMap = new HashMap<>();

    @Override
    public byte[] readFileContents(Path path) throws IOException {
        return java.nio.file.Files.readAllBytes(path);
    }

    @Override
    public Map<URI, String> parsePidMapping(Path pidMappingPath) throws IOException {
        if (Files.notExists(pidMappingPath)) {
            throw new IOException("Pid mapping file does not exist: " + pidMappingPath);
        }

        try (var lines = Files.lines(pidMappingPath, StandardCharsets.UTF_8)) {
            return lines.map(line -> line.split("\\s+", 2))
                // throw an exception if the line does not have exactly two parts
                .peek(parts -> {
                    if (parts.length == 1) {
                        throw new IllegalArgumentException("Line without separator in pid mapping file: '" + parts[0] + "'");
                    }
                })
                .collect(Collectors.toMap(
                    parts -> URI.create(parts[0]),
                    parts -> parts[1]
                ));
        }
    }

    @Override
    public Model readJsonLdAsRdfModel(Path jsonLdPath) throws IOException {
        var model = ModelFactory.createDefaultModel();
        model.read(Files.newInputStream(jsonLdPath), null, "JSON-LD");
        return model;
    }

    @Override
    public void loadNamedSparqlQueries(Map<String, Path> namedQueries) throws IOException {
        for (var entry : namedQueries.entrySet()) {
            var queryText = Files.readString(entry.getValue(), StandardCharsets.UTF_8);
            var query = QueryFactory.create(queryText);
            this.queryMap.put(entry.getKey(), query);
            log.info("Loaded SPARQL query '{}' from {}", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public QueryExecution executeNamedQuery(String queryName, Model model) throws IllegalArgumentException {
        var query = this.queryMap.get(queryName);
        if (query == null) {
            throw new IllegalArgumentException("No query found with name: " + queryName);
        }

        return QueryExecutionFactory.create(query, model);
    }
}
