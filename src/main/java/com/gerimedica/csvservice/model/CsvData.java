package com.gerimedica.csvservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "csv_data",
    uniqueConstraints = @UniqueConstraint(columnNames={"csv_file", "code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvData {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "csv_data_id", updatable = false, nullable = false)
    private UUID csvId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "csv_file")
    @JsonBackReference
    private CsvFile csvFile;

    @Column
    private Integer rowNumber;

    @Column
    private String source;

    @Column
    private String codeListCode;

    @Column(name = "code")
    private String code;

    @Column
    private String displayValue;

    @Column
    private String longDescription;

    @Column
    private LocalDate fromDate;

    @Column
    private LocalDate toDate;

    @Column
    private Long sortingPriority;

}
