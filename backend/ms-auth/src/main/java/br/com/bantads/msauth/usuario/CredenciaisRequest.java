package br.com.bantads.msauth.usuario;

import lombok.Getter;
import lombok.Setter;

/** Corpo de POST /auth/login — chamado só pelo Gateway, nunca pelo front direto. */
@Getter
@Setter
public class CredenciaisRequest {

    private String login;

    private String senha;
}
