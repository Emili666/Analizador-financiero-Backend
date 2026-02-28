package com.uniquindio.financial;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

public class ReportService {

    public byte[] generateTechnicalReport(List<Map<String, Object>> assets) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Universidad del Quindío - Ingeniería de Sistemas").setBold().setFontSize(16));
            document.add(new Paragraph("Proyecto: Análisis de Algoritmos Financieros").setBold().setFontSize(14));
            document.add(new Paragraph("Reporte Técnico de Activos").setFontSize(12));
            document.add(new Paragraph(" "));

            Table table = new Table(3);
            table.addCell("Activo");
            table.addCell("Riesgo");
            table.addCell("Volatilidad");

            for (Map<String, Object> asset : assets) {
                table.addCell(String.valueOf(asset.get("symbol")));
                table.addCell(String.valueOf(asset.get("risk")));
                table.addCell(String.format("%.4f", (double) asset.get("volatility")));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
