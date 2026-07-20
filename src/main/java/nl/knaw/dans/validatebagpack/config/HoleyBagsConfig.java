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
package nl.knaw.dans.validatebagpack.config;

import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HoleyBagsConfig {
    private boolean allow = false;
    private Map<String, Map<String, String>> addHeaders = new HashMap<>();
    private DataSize chunkSize = DataSize.megabytes(128);
    private int maxRetries = 5;
    private Duration retrySleep = Duration.seconds(5);
    private int maxRedirects = 20;
    private boolean fallBackToFullStreamOnRangeFail = false;
    /**
     * Maps fetch-URL base (e.g. "https://archaeology.datastations.nl") to the lob-store datastation name (e.g. "archaeology").
     * When a fetch item's URL starts with one of these keys its SHA-1 is checked against dd-lob-store before bag validation,
     * and the item is skipped for SHA-1 hashing if it is already present there.
     */
    private Map<String, String> lobstores = new HashMap<>();
}
