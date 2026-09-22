package br.com.bantads.msconta.evento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EventoContaRepository extends JpaRepository<EventoConta, Long> {

    List<EventoConta> findByObjetoIdOrderByVersaoAsc(String objetoId);

    @Query("select coalesce(max(e.versao), 0) from EventoConta e where e.objetoId = :objetoId")
    int buscarUltimaVersao(@Param("objetoId") String objetoId);

    /**
     * Usado pelos testes de integração para limpar contas de teste antes de
     * rodar (idempotência entre execuções). @Transactional explícito porque
     * um delete derivado, chamado fora de outro contexto transacional, não
     * herda transação automaticamente do proxy do repositório.
     */
    @Transactional
    void deleteByObjetoId(String objetoId);
}
