package br.com.bantads.msauth.usuario;

/**
 * Resposta de POST /auth/login bem-sucedido. Só cpf/tipo — o Gateway é quem
 * compõe nome/e-mail consultando MS Cliente/Gerente
 * ("Login é uma API Composition").
 */
public record IdentidadeResponse(String cpf, TipoUsuario tipo) {
}
