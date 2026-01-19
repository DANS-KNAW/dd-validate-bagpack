package nl.knaw.dans.validatebagpack.core.service;

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class DirectoryBagRootTest extends AbstractTestFixture {

    @Test
    void should_store_and_return_path() throws Exception {
        Path bagDir = testDir.resolve("bagdir");
        Files.createDirectory(bagDir);

        DirectoryBagRoot bagRoot = new DirectoryBagRoot(bagDir);

        assertThat(bagRoot.getPath()).isEqualTo(bagDir);
    }

    @Test
    void close_should_do_nothing() throws Exception {
        Path bagDir = testDir.resolve("bagdir");
        Files.createDirectory(bagDir);

        DirectoryBagRoot bagRoot = new DirectoryBagRoot(bagDir);

        // Should not throw
        bagRoot.close();
    }
}
