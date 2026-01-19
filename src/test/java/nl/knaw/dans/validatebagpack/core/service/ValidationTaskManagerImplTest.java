package nl.knaw.dans.validatebagpack.core.service;

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidationTaskManagerImplTest extends AbstractTestFixture {

    @Test
    void getValidationTask_should_return_empty_for_unknown_id() {
        RuleEngineService ruleEngineService = mock(RuleEngineService.class);
        ValidationTaskManagerImpl manager = new ValidationTaskManagerImpl(ruleEngineService, Duration.ofMinutes(1), 10);

        assertThat(manager.getValidationTask(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_expire_tasks_after_retention_time() throws InterruptedException {
        RuleEngineService ruleEngineService = mock(RuleEngineService.class);
        // Set retention time to 100 ms for quick expiration
        ValidationTaskManagerImpl manager = new ValidationTaskManagerImpl(ruleEngineService, Duration.ofMillis(100), 10);

        ValidationTask task = manager.createValidationTask("bag/location");
        UUID id = task.getId();

        assertThat(manager.getValidationTask(id)).isPresent();

        // Wait for expiration
        Thread.sleep(200);

        // Guava cache is lazy, so access triggers cleanup
        assertThat(manager.getValidationTask(id)).isEmpty();
    }

    @Test
    void should_remove_oldest_task_when_maximum_size_exceeded() {
        RuleEngineService ruleEngineService = mock(RuleEngineService.class);
        ValidationTaskManagerImpl manager = new ValidationTaskManagerImpl(ruleEngineService, Duration.ofMinutes(1), 2);

        ValidationTask task1 = manager.createValidationTask("bag/1");
        ValidationTask task2 = manager.createValidationTask("bag/2");
        ValidationTask task3 = manager.createValidationTask("bag/3");

        // Only two most recent tasks should remain
        assertThat(manager.getValidationTask(task1.getId())).isEmpty();
        assertThat(manager.getValidationTask(task2.getId())).isPresent();
        assertThat(manager.getValidationTask(task3.getId())).isPresent();
    }
}
