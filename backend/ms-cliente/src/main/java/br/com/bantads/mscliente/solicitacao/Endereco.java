package br.com.bantads.mscliente.solicitacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Endereço aninhado no corpo de POST /solicitacoes (R1) — contrato da API, não a entidade. */
@Getter
@Setter
public class Endereco {

    @NotBlank
    @Size(max = 160)
    private String logradouro;

    @NotBlank
    @Size(max = 20)
    private String numero;

    @Size(max = 80)
    private String complemento;

    @NotBlank
    @Size(max = 9)
    private String cep;

    @NotBlank
    @Size(max = 120)
    private String cidade;

    @NotBlank
    @Size(max = 2)
    private String uf;
}
