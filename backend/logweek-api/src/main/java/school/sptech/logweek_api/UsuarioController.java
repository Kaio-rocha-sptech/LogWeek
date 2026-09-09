package school.sptech.logweek_api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        },
        allowCredentials = "true"
)
public class UsuarioController {
    private final JdbcTemplate jdbcTemplate;
    private final BeanPropertyRowMapper<Usuario> mapearUsuario = new BeanPropertyRowMapper<>(Usuario.class);

    public UsuarioController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<Usuario> novoUsuario(@RequestBody Usuario usuario) {
        String nome = usuario.getNome();
        String senha = usuario.getSenha();
        if (usuario.getEmail() != null) {
            usuario.setEmail(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um nome de até 100 caracteres.");
        }
        if (!emailValido(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um email válido de até 254 caracteres.");
        }
        if (senha == null || senha.isBlank() || senha.length() < 8 || senha.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma senha de 8 a 128 caracteres.");
        }
        try {
            jdbcTemplate.update("INSERT INTO usuarios (nome, email, senha) VALUES (?, ?, ?)",
                    nome.trim(), usuario.getEmail(), SenhaUtil.gerar(senha));
        } catch (DuplicateKeyException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email ja esta cadastrado.");
        }
        Usuario salvo = jdbcTemplate.queryForObject("SELECT * FROM usuarios WHERE email = ?",
                mapearUsuario, usuario.getEmail());
        return ResponseEntity.status(201).body(salvo);
    }

    @PostMapping("/login")
    public Usuario validarLogin(@RequestBody Usuario usuario, HttpServletRequest request) {
        if (usuario.getEmail() == null || usuario.getEmail().isBlank() || usuario.getSenha() == null || usuario.getSenha().isBlank() || usuario.getSenha().length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe email e senha.");
        }
        List<Usuario> encontrados = jdbcTemplate.query("SELECT * FROM usuarios WHERE email = ?",
                mapearUsuario, usuario.getEmail().trim().toLowerCase(Locale.ROOT));
        if (encontrados.isEmpty() || !SenhaUtil.conferir(usuario.getSenha(), encontrados.get(0).getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha incorretos.");
        }
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        Usuario salvo = encontrados.get(0);
        request.getSession(true).setAttribute("emailUsuario", salvo.getEmail());
        return salvo;
    }

    @GetMapping("/atual")
    public Usuario usuarioAtual(HttpServletRequest request) {
        return jdbcTemplate.queryForObject("SELECT * FROM usuarios WHERE email = ?",
                mapearUsuario, emailDaSessao(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> sair(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private boolean emailValido(String email) {
        if (email == null || email.length() > 254 || email.contains(" ")) {
            return false;
        }
        int arroba = email.indexOf('@');
        return arroba > 0 && arroba < email.length() - 1 && arroba == email.lastIndexOf('@');
    }

    // Reutilizado na controller de apontamentos para identificar o dono das notas.
    public static String emailDaSessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao == null || sessao.getAttribute("emailUsuario") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre para acessar seus apontamentos.");
        }
        return (String) sessao.getAttribute("emailUsuario");
    }
}
