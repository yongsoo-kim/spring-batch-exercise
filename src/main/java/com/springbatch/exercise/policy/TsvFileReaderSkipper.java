package com.springbatch.exercise.policy;


import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TsvFileReaderSkipper implements SkipPolicy {
    @Override
    public boolean shouldSkip(Throwable exception, int i) throws SkipLimitExceededException {


        StringBuilder errorMessage = new StringBuilder();
        errorMessage
                .append("Unexpected exception ")
                .append(exception.toString())
//                    .append(ExceptionUtils.getStackFrames(exception))
                .append("\n");
        log.error("{}", errorMessage.toString());
        if (exception instanceof FlatFileParseException) {

            return true;
        } else if (exception instanceof IllegalArgumentException) {
            log.error("I will handele this...");
            return true;
        }
        return false;
    }
}
