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
package nl.knaw.dans.validatebagpack.resources;

import nl.knaw.dans.lib.util.ruleengine.RuleResult;
import nl.knaw.dans.validatebagpack.api.ValidateCommandDto;
import nl.knaw.dans.validatebagpack.api.ValidationJobStatusDto;
import nl.knaw.dans.validatebagpack.core.service.ValidationTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidateApiResourceTest {
    private ValidationTaskManager validationTaskManager;
    private ExecutorService executorService;
    private URI baseUri;
    private ValidateApiResource resource;

    @BeforeEach
    void setUp() {
        validationTaskManager = mock(ValidationTaskManager.class);
        executorService = mock(ExecutorService.class);
        baseUri = URI.create("http://localhost/api/");
        resource = new ValidateApiResource(validationTaskManager, executorService, baseUri);
    }

    @Test
    void getValidationStatus_returnsOk_whenTaskExists() {
        var jobId = UUID.randomUUID();
        var task = mock(nl.knaw.dans.validatebagpack.core.service.ValidationTask.class);
        when(validationTaskManager.getValidationTask(jobId)).thenReturn(Optional.of(task));
        var jobStatusDto = new ValidationJobStatusDto().status(ValidationJobStatusDto.StatusEnum.DONE).jobId(jobId);
        when(task.getStatus()).thenReturn(jobStatusDto);

        var response = resource.getValidationStatus(jobId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(jobStatusDto);
    }

    @Test
    void getValidationStatus_returnsNotFound_whenTaskMissing() {
        var jobId = UUID.randomUUID();
        when(validationTaskManager.getValidationTask(jobId)).thenReturn(Optional.empty());

        var response = resource.getValidationStatus(jobId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void validateBagPack_submitsTask_andReturnsCreated() {
        var dto = mock(ValidateCommandDto.class);
        var task = mock(nl.knaw.dans.validatebagpack.core.service.ValidationTask.class);
        var jobId = UUID.randomUUID();

        when(dto.getBagLocation()).thenReturn("bag-location");
        when(validationTaskManager.createValidationTask("bag-location")).thenReturn(task);
        when(task.getId()).thenReturn(jobId);

        try (var response = resource.validateBagPack(dto)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
            assertThat(response.getLocation()).isEqualTo(baseUri.resolve(jobId.toString()));
            verify(executorService).submit(task);
        }
    }
}
