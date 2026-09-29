package br.com.bantads.msauth.usuario;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Popula os 9 usuários pré-cadastrados (docs/specs/05-dados-pre-cadastrados.md)
 * na primeira subida — só se a coleção estiver vazia (idempotente entre
 * reinicializações, análogo aos scripts de init do Postgres, mas em código:
 * MongoDB não tem um mecanismo de init script equivalente para rodar Java).
 * Hash Argon2 calculado com o mesmo encoder usado no login, garantindo
 * consistência.
 */
@Component
public class SeedRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedRunner(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        String senhaHash = passwordEncoder.encode("tads");

        List<Usuario> usuarios = List.of(
                novoUsuario("12912861012", "cli1@bantads.com.br", TipoUsuario.CLIENTE, senhaHash),
                novoUsuario("09506382000", "cli2@bantads.com.br", TipoUsuario.CLIENTE, senhaHash),
                novoUsuario("85733854057", "cli3@bantads.com.br", TipoUsuario.CLIENTE, senhaHash),
                novoUsuario("58872160006", "cli4@bantads.com.br", TipoUsuario.CLIENTE, senhaHash),
                novoUsuario("76179646090", "cli5@bantads.com.br", TipoUsuario.CLIENTE, senhaHash),
                novoUsuario("98574307084", "ger1@bantads.com.br", TipoUsuario.GERENTE, senhaHash),
                novoUsuario("64065268052", "ger2@bantads.com.br", TipoUsuario.GERENTE, senhaHash),
                novoUsuario("723862179060", "ger3@bantads.com.br", TipoUsuario.GERENTE, senhaHash),
                novoUsuario("40501740066", "ger4@bantads.com.br", TipoUsuario.GERENTE, senhaHash));

        usuarioRepository.saveAll(usuarios);
    }

    private Usuario novoUsuario(String cpf, String login, TipoUsuario tipo, String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.setCpf(cpf);
        usuario.setLogin(login);
        usuario.setTipo(tipo);
        usuario.setSenha(senhaHash);
        usuario.setAtivo(true);
        return usuario;
    }
}
