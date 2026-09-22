package br.com.bantads.msconta.conta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findByNumeroContaOrderByDataHoraAsc(String numeroConta);

    /**
     * Usado pelos testes de integração para limpar contas de teste antes de
     * rodar (idempotência entre execuções). @Transactional explícito — ver
     * EventoContaRepository.deleteByObjetoId.
     */
    @Transactional
    void deleteByNumeroConta(String numeroConta);
}
