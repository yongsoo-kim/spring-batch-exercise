package com.springbatch.exercise.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SSItem {

    private int shopId;
    private long itemId;

    public SSItem(int shopId, long itemId) {
        this.shopId = shopId;
        this.itemId = itemId;
    }
}
