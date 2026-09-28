package com.br.ms_email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailMessage {
    private String sagaId;
    private String tipo;
    private Map<String, Object> payload;
    private String timestamp;
}