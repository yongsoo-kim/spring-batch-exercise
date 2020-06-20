package com.springbatch.exercise.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SSItem {

    private int shopId;
    private String mngNumber;

    public SSItem(int shopId, String mngNumber) {
        this.shopId = shopId;
        this.mngNumber = mngNumber;
    }
}
