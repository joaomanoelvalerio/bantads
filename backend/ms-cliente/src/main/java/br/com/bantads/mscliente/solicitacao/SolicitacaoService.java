package br.com.bantads.mscliente.solicitacao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    /** R8 — todas as solicitações, em qualquer status. */
    public List<Solicitacao> listarTodas() {
        return solicitacaoRepository.findAllByOrderByCriadoEmDesc();
    }

    /** SAGA Aprovar Cliente (R9, passo 1) — devolve os dados pro Orquestrador compor os próximos passos. */
    public Optional<Solicitacao> aprovarPendente(String cpf) {
        Optional<Solicitacao> pendente = solicitacaoRepository.findFirstByCpfAndStatus(cpf, StatusSolicitacao.PENDENTE);
        pendente.ifPresent(solicitacao -> {
            solicitacao.setStatus(StatusSolicitacao.APROVADO);
            solicitacao.setDecididoEm(OffsetDateTime.now());
            solicitacaoRepository.save(solicitacao);
        });
        return pendente;
    }

    /**
     * Compensação do passo 1 — caso geral: devolve a Pendente. Idempotente:
     * se não há uma linha Aprovada pra esse CPF (já revertida, ou nunca
     * chegou a ser aprovada), não faz nada.
     */
    public void reverterParaPendente(String cpf) {
        solicitacaoRepository.findFirstByCpfAndStatus(cpf, StatusSolicitacao.APROVADO).ifPresent(solicitacao -> {
            solicitacao.setStatus(StatusSolicitacao.PENDENTE);
            solicitacao.setDecididoEm(null);
            solicitacaoRepository.save(solicitacao);
        });
    }

    /**
     * Compensação do passo 1 — caso especial (login duplicado no MS Auth,
     * passo 5): NÃO devolve a Pendente, pois a mesma tentativa falharia de
     * novo indefinidamente (docs/specs/05-nao-funcionais/09-sagas-api-compositions.md).
     * Idempotente como a reversão normal.
     */
    public void marcarNaoAprovada(String cpf, String motivo) {
        solicitacaoRepository.findFirstByCpfAndStatus(cpf, StatusSolicitacao.APROVADO).ifPresent(solicitacao -> {
            solicitacao.setStatus(StatusSolicitacao.NAO_APROVADO);
            solicitacao.setMotivo(motivo);
            solicitacao.setDecididoEm(OffsetDateTime.now());
            solicitacaoRepository.save(solicitacao);
        });
    }

    /**
     * R1 — Autocadastro: grava a solicitação como Pendente e retorna (operação
     * síncrona; a aprovação é feita depois pelo gerente — R9). Unicidade de CPF
     * e e-mail entre solicitações "ativas" (Pendente/Aprovado) é garantida por
     * índices únicos parciais no banco (ver db/01-schema.sql e
     * docs/design/suposicoes.md) — mais seguro contra corrida do que checar e
     * inserir em dois passos separados.
     */
    public Solicitacao autocadastrar(NovoClienteRequest requisicao) {
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setCpf(requisicao.getCpf());
        solicitacao.setNome(requisicao.getNome());
        solicitacao.setEmail(requisicao.getEmail());
        solicitacao.setTelefone(requisicao.getTelefone());
        solicitacao.setSalario(requisicao.getSalario());
        solicitacao.setLogradouro(requisicao.getLogradouro());
        solicitacao.setNumero(requisicao.getNumero());
        solicitacao.setComplemento(requisicao.getComplemento());
        solicitacao.setCep(requisicao.getCep());
        solicitacao.setCidade(requisicao.getCidade());
        solicitacao.setUf(requisicao.getUf());
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);
        solicitacao.setCriadoEm(OffsetDateTime.now());

        try {
            return solicitacaoRepository.saveAndFlush(solicitacao);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagemDeConflito(ex));
        }
    }

    private String mensagemDeConflito(DataIntegrityViolationException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (causa.contains("uq_solicitacoes_cpf_ativa")) {
            return "Já existe uma solicitação em andamento para este CPF";
        }
        if (causa.contains("uq_solicitacoes_email_ativa")) {
            return "Já existe uma solicitação em andamento para este e-mail";
        }
        return "Solicitação conflita com um cadastro existente";
    }
}
