package com.springbatch.exercise.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbatch.exercise.domain.SSItem;
import com.springbatch.exercise.domain.SSItemResponseModel;
import com.springbatch.exercise.listener.JobCompletionNotificationListener;
import com.springbatch.exercise.listener.StepExecListener;
import com.springbatch.exercise.partitioner.CustomMultiResourcePartitioner;
import com.springbatch.exercise.policy.TsvFileReaderSkipper;
import com.springbatch.exercise.utils.BatchFileManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.webresources.FileResource;
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
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.validation.BindException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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


    private final static String GET_BASE_URL = "http://localhost:3000/item";

    private final static int CHUNK_SIZE = 10;


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
    public CustomMultiResourcePartitioner partitioner(@Value("#{jobParameters[inputFilePath]}") String inputFilePath) {
        //Resource[] resources;
//        Resource resource;
//        try {
//            //resources = resourcePatternResolver.getResources("file:src/main/resources/input/*part*.tsv");
//
//        } catch (){
//            throw new RuntimeException("I/O problems when resolving the input file pattern.", e);
//        }
//        partitioner.setResources(resource);
        CustomMultiResourcePartitioner partitioner = new CustomMultiResourcePartitioner();
        partitioner.setInputFilePath(inputFilePath);
        return partitioner;
    }


    @Bean
    public Step tsvFileProcessStep() {
        return stepBuilderFactory.get("tsvFileProcessStep")
                .<SSItem, SSItem>chunk(CHUNK_SIZE)
                .reader(tsvReader(null))
                //.processor(tsvProcessor())
                .processor(compositeProcessor())
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
    public FlatFileItemReader<SSItem> tsvReader(@Value("#{stepExecutionContext[splitFile]}") String splitFile) {

        return new FlatFileItemReaderBuilder<SSItem>()
                .name("tsvReader")
                //.resource(new ClassPathResource("input/" + filename))
                .resource(new FileSystemResource(splitFile))
                .lineTokenizer(new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB) {{
                    setNames(new String[]{"shopId", "mngNumber"});
                }})
                .fieldSetMapper(new SSItemSetMapper())
                .build();
    }


    @Bean
    @StepScope
    public FlatFileItemWriter<SSItem> tsvWriter(@Value("#{stepExecutionContext[doneFile]}") String doneFile) {
        //Create writer instance
        FlatFileItemWriter<SSItem> writer = new FlatFileItemWriter<>();

        //Set output file location
        writer.setResource(new FileSystemResource(doneFile));

        //All job repetitions should "append" to same output file
        writer.setAppendAllowed(true);

        //Name field values sequence based on object properties
        writer.setLineAggregator(new DelimitedLineAggregator<SSItem>() {
            {
                setDelimiter("\t");
                setFieldExtractor(new BeanWrapperFieldExtractor<SSItem>() {
                    {
                        setNames(new String[]{"shopId", "mngNumber"});
                    }
                });
            }
        });
        return writer;
    }

    @Bean
    public CompositeItemProcessor compositeProcessor() {
        List<ItemProcessor> delegates = new ArrayList<>(2);
        delegates.add(processor1(null));
        delegates.add(processor2(null));

        CompositeItemProcessor processor = new CompositeItemProcessor<>();
        processor.setDelegates(delegates);
        return processor;
    }

    @Bean
    @StepScope
    public ItemProcessor<SSItem, SSItem> processor1(@Value("#{stepExecutionContext[skippedFile]}") String skippedFile) {
        return new Processor1(skippedFile);
    }

