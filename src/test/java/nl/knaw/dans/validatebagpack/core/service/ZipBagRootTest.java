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

    private Path tempZip;

    @BeforeEach
    void setUpZip() throws IOException {
        tempZip = testDir.resolve("test-bag.zip");
        Files.deleteIfExists(tempZip);
    }

    private void createZipWithEntries(String... entries) throws IOException {
        try (FileSystem zipFs = FileSystems.newFileSystem(
            URI.create("jar:" + tempZip.toUri()),
            Map.of("create", "true"))) {
            for (String entry : entries) {
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
    void should_find_bag_root_when_zip_is_valid() throws Exception {
        createZipWithEntries("/bag/bagit.txt");
        try (ZipBagRoot bagRoot = new ZipBagRoot(tempZip)) {
            assertThat(bagRoot.getPath().getFileName().toString()).isEqualTo("bag");
            assertThat(Files.exists(bagRoot.getPath().resolve("bagit.txt"))).isTrue();
        }
    }

    @Test
    void open_not_existing_zip_fails() throws Exception {
        assertThat(Files.exists(tempZip)).isFalse();
        assertThatThrownBy(() -> new ZipBagRoot(tempZip))
            .isInstanceOf(NoSuchFileException.class)
            .hasMessage(tempZip.toString());
        assertThat(Files.exists(tempZip)).isFalse();
    }


    @Test
    void should_throw_when_zip_has_no_root_directory() throws IOException {
        createZipWithEntries();
        assertThatThrownBy(() -> new ZipBagRoot(tempZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void should_throw_when_zip_root_has_multiple_entries() throws IOException {
        createZipWithEntries("/bag/", "/extra/");
        assertThatThrownBy(() -> new ZipBagRoot(tempZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void should_throw_when_bag_directory_missing_bagit_txt() throws IOException {
        createZipWithEntries("/bag/");
        assertThatThrownBy(() -> new ZipBagRoot(tempZip))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Directory does not contain bagit.txt");
    }
}
