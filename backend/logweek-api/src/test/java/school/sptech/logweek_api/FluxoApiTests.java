package school.sptech.logweek_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.CookieManager;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:teste;DB_CLOSE_DELAY=-1", "spring.sql.init.mode=always"
})
class FluxoApiTests {
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;

    HttpClient cliente() { return HttpClient.newBuilder().cookieHandler(new CookieManager()).build(); }
    HttpResponse<String> enviar(HttpClient cliente, String metodo, String rota, String json) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + rota))
            .header("Content-Type", "application/json")
            .method(metodo, HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void frontendLocalPodeEnviarJsonComSessao() throws Exception {
        HttpClient c = cliente();
        for (String origem : new String[]{"http://localhost:3000", "http://127.0.0.1:3000"}) {
            for (String[] endpoint : new String[][]{
                    {"/usuarios/login", "POST"}, {"/usuarios", "POST"},
                    {"/usuarios/atual", "GET"}, {"/usuarios/logout", "POST"},
                    {"/apontamentos", "POST"}, {"/apontamentos/1", "PUT"},
                    {"/apontamentos/1", "DELETE"}, {"/apontamentos/exportar", "POST"}}) {
                String rota = endpoint[0];
                var resposta = c.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + rota))
                        .header("Origin", origem)
                        .header("Access-Control-Request-Method", endpoint[1])
                        .header("Access-Control-Request-Headers", "content-type")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, resposta.statusCode(), origem + rota);
                assertEquals(origem, resposta.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
                assertEquals("true", resposta.headers().firstValue("Access-Control-Allow-Credentials").orElse(""));
            }
        }
        var bloqueada = c.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/usuarios/login"))
                .header("Origin", "http://origem-desconhecida.invalid")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, bloqueada.statusCode());
    }

    @Test void dadosIniciaisNaoRecriamNotasExcluidas() {
        jdbc.update("DELETE FROM apontamento WHERE email_usuario = 'demo@email.com'");
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("data.sql"));
        script.execute(jdbc.getDataSource());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM apontamento WHERE email_usuario = 'demo@email.com'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE email = 'demo@email.com'", Integer.class));
    }

    @Test void cadastroAceitaNomeCurtoEmailSemListaDeDominiosESenhaSimples() throws Exception {
        HttpClient c = cliente();
        String usuario = "{\"nome\":\"Ana\",\"email\":\" ANA@EXEMPLO.COM \" ,\"senha\":\"abcdefgh\"}";
        var cadastro = enviar(c, "POST", "/usuarios", usuario);
        assertEquals(201, cadastro.statusCode(), cadastro.body());
        assertFalse(cadastro.body().contains("senha"));
        assertEquals("Ana", jdbc.queryForObject("SELECT nome FROM usuarios WHERE email = 'ana@exemplo.com'", String.class));
        assertEquals(409, enviar(c, "POST", "/usuarios", usuario).statusCode());
        assertEquals(200, enviar(c, "POST", "/usuarios/login", usuario).statusCode());
        assertTrue(enviar(c, "GET", "/usuarios/atual", "").body().contains("ana@exemplo.com"));
        assertEquals(204, enviar(c, "POST", "/usuarios/logout", "").statusCode());
        assertEquals(401, enviar(c, "GET", "/usuarios/atual", "").statusCode());
    }

    @Test void cadastroRejeitaCamposAusentesEmailInvalidoELimitesDoBanco() throws Exception {
        HttpClient c = cliente();
        String usuario = "{\"nome\":\"Ana\",\"email\":\"limites@exemplo.com\",\"senha\":\"abcdefgh\"}";
        for (String invalido : new String[]{
                "{}", usuario.replace("Ana", "   "), usuario.replace("Ana", "a".repeat(101)),
                usuario.replace("limites@exemplo.com", "sem-arroba"),
                usuario.replace("limites@exemplo.com", "@exemplo.com"),
                usuario.replace("limites@exemplo.com", "nome@"),
                usuario.replace("limites@exemplo.com", "a".repeat(255)),
                usuario.replace("abcdefgh", ""), usuario.replace("abcdefgh", "curta"),
                usuario.replace("abcdefgh", "a".repeat(129))}) {
            assertEquals(400, enviar(c, "POST", "/usuarios", invalido).statusCode());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE email = 'limites@exemplo.com'", Integer.class));
    }

    @Test void respostaDeErroIncluiMensagemParaOFrontend() throws Exception {
        var resposta = enviar(cliente(), "POST", "/usuarios/login",
                "{\"email\":\"inexistente@exemplo.com\",\"senha\":\"incorreta\"}");
        assertEquals(401, resposta.statusCode());
        assertTrue(resposta.body().contains("Email ou senha incorretos."), resposta.body());
    }

    @Test void tituloSemObservacaoEHorariosPreservados() throws Exception {
        HttpClient c = cliente();
        String usuario = "{\"nome\":\"Teste Horarios\",\"email\":\"horarios@email.com\",\"senha\":\"Teste123!\"}";
        assertEquals(201, enviar(c, "POST", "/usuarios", usuario).statusCode());
        assertEquals(200, enviar(c, "POST", "/usuarios/login", usuario).statusCode());
        String inicio = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).minusMinutes(51).withSecond(0).withNano(0).toString();
        String nota = "{\"titulo\":\"Nota rapida\",\"inicioEm\":\"" + inicio + "\"}";
        var criada = enviar(c, "POST", "/apontamentos", nota);
        assertEquals(201, criada.statusCode(), criada.body());
        Integer id = jdbc.queryForObject("SELECT id FROM apontamento WHERE email_usuario = 'horarios@email.com'", Integer.class);
        String fim = jdbc.queryForObject("SELECT fim_em FROM apontamento WHERE id = ?", String.class, id);
        assertNotNull(fim);
        assertEquals(200, enviar(c, "PUT", "/apontamentos/" + id, "{\"titulo\":\"Corrigida\"}").statusCode());
        assertEquals(fim, jdbc.queryForObject("SELECT fim_em FROM apontamento WHERE id = ?", String.class, id));
        assertEquals(400, enviar(c, "PUT", "/apontamentos/" + id, "{\"titulo\":\"Futuro\",\"inicioEm\":\"2099-01-01T12:00\"}").statusCode());
        assertEquals(400, enviar(c, "POST", "/apontamentos", "{\"titulo\":\"Invalida\",\"inicioEm\":\"abc\"}").statusCode());
    }

    @Test void relatorioTextoAgrupaOrdenaESomaSemInventarHorarios() throws Exception {
        HttpClient c = cliente();
        String usuario = "{\"nome\":\"Teste Relatorio\",\"email\":\"relatorio@email.com\",\"senha\":\"Teste123!\"}";
        enviar(c, "POST", "/usuarios", usuario);
        enviar(c, "POST", "/usuarios/login", usuario);
        String sql = "INSERT INTO apontamento (email_usuario, semana, titulo, conteudo, criado_em, inicio_em, fim_em) VALUES ('relatorio@email.com', '31 ago 2026 - 6 set 2026', ?, ?, ?, ?, ?)";
        jdbc.update(sql, "Terca", "", "2026-09-01 15:35:00", "2026-09-01 14:20:00", "2026-09-01 15:35:00");
        jdbc.update(sql, "VPC", "Revisei a rede.", "2026-08-31 16:03:00", "2026-08-31 15:12:00", "2026-08-31 16:03:00");
        jdbc.update(sql, "Meia noite", "", "2026-09-01 00:15:00", "2026-08-31 23:45:00", "2026-09-01 00:15:00");
        jdbc.update(sql, "Antiga", "## Resumo\n\n- Texto antigo", "2026-09-02 10:00:00", null, null);
        var resposta = enviar(c, "POST", "/apontamentos/exportar", "{\"semana\":\"2026-08-31\"}");
        assertEquals(200, resposta.statusCode(), resposta.body());
        String texto = resposta.body();
        assertTrue(resposta.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"));
        assertTrue(resposta.headers().firstValue("Content-Disposition").orElse("").contains(".txt"));
        assertTrue(texto.contains("SEGUNDA-FEIRA | 31/08/2026"), texto);
        assertTrue(texto.contains("[15:12 - 16:03 | 51 min]"), texto);
        assertTrue(texto.contains("[23:45 - 00:15 (01/09/2026) | 30 min]"), texto);
        assertTrue(texto.contains("Tempo registrado: 02h 36min"), texto);
        assertTrue(texto.contains("Total de notas: 4"), texto);
        assertTrue(texto.contains("Horário não registrado"), texto);
        assertTrue(texto.indexOf("VPC") < texto.indexOf("Meia noite"));
        assertTrue(texto.indexOf("Meia noite") < texto.indexOf("Terca"));
        assertFalse(texto.contains("##"));
        assertFalse(texto.contains("sem observações"));
        assertFalse(enviar(c, "POST", "/apontamentos/exportar", "{\"semana\":\"2026-09-07\"}").body().contains("VPC"));
    }

    @Test void cadastroLoginNotasExportacaoELogout() throws Exception {
        HttpClient a = cliente();
        HttpClient b = cliente();
        assertEquals(401, enviar(a, "GET", "/apontamentos", "").statusCode());
        assertEquals(400, enviar(a, "POST", "/usuarios", "{}").statusCode());
        String usuario = "{\"nome\":\"Pessoa Teste\",\"email\":\"teste@gmail.com\",\"senha\":\"Teste123!\"}";
        var cadastro = enviar(a, "POST", "/usuarios", usuario);
        assertEquals(201, cadastro.statusCode(), cadastro.body());
        assertFalse(cadastro.body().contains("senha"));
        assertNotEquals("Teste123!", jdbc.queryForObject("SELECT senha FROM usuarios WHERE email = 'teste@gmail.com'", String.class));
        assertEquals(409, enviar(a, "POST", "/usuarios", usuario).statusCode());
        assertEquals(401, enviar(a, "POST", "/usuarios/login", "{\"email\":\"teste@gmail.com\",\"senha\":\"errada\"}").statusCode());
        assertEquals(200, enviar(a, "POST", "/usuarios/login", usuario).statusCode());
        assertEquals(400, enviar(a, "POST", "/apontamentos/exportar", "{\"semana\":\"31/02/2026 - 08/03/2026\"}").statusCode());
        assertEquals("[]", enviar(a, "GET", "/apontamentos", "").body());
        String nota = "{\"inicioEm\":\"2026-08-24T15:00\",\"titulo\":\"Nota teste\",\"conteudo\":\"Conteudo\"}";
        assertEquals(201, enviar(a, "POST", "/apontamentos", nota).statusCode());
        Integer id = jdbc.queryForObject("SELECT id FROM apontamento WHERE titulo = 'Nota teste'", Integer.class);
        assertTrue(enviar(a, "GET", "/apontamentos", "").body().contains("Nota teste"));
        String outro = usuario.replace("teste@gmail.com", "outro@gmail.com");
        assertEquals(201, enviar(b, "POST", "/usuarios", outro).statusCode());
        assertEquals(200, enviar(b, "POST", "/usuarios/login", outro).statusCode());
        assertEquals(403, enviar(b, "GET", "/apontamentos?emailUsuario=teste%40gmail.com", "").statusCode());
        assertEquals(403, enviar(b, "PUT", "/apontamentos/" + id, nota).statusCode());
        assertEquals(403, enviar(b, "GET", "/apontamentos/" + id, "").statusCode());
        assertEquals(200, enviar(a, "GET", "/apontamentos/" + id, "").statusCode());
        assertEquals(403, enviar(b, "DELETE", "/apontamentos/" + id, "").statusCode());
        assertEquals(200, enviar(a, "PUT", "/apontamentos/" + id, nota.replace("Nota teste", "Nota editada")).statusCode());
        var exportacao = enviar(a, "POST", "/apontamentos/exportar", "{\"semana\":\"24 ago - 30 ago 2026\"}");
        assertEquals(200, exportacao.statusCode());
        assertTrue(exportacao.body().contains("Nota editada"));
        assertFalse(enviar(a, "POST", "/apontamentos/exportar", "{\"semana\":\"Outra semana\"}").body().contains("Nota editada"));
        assertEquals(204, enviar(a, "DELETE", "/apontamentos/" + id, "").statusCode());
        assertEquals(404, enviar(a, "DELETE", "/apontamentos/" + id, "").statusCode());
        assertEquals(204, enviar(a, "POST", "/usuarios/logout", "").statusCode());
        assertEquals(401, enviar(a, "GET", "/usuarios/atual", "").statusCode());
    }
}
