package com.uniquindio.financial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class FinancialApplication {

    private final AlgorithmService algorithmService = new AlgorithmService();
    private final EtlService etlService = new EtlService();

    public static void main(String[] args) {
        SpringApplication.run(FinancialApplication.class, args);
    }

    @GetMapping("/api/health")
    public String health() {
        return "Financial API is running";
    }

    // Endpoints for similarity, etl, etc. will be added here
}
