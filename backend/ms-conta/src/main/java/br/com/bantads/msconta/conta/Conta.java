package br.com.bantads.msconta.conta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Espelha ms_conta.contas — read model desnormalizado, lado QUERY do CQRS
 * (backend/ms-conta/db/01-schema.sql). O saldo aqui é só para leitura (R3/R7/
 * R11/R16); validação de saque/transferência sempre replaya o command side
 * — trabalho da S5.
 */
@Entity
@Table(schema = "ms_conta", name = "contas")
@Getter
@Setter
@NoArgsConstructor
public class Conta {

    // Coluna/campo interno seguem "numeroConta" (nome já usado em toda a
    // camada de persistência/serviço); só a serialização JSON precisa bater
    // com o contrato SwaggerHub, que chama esse campo de "numero".
    @Id
    @Column(name = "numero_conta")
    @JsonProperty("numero")
    private String numeroConta;

    @Column(name = "cpf_cliente")
    private String cpfCliente;

    @Column(name = "data_criacao")
    private LocalDate dataCriacao;

    private BigDecimal saldo;

    @Column(name = "cpf_gerente")
    private String cpfGerente;

    // Guarda interna da projeção idempotente (ProjecaoContaListener) — não faz
    // parte do contrato público do recurso Conta, não deve vazar na resposta.
    @Column(name = "ultima_versao_aplicada")
    @JsonIgnore
    private Integer ultimaVersaoAplicada;
}
