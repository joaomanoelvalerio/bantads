package br.com.bantads.msconta.conta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, String> {
}
