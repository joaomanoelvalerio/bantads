package br.com.bantads.msconta.conta;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, String> {

    Optional<Conta> findByCpfCliente(String cpfCliente);

    /** SAGA Aprovar Cliente (R9, passo 3) — "gerente com menos clientes" == menos contas (1 conta por cliente). */
    long countByCpfGerente(String cpfGerente);

    /** SAGA Inserir Gerente (R13, passo 3) — candidatos a doar uma conta ao gerente novo. */
    List<Conta> findByCpfGerenteIn(List<String> cpfsGerente);

    /** SAGA Remover Gerente (R15, passo 5) — todas as contas do gerente removido. */
    List<Conta> findByCpfGerente(String cpfGerente);
}
