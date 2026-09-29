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

    /** R14 — e-mail (login) e CPF não são alteráveis (docs/specs/02-requisitos-funcionais.md). */
    public Gerente atualizar(String cpf, String nome, String telefone) {
        Gerente gerente = buscarPorCpf(cpf);
        gerente.setNome(nome);
        gerente.setTelefone(telefone);
        return gerenteRepository.save(gerente);
    }
}
