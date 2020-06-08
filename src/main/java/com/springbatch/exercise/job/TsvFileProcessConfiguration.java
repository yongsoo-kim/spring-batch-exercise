package com.springbatch.exercise.job;

import com.springbatch.exercise.domain.SSItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.validation.BindException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TsvFileProcessConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final int CHUNK_SIZE=10;

//
//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//        return builder.build();
//    }

    @Bean
    public Job tsvFileProcessJob() {
        return jobBuilderFactory.get("tsvFileProcessJob")
                .start(tsvFileProcessStep())
                .build();

    }

    @Bean
    @JobScope
    public Step tsvFileProcessStep() {
        return stepBuilderFactory.get("tsvFileProcessStep")
                .<SSItem, SSItem>chunk(CHUNK_SIZE)
                .reader(tsvReader())
                .processor(tsvProcessor())
                .writer(tsvWriter())
                .build();
    }

    private ItemWriter<? super SSItem> tsvWriter() {
        return new ItemWriter<SSItem>() {
            @Override
            public void write(List<? extends SSItem> items) throws Exception {
                for(SSItem item: items) {
                    System.out.println(item);
                }
            }
        };
    }

    public ItemProcessor<? super SSItem,? extends SSItem> tsvProcessor() {
        return item -> new SSItem(item.getShopId(), item.getItemId());
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SSItem> tsvReader() {
        return new FlatFileItemReaderBuilder<SSItem>()
                .name("tsvReader")
                .resource(new FileSystemResource("C:/Users/yongs/OneDrive/Desktop/personal_projects/java/spring-batch-exercise/sample-data.tsv"))
                .lineTokenizer(new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB) {{
                    setNames(new String[]{"shopId", "itemId"});
                }})
                //.fieldSetMapper(new BeanWrapperFieldSetMapper<SSItem>())
                .fieldSetMapper(new SSItemSetMapper())
                .build();
    }

    private static class SSItemSetMapper implements FieldSetMapper<SSItem> {

        @Override
        public SSItem mapFieldSet(FieldSet fieldSet) throws BindException {
            SSItem ssItem = new SSItem();
            ssItem.setShopId(fieldSet.readInt("shopId"));
            ssItem.setItemId(fieldSet.readLong("itemId"));
            return ssItem;
        }
    }


}
