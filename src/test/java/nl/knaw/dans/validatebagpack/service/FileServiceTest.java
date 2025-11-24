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
package nl.knaw.dans.validatebagpack.service;

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import nl.knaw.dans.validatebagpack.core.service.FileServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

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
        Files.writeString(testDir.resolve("one-line.txt"), "http://example.com/12345 path/in/bag");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("one-line.txt"));

        // Then
        assertThat(result.keySet()).containsExactly(URI.create("http://example.com/12345"));
        assertThat(result.get(URI.create("http://example.com/12345"))).isEqualTo("path/in/bag");
    }

    @Test
    void parsePidMapping_should_return_several_items_for_file_with_serveral_lines() throws Exception {
        // Given
        Files.writeString(testDir.resolve("several-lines.txt"), """
            http://example.com/12345 path/in/bag1
            http://example.com/67890 path/in/bag2
            http://example.com/abcde path/in/bag3
            """);

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("several-lines.txt"));

        // Then
        assertThat(result.keySet()).containsExactlyInAnyOrder(
            URI.create("http://example.com/12345"),
            URI.create("http://example.com/67890"),
            URI.create("http://example.com/abcde")
        );
        assertThat(result.get(URI.create("http://example.com/12345"))).isEqualTo("path/in/bag1");
        assertThat(result.get(URI.create("http://example.com/67890"))).isEqualTo("path/in/bag2");
        assertThat(result.get(URI.create("http://example.com/abcde"))).isEqualTo("path/in/bag3");
    }

    @Test
    public void parsePidMapping_should_accept_multiple_spaces_between_uri_and_path() throws Exception {
        // Given
        Files.writeString(testDir.resolve("multiple-spaces.txt"), "http://example.com/12345     path/in/bag");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("multiple-spaces.txt"));

        // Then
        assertThat(result.keySet()).containsExactly(URI.create("http://example.com/12345"));
        assertThat(result.get(URI.create("http://example.com/12345"))).isEqualTo("path/in/bag");
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
        Files.writeString(testDir.resolve("path with spaces.txt"), "http://example.com/12345 path/in/bag with spaces");

        // When
        var result = new FileServiceImpl().parsePidMapping(testDir.resolve("path with spaces.txt"));

        // Then
        assertThat(result.keySet()).containsExactly(URI.create("http://example.com/12345"));
        assertThat(result.get(URI.create("http://example.com/12345"))).isEqualTo("path/in/bag with spaces");
    }

    @Test
    public void parsePidMapping_should_throw_IllegalArgumentException_line_without_spaces() throws Exception {
        // Given
        Files.writeString(testDir.resolve("invalid-line.txt"), "http://example.com/12345path/in/bag");

        // When / Then
        assertThatThrownBy(() -> new FileServiceImpl().parsePidMapping(testDir.resolve("invalid-line.txt")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Line without separator in pid mapping file: 'http://example.com/12345path/in/bag'");
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


}
