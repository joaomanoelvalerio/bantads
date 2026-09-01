package br.com.bantads.msconta.conta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Espelha ms_conta.movimentacoes — histórico para o extrato (R7), read model do
 * CQRS. Mapeada já na S2 para validar a conexão com o schema; a consulta de
 * extrato em si é trabalho da S5.
 */
@Entity
@Table(schema = "ms_conta", name = "movimentacoes")
@Getter
@Setter
@NoArgsConstructor
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_conta")
    private String numeroConta;

    @Column(name = "data_hora")
    private OffsetDateTime dataHora;

    private TipoMovimentacao tipo;

    @Column(name = "cpf_origem")
    private String cpfOrigem;

    @Column(name = "nome_origem")
    private String nomeOrigem;

    @Column(name = "cpf_destino")
    private String cpfDestino;

    @Column(name = "nome_destino")
    private String nomeDestino;

    private BigDecimal valor;

    @Column(name = "evento_id")
    private Long eventoId;
}
