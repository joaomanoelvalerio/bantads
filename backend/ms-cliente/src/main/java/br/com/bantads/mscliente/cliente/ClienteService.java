package br.com.bantads.mscliente.cliente;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorCpf(String cpf) {
        return clienteRepository.findById(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    /**
     * SAGA Aprovar Cliente (R9, passo 4) — copia os dados da solicitação já
     * aprovada. Idempotente: uma reentrega do comando (mesmo cpf) não duplica
     * nem falha, só devolve o que já existe — RabbitMQ entrega at-least-once.
     */
    public Cliente criar(Cliente novoCliente) {
        return clienteRepository.findById(novoCliente.getCpf()).orElseGet(() -> clienteRepository.save(novoCliente));
    }

    /** Compensação do passo 4 — idempotente: não achar nada não é erro. */
    public void remover(String cpf) {
        clienteRepository.deleteByCpf(cpf);
    }
}
