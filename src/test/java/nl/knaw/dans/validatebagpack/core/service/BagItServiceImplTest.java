package nl.knaw.dans.validatebagpack.core.service;

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.net.URI;

import static org.assertj.core.api.Assertions.*;

class BagItServiceImplTest extends AbstractTestFixture {

    private final BagItServiceImpl service = new BagItServiceImpl();

    @Test
    void getBagRoot_should_return_DirectoryBagRoot_for_directory_bag() throws Exception {
        Path bagDir = testDir.resolve("bagdir");
        Files.createDirectory(bagDir);
        Files.createFile(bagDir.resolve("bagit.txt"));

        var bagRoot = service.getBagRoot(bagDir);

        assertThat(bagRoot).isInstanceOf(DirectoryBagRoot.class);
        assertThat(bagRoot.getPath()).isEqualTo(bagDir);
    }

    @Test
    void getBagRoot_should_return_ZipBagRoot_for_zip_bag() throws Exception {
        Path zipFile = testDir.resolve("bag.zip");
        try (var zipFs = java.nio.file.FileSystems.newFileSystem(
            URI.create("jar:" + zipFile.toUri()), Map.of("create", "true"))) {
            Path bagDir = zipFs.getPath("/bag");
            Files.createDirectory(bagDir);
            Files.createFile(bagDir.resolve("bagit.txt"));
        }

        var bagRoot = service.getBagRoot(zipFile);

        assertThat(bagRoot).isInstanceOf(ZipBagRoot.class);
        assertThat(bagRoot.getPath().getFileName().toString()).isEqualTo("bag");
    }

    @Test
    void getBagRoot_should_throw_for_non_bag_path() {
        Path notABag = testDir.resolve("notabag.txt");
        assertThatThrownBy(() -> service.getBagRoot(notABag))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("neither a directory bag nor a zip bag");
    }

    @Test
    void getBagRoot_should_throw_for_directory_without_bagit_txt() throws Exception {
        Path bagDir = testDir.resolve("emptydir");
        Files.createDirectory(bagDir);

        assertThatThrownBy(() -> service.getBagRoot(bagDir))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("neither a directory bag nor a zip bag");
    }

    @Test
    void getBagRoot_should_throw_for_zip_without_bagit_txt() throws Exception {
        Path zipFile = testDir.resolve("empty.zip");
        try (OutputStream os = Files.newOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(os)) {
            // no entries
        }
        assertThatThrownBy(() -> service.getBagRoot(zipFile))
            .isInstanceOf(IllegalStateException.class);
    }
}
