package com.springbatch.exercise.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileSplitter {


    public String[] splitFile(final String filePath, final int splitCount) throws IOException {

        String[] splitFileNames = new String[splitCount];

        if(splitCount<1) {
            throw new IllegalArgumentException("splitCount must be at least 1. splitCount = "+splitCount);
        }

        try(BufferedReader bufferedReader  = Files.newBufferedReader(Paths.get(filePath))){

            //Original file line count
            final int fileLinesCount = (int) Files.lines(Paths.get(filePath)).count();

            //split file line count
            final int splitFileLinesCount = (fileLinesCount % splitCount == 0) ? fileLinesCount/splitCount : (fileLinesCount/splitCount )+1;

            String str;
            for (int i = 0; i<splitCount;i++){
                //change file name after split.
                String splitFileName=filePath+"_"+i;
                try(BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(splitFileName))){
                    for (int j = 0 ; j< splitFileLinesCount; j++){
                        if((str=bufferedReader.readLine()) !=null) {
                            bufferedWriter.write(str);
                            bufferedWriter.newLine();
                        }else {
                            break;
                        }
                    }


                }
                splitFileNames[i] = splitFileName;
            }

            return splitFileNames;


        }



    }
}
