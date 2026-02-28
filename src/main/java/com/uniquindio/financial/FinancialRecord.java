package com.uniquindio.financial;

import java.time.LocalDate;

public record FinancialRecord(
    LocalDate date,
    double open,
    double high,
    double low,
    double close,
    double volume,
    double adjClose
) {}
