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

    public Gerente buscarPorCpf(String cpf) {
        return gerenteRepository.findById(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gerente não encontrado"));
    }
}
