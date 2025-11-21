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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatebagpack.api.ValidationJobStatusDto;
import nl.knaw.dans.validatebagpack.api.ValidationJobStatusDto.StatusEnum;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class ValidationTask implements Runnable {
    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final ValidationJobStatusDto status = new ValidationJobStatusDto()
        .status(StatusEnum.PENDING)
        .jobId(id);

    /*
     * Will be initialized by the constructor
     */
    private final String bagLocation;
    private final RuleEngineService ruleEngineService;

    @Override
    public void run() {
        status.setStatus(StatusEnum.RUNNING);
        try {
            var validationResultDto = ruleEngineService.validateBag(bagLocation);
            status.setResult(validationResultDto);
            status.setStatus(StatusEnum.DONE);
        }
        catch (Exception e) {
            status.setError(e.getMessage());
            status.setStatus(StatusEnum.FAILED);
            log.error("Validation of bag {} failed", bagLocation, e);
        }
    }
}
