package com.springbatch.exercise.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SSItemResponseModel {

    private int shopId;
    private long itemId;
    private String mngNumber;
    private boolean stock;
}
