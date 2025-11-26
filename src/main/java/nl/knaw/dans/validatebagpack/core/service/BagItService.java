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

import java.nio.file.Path;
import java.util.Set;

public interface BagItService {

    /**
     * Returns the root directory of the bag. If the provided path is already the bag root, it is returned as is. If it is a ZIPPED bag, the root inside the zip is returned.
     *
     * @param bagPath the path to the bag
     * @return the root directory of the bag
     * @throws Exception if the bag root could not be determined or an error occurs during the operation
     */
    BagRoot getBagRoot(Path bagPath) throws Exception;

    /**
     * Verifies whether the bag at the given path is valid, according to BagIt specifications.
     *
     * @param path the root directory of the bag
     * @throws Exception if the bag is not valid or an error occurs during verification
     */
    void verifyBag(Path path) throws Exception;

    Set<String> listPayloadFiles(Path bagRoot) throws Exception;
}
