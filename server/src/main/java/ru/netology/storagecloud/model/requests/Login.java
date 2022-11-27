package ru.netology.storagecloud.model.requests;

import lombok.Data;

@Data
public class Login {
    private String login, password;
}
