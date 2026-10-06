package br.com.bantads.msauth.usuario;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Espelha o enunciado — "Dados de Usuário: Id
 * usuário, CPF da pessoa, Tipo (cliente/gerente), login, senha (hash Argon2),
 * Ativo/Inativo". `login` é o e-mail do cliente/gerente — único, é a fonte da
 * verdade.
 */
@Document(collection = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String login;

    private String cpf;

    private TipoUsuario tipo;

    /** Hash Argon2 — a senha em claro nunca é persistida. */
    private String senha;

    private boolean ativo;
}
