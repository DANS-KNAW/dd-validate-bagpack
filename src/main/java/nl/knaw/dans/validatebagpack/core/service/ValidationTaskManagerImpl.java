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

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ValidationTaskManagerImpl implements ValidationTaskManager {
    private final RuleEngineService ruleEngineService;
    private final Map<UUID, ValidationTask> validationTasks = new ConcurrentHashMap<>();

    public ValidationTask createValidationTask(String bagLocation) {
        var validationTask = new ValidationTask(bagLocation, ruleEngineService);
        validationTasks.put(validationTask.getId(), validationTask);
        return validationTask;
    }

    @Override
    public Optional<ValidationTask> getValidationTask(UUID taskId) {
        return Optional.ofNullable(validationTasks.get(taskId));
    }
}
