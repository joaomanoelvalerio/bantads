package br.com.bantads.msgerente.gerente;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GerenteService {

    private final GerenteRepository gerenteRepository;

    public GerenteService(GerenteRepository gerenteRepository) {
        this.gerenteRepository = gerenteRepository;
    }

    public List<Gerente> listarTodos() {
        return gerenteRepository.findAll();
    }

    /** SAGA Aprovar Cliente (R9, passo 2) e R12 — gerentes inativos não recebem novos clientes. */
    public List<Gerente> listarAtivos() {
        return gerenteRepository.findByAtivoTrueOrderByNomeAsc();
    }

    public Gerente buscarPorCpf(String cpf) {
        return gerenteRepository.findById(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gerente não encontrado"));
    }

    /** R14 — e-mail (login) e CPF não são alteráveis. */
    public Gerente atualizar(String cpf, String nome, String telefone) {
        Gerente gerente = buscarPorCpf(cpf);
        gerente.setNome(nome);
        gerente.setTelefone(telefone);
        return gerenteRepository.save(gerente);
    }

    /**
     * SAGA Remover Gerente (R15, passo 1). Idempotente: se já está inativo
     * (reentrega do comando), não faz nada — em particular, não refaz a
     * checagem de "último ativo", que já não contaria mais com este CPF e
     * daria falso positivo. "Não é permitido remover o último gerente
     * ativo" (R15) só se aplica à
     * transição ativo → inativo de verdade.
     */
    public void inativar(String cpf) {
        Gerente gerente = buscarPorCpf(cpf);
        if (!gerente.isAtivo()) {
            return;
        }
        if (gerenteRepository.countByAtivoTrue() <= 1) {
            throw new UltimoGerenteAtivoException();
        }
        gerente.setAtivo(false);
        gerenteRepository.save(gerente);
    }

    /** Compensação do passo 1 — idempotente: gerente inexistente não é erro. */
    public void reativar(String cpf) {
        gerenteRepository.findById(cpf).ifPresent(gerente -> {
            gerente.setAtivo(true);
            gerenteRepository.save(gerente);
        });
    }
}
