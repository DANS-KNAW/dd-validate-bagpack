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

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileServiceTest extends AbstractTestFixture {

    @Test
    void parsePidMapping_should_return_empty_map_for_empty_file() throws Exception {
        // Given
        Files.createFile(testDir.resolve("empty.txt"));

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("empty.txt"));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parsePidMapping_should_return_single_item_for_file_with_one_line() throws Exception {
        // Given
        Files.writeString(testDir.resolve("one-line.txt"), "https://example.dans.knaw.nl.com/12345 path/in/bag");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("one-line.txt"));

        // Then
        assertThat(result.keySet()).containsExactly(URI.create("https://example.dans.knaw.nl.com/12345"));
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/12345"))).isEqualTo("path/in/bag");
    }

    @Test
    void parsePidMapping_should_return_several_items_for_file_with_several_lines() throws Exception {
        // Given
        Files.writeString(testDir.resolve("several-lines.txt"), """
            https://example.dans.knaw.nl.com/12345 path/in/bag1
            https://example.dans.knaw.nl.com/67890 path/in/bag2
            https://example.dans.knaw.nl.com/abcde path/in/bag3
            """);

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("several-lines.txt"));

        // Then
        assertThat(result.keySet()).containsExactlyInAnyOrder(
            URI.create("https://example.dans.knaw.nl.com/12345"),
            URI.create("https://example.dans.knaw.nl.com/67890"),
            URI.create("https://example.dans.knaw.nl.com/abcde")
        );
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/12345"))).isEqualTo("path/in/bag1");
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/67890"))).isEqualTo("path/in/bag2");
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/abcde"))).isEqualTo("path/in/bag3");
    }

    @Test
    public void parsePidMapping_should_accept_multiple_spaces_between_uri_and_path() throws Exception {
        // Given
        Files.writeString(testDir.resolve("multiple-spaces.txt"), "https://example.dans.knaw.nl.com/12345     path/in/bag");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("multiple-spaces.txt"));

        // Then
        assertThat(result.keySet()).containsExactly(URI.create("https://example.dans.knaw.nl.com/12345"));
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/12345"))).isEqualTo("path/in/bag");
    }

    @Test
    public void parsePidMapping_should_throw_exception_for_nonexistent_file() {
        // When / Then
        assertThatThrownBy(() -> new FileServiceImpl().parsePidMapping(testDir.resolve("nonexistent.txt")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Pid mapping file does not exist");
    }

    @Test
    public void parsePidMapping_should_accept_paths_with_spaces() throws Exception {
        // Given
        Files.writeString(testDir.resolve("path with spaces.txt"), "https://example.dans.knaw.nl.com/12345 path/in/bag with spaces");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("path with spaces.txt"));

        // Then
        assertThat(result.keySet()).containsExactly(URI.create("https://example.dans.knaw.nl.com/12345"));
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/12345"))).isEqualTo("path/in/bag with spaces");
    }

    @Test
    public void parsePidMapping_should_throw_IllegalArgumentException_line_without_spaces() throws Exception {
        // Given
        Files.writeString(testDir.resolve("invalid-line.txt"), "https://example.dans.knaw.nl.com/12345path/in/bag");

        // When / Then
        assertThatThrownBy(() -> new FileServiceImpl().parsePidMapping(testDir.resolve("invalid-line.txt")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Line without separator in pid mapping file: 'https://example.dans.knaw.nl.com/12345path/in/bag'");
    }

    @Test
    public void parsePidMapping_should_throw_IllegalArgumentException_for_invalid_uri() throws Exception {
        // Given
        Files.writeString(testDir.resolve("invalid-uri.txt"), "ht!tp://example.com/12345 path/in/bag");

        // When / Then
        assertThatThrownBy(() -> new FileServiceImpl().parsePidMapping(testDir.resolve("invalid-uri.txt")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Illegal character in scheme name at index 2: ht!tp://example.com/12345");
    }

    @Test
    void readJsonLdAsRdfModel_should_parse_simple_jsonld() throws Exception {
        // Given
        String jsonld = """
            {
              "@context": { "ex": "https://example.dans.knaw.nl.org/" },
              "@id": "ex:subject",
              "ex:predicate": "object"
            }
            """;
        var jsonldFile = testDir.resolve("test.jsonld");
        Files.writeString(jsonldFile, jsonld);

        // When
        var model = new FileServiceImpl().readJsonLdAsRdfModel(jsonldFile);

        // Then
        var subject = model.createResource("https://example.dans.knaw.nl.org/subject");
        var predicate = model.createProperty("https://example.dans.knaw.nl.org/predicate");
        assertThat(model.contains(subject, predicate, "object")).isTrue();
    }

    @Test
    void readJsonLdAsRdfModel_should_throw_for_nonexistent_file() {
        var nonExistent = testDir.resolve("no-such.jsonld");
        assertThatThrownBy(() -> new FileServiceImpl().readJsonLdAsRdfModel(nonExistent))
            .isInstanceOf(IOException.class);
    }

    @Test
    void loadNamedSparqlQueries_should_load_and_store_queries() throws Exception {
        // Given
        var queryFile = testDir.resolve("query1.sparql");
        Files.writeString(queryFile, "SELECT * WHERE { ?s ?p ?o }");

        var namedQueries = Map.of("testQuery", queryFile);
        var fileService = new FileServiceImpl();

        // When
        fileService.loadNamedSparqlQueries(namedQueries);

        // Then
        var model = ModelFactory.createDefaultModel();
        assertThatThrownBy(() ->  fileService.executeNamedQuery("wrongQuery", model))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No query found with name: wrongQuery");
    }

    @Test
    void loadNamedSparqlQueries_should_throw_for_invalid_query() throws Exception {
        // Given
        var queryFile = testDir.resolve("invalid.sparql");
        Files.writeString(queryFile, "NOT A QUERY");

        var namedQueries = Map.of("badQuery", queryFile);
        var fileService = new FileServiceImpl();

        // When / Then
        assertThatThrownBy(() -> fileService.loadNamedSparqlQueries(namedQueries))
            .isInstanceOf(org.apache.jena.query.QueryParseException.class);
    }

    @Test
    void parsePidMapping_should_trim_trailing_whitespace() throws Exception {
        // Given
        Files.writeString(testDir.resolve("trailing.txt"), "https://example.dans.knaw.nl.com/1   path/one\n");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("trailing.txt"));

        // Then
        assertThat(result.get(URI.create("https://example.dans.knaw.nl.com/1"))).isEqualTo("path/one");
    }

    @Test
    void parsePidMapping_should_throw_on_duplicate_uri() throws Exception {
        // Given
        Files.writeString(testDir.resolve("duplicate.txt"), """
            https://example.dans.knaw.nl.com/1 path/one
            https://example.dans.knaw.nl.com/1 path/two
            """);

        // When / Then
        assertThatThrownBy(() -> new FileServiceImpl().parsePidMapping(testDir.resolve("duplicate.txt")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate key https://example.dans.knaw.nl.com/1 (attempted merging values path/one and path/two)");
    }

    @Test
    void readFileContents_should_return_file_bytes() throws Exception {
        var file = testDir.resolve("test.txt");
        var content = "hello world".getBytes(StandardCharsets.UTF_8);
        Files.write(file, content);

        var result = new FileServiceImpl().readFileContents(file);

        assertThat(result).isEqualTo(content);
    }

    @Test
    void readFileContents_should_return_empty_array_for_empty_file() throws Exception {
        var file = testDir.resolve("empty.txt");
        Files.createFile(file);

        byte[] result = new FileServiceImpl().readFileContents(file);

        assertThat(result).isEmpty();
    }

    @Test
    void readFileContents_should_throw_for_nonexistent_file() {
        var file = testDir.resolve("no-such.txt");

        assertThatThrownBy(() -> new FileServiceImpl().readFileContents(file))
            .isInstanceOf(IOException.class);
    }

}
