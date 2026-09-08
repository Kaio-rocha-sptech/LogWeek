package school.sptech.logweek_api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/apontamentos")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ApontamentoController {
    private final JdbcTemplate jdbcTemplate;
    private final BeanPropertyRowMapper<Apontamento> mapearNota = new BeanPropertyRowMapper<>(Apontamento.class);
    public ApontamentoController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    private String verificarUsuario(HttpServletRequest request, String emailInformado) {
        String email = UsuarioController.emailDaSessao(request);
        if (emailInformado != null && !email.equalsIgnoreCase(emailInformado.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesse apenas seus apontamentos.");
        }
        return email;
    }
    private void validar(Apontamento nota) {
        if (nota.getConteudo() == null) nota.setConteudo("");
        if (nota.getTitulo() == null || nota.getTitulo().isBlank() || nota.getTitulo().length() > 200 ||
                nota.getConteudo().length() > 100000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um título de até 200 caracteres. A observação pode ter até 100000 caracteres.");
        }
    }

    private void prepararHorarios(Apontamento nota, Apontamento original) {
        if (original != null && original.getInicioEm() == null) {
            if (nota.getInicioEm() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta nota antiga não possui período registrado.");
            nota.setFimEm(null);
            nota.setSemana(original.getSemana());
            return;
        }
        try {
            // O término vem do servidor na criação e não muda na edição.
            LocalDateTime fim = original == null ? TempoApontamento.agora() : TempoApontamento.dataHora(original.getFimEm());
            String inicioInformado = nota.getInicioEm();
            if (inicioInformado == null && original != null) inicioInformado = original.getInicioEm();
            LocalDateTime inicio = inicioInformado == null ? fim : TempoApontamento.dataHora(inicioInformado);
            if (inicio.isAfter(fim)) throw new IllegalArgumentException();
            nota.setInicioEm(inicio.toString());
            nota.setFimEm(fim.toString());
            nota.setSemana(TempoApontamento.periodo(TempoApontamento.segundaFeira(inicio.toLocalDate())));
        } catch (RuntimeException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um início válido, anterior ou igual ao término.");
        }
    }
    private Apontamento buscar(Integer id, String email) {
        List<Apontamento> notas = jdbcTemplate.query("SELECT * FROM apontamento WHERE id = ?", mapearNota, id);
        if (notas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Apontamento nao encontrado.");
        Apontamento nota = notas.get(0);
        if (!nota.getEmailUsuario().equals(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este apontamento pertence a outro usuario.");
        return nota;
    }
    @GetMapping
    public List<Apontamento> listar(@RequestParam(required = false) String emailUsuario, HttpServletRequest request) {
        String email = verificarUsuario(request, emailUsuario);
        return jdbcTemplate.query("SELECT * FROM apontamento WHERE email_usuario = ? ORDER BY criado_em DESC, id DESC",
                mapearNota, email);
    }
    @GetMapping("/{id}")
    public Apontamento buscarPorId(@PathVariable Integer id, HttpServletRequest request) {
        return buscar(id, UsuarioController.emailDaSessao(request));
    }
    @PostMapping
    public ResponseEntity<Apontamento> criar(@RequestBody Apontamento nota, HttpServletRequest request) {
        String email = verificarUsuario(request, nota.getEmailUsuario());
        validar(nota);
        prepararHorarios(nota, null);
        GeneratedKeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(conexao -> {
            PreparedStatement comando = conexao.prepareStatement(
                    "INSERT INTO apontamento (email_usuario, semana, titulo, conteudo, inicio_em, fim_em) VALUES (?, ?, ?, ?, ?, ?)", new String[]{"id"});
            comando.setString(1, email);
            comando.setString(2, nota.getSemana().trim());
            comando.setString(3, nota.getTitulo().trim());
            comando.setString(4, nota.getConteudo());
            comando.setString(5, nota.getInicioEm());
            comando.setString(6, nota.getFimEm());
            return comando;
        }, chave);
        return ResponseEntity.status(201).body(buscar(chave.getKey().intValue(), email));
    }
    @PutMapping("/{id}")
    public Apontamento editar(@PathVariable Integer id, @RequestBody Apontamento nota, HttpServletRequest request) {
        String email = verificarUsuario(request, nota.getEmailUsuario());
        Apontamento original = buscar(id, email);
        validar(nota);
        prepararHorarios(nota, original);
        jdbcTemplate.update("UPDATE apontamento SET semana = ?, titulo = ?, conteudo = ?, inicio_em = ?, atualizado_em = CURRENT_TIMESTAMP WHERE id = ? AND email_usuario = ?",
                nota.getSemana(), nota.getTitulo().trim(), nota.getConteudo(), nota.getInicioEm(), id, email);
        return buscar(id, email);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id, HttpServletRequest request) {
        String email = UsuarioController.emailDaSessao(request);
        buscar(id, email);
        jdbcTemplate.update("DELETE FROM apontamento WHERE id = ? AND email_usuario = ?", id, email);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/exportar")
    public ResponseEntity<String> exportar(@RequestBody Apontamento filtro, HttpServletRequest request) {
        String email = verificarUsuario(request, filtro.getEmailUsuario());
        LocalDate semana;
        try {
            semana = TempoApontamento.lerSemana(filtro.getSemana());
        } catch (RuntimeException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma semana válida para exportar.");
        }
        List<Apontamento> notas = jdbcTemplate.query("SELECT * FROM apontamento WHERE email_usuario = ?",
                mapearNota, email);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logweek-" + semana + ".txt")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).body(RelatorioSemanal.gerar(notas, semana));
    }
}

