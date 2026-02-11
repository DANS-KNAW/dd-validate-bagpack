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

import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.lib.util.ruleengine.RuleEngine;
import nl.knaw.dans.lib.util.ruleengine.RuleEngineConfigurationException;
import nl.knaw.dans.lib.util.ruleengine.RuleEngineImpl;
import nl.knaw.dans.lib.util.ruleengine.RuleValidationResult;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RuleEngineServiceTest extends AbstractTestFixture {

    @Test
    void validateBag_should_return_compliant_result_when_no_failures() throws Exception {
        var bagDir = testDir.resolve("bagDir");
        Files.createDirectory(bagDir);
        Files.createFile(bagDir.resolve("bagit.txt"));

        var ruleEngine = mock(RuleEngine.class);
        var bagItService = mock(BagItService.class);
        when(bagItService.getBagRoot(any())).thenAnswer(invocation -> {
            Path arg = invocation.getArgument(0);
            if (arg.toAbsolutePath().normalize().equals(bagDir.toAbsolutePath().normalize())) {
                return new DirectoryBagRoot(bagDir);
            }
            throw new IllegalArgumentException("Unknown bag path: " + arg);
        });
        when(ruleEngine.validateBag(eq(bagDir), any())).thenReturn(List.of(
            new RuleValidationResult("1", RuleValidationResult.RuleValidationResultStatus.SUCCESS, null)
        ));

        var service = new RuleEngineServiceImpl(ruleEngine, List.of(), bagItService, testDir);

        var result = service.validateBag(bagDir.toString());

        assertThat(result.getIsCompliant()).isTrue();
        assertThat(result.getRuleViolations()).isEmpty();

        verify(ruleEngine).validateRuleSet(any());
        verify(ruleEngine).validateBag(eq(bagDir), any());
        verify(bagItService).getBagRoot(any());
        verifyNoMoreInteractions(ruleEngine, bagItService);
    }

    @Test
    void validateBag_should_return_noncompliant_result_with_violations() throws Exception {
        var bagDir = testDir.resolve("bagDir2");
        Files.createDirectory(bagDir);
        Files.createFile(bagDir.resolve("bagit.txt"));

        var ruleEngine = mock(RuleEngine.class);
        var bagItService = mock(BagItService.class);
        when(bagItService.getBagRoot(any())).thenAnswer(invocation -> {
            Path arg = invocation.getArgument(0);
            if (arg.toAbsolutePath().normalize().equals(bagDir.toAbsolutePath().normalize())) {
                return new DirectoryBagRoot(bagDir);
            }
            throw new IllegalArgumentException("Unknown bag path: " + arg);
        });
        when(ruleEngine.validateBag(eq(bagDir), any())).thenReturn(List.of(
            new RuleValidationResult("2", RuleValidationResult.RuleValidationResultStatus.FAILURE, "error message")
        ));

        var service = new RuleEngineServiceImpl(ruleEngine, List.of(), bagItService, testDir);

        var result = service.validateBag(bagDir.toString());

        assertThat(result.getIsCompliant()).isFalse();
        assertThat(result.getRuleViolations()).hasSize(1);
        assertThat(result.getRuleViolations().get(0).getRule()).isEqualTo("2");
        assertThat(result.getRuleViolations().get(0).getViolation()).contains("error message");

        verify(ruleEngine).validateRuleSet(any());
        verify(ruleEngine).validateBag(eq(bagDir), any());
        verify(bagItService).getBagRoot(any());
        verifyNoMoreInteractions(ruleEngine, bagItService);
    }

    @Test
    void validateBag_should_throw_if_path_outside_base_folder() throws Exception {
        var outside = Files.createTempDirectory("outside");
        try {
            var ruleEngine = mock(RuleEngine.class);
            var bagItService = mock(BagItService.class);

            var service = new RuleEngineServiceImpl(ruleEngine, List.of(), bagItService, testDir);

            assertThatThrownBy(() -> service.validateBag(outside.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("Path '%s' is outside the allowed base folder.", outside));

            verify(ruleEngine).validateRuleSet(any());
            verifyNoMoreInteractions(ruleEngine, bagItService);
        }
        finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void validateBag_should_throw_if_no_base_folder_provided() throws Exception {
        var ruleEngine = mock(RuleEngine.class);
        var bagItService = mock(BagItService.class);

        var service = new RuleEngineServiceImpl(ruleEngine, List.of(), bagItService, null);

        assertThatThrownBy(() -> service.validateBag(testDir.toString()))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("""
                Cannot invoke "nl.knaw.dans.validatebagpack.core.service.BagRoot.getPath()" because "bagRoot" is null""");

        verify(ruleEngine).validateRuleSet(any());
        verify(bagItService).getBagRoot(any());
        verifyNoMoreInteractions(ruleEngine, bagItService);
    }

    @Test
    void validateBag_should_throw_if_path_does_not_exist() throws Exception {
        var ruleEngine = mock(RuleEngine.class);
        var bagItService = mock(BagItService.class);
        var notReadable = testDir.resolve("notReadable");
        Files.createFile(notReadable);
        notReadable.toFile().setReadable(false, false);
        var service = new RuleEngineServiceImpl(ruleEngine, List.of(), bagItService, null);

        assertThatThrownBy(() -> service.validateBag(notReadable.toString()))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageEndingWith("notReadable' could not be found or read");

        verify(ruleEngine).validateRuleSet(any());
        verifyNoMoreInteractions(ruleEngine, bagItService);
    }

    @Test
    void constructor_should_throw_with_invalid_configuration() throws RuleEngineConfigurationException {
        var invalidRuleSet = List.of(
            new NumberedRule("1", null),
            new NumberedRule("1", null)
        );

        assertThatThrownBy(() -> new RuleEngineServiceImpl(new RuleEngineImpl(), invalidRuleSet, null, testDir))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Rule configuration is not valid");
    }

    @Test
    void validateBag_should_throw_if_path_not_readable() throws RuleEngineConfigurationException {
        var notExist = testDir.resolve("doesNotExist");
        var ruleEngine = mock(RuleEngine.class);
        var bagItService = mock(BagItService.class);

        var service = new RuleEngineServiceImpl(ruleEngine, List.of(), bagItService, testDir);

        assertThatThrownBy(() -> service.validateBag(notExist.toString()))
            .isInstanceOf(NoSuchFileException.class)
            .hasMessage("" + notExist.toAbsolutePath());

        verify(ruleEngine).validateRuleSet(any());
        verifyNoMoreInteractions(ruleEngine, bagItService);
    }
}
