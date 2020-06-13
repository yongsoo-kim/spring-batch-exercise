package com.springbatch.exercise.utils;

public class SingletonExitCode {

    private static final int NORMAL_EXIT = 0;
    private static final int ERROR_EXIT = 1;

    private int exitCode = NORMAL_EXIT;

    private static SingletonExitCode instance = new SingletonExitCode();

    private SingletonExitCode() {
    }

    public static SingletonExitCode getInstance() {
        return instance;
    }

    public void setErrorExitExit(){
        this.exitCode = ERROR_EXIT;
    }


    public int getExitCode() {
        return exitCode;
    }
}
