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
import nl.knaw.dans.validatebagpack.core.service.RuleEngineService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.nio.file.Path;

@RequiredArgsConstructor
public class ValidateApiResource implements ValidateApi {
    private final RuleEngineService ruleEngineService;


    @Override
    public Response validateBagPack(ValidateCommandDto validateCommandDto) {
        try {
            var result = ruleEngineService.validateBag(Path.of(validateCommandDto.getBagLocation()),
                validateCommandDto.getBagLocation());
            return Response.ok(result).build();
        }
        catch (FileNotFoundException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
