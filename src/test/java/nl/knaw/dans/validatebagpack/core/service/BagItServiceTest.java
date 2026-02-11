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

import nl.knaw.dans.bagit.exceptions.MissingPayloadDirectoryException;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BagItServiceTest extends AbstractTestFixture {

    private final BagItServiceImpl service = new BagItServiceImpl();

    @Test
    void getBagRoot_should_return_DirectoryBagRoot_for_directory_bag() throws Exception {
        var bagDir = testDir.resolve("bagDir");
        Files.createDirectory(bagDir);
        Files.createFile(bagDir.resolve("bagit.txt"));

        var bagRoot = service.getBagRoot(bagDir);

        assertThat(bagRoot).isInstanceOf(DirectoryBagRoot.class);
        assertThat(bagRoot.getPath()).isEqualTo(bagDir);
    }

    @Test
    void getBagRoot_should_return_ZipBagRoot_for_zip_bag() throws Exception {
        var zipFile = testDir.resolve("bag.zip");
        try (var zipFs = java.nio.file.FileSystems.newFileSystem(
            URI.create("jar:" + zipFile.toUri()), Map.of("create", "true"))) {
            var bagDir = zipFs.getPath("/bag");
            Files.createDirectory(bagDir);
            Files.createFile(bagDir.resolve("bagit.txt"));
        }

        var bagRoot = service.getBagRoot(zipFile);

        assertThat(bagRoot).isInstanceOf(ZipBagRoot.class);
        assertThat(bagRoot.getPath().getFileName().toString()).isEqualTo("bag");
    }

    @Test
    void getBagRoot_should_throw_for_non_bag_path() {
        var notABag = testDir.resolve("notabag.txt");
        assertThatThrownBy(() -> service.getBagRoot(notABag))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("neither a directory bag nor a zip bag");
    }

    @Test
    void getBagRoot_should_throw_for_directory_without_bagit_txt() throws Exception {
        var bagDir = testDir.resolve("emptyDir");
        Files.createDirectory(bagDir);

        assertThatThrownBy(() -> service.getBagRoot(bagDir))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("neither a directory bag nor a zip bag");
    }

    @Test
    void getBagRoot_should_throw_for_zip_without_bagit_txt() throws Exception {
        var zipFile = testDir.resolve("empty.zip");
        try (OutputStream os = Files.newOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(os)) {
            // no entries
            zos.flush();
        }
        assertThatThrownBy(() -> service.getBagRoot(zipFile))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyBag_should_succeed_for_valid_bag() throws Exception {
        var bagDir = testDir.resolve("validBag");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        var dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        var file1 = dataDir.resolve("file1.txt");
        Files.writeString(file1, "content1");
        // Minimal manifest with dummy checksum
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            "7e55db001d319a94b0b713529a756623  data/file1.txt\n");

        // Should not throw
        service.verifyBag(bagDir);
    }

    @Test
    void verifyBag_should_throw_for_incomplete_bag() throws Exception {
        var bagDir = testDir.resolve("incompleteBag");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        // No manifest or data

        assertThatThrownBy(() -> service.verifyBag(bagDir))
            .isInstanceOf(MissingPayloadDirectoryException.class);
    }

    @Test
    void listPayloadFiles_should_return_all_payload_files() throws Exception {
        var bagDir = testDir.resolve("bagDir");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        var dataDir = bagDir.resolve("data");
        Files.createDirectory(dataDir);
        var file1 = dataDir.resolve("file1.txt");
        var file2 = dataDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");
        // Minimal manifest
        Files.writeString(bagDir.resolve("manifest-md5.txt"),
            String.format("%032x  data/file1.txt\n%032x  data/file2.txt\n", 0, 0));

        var files = service.listPayloadFiles(bagDir);

        assertThat(files).containsExactlyInAnyOrder("data/file1.txt", "data/file2.txt");
    }

    @Test
    void listPayloadFiles_should_return_empty_set_when_no_payload_files() throws Exception {
        var bagDir = testDir.resolve("emptyBag");
        Files.createDirectory(bagDir);
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 0.97\nTag-File-Character-Encoding: UTF-8\n");
        Files.writeString(bagDir.resolve("manifest-md5.txt"), "");

        var files = service.listPayloadFiles(bagDir);

        assertThat(files).isEmpty();
    }

    @Test
    void listPayloadFiles_should_throw_for_non_bag_path() {
        var notABag = testDir.resolve("notaBag");
        assertThatThrownBy(() -> service.listPayloadFiles(notABag))
            .isInstanceOf(NoSuchFileException.class);
    }
}
