package br.com.bantads.msconta.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Corpo de POST /contas/{numeroConta}/transferencia (R6). cpfDestino é
 * resolvido pelo próprio MS Conta a partir de contaDestino — não precisa vir
 * no corpo. nomeOrigem/nomeDestino são opcionais aqui: por especificação
 * (docs/specs/05-nao-funcionais/10-cqrs.md) quem os obtém é o API Gateway,
 * consultando o MS Cliente, e os inclui ao enriquecer a requisição antes de
 * rotear — como o Gateway ainda não existe neste repo (S5, escopo PL), o
 * endpoint aceita a chamada sem eles (nomes ficam null no extrato até o
 * enriquecimento ser ligado) em vez de depender de um serviço que ainda não
 * roda — ver docs/design/suposicoes.md.
 */
@Getter
@Setter
public class TransferenciaRequest {

    @NotBlank
    private String contaDestino;

    @NotNull
    @Positive
    private BigDecimal valor;

    private String nomeOrigem;

    private String nomeDestino;
}
