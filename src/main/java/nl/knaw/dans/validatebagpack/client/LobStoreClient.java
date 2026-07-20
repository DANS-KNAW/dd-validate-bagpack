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
package nl.knaw.dans.validatebagpack.client;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lobstore.client.invoker.ApiException;
import nl.knaw.dans.lobstore.client.resources.DefaultApi;

@Slf4j
public class LobStoreClient {
    private final DefaultApi api;

    public LobStoreClient(DefaultApi api) {
        this.api = api;
    }

    /**
     * Returns true if the given SHA-1 hash is already present in dd-lob-store for the given datastation.
     *
     * @param datastation the lob-store datastation (store) name
     * @param sha1        the SHA-1 hash of the file
     * @return true if a location was found, false if not (404), or on error (logs warning and returns false)
     */
    public boolean isPresent(String datastation, String sha1) {
        try {
            api.getLocationByHash(datastation, sha1);
            return true;
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            log.warn("Error querying dd-lob-store for datastation={} sha1={}: {} {}", datastation, sha1, e.getCode(), e.getMessage());
            return false;
        }
    }
}
