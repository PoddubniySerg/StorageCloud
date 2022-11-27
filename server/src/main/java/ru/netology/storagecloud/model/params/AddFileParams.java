package ru.netology.storagecloud.model.params;

import org.springframework.web.multipart.MultipartFile;

public record AddFileParams(String fileName, MultipartFile content) {
}
