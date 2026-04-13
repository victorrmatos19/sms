package com.erp.model.dto.dashboard;

/**
 * Item para o painel "Requer Atenção" do dashboard.
 * nivel: CRITICO | AVISO | INFO
 */
public record AlertaDTO(String mensagem, String nivel, String valor) {}
