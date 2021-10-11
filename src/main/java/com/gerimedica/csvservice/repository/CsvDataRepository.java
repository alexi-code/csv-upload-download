package com.gerimedica.csvservice.repository;

import com.gerimedica.csvservice.model.CsvData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CsvDataRepository extends JpaRepository<CsvData, UUID> {

}
