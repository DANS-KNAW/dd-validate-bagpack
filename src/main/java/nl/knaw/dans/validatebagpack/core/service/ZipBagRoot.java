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

import lombok.Getter;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ZipBagRoot implements BagRoot {
    private final FileSystem fileSystem;

    @Getter
    private final Path path;

    public ZipBagRoot(Path path) throws Exception {
        this.fileSystem = FileSystems.newFileSystem(path, Map.of("create", "false"));
        this.path = findBagRoot();
    }

    @Override
    public void close() throws Exception {
        fileSystem.close();
    }

    private Path findBagRoot() throws IOException {
        // There must be exactly one directory in the root of the zip that contains a "bagit.txt" file.
        // There must be no other files or directories in the root of the zip.
        // If these conditions are not met, an exception is thrown.
        var rootStream = fileSystem.getRootDirectories().iterator();
        if (!rootStream.hasNext()) {
            throw new IllegalStateException("Zip has no root directory");
        }
        Path root = rootStream.next();
        try (var stream = Files.list(root)) {
            var entries = stream.toList();
            if (entries.size() != 1 || !java.nio.file.Files.isDirectory(entries.get(0))) {
                throw new IllegalStateException("Zip root must contain exactly one directory");
            }
            Path candidate = entries.get(0);
            Path bagitFile = candidate.resolve("bagit.txt");
            if (!Files.exists(bagitFile)) {
                throw new IllegalStateException("Directory does not contain bagit.txt");
            }
            return candidate;
        }

    }

}
