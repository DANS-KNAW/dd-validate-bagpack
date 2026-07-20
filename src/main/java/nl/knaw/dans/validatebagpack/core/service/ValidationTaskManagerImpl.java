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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import nl.knaw.dans.lib.util.healthcheck.DependenciesReadyCheck;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class ValidationTaskManagerImpl implements ValidationTaskManager {
    private final RuleEngineService ruleEngineService;
    private final Cache<UUID, ValidationTask> validationTasks;
    private final DependenciesReadyCheck readyCheck;

    public ValidationTaskManagerImpl(RuleEngineService ruleEngineService, Duration retentionTime, long maximumNumberOfTasks, DependenciesReadyCheck readyCheck) {
        this.ruleEngineService = ruleEngineService;
        this.readyCheck = readyCheck;
        this.validationTasks = CacheBuilder.newBuilder()
            .expireAfterWrite(retentionTime)
            .maximumSize(maximumNumberOfTasks)
            .removalListener(notification ->
                log.debug("Removed validation task {} due to {}",
                    notification.getKey(), notification.getCause()))
            .build();
    }

    @Override
    public ValidationTask createValidationTask(String bagLocation) {
        var validationTask = new ValidationTask(bagLocation, ruleEngineService, readyCheck);
        validationTasks.put(validationTask.getId(), validationTask);
        return validationTask;
    }

    @Override
    public Optional<ValidationTask> getValidationTask(UUID taskId) {
        return Optional.ofNullable(validationTasks.getIfPresent(taskId));
    }
}
