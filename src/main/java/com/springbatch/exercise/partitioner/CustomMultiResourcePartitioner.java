package com.springbatch.exercise.partitioner;

import com.springbatch.exercise.utils.BatchFileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CustomMultiResourcePartitioner implements Partitioner {

    private static final String DONE_FILE_KEY_NAME = "doneFile";
    private static final String SKIPPED_FILE_KEY_NAME = "skippedFile";
    private static final String PARTITION_KEY = "partition";

    private static final String DONE_SUFFIX = "_DONE_";
    private static final String SKIPPED_SUFFIX = "_SKIPPED_";

    private String inputFilePath;


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);

        // Split file
        BatchFileManager fileManager = new BatchFileManager();
        String[] splitFilePaths = fileManager.splitFile(inputFilePath, gridSize);
        log.info("SPLIT---> END!!!!");

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putString(DONE_FILE_KEY_NAME, inputFilePath + DONE_SUFFIX + i);
            context.putString(SKIPPED_FILE_KEY_NAME, inputFilePath + SKIPPED_SUFFIX + i);
            context.putString("splitFile", splitFilePaths[i]);
            map.put(PARTITION_KEY + i, context);
            //log.info(map.toString());
        }
        return map;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }
}
