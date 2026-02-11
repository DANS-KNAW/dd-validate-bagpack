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
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ZipBagRootTest extends AbstractTestFixture {

    private Path testZip;

    @BeforeEach
    void setUpZip() throws IOException {
        testZip = testDir.resolve("test-bag.zip");
        Files.deleteIfExists(testZip);
    }

    private void createZipWithEntries(String... entries) throws IOException {
        try (var zipFs = FileSystems.newFileSystem(
            URI.create("jar:" + testZip.toUri()),
            Map.of("create", "true"))) {
            for (var entry : entries) {
                var path = zipFs.getPath(entry);
                if (entry.endsWith("/")) {
                    Files.createDirectory(path);
                } else {
                    // Ensure parent directory exists
                    var parent = path.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    Files.createFile(path);
                }
            }
        }
    }

    @Test
    void constructor_should_find_bag_root_when_zip_is_valid() throws Exception {
        createZipWithEntries("/bag/bagit.txt");
        try (ZipBagRoot bagRoot = new ZipBagRoot(testZip)) {
            assertThat(bagRoot.getPath().getFileName().toString()).isEqualTo("bag");
            assertThat(Files.exists(bagRoot.getPath().resolve("bagit.txt"))).isTrue();
        }
    }

    @Test
    void constructor_should_fail_when_zip_does_not_exist() {
        assertThat(Files.exists(testZip)).isFalse();
        assertThatThrownBy(() -> new ZipBagRoot(testZip))
            .isInstanceOf(NoSuchFileException.class)
            .hasMessage(testZip.toString());
        assertThat(Files.exists(testZip)).isFalse();
    }

    @Test
    void constructor_should_throw_when_zip_is_empty() throws IOException {
        createZipWithEntries();
        // TODO confusing terminology:
        //   getRootDirectories().iterator().hasNext() in findBagRoot will return true, but the root directory will be empty.
        //   the bagRoot is supposed to be a directory inside the root directory.
        assertThatThrownBy(() -> new ZipBagRoot(testZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void constructor_should_throw_when_zip_has_no_root_directory() throws IOException {
        createZipWithEntries("bagit.txt");
        assertThatThrownBy(() -> new ZipBagRoot(testZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void constructor_should_throw_when_zip_root_has_multiple_entries() throws IOException {
        createZipWithEntries("/bag/", "/extra/");
        assertThatThrownBy(() -> new ZipBagRoot(testZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void constructor_should_throw_when_bag_directory_missing_bagit_txt() throws IOException {
        createZipWithEntries("/bag/");
        assertThatThrownBy(() -> new ZipBagRoot(testZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Directory does not contain bagit.txt");
    }

    @Test
    void close_should_do_nothing() throws Exception {
        createZipWithEntries("/bag/bagit.txt");

        var bagRoot = new ZipBagRoot(testZip);

        // Should not throw
        bagRoot.close();
    }
}
