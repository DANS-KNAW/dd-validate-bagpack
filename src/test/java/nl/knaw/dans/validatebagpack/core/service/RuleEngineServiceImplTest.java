package nl.knaw.dans.validatebagpack.core.service;

import nl.knaw.dans.lib.util.ruleengine.NumberedRule;
import nl.knaw.dans.lib.util.ruleengine.RuleEngine;
import nl.knaw.dans.lib.util.ruleengine.RuleValidationResult;
import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import nl.knaw.dans.validatebagpack.api.ValidationResultDto;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleEngineServiceImplTest extends AbstractTestFixture {

    @Test
    void validateBag_should_return_compliant_result_when_no_failures() throws Exception {
        Path bagDir = testDir.resolve("bagdir");
        Files.createDirectory(bagDir);
        Files.createFile(bagDir.resolve("bagit.txt"));

        RuleEngine ruleEngine = mock(RuleEngine.class);
        BagItService bagItService = mock(BagItService.class);
        List<NumberedRule> rules = List.of();
        when(bagItService.getBagRoot(bagDir)).thenReturn(new DirectoryBagRoot(bagDir));
        when(ruleEngine.validateBag(eq(bagDir), any())).thenReturn(List.of(
            new RuleValidationResult("1", RuleValidationResult.RuleValidationResultStatus.SUCCESS, null)
        ));

        RuleEngineServiceImpl service = new RuleEngineServiceImpl(ruleEngine, rules, bagItService, testDir);

        ValidationResultDto result = service.validateBag(bagDir.toString());

        assertThat(result.getIsCompliant()).isTrue();
        assertThat(result.getRuleViolations()).isEmpty();
    }

    @Test
    void validateBag_should_return_noncompliant_result_with_violations() throws Exception {
        Path bagDir = testDir.resolve("bagdir2");
        Files.createDirectory(bagDir);
        Files.createFile(bagDir.resolve("bagit.txt"));

        RuleEngine ruleEngine = mock(RuleEngine.class);
        BagItService bagItService = mock(BagItService.class);
        List<NumberedRule> rules = List.of();
        when(bagItService.getBagRoot(bagDir)).thenReturn(new DirectoryBagRoot(bagDir));
        when(ruleEngine.validateBag(eq(bagDir), any())).thenReturn(List.of(
            new RuleValidationResult("2", RuleValidationResult.RuleValidationResultStatus.FAILURE, "error message")
        ));

        RuleEngineServiceImpl service = new RuleEngineServiceImpl(ruleEngine, rules, bagItService, testDir);

        ValidationResultDto result = service.validateBag(bagDir.toString());

        assertThat(result.getIsCompliant()).isFalse();
        assertThat(result.getRuleViolations()).hasSize(1);
        assertThat(result.getRuleViolations().get(0).getRule()).isEqualTo("2");
        assertThat(result.getRuleViolations().get(0).getViolation()).contains("error message");
    }

    @Test
    void validateBag_should_throw_if_path_outside_base_folder() throws Exception {
        Path outside = Files.createTempDirectory("outside");
        try {
            RuleEngine ruleEngine = mock(RuleEngine.class);
            BagItService bagItService = mock(BagItService.class);
            List<NumberedRule> rules = List.of();

            RuleEngineServiceImpl service = new RuleEngineServiceImpl(ruleEngine, rules, bagItService, testDir);

            assertThatThrownBy(() -> service.validateBag(outside.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the allowed base folder");
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void validateBag_should_throw_if_path_not_readable() {
        Path notExist = testDir.resolve("doesnotexist");
        RuleEngine ruleEngine = mock(RuleEngine.class);
        BagItService bagItService = mock(BagItService.class);
        List<NumberedRule> rules = List.of();

        RuleEngineServiceImpl service = new RuleEngineServiceImpl(ruleEngine, rules, bagItService, testDir);

        assertThatThrownBy(() -> service.validateBag(notExist.toString()))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("could not be found or read");
    }
}
