package br.com.bantads.mscliente.solicitacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Corpo de POST /clientes (R1). DTO dedicado — não vincula direto na entidade
 * Solicitacao para não expor campos controlados pelo servidor (status, motivo,
 * datas de decisão) a escrita pelo cliente da API.
 */
@Getter
@Setter
public class NovoClienteRequest {

    @NotBlank
    @Size(max = 14)
    private String cpf;

    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotBlank
    @Email
    @Size(max = 160)
    private String email;

    @NotBlank
    @Size(max = 20)
    private String telefone;

    @NotNull
    @Positive
    private BigDecimal salario;

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
