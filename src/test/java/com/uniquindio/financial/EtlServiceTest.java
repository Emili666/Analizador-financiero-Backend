package com.uniquindio.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EtlServiceTest {

    private EtlService etlService;

    @BeforeEach
    void setUp() {
        etlService = new EtlService();
    }

    @Test
    void testParseCsv() {
        // Tiingo format: date,close,high,low,open,volume,adjClose...
        String csv = "date,close,high,low,open,volume,adjClose\n" +
                "2023-01-01,154.0,155.0,149.0,150.0,1000000,154.0\n" +
                "2023-01-02,157.0,158.0,153.0,154.0,1200000,157.0";

        List<FinancialRecord> records = etlService.parseCsv(csv);
        assertEquals(2, records.size());
        assertEquals(LocalDate.parse("2023-01-01"), records.get(0).date());
        assertEquals(150.0, records.get(0).open());
    }

    @Test
    void testCleanAndUnifyData() {
        FinancialRecord r1_A = new FinancialRecord(LocalDate.parse("2023-01-01"), 10, 10, 10, 10, 100, 10);
        FinancialRecord r2_A = new FinancialRecord(LocalDate.parse("2023-01-03"), 12, 12, 12, 12, 100, 12); // Skips
                                                                                                            // 01-02

        FinancialRecord r1_B = new FinancialRecord(LocalDate.parse("2023-01-01"), 20, 20, 20, 20, 200, 20);
        FinancialRecord r2_B = new FinancialRecord(LocalDate.parse("2023-01-02"), 21, 21, 21, 21, 200, 21); // Has 01-02
        FinancialRecord r3_B = new FinancialRecord(LocalDate.parse("2023-01-03"), Double.NaN, Double.NaN, Double.NaN,
                Double.NaN, 0, Double.NaN); // NaN values

        Map<String, List<FinancialRecord>> raw = new HashMap<>();
        raw.put("A", Arrays.asList(r1_A, r2_A));
        raw.put("B", Arrays.asList(r1_B, r2_B, r3_B));

        Map<String, List<FinancialRecord>> unified = etlService.cleanAndUnifyData(raw);

        List<FinancialRecord> unifiedA = unified.get("A");
        List<FinancialRecord> unifiedB = unified.get("B");

        assertEquals(3, unifiedA.size()); // Should have 01-01, 01-02, 01-03
        assertEquals(3, unifiedB.size());

        // Check A filled the gap using last known (01-01 value for 01-02)
        assertEquals(LocalDate.parse("2023-01-02"), unifiedA.get(1).date());
        assertEquals(10.0, unifiedA.get(1).close()); // Last known

        // Check B replaced NaN with last known (01-02 value for 01-03)
        assertEquals(LocalDate.parse("2023-01-03"), unifiedB.get(2).date());
        assertFalse(Double.isNaN(unifiedB.get(2).close()));
        assertEquals(21.0, unifiedB.get(2).close()); // Last known
    }
}
