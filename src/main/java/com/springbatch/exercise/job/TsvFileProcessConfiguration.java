package com.springbatch.exercise.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbatch.exercise.domain.SSItem;
import com.springbatch.exercise.domain.SSItemResponseModel;
import com.springbatch.exercise.listener.JobCompletionNotificationListener;
import com.springbatch.exercise.listener.StepExecListener;
import com.springbatch.exercise.partitioner.CustomMultiResourcePartitioner;
import com.springbatch.exercise.policy.TsvFileReaderSkipper;
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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.BindException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TsvFileProcessConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ResourcePatternResolver resourcePatternResolver;
    private final StepExecListener stepExecListener;
    private final TsvFileReaderSkipper tsvFileReaderSkipper;
    private final JobCompletionNotificationListener jobCompletionListener;


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



    // About partitioner
    //https://www.baeldung.com/spring-batch-partitioner
    @Bean
    public Job tsvFileProcessJob() {
        return jobBuilderFactory.get("tsvFileProcessJob")
                .preventRestart()
                .incrementer(new RunIdIncrementer())
                //.start(tsvFileProcessStep())
                .start(partitionStep())
                .listener(jobCompletionListener)
                .build();

    }


    @Bean
    public CustomMultiResourcePartitioner partitioner() {
        CustomMultiResourcePartitioner partitioner = new CustomMultiResourcePartitioner();
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources("file:src/main/resources/input/*.tsv");
        } catch (IOException e ){
            throw new RuntimeException("I/O problems when resolving the input file pattern.", e);
        }
        partitioner.setResources(resources);
        return partitioner;
    }



//
//    @Bean
//    public Step destroyPool() {
//        return stepBuilderFactory.get("destroypool")
//                .partitioner("tsvFileProcessStep", partitioner())
//                .step(tsvFileProcessStep())
//                .taskExecutor(taskExecutor())
//                .build();
//    }

    @Bean
    @JobScope // Bean will be newly created every time Job runs.
    public Step partitionStep() {
        return stepBuilderFactory.get("partitionStep")
                .partitioner("tsvFileProcessStep", partitioner())
                .gridSize(4)
                .step(tsvFileProcessStep())
                .taskExecutor(taskExecutor())
                .listener(stepExecListener)
                .build();
    }

    @Bean
    public Step tsvFileProcessStep() {
        return stepBuilderFactory.get("tsvFileProcessStep")
                .<SSItem, SSItem>chunk(CHUNK_SIZE)
                .reader(tsvReader(null))
                .processor(tsvProcessor())
                .writer(tsvWriter())
                .faultTolerant().skipPolicy(tsvFileReaderSkipper)
                .build();
    }

//    @Bean
//    public TaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//        taskExecutor.setMaxPoolSize(5);
//        taskExecutor.setCorePoolSize(5);
//        taskExecutor.setQueueCapacity(5);
//        taskExecutor.afterPropertiesSet();
//        return taskExecutor;
//    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("Batch_task");
    }



    @Bean
    @StepScope
    //public FlatFileItemReader<SSItem> tsvReader(@Value("#{jobParameters[inputFile]}") String inputFilePath) {
    public FlatFileItemReader<SSItem> tsvReader(@Value("#{stepExecutionContext[fileName]}") String filename)  {

        return new FlatFileItemReaderBuilder<SSItem>()
                .name("tsvReader")
                .resource(new ClassPathResource("input/" + filename))
                //.resource(new FileSystemResource(inputFilePath))
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
        //return item -> new SSItem(item.getShopId(), item.getItemId());
        return item -> {
            if(item.getShopId() == 99999){
                //throw new RuntimeException("!!!!!!!!!!!!!!!!!!");
                log.error(">>>>>>> 99999 is illegal shop. I will skip this.");
                return null;
            }
            return item;
        };
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
