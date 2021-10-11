package com.gerimedica.csvservice.service.impl;

import com.gerimedica.csvservice.exception.NonUniqueCodeException;
import com.gerimedica.csvservice.model.CsvData;
import com.gerimedica.csvservice.model.CsvFile;
import com.gerimedica.csvservice.model.dto.CsvDataUploadResponseDto;
import com.gerimedica.csvservice.repository.CsvFileRepository;
import com.gerimedica.csvservice.service.CsvDataService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.swagger.v3.core.util.Constants.COMMA;
import static org.apache.catalina.security.Constants.CRLF;

@Service
@Log4j2
public class CsvDataServiceImpl implements CsvDataService {

    private final static String CSV_FIELD_DELIMITER = COMMA;
    private final static String CSV_RECORD_SEPARATOR = CRLF;
    private final static String CSV_DATE_FORMAT = "dd-MM-yyyy";
    private final static String[] CSV_DEFAULT_HEADERS = {
        "source", "codeListCode", "code", "displayValue", "longDescription", "fromDate", "toDate", "sortingPriority"
    };

    CsvFileRepository csvFileRepository;

    @Autowired
    public void setCsvFileRepository(CsvFileRepository csvFileRepository) {
        this.csvFileRepository = csvFileRepository;
    }

    @Override
    public ResponseEntity<CsvDataUploadResponseDto> uploadCsvDataFromFile(MultipartFile multipartFile) {
        if(multipartFile == null || multipartFile.isEmpty() || !isCSVContentType(multipartFile)) {
            log.warn("{} uploadCsvDataFromFile error: file is empty or wrong content type={}",
                LOG_PREFIX,
                multipartFile != null ? multipartFile.getContentType() : "null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        List<CsvData> csvDataList;
        try(BufferedReader fileReader = new BufferedReader(
            new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8))) {
            CSVParser csvParser = configureNewCsvParser(fileReader);
            try {
                csvDataList = getCsvDataListFromRecords(csvParser);
            } catch (NonUniqueCodeException e) {
                log.warn("{} uploadCsvDataFromFile code not unique error={}", LOG_PREFIX, e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        } catch (IOException ioe) {
            log.warn("{} uploadCsvDataFromFile error={}", LOG_PREFIX, ioe.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        csvDataList.forEach(x -> log.info(x.getCode()));
        String filename = persistNewCsvFile(csvDataList, multipartFile.getOriginalFilename()).getCsvId().toString();
        return ResponseEntity.status(HttpStatus.OK)
            .body(CsvDataUploadResponseDto.builder()
                .processedRows(csvDataList.size())
                .filename(filename)
                .originalFilename(multipartFile.getOriginalFilename())
                .status("All records were processed successfully")
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CsvFile persistNewCsvFile(List<CsvData> csvDataList, String originalFilename) {
        return csvFileRepository.saveAndFlush(CsvFile.builder()
            .csvDataSet(csvDataList)
            .csvOriginalFilename(originalFilename)
            .build());
    }

    @Override
    public ResponseEntity<Resource> downloadCsvDataAsFile(UUID fileUUID) {
        Optional<CsvFile> csvFile = csvFileRepository.findById(fileUUID);
        if(csvFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<CsvData> csvDataList = csvFile.get().getCsvDataSet().stream()
            .sorted(Comparator.comparing(CsvData::getRowNumber))
            .collect(Collectors.toList());
        log.info("{} downloadCsvDataAsFile saving... file={} size={}",
            LOG_PREFIX, fileUUID.toString(), csvDataList.size());
        try {
            InputStreamResource file = new InputStreamResource(csvDataListToBuffer(csvDataList));
            return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + csvFile.get().getCsvOriginalFilename())
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(file);
        } catch (IOException ioe) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private List<CsvData> getCsvDataListFromRecords(CSVParser csvParser) {
        AtomicInteger counter = new AtomicInteger(0);
        Map<String, Integer> headerMap = new HashMap<>();
        List<CsvData> csvDataList = new LinkedList<>();
        Set<String> uniqueCodeSet = new HashSet<>();
        csvParser.stream().iterator().forEachRemaining(row -> {
            if(counter.get() == 0) {
                row.forEach(header -> headerMap.put(header, counter.getAndIncrement()));
                counter.incrementAndGet();
                return;
            }
            csvDataList.add(csvRecordToCsvDataConverter(row, headerMap, uniqueCodeSet, counter.get()));
            counter.incrementAndGet();
        });
        return csvDataList;
    }

    public static ByteArrayInputStream csvDataListToBuffer(List<CsvData> csvDataList) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), getCSVFormat())) {
            log.info("{} csvDataListToBuffer saving rows={}", LOG_PREFIX, csvDataList.size());
            csvPrinter.printRecord(Arrays.asList(CSV_DEFAULT_HEADERS));
            for (CsvData csvData : csvDataList) {
                csvPrinter.printRecord(cvsDataToFieldArray(csvData));
            }
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            log.warn("{} csvDataListToBuffer error={}", LOG_PREFIX, e.getMessage());
            throw new IOException("Fail to import data to CSV file: " + e.getMessage());
        }
    }

    private CSVParser configureNewCsvParser(BufferedReader reader) throws IOException {
        return new CSVParser(reader, getCSVFormat());
    }

    private static CSVFormat getCSVFormat() {
        return CSVFormat.DEFAULT.builder()
            .setDelimiter(CSV_FIELD_DELIMITER)
            .setRecordSeparator(CSV_RECORD_SEPARATOR)
            .setTrim(true)
            .setSkipHeaderRecord(true)
            .build();
    }

    private CsvData csvRecordToCsvDataConverter(
        CSVRecord csvRecord, Map<String, Integer> headers, Set<String> uniqueCodeSet, int index
    ) {
        String codeField = csvRecord.get(headers.get("code"));
        if(uniqueCodeSet.contains(codeField)) {
            throw new NonUniqueCodeException("Code " + codeField + " not unique for this CSV file");
        }
        uniqueCodeSet.add(codeField);
        String sourceField = csvRecord.get(headers.get("source"));
        String codeListCode = csvRecord.get(headers.get("codeListCode"));
        String fromDateField = csvRecord.get(headers.get("fromDate"));
        String toDateField = csvRecord.get(headers.get("toDate"));
        String displayValueField = csvRecord.get(headers.get("displayValue"));
        String longDescriptionField = csvRecord.get(headers.get("longDescription"));
        String sortingPriorityField = csvRecord.get(headers.get("sortingPriority"));
        return CsvData.builder()
            .code(codeField.isBlank() ? null : codeField)
            .source(sourceField.isBlank() ? null : sourceField)
            .codeListCode(codeListCode.isBlank() ? null : codeListCode)
            .fromDate(fromDateField.isEmpty() ? null : LocalDate.parse(fromDateField, DateTimeFormatter.ofPattern(CSV_DATE_FORMAT)))
            .toDate(toDateField.isEmpty() ? null : LocalDate.parse(toDateField, DateTimeFormatter.ofPattern(CSV_DATE_FORMAT)))
            .displayValue(displayValueField.isBlank() ? null : displayValueField)
            .longDescription(longDescriptionField.isBlank() ? null : longDescriptionField)
            .sortingPriority(sortingPriorityField.isBlank() ? null : Long.parseLong(sortingPriorityField))
            .rowNumber(index)
            .build();
    }

    private static List<String> cvsDataToFieldArray(CsvData csvData) {
        return Arrays.asList(
            csvData.getSource(),
            csvData.getCodeListCode(),
            csvData.getCode(),
            csvData.getDisplayValue(),
            csvData.getLongDescription(),
            csvData.getFromDate() == null ? null : csvData.getFromDate().format(DateTimeFormatter.ofPattern(CSV_DATE_FORMAT)),
            csvData.getToDate()  == null ? null : csvData.getToDate().format(DateTimeFormatter.ofPattern(CSV_DATE_FORMAT)),
            csvData.getSortingPriority() == null ? null : csvData.getSortingPriority().toString()
        );
    }

    private static boolean isCSVContentType(MultipartFile file) {
        return "text/csv".equalsIgnoreCase(file.getContentType());
    }

}
