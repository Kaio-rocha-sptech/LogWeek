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
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UsuarioController {
    private final JdbcTemplate jdbcTemplate;
    private final BeanPropertyRowMapper<Usuario> mapearUsuario = new BeanPropertyRowMapper<>(Usuario.class);
    public UsuarioController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    private boolean emailValido(String email) {
        if (email == null || email.length() > 254 || email.contains(" ")) return false;
        String[] dominios = {"@email.com", "@gmail.com", "@hotmail.com", "@outlook.com", "@sptech.school"};
        for (String dominio : dominios) {
            if (email.endsWith(dominio) && email.indexOf('@') > 0 && email.indexOf('@') == email.lastIndexOf('@')) return true;
        }
        return false;
    }
    private boolean senhaValida(String senha) {
        if (senha == null || senha.length() < 8 || senha.length() > 128) return false;
        boolean letra = false;
        boolean especial = false;
        for (int i = 0; i < senha.length(); i++) {
            if (Character.isLetter(senha.charAt(i))) letra = true;
            if ("@!#$*&-_=+".indexOf(senha.charAt(i)) >= 0) especial = true;
        }
        return letra && especial;
    }
    private boolean nomeValido(String nome) {
        if (nome == null || nome.trim().length() < 6 || nome.length() > 100) return false;
        for (int i = 0; i < nome.length(); i++) {
            if (!Character.isLetter(nome.charAt(i)) && nome.charAt(i) != ' ') return false;
        }
        return true;
    }
    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> novoUsuario(@RequestBody Usuario usuario) {
        if (usuario.getEmail() != null) usuario.setEmail(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
        if (!nomeValido(usuario.getNome())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome: use de 6 a 100 caracteres, somente letras e espaços.");
        if (!emailValido(usuario.getEmail())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use um email válido de email.com, gmail.com, hotmail.com, outlook.com ou sptech.school.");
        if (!senhaValida(usuario.getSenha())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha: use de 8 a 128 caracteres, uma letra e um símbolo: @ ! # $ * & - _ = +.");
        try {
            jdbcTemplate.update("INSERT INTO usuarios (nome, email, senha) VALUES (?, ?, ?)",
                    usuario.getNome().trim(), usuario.getEmail(), SenhaUtil.gerar(usuario.getSenha()));
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
        if (anterior != null) anterior.invalidate();
        Usuario salvo = encontrados.get(0);
        request.getSession(true).setAttribute("emailUsuario", salvo.getEmail());
        return salvo;
    }
    public static String emailDaSessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao == null || sessao.getAttribute("emailUsuario") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre para acessar seus apontamentos.");
        }
        return (String) sessao.getAttribute("emailUsuario");
    }
    @GetMapping("/auth/me")
    public Usuario usuarioAtual(HttpServletRequest request) {
        return jdbcTemplate.queryForObject("SELECT * FROM usuarios WHERE email = ?",
                mapearUsuario, emailDaSessao(request));
    }
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> sair(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
        return ResponseEntity.noContent().build();
    }
}

