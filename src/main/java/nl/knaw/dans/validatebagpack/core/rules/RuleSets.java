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
package nl.knaw.dans.validatebagpack.core.rules;

import lombok.AllArgsConstructor;
import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.validatebagpack.core.service.BagItService;

import java.util.List;

@AllArgsConstructor
public class RuleSets {
    public static final String PROFILE_VERSION = "1.0.0";

    private final BagItService bagItService;

    public List<NumberedRule> getCommonRules() {
        return List.of(
            new NumberedRule("1.1", new BagIsValid(bagItService)),
            new NumberedRule("1.2(a)", new BagContainsRegularFile("metadata/datacite.xml"), List.of("1.1"))
            /*
X 1. A DANS BagPack MUST be valid according to [BagIt v1.0]{:target=_blank}.
2. A DANS BagPack MUST contain a file `metadata/datacite.xml` (a) this file MUST be valid according to the
   [DataCite schema version 4.0 or later]{:target=_blank}, except for the requirement that there MUST be a DOI present: a DOI is not required for a DANS
   BagPack; (b) [DataCite's recommended properties]{:target=_blank} SHOULD be present.
3. Other files besides `datacite.xml` MAY be present in the `metadata` folder.
4. The files in the `metadata` folder MUST be mentioned in the `tag-manifest` (this is optional in BagIt, but required by RDA BagPack).
5. `BagIt-Profile-Identifier` MUST be provided.

### 2. Extra Requirements for DANS BagPack

The following items are required by the DANS BagPack Profile, in addition to the requirements of RDA BagPack:

1. `BagIt-Profile-Identifier` MUST contain `https://doi.org/10.17026/e948-0r32`.
2. The bag must be valid according to the [DANS BagPack BagIt Profile]{:target=_blank}.
3. There MUST be a file called `metadata/pid-mapping.txt`: the structure of this file MUST be rows of `<identifier>  <referenced object>`, where `<identifier>`
   is a unique URI and `<referenced object>` is the path to the file relative to the root of the bag, and both are separated by one or more spaces.
4. (a) There MUST be a file called `metadata/oai-ore.jsonld`; (b) this file MUST be well-formed JSON.
5. There MUST be a one-to-one mapping between the files in the `data` folder and the files described in the Aggregation contained in  `oai-ore.jsonld` file: (a)
   all identifiers mentioned in the `oai-ore.jsonld` that refer to files in the `data` folder MUST be present in `pid-mapping.txt`; (b) all file objects
   mentioned in the `pid-mapping.txt` MUST be present in the `oai-ore.jsonld`.

             */
        );
    }
}
