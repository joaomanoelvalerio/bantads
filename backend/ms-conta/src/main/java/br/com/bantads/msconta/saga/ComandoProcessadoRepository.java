package br.com.bantads.msconta.saga;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComandoProcessadoRepository extends JpaRepository<ComandoProcessado, ComandoProcessadoId> {

    Optional<ComandoProcessado> findByIdSagaIdAndIdTipo(String sagaId, String tipo);
}
