package br.com.bantads.msauth.usuario;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByLogin(String login);

    Optional<Usuario> findByCpf(String cpf);

    /** Compensação da SAGA Aprovar Cliente (R9) — idempotente: não achar nada não é erro. */
    void deleteByCpf(String cpf);
}
