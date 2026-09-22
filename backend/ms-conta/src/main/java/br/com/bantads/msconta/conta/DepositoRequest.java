package br.com.bantads.msconta.conta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Corpo de POST /contas/{numeroConta}/deposito (R4). */
@Getter
@Setter
public class DepositoRequest {

    @NotNull
    @Positive
    private BigDecimal valor;
}
