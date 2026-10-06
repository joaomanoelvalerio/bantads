package br.com.bantads.msgerente.gerente;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GerenteRepository extends JpaRepository<Gerente, String> {

    List<Gerente> findByAtivoTrueOrderByNomeAsc();

    /** SAGA Remover Gerente (R15) — "não é permitido remover o último gerente ativo". */
    long countByAtivoTrue();
}
