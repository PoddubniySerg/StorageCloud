package ru.netology.storagecloud.model.responses;

public record UserFileResponse(String hash, byte[] file) {
}
