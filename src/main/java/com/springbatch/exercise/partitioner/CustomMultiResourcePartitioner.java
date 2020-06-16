package com.springbatch.exercise.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomMultiResourcePartitioner implements Partitioner {

    private static final String DEFAULT_KEY_NAME = "fileName";
    private static final String PARTITION_KEY = "partition";
    private String keyName = DEFAULT_KEY_NAME;
    private Resource[] resources = new Resource[0];

    private Resource resource;


    /**
     * The resources to assign to each partition. In Spring configuration you
     * can use a pattern to select multiple resources.
     * @param resources the resources to use
     */
    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

    /**
     * The name of the key for the file name in each {@link ExecutionContext}.
     * Defaults to "fileName".
     * @param keyName the value of the key
     */
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        System.out.println("***********");
        System.out.println(resource);
        String absPath=null;



        try {
            absPath =resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("***********");
        ExecutionContext context = new ExecutionContext();
        Assert.state(resource.exists(), "Resource does not exist:" + resource);
        context.putString(keyName, absPath);
        context.putString("opFileName", "output1.xml");
        map.put(PARTITION_KEY+1, context);


//        int i=0, k=1;
//        for (Resource resource: resources) {
//            ExecutionContext context = new ExecutionContext();
//            Assert.state(resource.exists(), "Resource does not exist:" + resource);
//            context.putString(keyName, resource.getFilename());
//            context.putString("opFileName", "output" + (k++) +".xml");
//            map.put(PARTITION_KEY+i, context);
//            i++;
//        }
        return map;
    }


    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
