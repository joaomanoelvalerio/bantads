package br.com.bantads.msauth.usuario;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Valida credenciais (login/senha). Não gera token nem acessa Redis — isso é
 * responsabilidade exclusiva do Gateway.
 */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Vazio se credencial inválida OU usuário inativo — mesma resposta nos dois casos, para não vazar qual delas. */
    public Optional<IdentidadeResponse> autenticar(String login, String senhaEmClaro) {
        return usuarioRepository.findByLogin(login)
                .filter(Usuario::isAtivo)
                .filter(usuario -> passwordEncoder.matches(senhaEmClaro, usuario.getSenha()))
                .map(usuario -> new IdentidadeResponse(usuario.getCpf(), usuario.getTipo()));
    }
}
