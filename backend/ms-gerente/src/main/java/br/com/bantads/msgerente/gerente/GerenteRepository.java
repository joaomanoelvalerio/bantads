package br.com.bantads.msgerente.gerente;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GerenteRepository extends JpaRepository<Gerente, String> {

    List<Gerente> findByAtivoTrueOrderByNomeAsc();
}
