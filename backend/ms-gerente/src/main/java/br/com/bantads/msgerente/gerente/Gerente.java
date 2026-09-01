package br.com.bantads.msgerente.gerente;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Espelha ms_gerente.gerentes (backend/ms-gerente/db/01-schema.sql). */
@Entity
@Table(schema = "ms_gerente", name = "gerentes")
@Getter
@Setter
@NoArgsConstructor
public class Gerente {

    @Id
    private String cpf;

    private String nome;

    private String email;

    private String telefone;

    private boolean ativo;
}
