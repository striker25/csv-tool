package com.striker25.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    @JsonProperty
    private String csvFullPath;

    @JsonProperty
    private List<String> columnsToDelete;

    @JsonProperty
    private List<HeaderRename> headersToRename;

    @JsonProperty
    private List<HeaderSwap> headersToSwap = new ArrayList<>();

    @JsonProperty
    private boolean headersToUpperCase = false;

    @JsonProperty
    private String outFolderName;


    public Configuration() {}

    public String getCsvFullPath() {
        return csvFullPath;
    }

    public List<String> getColumnsToDelete() {
        return columnsToDelete;
    }

    public List<HeaderRename> getHeadersToRename() {
        return headersToRename;
    }

    public boolean isHeadersToUpperCase() {
        return headersToUpperCase;
    }

    public String getOutFolderName() {
        return outFolderName;
    }

    public List<HeaderSwap> getHeadersToSwap() {
        return headersToSwap;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "csvFullPath='" + csvFullPath + '\'' +
                ", columnsToDelete=" + columnsToDelete +
                ", headersToRename=" + headersToRename +
                ", headersToSwap=" + headersToSwap +
                ", headersToUpperCase=" + headersToUpperCase +
                ", outFolderName='" + outFolderName + '\'' +
                '}';
    }
}
