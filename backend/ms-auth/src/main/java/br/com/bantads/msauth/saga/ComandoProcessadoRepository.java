package br.com.bantads.msauth.saga;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ComandoProcessadoRepository extends MongoRepository<ComandoProcessado, String> {

    Optional<ComandoProcessado> findBySagaIdAndTipo(String sagaId, String tipo);
}
