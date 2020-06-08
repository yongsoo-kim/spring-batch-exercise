package com.springbatch.exercise;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@EnableBatchProcessing //Enable batch functions. Essential.
@SpringBootApplication
public class SpringBatchExerciseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchExerciseApplication.class, args);
    }

}
