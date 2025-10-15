package com.proautokimium.api.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NfeDataInfo {
    private String partner;
    private String nfeNum;
    private Date nfeDate;
    private String product;
    private String unitValue;
    private String totalValue;
    private String cfop;
}
