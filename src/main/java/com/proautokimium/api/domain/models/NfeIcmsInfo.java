package com.proautokimium.api.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NfeIcmsInfo {
    private String nNF;
    private double vIcms;
    private double vPis;
    private double vCofins;
}
