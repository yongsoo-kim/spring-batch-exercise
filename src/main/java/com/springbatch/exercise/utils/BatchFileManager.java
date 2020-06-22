package com.springbatch.exercise.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


@Slf4j
public class BatchFileManager {

    public String[] splitFile(final String filePath, final int splitCount) {

        String[] splitFilePaths = new String[splitCount];

        if (splitCount < 1) {
            throw new IllegalArgumentException("splitCount must be at least 1. splitCount = " + splitCount);
        }

        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(filePath))) {

            //Original file line count
            final int fileLinesCount = (int) Files.lines(Paths.get(filePath)).count();

            //split file line count
            final int splitFileLinesCount = (fileLinesCount % splitCount == 0) ? fileLinesCount / splitCount : (fileLinesCount / splitCount) + 1;

            String str;
            for (int i = 0; i < splitCount; i++) {
                //change file name after split.
                String splitFileName = filePath + "_" + i;
                try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(splitFileName))) {
                    for (int j = 0; j < splitFileLinesCount; j++) {
                        if ((str = bufferedReader.readLine()) != null) {
                            bufferedWriter.write(str);
                            bufferedWriter.newLine();
                        } else {
                            break;
                        }
                    }
                }
                splitFilePaths[i] = splitFileName;
            }

        } catch (IOException e) {
            throw new RuntimeException("File split failed!", e);
        }
        return splitFilePaths;
    }


    public BufferedWriter getWriter(String filePath) {

        //Default buffer is 8192 byte
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed in creating buffered writer!", e);
        }
        return writer;
    }


    public void writeLineToFile(BufferedWriter writer, String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            log.error(String.format("Write file content failed. line: {}",line));
            throw new RuntimeException(e);
        }
    }

}

