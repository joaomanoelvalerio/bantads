package br.com.bantads.mscliente.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Espelha ms_cliente.clientes (backend/ms-cliente/db/01-schema.sql). */
@Entity
@Table(schema = "ms_cliente", name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
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
}
