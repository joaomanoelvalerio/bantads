package br.com.bantads.msgerente.gerente;

/** SAGA Remover Gerente (R15) — "não é permitido remover o último gerente ativo". */
public class UltimoGerenteAtivoException extends RuntimeException {

    public UltimoGerenteAtivoException() {
        super("Não é permitido remover o último gerente ativo");
    }
}
