package com.gerimedica.csvservice.mapper;

import com.gerimedica.csvservice.model.CsvData;
import com.gerimedica.csvservice.model.dto.CsvDataDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DtoMapper {

    DtoMapper DTO_MAPPER = Mappers.getMapper(DtoMapper.class);

    CsvDataDto toDto(CsvData csvData);
    CsvData toEntity(CsvDataDto csvDataDto);

}