//    @Bean
//    @StepScope
//    public ItemProcessor<SSItem, SSItem> processor1(@Value("#{stepExecutionContext[skippedFile]}") String skippedFile) {
//        return item -> {
//            //log.info("===PROCESSOR1111111111111111111====");
//            //log.info(skippedFile);
//            if (item.getShopId() == 33333) {
//                String line = new StringBuilder()
//                        .append(item.getShopId())
//                        .append("\t")
//                        .append(item.getMngNumber())
//                        .toString();
//
//                BatchFileManager fileManager = new BatchFileManager();
//                fileManager.writeLineToFile(fileManager.getWriter(skippedFile), line);
//
//                return null;
//            }
//            return item;
//        };
//    }


    @Bean
    @StepScope
    public ItemProcessor<SSItem, SSItem> processor2(@Value("#{stepExecutionContext[skippedFile]}") String skippedFile) {
        return item -> {

            //log.info("===PROCESSOR2222222222222222222====");
            if (item.getShopId() == 44444) {
                String line = new StringBuilder()
                        .append(item.getShopId())
                        .append("\t")
                        .append(item.getMngNumber())
                        .toString();

                BatchFileManager fileManager = new BatchFileManager();
                fileManager.writeLineToFile(fileManager.getWriter(skippedFile), line);

                return null;
            }
            return item;
        };
    }


//    @Bean
//    @StepScope
//    public ItemWriter<? super SSItem> tsvWriter(@Value("#{stepExecutionContext[doneFile]}") String doneFile) {
//
//        StringBuffer sbf = new StringBuffer();
//        BatchFileManager fileManager = new BatchFileManager();
//        BufferedWriter writer = fileManager.getWriter(doneFile);
//
//        return new ItemWriter<SSItem>() {
//
//            String lineContents = null;
//
//
//
//
//
//            @Override
//            public void write(List<? extends SSItem> items) throws IOException {
//
//                SSItemResponseModel resItem = null;
//                //ResponseEntity<String> response = null;
//                String response = null;
//                String json = null;
//                for (SSItem item : items) {
//
//                    //log.info("((((((((("+doneFile+")))))))))))))");
////        HttpHeaders headers = new HttpHeaders();
////        headers.setContentType(MediaType.APPLICATION_JSON);
//
//                    // System.out.println(response.getStatusCode());
////                    ResponseEntity<String> response = restTemplate.getForEntity(GET_BASE_URL,String.class);
////                    String json = response.getBody();
////                    SSItemResponseModel resItem = objectMapper.readValue(json, SSItemResponseModel.class);
////
//                    ///PERFORMANCE???
//                    //response = restTemplate.getForObject(GET_BASE_URL,String.class);
////                    json = response.getBody();
//                    //resItem = objectMapper.readValue(json, SSItemResponseModel.class);
//
//
//                    //SSItemResponseModel resItem = objectMapper.readValue(json, SSItemResponseModel.class);
////                    System.out.println(item);
////                    Thread currentThread = Thread.currentThread();
////                    System.out.println(currentThread.getId());
////                    System.out.println(currentThread.getName());
////                    System.out.println("========MDC==========");
////                    System.out.println(MDC.get("IAM"));
//
//
////                    String contents =sbf.append(resItem.getShopId())
////                            .append("\t")
////                            .append(resItem.getMngNumber()).toString();
//
//                    lineContents = sbf.append(item.getShopId())
//                            .append("\t")
//                            .append(item.getMngNumber()).toString();
//
//
//                    fileManager.writeLineToFile(writer, lineContents);
//
//                    sbf.setLength(0);
//
//                }
//                writer.flush();
//
//            }
//        };
//    }


//    public ItemProcessor<? super SSItem, ? extends SSItem> tsvProcessor() {
//        //return item -> new SSItem(item.getShopId(), item.getItemId());
//        return item -> {
//            if (item.getShopId() == 99999) {
//                //throw new RuntimeException("!!!!!!!!!!!!!!!!!!");
//                return null;
//            }
//            return item;
//        };
//    }


    private static class SSItemSetMapper implements FieldSetMapper<SSItem> {

        @Override
        public SSItem mapFieldSet(FieldSet fieldSet) throws BindException {
            SSItem ssItem = new SSItem();
            ssItem.setShopId(fieldSet.readInt("shopId"));
            ssItem.setMngNumber(fieldSet.readString("mngNumber"));
            return ssItem;
        }
    }


}
