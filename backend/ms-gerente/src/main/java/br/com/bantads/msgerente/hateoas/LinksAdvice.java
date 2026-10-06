package br.com.bantads.msgerente.hateoas;

import br.com.bantads.msgerente.gerente.Gerente;
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
        if (!(recurso instanceof Gerente gerente)) {
            return recurso;
        }

        String self = "/gerentes/" + gerente.getCpf();
        Map<String, Map<String, String>> links = new LinkedHashMap<>();
        links.put("self", link(self));
        links.put("gerentes", link("/gerentes"));
        if (gerente.isAtivo()) {
            links.put("atualizar", link(self));
            links.put("remover", link(self));
        }

        Map<String, Object> mapa = objectMapper.convertValue(recurso, MAPA);
        mapa.put("_links", links);
        return mapa;
    }

    private Map<String, String> link(String caminho) {
        return Map.of("href", ServletUriComponentsBuilder.fromCurrentContextPath().path(caminho).toUriString());
    }
}
