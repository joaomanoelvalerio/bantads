package br.com.bantads.mscliente.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    /**
     * Compensação da SAGA Aprovar Cliente (R9) — idempotente: não achar nada
     * não é erro. @Transactional explícito porque um delete derivado, chamado
     * fora de outro contexto transacional, não herda transação automaticamente
     * do proxy do repositório (mesmo motivo de EventoContaRepository.deleteByObjetoId no MS Conta).
     */
    @Transactional
    void deleteByCpf(String cpf);
}
