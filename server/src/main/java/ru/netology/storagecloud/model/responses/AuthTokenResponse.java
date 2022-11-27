package ru.netology.storagecloud.model.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthTokenResponse(

        @JsonProperty("auth-token")
        String authToken
) {
}
