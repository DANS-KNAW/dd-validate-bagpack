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

import lombok.RequiredArgsConstructor;
import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.core.service.BagItService;
import nl.knaw.dans.validatebagpack.core.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PidMappingTargetsMustEqualsPayloadFiles implements BagValidatorRule {
    private final FileService fileService;
    private final BagItService bagItService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var pidMappingFile = path.resolve(Constants.PID_MAPPING_PATH);
        if (Files.exists(pidMappingFile)) {
            var pidTargets = fileService.parsePidMapping(pidMappingFile).values().stream()
                .filter(s -> !s.endsWith("/")) // exclude directory targets
                .map(String::trim).collect(Collectors.toSet());
            var payloadFiles = bagItService.listPayloadFiles(path);

            var notInPayload = pidTargets.stream()
                .filter(pid -> !payloadFiles.contains(pid))
                .collect(Collectors.toSet());
            var notInPidTargets = payloadFiles.stream()
                .filter(payload -> !pidTargets.contains(payload))
                .collect(Collectors.toSet());

            if (!notInPayload.isEmpty() || !notInPidTargets.isEmpty()) {
                var errorMessage = "PID mapping targets and payload files do not match.";
                if (!notInPayload.isEmpty()) {
                    errorMessage += " Targets not in payload files: " + notInPayload;
                }
                if (!notInPidTargets.isEmpty()) {
                    errorMessage += " Payload files not in targets: " + notInPidTargets;
                }
                return RuleResult.error(errorMessage);
            }
        }
        else {
            return RuleResult.error("PID mapping file does not exist: " + pidMappingFile);
        }
        return RuleResult.ok();
    }
}
