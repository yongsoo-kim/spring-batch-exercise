package com.springbatch.exercise.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbatch.exercise.domain.SSItem;
import com.springbatch.exercise.domain.SSItemResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TsvFileProcessConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

//    This is also working.
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }

    // You can do more setting with this style.
//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//
//
//        // Do any additional configuration here
//        return builder.build();
//    }


//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//
//
//        // Do any additional configuration here
//        return builder.build();
//    }



//
//    @Autowired
//    private RestTemplate restTemplate;


    private final static String GET_BASE_URL="http://localhost:3000/item";

    private final static int CHUNK_SIZE=10;



    @Bean
    public Job tsvFileProcessJob() {
        return jobBuilderFactory.get("tsvFileProcessJob")
                .incrementer(new RunIdIncrementer())
                .start(tsvFileProcessStep())
                .build();

    }

    @Bean
    @JobScope
    public Step tsvFileProcessStep() {
        return stepBuilderFactory.get("tsvFileProcessStep")
                .<SSItem, SSItem>chunk(CHUNK_SIZE)
                .reader(tsvReader(null))
                .processor(tsvProcessor())
                .writer(tsvWriter())
                .build();
    }


    @Bean
    @StepScope
    public FlatFileItemReader<SSItem> tsvReader(@Value("#{jobParameters[inputFile]}") String inputFilePath) {

        return new FlatFileItemReaderBuilder<SSItem>()
                .name("tsvReader")
                .resource(new FileSystemResource(inputFilePath))
                .lineTokenizer(new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB) {{
                    setNames(new String[]{"shopId", "itemId"});
                }})
                .fieldSetMapper(new SSItemSetMapper())
                .build();
    }


    public ItemWriter<? super SSItem> tsvWriter() {
        return new ItemWriter<SSItem>() {
            @Override
            public void write(List<? extends SSItem> items) throws Exception {
                for(SSItem item: items) {


                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    ResponseEntity<String> response = restTemplate.getForEntity(GET_BASE_URL,String.class);
                    System.out.println(response.getStatusCode());
                    String json = response.getBody();
                    SSItemResponseModel ttt = objectMapper.readValue(json, SSItemResponseModel.class);
                    System.out.println(item);
                    System.out.println(ttt);
                    Thread currentThread = Thread.currentThread();
                    System.out.println(currentThread.getId());
                    System.out.println(currentThread.getName());
                }
            }
        };
    }

    public ItemProcessor<? super SSItem,? extends SSItem> tsvProcessor() {
        return item -> new SSItem(item.getShopId(), item.getItemId());
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
