package br.com.bantads.msauth.saga;

import br.com.bantads.msauth.config.RabbitMqConfig;
import br.com.bantads.msauth.usuario.TipoUsuario;
import br.com.bantads.msauth.usuario.Usuario;
import br.com.bantads.msauth.usuario.UsuarioRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * SAGA Aprovar Cliente (R9, passo 5) e SAGA Inserir Gerente (R13, passo 2) —
 * mesmo comando `auth.criar-credencial` para as duas: R9 não manda `senha`
 * no payload (gera uma aleatória e devolve em claro na resposta, só pra
 * poder mandar por e-mail); R13 manda a senha escolhida no formulário
 * (docs/specs/02-requisitos-funcionais.md, R13 — "senha informada no
 * formulário, não enviada por e-mail", então não precisa vir de volta na
 * resposta). A senha em claro nunca é logada, gravada ou incluída no estado
 * da SAGA no Redis (docs/specs/05-nao-funcionais/09-sagas-api-compositions.md).
 */
@Component
public class AuthComandoListener {

    private static final Logger log = LoggerFactory.getLogger(AuthComandoListener.class);

    private static final String TIPO_CRIAR = "auth.criar-credencial";
    private static final String TIPO_REMOVER = "auth.remover-credencial";
    private static final String TIPO_DESATIVAR = "auth.desativar-credencial";
    private static final String TIPO_REATIVAR = "auth.reativar-credencial";

    /** Sem caracteres ambíguos (0/O, 1/l/I) — a senha vai por e-mail, não é digitada na hora. */
    private static final String ALFABETO_SENHA = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_SENHA = 12;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ComandoProcessadoRepository comandoProcessadoRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SecureRandom aleatorio = new SecureRandom();

    public AuthComandoListener(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            ComandoProcessadoRepository comandoProcessadoRepository,
            RabbitTemplate rabbitTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.comandoProcessadoRepository = comandoProcessadoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_COMANDO)
    public void aoReceberComando(ComandoSaga comando) {
        log.info("Comando SAGA recebido em ms.auth.cmd: sagaId={} tipo={}", comando.getSagaId(), comando.getTipo());

        switch (comando.getTipo()) {
            case TIPO_CRIAR -> criarCredencial(comando);
            case TIPO_REMOVER -> removerCredencial(comando);
            case TIPO_DESATIVAR -> desativarCredencial(comando);
            case TIPO_REATIVAR -> reativarCredencial(comando);
            default -> {
                log.warn("Tipo de comando desconhecido em ms.auth.cmd: {}", comando.getTipo());
                responder(comando, "FALHA", null, "Tipo de comando desconhecido: " + comando.getTipo());
            }
        }
    }

