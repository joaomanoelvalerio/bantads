package br.com.bantads.mscliente.solicitacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    private String cpf;

    @NotBlank
    private String nome;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefone;

    @NotNull
    @Positive
    private BigDecimal salario;

    @NotBlank
    private String logradouro;

    @NotBlank
    private String numero;

    private String complemento;

    @NotBlank
    private String cep;

    @NotBlank
    private String cidade;

    @NotBlank
    private String uf;
}
