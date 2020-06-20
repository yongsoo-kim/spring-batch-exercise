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
import org.slf4j.MDC;
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
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    @JobScope // Bean will be newly created every time Job runs.
    public Step partitionStep() {
        return stepBuilderFactory.get("partitionStep")
                .partitioner("tsvFileProcessStep", partitioner(null))
//                .partitioner("step99", partitioner(null))
                .gridSize(4)
//                .step(step99(null))
                .step(tsvFileProcessStep())
                .taskExecutor(taskExecutor())
                .listener(stepExecListener)
                .build();
    }

    @Bean
    @JobScope
    public CustomMultiResourcePartitioner partitioner(@Value("#{jobParameters[inputFile]}") String inputFile) {
        System.out.println("-------------------------------");
        System.out.println(inputFile);
        System.out.println("-------------------------------");
        CustomMultiResourcePartitioner partitioner = new CustomMultiResourcePartitioner();

        Resource resource;
        resource = new FileSystemResource(inputFile);

//        //Resource[] resources;
//        Resource resource;
//        try {
//            //resources = resourcePatternResolver.getResources("file:src/main/resources/input/*part*.tsv");
//
//        } catch (){
//            throw new RuntimeException("I/O problems when resolving the input file pattern.", e);
//        }
//        partitioner.setResources(resource);
        partitioner.setResource(resource);
        return partitioner;
    }

//
//    @Bean
//    @JobScope
//    public Step step99(@Value("#{stepExecutionContext[fileName]}") String filename) {
//        log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//        log.info(filename);
//        log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//        return stepBuilderFactory.get("step99")
//                .tasklet((stepContribution, chunkContext) -> {
//                    log.info(">>> This is Step1");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }


    @Bean
    public Step tsvFileProcessStep() {
        return stepBuilderFactory.get("tsvFileProcessStep")
                .<SSItem, SSItem>chunk(CHUNK_SIZE)
                .reader(tsvReader(null))
                .processor(tsvProcessor())
                .writer(tsvWriter(null))
                .faultTolerant().skipPolicy(tsvFileReaderSkipper)
                .build();
    }



    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("Batch_task");
    }



    @Bean
    @StepScope
    //public FlatFileItemReader<SSItem> tsvReader(@Value("#{jobParameters[inputFile]}") String inputFilePath) {
    public FlatFileItemReader<SSItem> tsvReader(@Value("#{stepExecutionContext[fileName]}") String filename)  {
        log.info(System.getProperty("os.name").toLowerCase());

        System.out.println("===========================");
        System.out.println(filename);
        System.out.println("===========================");
        MDC.put("IAM", Thread.currentThread().getName());
        return new FlatFileItemReaderBuilder<SSItem>()
                .name("tsvReader")
                //.resource(new ClassPathResource("input/" + filename))
                .resource(new FileSystemResource(filename))
                .lineTokenizer(new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB) {{
                    setNames(new String[]{"shopId", "mngNumber"});
                }})
                .fieldSetMapper(new SSItemSetMapper())
                .build();
    }


    @Bean
    @StepScope
    public ItemWriter<? super SSItem> tsvWriter(@Value("#{stepExecutionContext[doneFile]}") String doneFile) {

        StringBuffer sbf = new StringBuffer();
        File file = new File(doneFile);
//        StringBuilder sbf = new StringBuilder();

        return new ItemWriter<SSItem>() {
            @Override
            public void write(List<? extends SSItem> items) throws Exception {
                for(SSItem item: items) {

//                    log.info("((((((((("+doneFile+")))))))))))))");
//                    HttpHeaders headers = new HttpHeaders();
//                    headers.setContentType(MediaType.APPLICATION_JSON);
//                    ResponseEntity<String> response = restTemplate.getForEntity(GET_BASE_URL,String.class);
//                    System.out.println(response.getStatusCode());
//                    String json = response.getBody();
//                    SSItemResponseModel ttt = objectMapper.readValue(json, SSItemResponseModel.class);
//                    System.out.println(item);
//                    System.out.println(ttt);
//                    Thread currentThread = Thread.currentThread();
//                    System.out.println(currentThread.getId());
//                    System.out.println(currentThread.getName());
//                    System.out.println("========MDC==========");
//                    System.out.println(MDC.get("IAM"));



                    String contents =sbf.append(item.getShopId())
                            .append("\t")
                            .append(item.getMngNumber()).toString();

                    writeLineToFile(doneFile, contents);





                    sbf.setLength(0);

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
            ssItem.setMngNumber(fieldSet.readString("mngNumber"));
            return ssItem;
        }
    }


    private void writeLineToFile(String filePath, String line) throws IOException {

        //Default buffer is 8192 byte

        BufferedWriter writer = new BufferedWriter
                (new OutputStreamWriter(new FileOutputStream(filePath, true), StandardCharsets.UTF_8));

//        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
        writer.write(line);
        writer.newLine();
        writer.flush();
        writer.close();
    }


//
//                    java.nio.file.Files.write(Paths.get(file.toURI()),
//                            contents.getBytes("utf-8"),
//    StandardOpenOption.CREATE,
//    StandardOpenOption.APPEND);



//    @Bean
//    public TaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//        taskExecutor.setMaxPoolSize(5);
//        taskExecutor.setCorePoolSize(5);
//        taskExecutor.setQueueCapacity(5);
//        taskExecutor.afterPropertiesSet();
//        return taskExecutor;
//    }

    //
//    @Bean
//    public Step destroyPool() {
//        return stepBuilderFactory.get("destroypool")
//                .partitioner("tsvFileProcessStep", partitioner())
//                .step(tsvFileProcessStep())
//                .taskExecutor(taskExecutor())
//                .build();
//    }

}
