package br.com.bantads.mscliente.hateoas;

import br.com.bantads.mscliente.cliente.Cliente;
import br.com.bantads.mscliente.solicitacao.Solicitacao;
import br.com.bantads.mscliente.solicitacao.StatusSolicitacao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestControllerAdvice
public class LinksAdvice implements ResponseBodyAdvice<Object> {

    private static final TypeReference<LinkedHashMap<String, Object>> MAPA = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public LinksAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof List<?> lista) {
            return lista.stream().map(this::comLinks).toList();
        }
        return comLinks(body);
    }

    private Object comLinks(Object recurso) {
        Map<String, Map<String, String>> links = new LinkedHashMap<>();
        if (recurso instanceof Cliente cliente) {
            links.put("self", link("/clientes/" + cliente.getCpf()));
            links.put("conta", link("/contas/cliente/" + cliente.getCpf()));
        } else if (recurso instanceof Solicitacao solicitacao) {
            links.put("solicitacoes", link("/solicitacoes"));
            if (solicitacao.getStatus() == StatusSolicitacao.PENDENTE) {
                links.put("aprovar", link("/solicitacoes/" + solicitacao.getCpf() + "/aprovar"));
                links.put("rejeitar", link("/solicitacoes/" + solicitacao.getCpf() + "/rejeitar"));
            } else if (solicitacao.getStatus() == StatusSolicitacao.APROVADO) {
                links.put("cliente", link("/clientes/" + solicitacao.getCpf()));
            }
        } else {
            return recurso;
        }

        Map<String, Object> mapa = objectMapper.convertValue(recurso, MAPA);
        mapa.put("_links", links);
        return mapa;
    }

    private Map<String, String> link(String caminho) {
        return Map.of("href", ServletUriComponentsBuilder.fromCurrentContextPath().path(caminho).toUriString());
    }
}
