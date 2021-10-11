package com.gerimedica.csvservice.service;

import com.gerimedica.csvservice.model.dto.CsvDataUploadResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;
import java.util.UUID;

public interface CsvDataService {

    String LOG_PREFIX = "[CsvDataService]";

    ResponseEntity<CsvDataUploadResponseDto> uploadCsvDataFromFile(MultipartFile multipartFile);

    ResponseEntity<Resource> downloadCsvDataAsFile(UUID filename);

}
