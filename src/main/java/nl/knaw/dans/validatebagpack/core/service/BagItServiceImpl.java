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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.bagit.reader.BagReader;
import nl.knaw.dans.bagit.verify.BagVerifier;

import nl.knaw.dans.validatebagpack.config.HoleyBagsConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class BagItServiceImpl implements BagItService {
    @NonNull
    private final HoleyBagsConfig holeyBagsConfig;

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
    public void verifyBag(Path path) throws Exception {
        var bag = new BagReader().read(path);

        try (var verifier = new BagVerifier()) {
            var ignoreHiddenFiles = false;
            var urlConfigs = holeyBagsConfig.isAllow() ? holeyBagsConfig.getAddHeaders() : Collections.<String, Map<String, String>>emptyMap();

            if (holeyBagsConfig.isAllow()) {
                verifier.setChunkSize((int) holeyBagsConfig.getChunkSize().toBytes());
                verifier.setMaxRetries(holeyBagsConfig.getMaxRetries());
                verifier.setRetrySleepMs((int) holeyBagsConfig.getRetrySleep().toMilliseconds());
                verifier.setMaxRedirects(holeyBagsConfig.getMaxRedirects());
                verifier.setFallBackToFullStreamOnRangeFail(holeyBagsConfig.isFallBackToFullStreamOnRangeFail());
            }

            log.debug("Verifying bag is complete on path {} (allowHoleyBags={})", path, holeyBagsConfig.isAllow());
            verifier.isComplete(bag, ignoreHiddenFiles, holeyBagsConfig.isAllow());

            log.debug("Verifying bag is valid on path {} (allowHoleyBags={}, urlConfigs={})", path, holeyBagsConfig.isAllow(), urlConfigs);
            verifier.isValid(bag, ignoreHiddenFiles, holeyBagsConfig.isAllow(), Collections.emptyMap(), urlConfigs);
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
