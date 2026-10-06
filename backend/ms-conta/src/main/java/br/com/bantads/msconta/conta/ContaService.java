package br.com.bantads.msconta.conta;

import br.com.bantads.msconta.evento.EventoContaRepository;
import br.com.bantads.msconta.evento.EventoContaService;
import br.com.bantads.msconta.evento.TipoEvento;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContaService {

    private static final int TENTATIVAS_MAXIMAS_NUMERO = 20;

    private final ContaRepository contaRepository;
    private final EventoContaRepository eventoContaRepository;
    private final EventoContaService eventoContaService;
    private final SecureRandom aleatorio = new SecureRandom();

    public ContaService(
            ContaRepository contaRepository,
            EventoContaRepository eventoContaRepository,
            EventoContaService eventoContaService) {
        this.contaRepository = contaRepository;
        this.eventoContaRepository = eventoContaRepository;
        this.eventoContaService = eventoContaService;
    }

    /** Uso interno (Gateway compõe R11/R12/R16) — ver comentário em ContaController.listarTodas. */
    public List<Conta> listarTodas() {
        return contaRepository.findAll();
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
     * CPF do header X-User-CPF, injetado pelo Gateway após validar a sessão.
     * Conta inexistente -&gt; 404
     * (buscarPorNumero); conta de outro cliente -&gt; 403.
     */
    public Conta buscarEVerificarPosse(String numeroConta, String cpfSolicitante) {
        Conta conta = buscarPorNumero(numeroConta);
        if (!conta.getCpfCliente().equals(cpfSolicitante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta não pertence ao usuário autenticado");
        }
        return conta;
    }

    /**
     * SAGA Aprovar Cliente (R9, passo 6) — número de 4 dígitos sorteado, único
     * (em colisão, sorteia de novo — R9); grava o evento Criado no command
     * side E o read model diretamente, sem esperar a projeção assíncrona: o
     * Orquestrador precisa do número já definido na resposta, e
     * ProjecaoContaListener.aplicarCriacao já é idempotente
     * (`existsById`), então não duplica quando o evento chegar depois — mesmo
     * padrão do script de seed (db/02-seed.sql), que grava os dois lados
     * juntos.
     */
    @Transactional
    public Conta criarConta(String cpfCliente, String cpfGerente) {
        String numeroConta = sortearNumeroUnico();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cpfCliente", cpfCliente);
        payload.put("cpfGerente", cpfGerente);
        eventoContaService.registrar(numeroConta, TipoEvento.CRIADO, payload);

        Conta conta = new Conta();
        conta.setNumeroConta(numeroConta);
        conta.setCpfCliente(cpfCliente);
        conta.setCpfGerente(cpfGerente);
        conta.setDataCriacao(LocalDate.now());
        conta.setSaldo(BigDecimal.ZERO);
        conta.setUltimaVersaoAplicada(1);
        return contaRepository.save(conta);
    }

    /**
     * Compensação do passo 6 — só é chamada para uma conta recém-criada nesta
     * mesma SAGA (nunca movimentada), então apagar os dois lados é seguro.
     * Idempotente: conta já removida (retry) não é erro.
     */
    @Transactional
    public void remover(String numeroConta) {
        eventoContaRepository.deleteByObjetoId(numeroConta);
        if (contaRepository.existsById(numeroConta)) {
            contaRepository.deleteById(numeroConta);
        }
    }

    /**
     * SAGA Inserir Gerente (R13, passo 3) — escolhe o gerente ativo com MAIS
     * contas (empate: MENOR saldo agregado), depois pega a conta de MENOR
     * saldo desse gerente. Se o máximo de contas entre os candidatos for
     * menor que 2, ninguém é escolhido — transferir tiraria a única conta de
     * alguém, e "a inserção nunca deixa um gerente existente com 0 contas"
     * (R13). `cpfsGerentesAtivos` já
     * exclui o gerente recém-criado (que começa sem contas mesmo).
     */
    public Optional<Conta> identificarContaParaTransferir(List<String> cpfsGerentesAtivos) {
        List<Conta> contasDosCandidatos = contaRepository.findByCpfGerenteIn(cpfsGerentesAtivos);

        Map<String, List<Conta>> porGerente = contasDosCandidatos.stream()
                .collect(Collectors.groupingBy(Conta::getCpfGerente));

        Optional<List<Conta>> escolhido = porGerente.values().stream()
                .max(Comparator
                        .<List<Conta>>comparingInt(List::size)
                        .thenComparing(contas -> somaSaldo(contas).negate()));

        if (escolhido.isEmpty() || escolhido.get().size() < 2) {
            return Optional.empty();
        }

        return escolhido.get().stream().min(Comparator.comparing(Conta::getSaldo));
    }

    private BigDecimal somaSaldo(List<Conta> contas) {
        return contas.stream().map(Conta::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * SAGA Inserir Gerente (R13, passo 4) e sua compensação (reassocia ao
     * gerente original chamando isto de novo com o cpf antigo). Evento
     * `GerenteAlterado` já era tratado por `ProjecaoContaListener` desde a
     * S4 (antecipado), mas nada disparava esse evento até agora — grava o
     * evento e sincroniza o read model direto, mesmo padrão de criarConta.
     */
    @Transactional
    public void atribuirGerente(String numeroConta, String cpfGerenteNovo) {
        eventoContaService.registrar(numeroConta, TipoEvento.GERENTE_ALTERADO, Map.of("cpfGerenteNovo", cpfGerenteNovo));

        Conta conta = buscarPorNumero(numeroConta);
        conta.setCpfGerente(cpfGerenteNovo);
        contaRepository.save(conta);
    }

    /**
     * SAGA Remover Gerente (R15, passo 5) — transfere TODAS as contas do
     * gerente removido pro gerente ativo com MENOS contas no momento
     * (R15). `cpfsGerentesAtivos` já
     * exclui o removido (o passo 1 já o inativou antes deste passo rodar).
     * Devolve vazio se o gerente removido não tinha nenhuma conta — passo
     * trivial, a SAGA segue em frente sem transferir nada.
     */
    @Transactional
    public Optional<TransferenciaDeContas> transferirTodasDoGerente(String cpfGerenteOrigem, List<String> cpfsGerentesAtivos) {
        List<Conta> contasDoGerente = contaRepository.findByCpfGerente(cpfGerenteOrigem);
        if (contasDoGerente.isEmpty()) {
            return Optional.empty();
        }

        Map<String, List<Conta>> porGerente = contaRepository.findByCpfGerenteIn(cpfsGerentesAtivos).stream()
                .collect(Collectors.groupingBy(Conta::getCpfGerente));

        String cpfGerenteDestino = cpfsGerentesAtivos.stream()
                .min(Comparator.comparingInt(cpf -> porGerente.getOrDefault(cpf, List.of()).size()))
                .orElseThrow(() -> new IllegalStateException("Nenhum gerente ativo candidato a receber as contas"));

        List<String> numerosConta = new ArrayList<>();
        List<String> cpfsClientes = new ArrayList<>();
        for (Conta conta : contasDoGerente) {
            atribuirGerente(conta.getNumeroConta(), cpfGerenteDestino);
            numerosConta.add(conta.getNumeroConta());
            cpfsClientes.add(conta.getCpfCliente());
        }

        return Optional.of(new TransferenciaDeContas(cpfGerenteDestino, numerosConta, cpfsClientes));
    }

    @Transactional
    public void reverterTransferenciaDoGerente(List<String> numerosConta, String cpfGerenteDestino, String cpfGerenteOrigem) {
        for (String numeroConta : numerosConta) {
            Conta conta = buscarPorNumero(numeroConta);
            if (cpfGerenteDestino.equals(conta.getCpfGerente())) {
                atribuirGerente(numeroConta, cpfGerenteOrigem);
            }
        }
    }

    public record TransferenciaDeContas(String cpfGerenteDestino, List<String> numerosConta, List<String> cpfsClientes) {
    }

    private String sortearNumeroUnico() {
        for (int tentativa = 1; tentativa <= TENTATIVAS_MAXIMAS_NUMERO; tentativa++) {
            String candidato = String.format("%04d", aleatorio.nextInt(10_000));
            if (!contaRepository.existsById(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException("Não foi possível sortear um número de conta único após "
                + TENTATIVAS_MAXIMAS_NUMERO + " tentativas");
    }
}
