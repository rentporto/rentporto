package pt.rentporto.configserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Serves a Git repository, as in the k8s profile, and picks up a change committed after it started: restarting a
 * service applies the new value without building a new image.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("k8s")
class GitBackendTest {

    private static final Path REPOSITORY = createRepository();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void gitRepository(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.server.git.uri", () -> REPOSITORY.toUri().toString());
    }

    @Test
    void servesAValueChangedAfterItStarted() throws Exception {
        assertSetting("first");

        commit("second");

        assertSetting("second");
    }

    private void assertSetting(String expected) throws Exception {
        mockMvc.perform(get("/customer-service/k8s"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source['rentporto.setting']").value(expected));
    }

    private static Path createRepository() {
        try {
            Path directory = Files.createTempDirectory("config-repository").toRealPath();
            Git.init().setDirectory(directory.toFile()).setInitialBranch("main").call().close();
            commit(directory, "first");
            return directory;
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void commit(String setting) throws IOException, GitAPIException {
        commit(REPOSITORY, setting);
    }

    private static void commit(Path directory, String setting) throws IOException, GitAPIException {
        Path file = directory.resolve("config/customer-service.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "rentporto:\n  setting: " + setting + "\n");
        try (Git git = Git.open(directory.toFile())) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Set " + setting).setAuthor("test", "test@rentporto.pt").setSign(false).call();
        }
    }
}
