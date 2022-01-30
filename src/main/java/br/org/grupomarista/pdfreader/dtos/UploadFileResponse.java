package br.org.grupomarista.pdfreader.dtos;

import lombok.Data;

@Data
public class UploadFileResponse {
    private String fileName;
    private String fileDownloadUri;
    private String fileType;
    private long size;
    private String message;

    public UploadFileResponse(String fileName, String fileDownloadUri, String fileType, long size, String message) {
        this.fileName = fileName;
        this.fileDownloadUri = fileDownloadUri;
        this.fileType = fileType;
        this.size = size;
        this.message = message;
    }
}
