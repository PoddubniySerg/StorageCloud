package ru.netology.storagecloud.model.responses;

import java.util.List;

public record UserFilesListResponse(List<FileDescription> files) {
}
