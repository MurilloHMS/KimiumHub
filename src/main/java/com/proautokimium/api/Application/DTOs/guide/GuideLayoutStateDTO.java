package com.proautokimium.api.Application.DTOs.guide;

/** O que está valendo, e o rascunho por cima dele — nulo quando não há. */
public record GuideLayoutStateDTO(GuideLayoutDTO published, GuideLayoutDTO draft) {}
