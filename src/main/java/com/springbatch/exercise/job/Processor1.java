package com.springbatch.exercise.job;

import com.springbatch.exercise.domain.SSItem;
import com.springbatch.exercise.utils.BatchFileManager;
import org.springframework.batch.item.ItemProcessor;


public class Processor1 implements ItemProcessor<SSItem, SSItem> {

    private String skippedFile;

    public Processor1(String skippedFile) {
        this.skippedFile = skippedFile;
    }

    @Override
    public SSItem process(SSItem ssItem) throws Exception {

        if (ssItem.getShopId() == 33333) {
            String line = new StringBuilder()
                    .append(ssItem.getShopId())
                    .append("\t")
                    .append(ssItem.getMngNumber())
                    .toString();

            BatchFileManager fileManager = new BatchFileManager();
            fileManager.writeLineToFile(fileManager.getWriter(skippedFile), line);

            return null;
        }
        return ssItem;
    }
}
