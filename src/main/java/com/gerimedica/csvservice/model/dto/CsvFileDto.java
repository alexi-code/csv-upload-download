package com.gerimedica.csvservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CsvFileDto {
    private UUID csvId;
    List<CsvDataDto> csvData;
}
