package com.gerimedica.csvservice.repository;

import com.gerimedica.csvservice.model.CsvFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CsvFileRepository extends JpaRepository<CsvFile, UUID> {

}
