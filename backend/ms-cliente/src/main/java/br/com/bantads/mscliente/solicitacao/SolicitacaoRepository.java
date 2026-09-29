package br.com.bantads.mscliente.solicitacao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    List<Solicitacao> findAllByOrderByCriadoEmDesc();

    /**
     * No máximo uma linha "ativa" (Pendente ou Aprovado) por CPF a qualquer
     * momento — garantido pelos índices únicos parciais do schema — então
     * filtrar por cpf + status identifica a solicitação sem ambiguidade,
     * mesmo que o CPF tenha histórico de tentativas anteriores.
     */
    Optional<Solicitacao> findFirstByCpfAndStatus(String cpf, StatusSolicitacao status);
}
