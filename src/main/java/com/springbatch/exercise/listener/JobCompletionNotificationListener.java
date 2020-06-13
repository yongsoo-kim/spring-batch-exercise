package com.springbatch.exercise.listener;

import com.springbatch.exercise.utils.SingletonExitCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    SingletonExitCode exitCode = SingletonExitCode.getInstance();

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("======JOB RESULT======");

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            exitCode.setErrorExitExit();
        }
    }
}
