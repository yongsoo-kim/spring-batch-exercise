package com.springbatch.exercise.job;

import com.springbatch.exercise.tasklet.SimpleJobTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobParamAndScopeConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final SimpleJobTasklet tasklet3;

    @Bean
    public Job scopeJob() {
        return jobBuilderFactory.get("scopeJob")
                .start(scopeStep1(null)) // "Late binding" enable us to set "null" here.
                .next(scopeStep2())
                .next(scopeStep3())
                .build();
    }


    @Bean
    @JobScope // This can be use in Step. This Bean will be created when this is called in runtime.
    public  Step scopeStep1(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("scopeStep1")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info(">>> This is scope1");
                    log.info(">>> requestDate={}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step scopeStep2() {
        return stepBuilderFactory.get("scopeStep2")
                .tasklet((scopeStep2Tasklet(null)))
                .build();
    }
    @Bean
    @StepScope // This can be used in Tasklet,ItemReader,ItemWriter or ItemProcessor.
    public Tasklet scopeStep2Tasklet(@Value("#{jobParameters[requestDate]}") String requestDate) {

        return (stepContribution, chunkContext) -> {
          log.info(">>> This is scopeStep2");
          log.info(">>> requestDate = {}", requestDate);
          return RepeatStatus.FINISHED;
        };
    }

    public Step scopeStep3() {
        log.info(">>> step3");
        return stepBuilderFactory.get("simpleStep3")
                .tasklet(tasklet3)
                .build();
    }

}
