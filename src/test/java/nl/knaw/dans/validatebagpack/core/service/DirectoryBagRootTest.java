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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryBagRootTest extends AbstractTestFixture {

    @Test
    void should_store_and_return_path() throws Exception {
        Path bagDir = testDir.resolve("bagdir");
        Files.createDirectory(bagDir);

        DirectoryBagRoot bagRoot = new DirectoryBagRoot(bagDir);

        assertThat(bagRoot.getPath()).isEqualTo(bagDir);
    }

    @Test
    void close_should_do_nothing() throws Exception {
        Path bagDir = testDir.resolve("bagdir");
        Files.createDirectory(bagDir);

        DirectoryBagRoot bagRoot = new DirectoryBagRoot(bagDir);

        // Should not throw
        bagRoot.close();
    }
}
