package br.com.bantads.msconta.conta;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContaService {

    private final ContaRepository contaRepository;

    public ContaService(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    public Conta buscarPorNumero(String numeroConta) {
        return contaRepository.findById(numeroConta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    /** R3 — tela inicial do cliente: resolve a conta a partir do CPF da sessão (cada cliente tem só uma). */
    public Conta buscarPorCpfCliente(String cpfCliente) {
        return contaRepository.findByCpfCliente(cpfCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    /**
     * Verificação de posse (R4/R5/R6/R7): a conta operada precisa pertencer ao
     * CPF do header X-User-CPF, injetado pelo Gateway após validar a sessão
     * (docs/specs/05-nao-funcionais/03-api-gateway.md). Conta inexistente -&gt; 404
     * (buscarPorNumero); conta de outro cliente -&gt; 403.
     */
    public Conta buscarEVerificarPosse(String numeroConta, String cpfSolicitante) {
        Conta conta = buscarPorNumero(numeroConta);
        if (!conta.getCpfCliente().equals(cpfSolicitante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta não pertence ao usuário autenticado");
        }
        return conta;
    }
}
