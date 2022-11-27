package ru.netology.storagecloud.model.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileDescription(

        @JsonProperty("filename")
        String fileName,

        @JsonProperty("size")
        int size
) {
}
