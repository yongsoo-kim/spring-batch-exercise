package com.springbatch.exercise.writer;

import org.springframework.batch.item.support.AbstractFileItemWriter;

import java.util.List;

public class CustomTsvWriter extends AbstractFileItemWriter {

    @Override
    protected String doWrite(List list) {
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
