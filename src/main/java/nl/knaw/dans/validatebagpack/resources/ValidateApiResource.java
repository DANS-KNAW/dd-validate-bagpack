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

import lombok.RequiredArgsConstructor;
import nl.knaw.dans.validatebagpack.api.ValidateCommandDto;
import nl.knaw.dans.validatebagpack.core.service.ValidationTaskManager;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class ValidateApiResource implements ValidateApi {
    private final ValidationTaskManager validationTaskManager;
    private final ExecutorService executorService;
    private final URI baseUri;

    @Override
    public Response getValidationStatus(UUID jobId) {
        try {
            var task = validationTaskManager.getValidationTask(jobId);
            return task.map(t -> Response.ok(t.getStatus()).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response validateBagPack(ValidateCommandDto validateCommandDto) {
        try {
            var task = validationTaskManager.createValidationTask(validateCommandDto.getBagLocation());
            executorService.submit(task);
            return Response.created(baseUri.resolve(task.getId().toString())).build();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
