package com.gerimedica.csvservice.controller;

import com.gerimedica.csvservice.model.dto.CsvDataUploadResponseDto;
import com.gerimedica.csvservice.service.CsvDataService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/csv")
public class CsvDataController {

    CsvDataService csvDataService;

    @Autowired
    public void setCsvDataService(CsvDataService csvDataService) {
        this.csvDataService = csvDataService;
    }

    @Operation(summary = "Download by filename")
    @GetMapping(
        value = "/download/{filename}",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<Resource> downloadCsvDataAsFile(
        @PathVariable("filename") UUID filename
    ) {
        return csvDataService.downloadCsvDataAsFile(filename);
    }

    @Operation(summary = "Upload the CSV file")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvDataUploadResponseDto> uploadCsvDataFromFile(
        @RequestPart(name = "file") MultipartFile multipartFile
    ) {
        return csvDataService.uploadCsvDataFromFile(multipartFile);
    }

}
