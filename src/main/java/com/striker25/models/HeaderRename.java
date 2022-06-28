package com.striker25.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeaderRename {

    @JsonProperty
    private String source;

    @JsonProperty
    private String target;

    public HeaderRename() {
    }

    public HeaderRename(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "HeaderRename{" +
                "source='" + source + '\'' +
                ", target='" + target + '\'' +
                '}';
    }
}
