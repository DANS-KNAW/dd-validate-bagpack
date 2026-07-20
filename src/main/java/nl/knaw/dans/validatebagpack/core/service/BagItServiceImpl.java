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
import nl.knaw.dans.bagit.domain.FetchItem;
import nl.knaw.dans.bagit.hash.StandardSupportedAlgorithms;
import nl.knaw.dans.bagit.hash.SupportedAlgorithm;
import nl.knaw.dans.bagit.reader.BagReader;
import nl.knaw.dans.bagit.verify.BagVerifier;
import nl.knaw.dans.validatebagpack.client.LobStoreClient;
import nl.knaw.dans.validatebagpack.config.HoleyBagsConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BagItServiceImpl implements BagItService {
    private final HoleyBagsConfig holeyBagsConfig;
    private final LobStoreClient lobStoreClient;

    public BagItServiceImpl(HoleyBagsConfig holeyBagsConfig) {
        this(holeyBagsConfig, null);
    }

    public BagItServiceImpl(HoleyBagsConfig holeyBagsConfig, LobStoreClient lobStoreClient) {
        this.holeyBagsConfig = holeyBagsConfig;
        this.lobStoreClient = lobStoreClient;
    }

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

            var ignoredFetchItems = buildIgnoredFetchItems(bag);
            log.debug("Verifying bag is valid on path {} (allowHoleyBags={}, urlConfigs={}, ignoredFetchItems={})", path, holeyBagsConfig.isAllow(), urlConfigs,
                ignoredFetchItems);
            verifier.isValid(bag, ignoreHiddenFiles, holeyBagsConfig.isAllow(), Collections.emptyMap(), urlConfigs, ignoredFetchItems);
        }
    }

    /**
     * Queries dd-lob-store for each fetch item whose URL base matches a configured datastation.
     * Returns a map from SHA-1 algorithm to the fetch items that are already present in the store,
     * so the bag verifier can skip re-fetching and re-hashing them for SHA-1.
     */
    private Map<SupportedAlgorithm, Collection<FetchItem>> buildIgnoredFetchItems(nl.knaw.dans.bagit.domain.Bag bag) {
        if (lobStoreClient == null || holeyBagsConfig.getLobstores().isEmpty() || bag.getItemsToFetch().isEmpty()) {
            return Collections.emptyMap();
        }

        // Build a path→SHA-1 lookup from the SHA-1 payload manifest (if present)
        var sha1Checksums = new HashMap<Path, String>();
        for (var manifest : bag.getPayLoadManifests()) {
            if (StandardSupportedAlgorithms.SHA1.equals(manifest.getAlgorithm())) {
                sha1Checksums.putAll(manifest.getFileToChecksumMap());
                break;
            }
        }

        if (sha1Checksums.isEmpty()) {
            log.debug("No SHA-1 payload manifest found; skipping lob-store lookup");
            return Collections.emptyMap();
        }

        var ignoredItems = new ArrayList<FetchItem>();
        for (var fetchItem : bag.getItemsToFetch()) {
            var sha1 = sha1Checksums.get(fetchItem.path);
            if (sha1 == null) {
                continue;
            }
            var datastation = resolveDatastation(fetchItem.url.toString());
            if (datastation == null) {
                continue;
            }
            if (lobStoreClient.isPresent(datastation, sha1)) {
                log.debug("Fetch item {} (sha1={}) is already present in lob-store for datastation {}; skipping SHA-1 hash check", fetchItem.path, sha1, datastation);
                ignoredItems.add(fetchItem);
            }
        }

        if (ignoredItems.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.of(StandardSupportedAlgorithms.SHA1, ignoredItems);
    }

    private String resolveDatastation(String url) {
        for (var entry : holeyBagsConfig.getLobstores().entrySet()) {
            if (url.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
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

