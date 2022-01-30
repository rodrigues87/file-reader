package br.org.grupomarista.pdfreader.services;

import br.org.grupomarista.pdfreader.configs.FileStorageProperties;
import br.org.grupomarista.pdfreader.controllers.FileController;
import br.org.grupomarista.pdfreader.dtos.UploadFileResponse;
import br.org.grupomarista.pdfreader.exception.FileStorageException;
import br.org.grupomarista.pdfreader.exception.MyFileNotFoundException;
import com.google.cloud.spring.vision.CloudVisionTemplate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);


    private final Path fileStorageLocation;

    @Autowired private CloudVisionTemplate cloudVisionTemplate;

    @Autowired private ResourceLoader resourceLoader;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Não foi possivel criar o diretorio onde os arquivos irão ser salvos", ex);
        }
    }


    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("Arquivo não encontrado " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("Arquivo não encontrado " + fileName, ex);
        }
    }

    public UploadFileResponse uploadFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("O nome do arquivo possui caminho inválido " + fileName);
            }
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível salvar o arquivo " + fileName + ". Tente novamente", ex);
        }

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize(),"");
    }


    public List<UploadFileResponse> uploadMultipleFiles(MultipartFile[] files) {
        return Arrays.stream(files)
                .map(file -> {
                    try {
                        return uploadFile(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .collect(Collectors.toList());
    }


    public UploadFileResponse extractTextFromPdf(MultipartFile file) throws IOException {

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        String imageName = this.generateImageFromPDF(fileName);

        String imageUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(imageName)
                .toUriString();

        String message =  this.cloudVisionTemplate.extractTextFromImage(this.resourceLoader.getResource(imageUri));

        return new UploadFileResponse(fileName,imageUri,file.getContentType(),file.getSize(),message);
    }

    private String generateImageFromPDF(String filename) throws IOException {

        PDDocument document = PDDocument.load(new File("uploads/"+filename));

        if(filename.contains(".pdf")){
            filename = filename.split("\\.")[0];
        }

        String imageName=  filename + ".png";


        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(
                    page, 300, ImageType.RGB);
            ImageIOUtil.writeImage(
                    bim, "uploads/" + imageName, 300);
        }
        document.close();

        return imageName;
    }

    public ResponseEntity<Resource> downloadFile(String fileName, HttpServletRequest request) {
        Resource resource = this.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Não foi possível determinar o tipo do arquivo.");
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);

    }


}
