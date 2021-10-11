package com.gerimedica.csvservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CsvDataUploadResponseDto {
    String status;
    String originalFilename;
    String filename;
    Integer processedRows;
}
