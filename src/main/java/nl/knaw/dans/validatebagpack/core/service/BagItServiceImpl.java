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
import nl.knaw.dans.bagit.reader.BagReader;
import nl.knaw.dans.bagit.verify.BagVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BagItServiceImpl implements BagItService {
    @Override
    public BagRoot getBagRoot(Path bagPath) throws Exception {
        if (isDirectoryBag(bagPath)) {
            return new DirectoryBagRoot(bagPath);
        }
        else if (isZipBag(bagPath)) {
            return new ZipBagRoot(bagPath);
        }
        else {
            throw new IllegalArgumentException("The provided path is neither a directory bag nor a zip bag: " + bagPath);
        }
    }

    private boolean isDirectoryBag(Path path) {
        return Files.isDirectory(path) && Files.exists(path.resolve("bagit.txt"));
    }

    private boolean isZipBag(Path path) {
        return Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".zip");
    }

    @Override
    public void verifyBag(Path path, boolean allowHoleyBags, Map<String, Map<String, String>> urlConfigs) throws Exception {
        var bag = new BagReader().read(path);

        try (var verifier = new BagVerifier()) {
            var ignoreHiddenFiles = false;

            log.debug("Verifying bag is complete on path {} (allowHoleyBags={})", path, allowHoleyBags);
            verifier.isComplete(bag, ignoreHiddenFiles, allowHoleyBags);

            log.debug("Verifying bag is valid on path {} (allowHoleyBags={}, urlConfigs={})", path, allowHoleyBags, urlConfigs);
            verifier.isValid(bag, ignoreHiddenFiles, allowHoleyBags, Collections.emptyMap(), urlConfigs);
        }
    }

    @Override
    public Set<String> listPayloadFiles(Path bagRoot) throws Exception {
        var bag = new BagReader().read(bagRoot);
        return bag.getPayLoadManifests().stream()
            .flatMap(manifest -> manifest.getFileToChecksumMap().keySet().stream())
            .map(bagRoot::relativize)
            .map(Path::toString)
            .collect(Collectors.toSet());
    }

}
