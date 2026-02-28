package com.uniquindio.financial;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class EtlService {

    private final HttpClient httpClient;

    public EtlService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Downloads historical data for a given symbol using manual HTTP request.
     * Requirement: No high-level libraries for data download.
     */
    public String downloadHistoricalData(String symbol) throws IOException, InterruptedException {
        // Using Yahoo Finance query2 API (direct HTTP)
        // Note: In a real scenario, we would need to handle crumbs and cookies if using
        // the download endpoint
        // For this project, we'll use a public-facing URL or a mocked response if
        // external access is blocked.
        String url = String.format(
                "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=1456617600&period2=1740614400&interval=1d&events=history&includeAdjustedClose=true",
                symbol);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download data for " + symbol + ". Status code: " + response.statusCode());
        }

        return response.body();
    }

    public List<FinancialRecord> parseCsv(String csvData) {
        List<FinancialRecord> records = new ArrayList<>();
        String[] lines = csvData.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split(",");
            if (parts.length >= 7) {
                try {
                    String dateStr = parts[0].trim();
                    if (dateStr.isEmpty() || dateStr.equals("null"))
                        continue;

                    FinancialRecord record = new FinancialRecord(
                            LocalDate.parse(dateStr),
                            parseSafe(parts[1]), // Open
                            parseSafe(parts[2]), // High
                            parseSafe(parts[3]), // Low
                            parseSafe(parts[4]), // Close
                            parseSafe(parts[6]), // Volume
                            parseSafe(parts[5]) // Adj Close
                    );
                    records.add(record);
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
        return records;
    }

    private double parseSafe(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * Requirement: Data Cleaning and Unification.
     * Interpolates missing values and aligns series to a common date range.
     */
    public Map<String, List<FinancialRecord>> cleanAndUnifyData(Map<String, List<FinancialRecord>> rawData) {
        // 1. Identify all unique dates across all assets
        Set<LocalDate> allDates = new TreeSet<>();
        rawData.values().forEach(list -> list.forEach(r -> allDates.add(r.date())));

        Map<String, List<FinancialRecord>> unifiedData = new HashMap<>();

        for (Map.Entry<String, List<FinancialRecord>> entry : rawData.entrySet()) {
            String symbol = entry.getKey();
            List<FinancialRecord> original = entry.getValue();
            Map<LocalDate, FinancialRecord> dateMap = original.stream()
                    .collect(Collectors.toMap(FinancialRecord::date, r -> r, (r1, r2) -> r1));

            List<FinancialRecord> unifiedList = new ArrayList<>();
            FinancialRecord lastKnown = null;

            for (LocalDate date : allDates) {
                if (dateMap.containsKey(date)) {
                    FinancialRecord rec = dateMap.get(date);
                    // Handle internal NaNs (Data Cleaning)
                    if (Double.isNaN(rec.close()) && lastKnown != null) {
                        rec = interpolate(rec, lastKnown);
                    }
                    unifiedList.add(rec);
                    lastKnown = rec;
                } else if (lastKnown != null) {
                    // Fill gaps (Requirement: Handle missing records)
                    unifiedList.add(new FinancialRecord(date, lastKnown.open(), lastKnown.high(),
                            lastKnown.low(), lastKnown.close(), 0, lastKnown.adjClose()));
                }
            }
            unifiedData.put(symbol, unifiedList);
        }
        return unifiedData;
    }

    private FinancialRecord interpolate(FinancialRecord current, FinancialRecord last) {
        return new FinancialRecord(
                current.date(),
                Double.isNaN(current.open()) ? last.close() : current.open(),
                Double.isNaN(current.high()) ? last.high() : current.high(),
                Double.isNaN(current.low()) ? last.low() : current.low(),
                Double.isNaN(current.close()) ? last.close() : current.close(),
                Double.isNaN(current.volume()) ? 0 : current.volume(),
                Double.isNaN(current.adjClose()) ? last.adjClose() : current.adjClose());
    }
}
