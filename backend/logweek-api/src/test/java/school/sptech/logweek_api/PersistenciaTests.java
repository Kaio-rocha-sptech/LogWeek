package school.sptech.logweek_api;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaTests {
    @TempDir Path pasta;

    private ConfigurableApplicationContext iniciar() {
        return new SpringApplicationBuilder(LogweekApiApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:" + pasta.resolve("logweek").toString().replace('\\', '/'),
                "--spring.h2.console.enabled=false");
    }

    @Test void edicoesEExclusoesPersistemAposFecharEReabrirAplicacao() {
        // close() encerra o pool JDBC e o servidor, como no desligamento normal.
        try (var aplicacao = iniciar()) {
            var jdbc = aplicacao.getBean(JdbcTemplate.class);
            jdbc.update("UPDATE apontamento SET titulo = ? WHERE email_usuario = ?",
                    "Edicao persistida", "demo@email.com");
        }
        try (var aplicacao = iniciar()) {
            var jdbc = aplicacao.getBean(JdbcTemplate.class);
            assertEquals("Edicao persistida", jdbc.queryForObject(
                    "SELECT titulo FROM apontamento WHERE email_usuario = ?", String.class, "demo@email.com"));
            jdbc.update("DELETE FROM apontamento WHERE email_usuario = ?", "demo@email.com");
        }
        try (var aplicacao = iniciar()) {
            var jdbc = aplicacao.getBean(JdbcTemplate.class);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM apontamento", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
        }
    }
}
