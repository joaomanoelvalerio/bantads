package br.com.bantads.msconta.conta;

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
 * (ver docs/design/modelagem-ct.md) — trabalho da S5.
 */
@Entity
@Table(schema = "ms_conta", name = "contas")
@Getter
@Setter
@NoArgsConstructor
public class Conta {

    @Id
    @Column(name = "numero_conta")
    private String numeroConta;

    @Column(name = "cpf_cliente")
    private String cpfCliente;

    @Column(name = "data_criacao")
    private LocalDate dataCriacao;

    private BigDecimal saldo;

    @Column(name = "cpf_gerente")
    private String cpfGerente;

    @Column(name = "ultima_versao_aplicada")
    private Integer ultimaVersaoAplicada;
}