    /**
     * Idempotência por (sagaId, tipo) — docs/specs/05-nao-funcionais/07-rabbitmq-filas.md,
     * S8. Sem o marcador, uma reentrega bateria no `login` já criado pela
     * primeira tentativa e pareceria um "login_duplicado" de verdade — o
     * caso especial de R9 que marca a solicitação como Não aprovada, quando
     * na real a credencial já tinha sido criada com sucesso. O marcador de
     * idempotência NUNCA guarda a senha em claro (só confirma que esta saga
     * já passou por aqui) — ela só existe na resposta publicada em
     * `orquestrador.reply`, nunca em disco (ver Javadoc da classe).
     */
    private void criarCredencial(ComandoSaga comando) {
        if (comandoProcessadoRepository.findBySagaIdAndTipo(comando.getSagaId(), comando.getTipo()).isPresent()) {
            log.info("auth.criar-credencial já processado antes para a saga {} — confirmando sucesso de novo (sem repetir a senha)", comando.getSagaId());
            responder(comando, "SUCESSO", null, null);
            return;
        }

        String cpf = String.valueOf(comando.getPayload().get("cpf"));
        String login = String.valueOf(comando.getPayload().get("login"));
        String tipoBruto = String.valueOf(comando.getPayload().get("tipo"));

        if (usuarioRepository.findByLogin(login).isPresent()) {
            log.warn("Login já cadastrado para a saga {} — caso especial de R9, sem retornar a solicitação a Pendente", comando.getSagaId());
            // "motivo" estruturado em vez de casar texto de erro — é o que o
            // Orquestrador usa para distinguir este caso especial (compensação
            // do passo 1 marca "Não aprovada" em vez de devolver a Pendente).
            responder(comando, "FALHA", Map.of("motivo", "login_duplicado"), "Login já cadastrado");
            return;
        }

        try {
            Object senhaInformada = comando.getPayload().get("senha");
            boolean senhaVeioDoFormulario = senhaInformada != null && !String.valueOf(senhaInformada).isBlank();
            String senhaEmClaro = senhaVeioDoFormulario ? String.valueOf(senhaInformada) : gerarSenhaAleatoria();

            Usuario usuario = new Usuario();
            usuario.setLogin(login);
            usuario.setCpf(cpf);
            usuario.setTipo(TipoUsuario.valueOf(tipoBruto));
            usuario.setSenha(passwordEncoder.encode(senhaEmClaro));
            usuario.setAtivo(true);
            usuarioRepository.save(usuario);
            salvarComandoProcessado(comando);

            // R13 não precisa da senha de volta (já veio do formulário, não vai por e-mail).
            Map<String, Object> resposta = senhaVeioDoFormulario ? null : Map.of("senha", senhaEmClaro);
            responder(comando, "SUCESSO", resposta, null);
        } catch (Exception e) {
            log.error("Falha ao criar credencial para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao criar credencial: " + e.getMessage());
        }
    }

    private void salvarComandoProcessado(ComandoSaga comando) {
        try {
            comandoProcessadoRepository.save(new ComandoProcessado(comando.getSagaId(), comando.getTipo(), Map.of()));
        } catch (DuplicateKeyException e) {
            log.warn("Corrida ao salvar o registro de idempotência da saga {} — outra entrega venceu", comando.getSagaId());
        }
    }

    /** Compensação — idempotente: se a credencial já não existe (ou nunca existiu), ainda é SUCESSO. */
    private void removerCredencial(ComandoSaga comando) {
        String cpf = String.valueOf(comando.getPayload().get("cpf"));
        try {
            usuarioRepository.deleteByCpf(cpf);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao remover credencial (compensação) para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao remover credencial: " + e.getMessage());
        }
    }

    /** SAGA Remover Gerente (R15, passo 2). */
    private void desativarCredencial(ComandoSaga comando) {
        String cpf = String.valueOf(comando.getPayload().get("cpf"));
        try {
            usuarioRepository.findByCpf(cpf).ifPresent(usuario -> {
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
            });
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao desativar credencial para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao desativar credencial: " + e.getMessage());
        }
    }

    /** Compensação do passo 2 — idempotente: credencial inexistente não é erro. */
    private void reativarCredencial(ComandoSaga comando) {
        String cpf = String.valueOf(comando.getPayload().get("cpf"));
        try {
            usuarioRepository.findByCpf(cpf).ifPresent(usuario -> {
                usuario.setAtivo(true);
                usuarioRepository.save(usuario);
            });
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao reativar credencial (compensação) na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao reativar credencial: " + e.getMessage());
        }
    }

    private String gerarSenhaAleatoria() {
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            senha.append(ALFABETO_SENHA.charAt(aleatorio.nextInt(ALFABETO_SENHA.length())));
        }
        return senha.toString();
    }

    private void responder(ComandoSaga comando, String status, Map<String, Object> payload, String erro) {
        RespostaSaga resposta = new RespostaSaga(
                comando.getSagaId(), comando.getTipo(), payload, OffsetDateTime.now().toString(), status, erro);
        rabbitTemplate.convertAndSend(RabbitMqConfig.FILA_RESPOSTA, resposta);
    }
}
