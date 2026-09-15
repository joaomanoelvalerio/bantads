package br.com.bantads.msconta.conta;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, String> {

    Optional<Conta> findByCpfCliente(String cpfCliente);
}
