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

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ValidationTaskManagerTest extends AbstractTestFixture {

    @Test
    void getValidationTask_should_return_empty_for_unknown_id() {
        var ruleEngineService = mock(RuleEngineService.class);
        var manager = new ValidationTaskManagerImpl(ruleEngineService, Duration.ofMinutes(1), 10, null);

        assertThat(manager.getValidationTask(UUID.randomUUID())).isEmpty();
        verifyNoInteractions(ruleEngineService);
    }

    @Test
    void getValidationTask_should_expire_tasks_after_retention_time() throws InterruptedException {
        var ruleEngineService = mock(RuleEngineService.class);
        // Set retention time to 100 ms for quick expiration
        var manager = new ValidationTaskManagerImpl(ruleEngineService, Duration.ofMillis(100), 10, null);

        var task = manager.createValidationTask("bag/location");
        var id = task.getId();

        assertThat(manager.getValidationTask(id)).isPresent();

        // Wait for expiration
        Thread.sleep(200);

        // Guava cache is lazy, so access triggers cleanup
        assertThat(manager.getValidationTask(id)).isEmpty();
        verifyNoInteractions(ruleEngineService);
    }

    @Test
    void getValidationTask_should_remove_oldest_task_when_maximum_size_exceeded() {
        var ruleEngineService = mock(RuleEngineService.class);
        var manager = new ValidationTaskManagerImpl(ruleEngineService, Duration.ofMinutes(1), 2, null);

        var task1 = manager.createValidationTask("bag/1");
        var task2 = manager.createValidationTask("bag/2");
        var task3 = manager.createValidationTask("bag/3");

        // Only two most recent tasks should remain
        assertThat(manager.getValidationTask(task1.getId())).isEmpty();
        assertThat(manager.getValidationTask(task2.getId())).isPresent();
        assertThat(manager.getValidationTask(task3.getId())).isPresent();
        verifyNoInteractions(ruleEngineService);
    }
}
