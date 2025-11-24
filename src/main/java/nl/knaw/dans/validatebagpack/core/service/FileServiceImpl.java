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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class FileServiceImpl implements FileService {
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
}
