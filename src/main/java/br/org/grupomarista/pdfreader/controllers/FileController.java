package br.org.grupomarista.pdfreader.controllers;


import br.org.grupomarista.pdfreader.dtos.UploadFileResponse;
import br.org.grupomarista.pdfreader.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/")
public class FileController {


    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        return fileStorageService.uploadFile(file);
    }

    @PostMapping("/extractTextFromPf")
    public ResponseEntity<?>  extractTextFromPdf(@RequestParam("file") MultipartFile file) throws Exception {

        return ResponseEntity.status(HttpStatus.OK).body(fileStorageService.extractTextFromPdf(file));
    }

    @PostMapping("/uploadMultipleFiles")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {

        return fileStorageService.uploadMultipleFiles(files);
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {

        return fileStorageService.downloadFile(fileName,request);
    }


}
