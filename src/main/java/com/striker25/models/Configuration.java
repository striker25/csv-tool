package com.striker25.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Configuration {

    @JsonProperty
    private String csvFullPath;

    @JsonProperty
    private List<String> columnsToDelete;

    @JsonProperty
    private List<HeaderRename> headersToRename;

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

    @Override
    public String toString() {
        return "Configuration{" +
                "csvFullPath='" + csvFullPath + '\'' +
                ", deleteHeaders=" + columnsToDelete +
                ", headersToRename=" + headersToRename +
                '}';
    }
}
