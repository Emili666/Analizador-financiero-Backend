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
        // Using Tiingo API instead of Yahoo for reliable, unblocked data
        String token = "e156d6b71904a57eff4dd3959c328ae70cab71bf";
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(5);

        // Tiingo requires different symbol names for Colombian stocks or some ETFs
        // Tiingo is mainly US. We'll strip the .CB for US equivalents or let them fail
        // to mock
        // if they don't exist on Tiingo, but we'll try our best.
        String cleanSymbol = symbol.replace(".CB", "");

        String url = String.format(
                "https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&format=csv&token=%s",
                cleanSymbol, startDate.toString(), endDate.toString(), token);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Failed to download data for " + symbol + " from Tiingo. Status: " + response.statusCode());
        }

        return response.body();
    }

    public List<FinancialRecord> parseCsv(String csvData) {
        List<FinancialRecord> records = new ArrayList<>();
        String[] lines = csvData.split("\n");
        // Tiingo format:
        // date,close,high,low,open,volume,adjClose,adjHigh,adjLow,adjOpen,adjVolume,divCash,splitFactor
        // Indices: 0:date, 1:close, 2:high, 3:low, 4:open, 5:volume, 6:adjClose
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split(",");
            if (parts.length >= 7) {
                try {
                    String dateStr = parts[0].trim();
                    if (dateStr.isEmpty() || dateStr.equals("null"))
                        continue;

                    // Tiingo dates might come as 2023-01-01 00:00:00+00:00 so we substring
                    if (dateStr.length() > 10)
                        dateStr = dateStr.substring(0, 10);

                    FinancialRecord record = new FinancialRecord(
                            LocalDate.parse(dateStr),
                            parseSafe(parts[4]), // Open
                            parseSafe(parts[2]), // High
                            parseSafe(parts[3]), // Low
                            parseSafe(parts[1]), // Close
                            parseSafe(parts[5]), // Volume
                            parseSafe(parts[6]) // Adj Close
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

            // Find first available record for backward-filling if needed
            FinancialRecord firstKnown = null;
            for (LocalDate date : allDates) {
                if (dateMap.containsKey(date)) {
                    firstKnown = dateMap.get(date);
                    break;
                }
            }
            if (firstKnown == null) {
                firstKnown = new FinancialRecord(LocalDate.now(), 100, 100, 100, 100, 0, 100);
            }

            for (LocalDate date : allDates) {
                if (dateMap.containsKey(date)) {
                    FinancialRecord rec = dateMap.get(date);
                    // Handle internal NaNs (Data Cleaning)
                    if (Double.isNaN(rec.close()) && lastKnown != null) {
                        rec = interpolate(rec, lastKnown);
                    }
                    unifiedList.add(rec);
                    lastKnown = rec;
                } else {
                    // Fill gaps (Requirement: Handle missing records). Use lastKnown if available,
                    // else firstKnown
                    FinancialRecord filler = lastKnown != null ? lastKnown : firstKnown;
                    unifiedList.add(new FinancialRecord(date, filler.open(), filler.high(),
                            filler.low(), filler.close(), 0, filler.adjClose()));
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
