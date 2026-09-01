package br.com.bantads.mscliente.solicitacao;

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
 * Espelha ms_cliente.solicitacoes (backend/ms-cliente/db/01-schema.sql). Entidade
 * mapeada já na S2 para validar a conexão com o schema; o fluxo de negócio de R1
 * (autocadastro) e R8/R9/R10 (decisão do gerente) entra na S4/S6.
 */
@Entity
@Table(schema = "ms_cliente", name = "solicitacoes")
@Getter
@Setter
@NoArgsConstructor
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cpf;

    private String nome;

    private String email;

    private String telefone;

    private BigDecimal salario;

    private String logradouro;

    private String numero;

    private String complemento;

    private String cep;

    private String cidade;

    @Column(length = 2)
    private String uf;

    private StatusSolicitacao status;

    private String motivo;

    @Column(name = "decidido_em")
    private OffsetDateTime decididoEm;

    @Column(name = "criado_em")
    private OffsetDateTime criadoEm;
}
