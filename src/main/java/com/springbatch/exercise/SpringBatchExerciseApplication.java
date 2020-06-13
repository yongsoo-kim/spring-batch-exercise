package com.springbatch.exercise;

import com.springbatch.exercise.utils.SingletonExitCode;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@EnableBatchProcessing //Enable batch functions. Essential.
@SpringBootApplication
public class SpringBatchExerciseApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringBatchExerciseApplication.class, args);
        SpringApplication.exit(context);
        System.exit(SingletonExitCode.getInstance().getExitCode());
    }

}
