package br.com.bantads.mscliente.solicitacao;

import java.time.OffsetDateTime;
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
