package com.br.orquestrador.saga;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * SAGA Inserir Gerente (R13, passo 5 — "MS Cliente: obtém nome/e-mail do
 * cliente dono da conta transferida"): passo de só-consulta, sem
 * compensação, então não precisa do mecanismo de correlação por
 * `orquestrador.reply` — uma chamada REST direta e síncrona já resolve
 * (mesma ideia de API Composition que o Gateway já usa em outros lugares).
 */
@Component
public class ConsultaRestClient {

    private static final Logger log = LoggerFactory.getLogger(ConsultaRestClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper;
    private final String contaMsApiUrl;
    private final String clienteMsApiUrl;

    public ConsultaRestClient(
            ObjectMapper objectMapper,
            @Value("${bantads.conta-ms-api-url}") String contaMsApiUrl,
            @Value("${bantads.cliente-ms-api-url}") String clienteMsApiUrl) {
        this.objectMapper = objectMapper;
        this.contaMsApiUrl = contaMsApiUrl;
        this.clienteMsApiUrl = clienteMsApiUrl;
    }

    public Optional<Map<String, Object>> buscarConta(String numeroConta) {
        return buscar(contaMsApiUrl + "/contas/" + numeroConta);
    }

    public Optional<Map<String, Object>> buscarCliente(String cpf) {
        return buscar(clienteMsApiUrl + "/clientes/" + cpf);
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> buscar(String url) {
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(resposta.body(), Map.class));
        } catch (Exception e) {
            log.error("Falha ao consultar {}", url, e);
            return Optional.empty();
        }
    }
}
