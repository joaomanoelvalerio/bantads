package br.com.bantads.msconta.hateoas;

import br.com.bantads.msconta.conta.Conta;
import br.com.bantads.msconta.conta.ExtratoResponse;
import br.com.bantads.msconta.conta.Movimentacao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
        if (body instanceof ExtratoResponse extrato) {
            return extratoComLinks(extrato);
        }
        return comLinks(body);
    }

    private Object comLinks(Object recurso) {
        if (recurso instanceof Conta conta) {
            return comLinks(recurso, linksDaConta(conta));
        }
        if (recurso instanceof Movimentacao movimentacao) {
            return comLinks(recurso, Map.of("conta", link("/contas/" + movimentacao.getNumeroConta())));
        }
        return recurso;
    }

    private Map<String, Object> extratoComLinks(ExtratoResponse extrato) {
        Map<String, Map<String, String>> links = new LinkedHashMap<>();
        links.put("self", Map.of("href", ServletUriComponentsBuilder.fromCurrentRequest().toUriString()));
        links.put("conta", link("/contas/" + extrato.numeroConta()));

        List<Object> movimentacoes = new ArrayList<>();
        extrato.movimentacoes().forEach(movimentacao -> movimentacoes.add(comLinks(movimentacao)));

        Map<String, Object> mapa = comLinks(extrato, links);
        mapa.put("movimentacoes", movimentacoes);
        return mapa;
    }

    private Map<String, Map<String, String>> linksDaConta(Conta conta) {
        String self = "/contas/" + conta.getNumeroConta();
        Map<String, Map<String, String>> links = new LinkedHashMap<>();
        links.put("self", link(self));
        links.put("extrato", link(self + "/extrato"));
        links.put("deposito", link(self + "/deposito"));
        links.put("saque", link(self + "/saque"));
        links.put("transferencia", link(self + "/transferencia"));
        links.put("cliente", link("/clientes/" + conta.getCpfCliente()));
        return links;
    }

    private Map<String, Object> comLinks(Object recurso, Map<String, Map<String, String>> links) {
        Map<String, Object> mapa = objectMapper.convertValue(recurso, MAPA);
        mapa.put("_links", links);
        return mapa;
    }

    private Map<String, String> link(String caminho) {
        return Map.of("href", ServletUriComponentsBuilder.fromCurrentContextPath().path(caminho).toUriString());
    }
}
