package nl.knaw.dans.validatebagpack.core.service;

import nl.knaw.dans.validatebagpack.AbstractTestFixture;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import java.io.OutputStream;
import static org.assertj.core.api.Assertions.*;

class ZipBagRootTest extends AbstractTestFixture {

    private Path tempZip;

    @BeforeEach
    void setUpZip() throws IOException {
        tempZip = testDir.resolve("test-bag.zip");
        Files.deleteIfExists(tempZip);
    }

    private void createZipWithBagStructure(boolean withBagit, boolean extraRootEntry) throws IOException {
        try (FileSystem zipFs = FileSystems.newFileSystem(
            URI.create("jar:" + tempZip.toUri()),
            Map.of("create", "true"))) {
            Path bagDir = zipFs.getPath("/bag");
            Files.createDirectory(bagDir);
            if (withBagit) {
                Files.createFile(bagDir.resolve("bagit.txt"));
            }
            if (extraRootEntry) {
                Files.createDirectory(zipFs.getPath("/extra"));
            }
        }
    }

    @Test
    void should_find_bag_root_when_zip_is_valid() throws Exception {
        createZipWithBagStructure(true, false);
        try (ZipBagRoot bagRoot = new ZipBagRoot(tempZip, true)) {
            assertThat(bagRoot.getPath().getFileName().toString()).isEqualTo("bag");
            assertThat(Files.exists(bagRoot.getPath().resolve("bagit.txt"))).isTrue();
        }
    }

    @Test
    void should_throw_when_zip_has_no_root_directory() throws IOException {
        try (OutputStream os = Files.newOutputStream(tempZip);
            ZipOutputStream zos = new ZipOutputStream(os)) {
            // no entries
        }
        assertThatThrownBy(() -> new ZipBagRoot(tempZip, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void should_throw_when_zip_has_multiple_root_directories() throws IOException {
        // Create a zip with two root directories: /bag and /extra
        try (FileSystem zipFs = FileSystems.newFileSystem(
            URI.create("jar:" + tempZip.toUri()),
            Map.of("create", "true"))) {
            Files.createDirectory(zipFs.getPath("/bag"));
            Files.createFile(zipFs.getPath("/bag/bagit.txt"));
            Files.createDirectory(zipFs.getPath("/extra"));
        }
        assertThatThrownBy(() -> new ZipBagRoot(tempZip, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void should_throw_when_zip_root_has_multiple_entries() throws IOException {
        createZipWithBagStructure(true, true);
        assertThatThrownBy(() -> new ZipBagRoot(tempZip, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Zip root must contain exactly one directory");
    }

    @Test
    void should_throw_when_bag_directory_missing_bagit_txt() throws IOException {
        createZipWithBagStructure(false, false);
        assertThatThrownBy(() -> new ZipBagRoot(tempZip, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Directory does not contain bagit.txt");
    }
}
