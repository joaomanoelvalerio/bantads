package br.com.bantads.msconta.evento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoContaRepository extends JpaRepository<EventoConta, Long> {

    List<EventoConta> findByObjetoIdOrderByVersaoAsc(String objetoId);

    @Query("select coalesce(max(e.versao), 0) from EventoConta e where e.objetoId = :objetoId")
    int buscarUltimaVersao(@Param("objetoId") String objetoId);
}
