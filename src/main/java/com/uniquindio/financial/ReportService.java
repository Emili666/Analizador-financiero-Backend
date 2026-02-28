package com.uniquindio.financial;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

public class ReportService {

        public byte[] generateTechnicalReport(List<Map<String, Object>> assets) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);

                        DeviceRgb primaryColor = new DeviceRgb(0, 150, 60); // Uniquindio green
                        DeviceRgb darkColor = new DeviceRgb(20, 30, 48);

                        // Headers
                        document.add(new Paragraph("UNIVERSIDAD DEL QUINDÍO")
                                        .setBold().setFontSize(18).setFontColor(primaryColor)
                                        .setTextAlignment(TextAlignment.CENTER));

                        document.add(new Paragraph(
                                        "Facultad de Ingeniería\nPrograma de Ingeniería de Sistemas y Computación")
                                        .setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                                        .setFontColor(darkColor));

                        document.add(new Paragraph("\nAnálisis de Algoritmos - Reporte del Portafolio Bursátil\n")
                                        .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));

                        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                        document.add(new Paragraph("Fecha de generación: " + date + "\n\n")
                                        .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

                        // Description
                        document.add(new Paragraph("El siguiente reporte consolida los activos financieros analizados "
                                        +
                                        "mediante métodos algorítmicos. Los activos se encuentran ordenados por su nivel de volatilidad histórica "
                                        +
                                        "asociada, lo que determina directamente su Perfil de Riesgo (Conservador, Moderado o Agresivo).\n\n")
                                        .setTextAlignment(TextAlignment.JUSTIFIED));

                        // Metodología
                        document.add(new Paragraph("1. Metodología de Clasificación (Volatilidad)\n")
                                        .setBold().setFontSize(12).setFontColor(primaryColor));
                        document.add(new Paragraph(
                                        "La volatilidad se calcula como la desviación estándar anualizada de los retornos diarios "
                                                        +
                                                        "del precio de cierre del activo. Matemáticamente, representa la dispersión de los retornos alrededor de "
                                                        +
                                                        "su media (riesgo). \n- Conservador: Volatilidad < 0.15\n- Moderado: 0.15 <= Volatilidad < 0.30\n- Agresivo: Volatilidad >= 0.30\n\n")
                                        .setTextAlignment(TextAlignment.JUSTIFIED));

                        // Add Chart
                        try {
                                byte[] chartBytes = generateVolatilityChart(assets);
                                Image img = new Image(ImageDataFactory.create(chartBytes));
                                img.setAutoScale(true);
                                document.add(img);
                                document.add(new Paragraph("\n"));
                        } catch (Exception e) {
                                System.err.println("Error generating chart: " + e.getMessage());
                        }

                        document.add(new Paragraph("2. Análisis Portafolio\n")
                                        .setBold().setFontSize(12).setFontColor(primaryColor));

                        // Custom Table
                        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 40, 30 }))
                                        .useAllAvailableWidth();

                        // Table Headers
                        table.addHeaderCell(createHeaderCell("Activo Financiero (Símbolo)", primaryColor));
                        table.addHeaderCell(createHeaderCell("Perfil de Riesgo", primaryColor));
                        table.addHeaderCell(createHeaderCell("Volatilidad Anualizada", primaryColor));

                        // Rows
                        boolean alternateRow = false;
                        DeviceRgb lightGray = new DeviceRgb(245, 245, 245);
                        DeviceRgb white = new DeviceRgb(255, 255, 255);

                        for (Map<String, Object> asset : assets) {
                                DeviceRgb bg = alternateRow ? lightGray : white;

                                String risk = String.valueOf(asset.get("risk"));
                                DeviceRgb riskColor = darkColor;
                                if (risk.equals("CONSERVATIVE"))
                                        riskColor = new DeviceRgb(46, 125, 50);
                                if (risk.equals("MODERATE"))
                                        riskColor = new DeviceRgb(230, 81, 0);
                                if (risk.equals("AGGRESSIVE"))
                                        riskColor = new DeviceRgb(198, 40, 40);

                                table.addCell(createCell(String.valueOf(asset.get("symbol")), bg, darkColor).setBold());
                                table.addCell(createCell(risk, bg, riskColor).setBold());
                                table.addCell(createCell(String.format("%.4f", (double) asset.get("volatility")), bg,
                                                darkColor));

                                alternateRow = !alternateRow;
                        }

                        document.add(table);

                        document.add(new Paragraph(
                                        "\n\n* Las estadísticas mostradas son netamente de propósitos educativos e ilustrativos para el análisis de complejidad algorítmica.")
                                        .setFontSize(9).setItalic());

                        document.close();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return baos.toByteArray();
        }

        private Cell createHeaderCell(String text, DeviceRgb bgColor) {
                return new Cell().add(new Paragraph(text).setBold().setFontColor(new DeviceRgb(255, 255, 255)))
                                .setBackgroundColor(bgColor)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(8);
        }

        private Cell createCell(String text, DeviceRgb bgColor, DeviceRgb fontColor) {
                return new Cell().add(new Paragraph(text).setFontColor(fontColor))
                                .setBackgroundColor(bgColor)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(5);
        }

        public byte[] generateComparativeReport(String sym1, String sym2, Map<String, Double> similarityMetrics,
                        List<FinancialRecord> hist1, List<FinancialRecord> hist2) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);

                        DeviceRgb primaryColor = new DeviceRgb(41, 121, 255); // Blue for comparative
                        DeviceRgb darkColor = new DeviceRgb(20, 30, 48);

                        // Headers
                        document.add(new Paragraph("UNIVERSIDAD DEL QUINDÍO")
                                        .setBold().setFontSize(18).setFontColor(primaryColor)
                                        .setTextAlignment(TextAlignment.CENTER));

                        document.add(new Paragraph(
                                        "Facultad de Ingeniería\nPrograma de Ingeniería de Sistemas y Computación")
                                        .setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                                        .setFontColor(darkColor));

                        document.add(new Paragraph("\nAnálisis de Similitud - Reporte Específico\n")
                                        .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));

                        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                        document.add(new Paragraph("Fecha de generación: " + date + "\n\n")
                                        .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

                        // Description
                        document.add(new Paragraph(String.format(
                                        "El presente reporte detalla el análisis matemático y algorítmico de la similitud histórica "
                                                        +
                                                        "entre los activos financieros %s y %s. Se han empleado métricas de distancia y correlación para determinar el grado de dependencia de ambas series de precios.\n\n",
                                        sym1, sym2))
                                        .setTextAlignment(TextAlignment.JUSTIFIED));

                        // Visual Chart
                        try {
                                byte[] timeSeriesBytes = generateTimeSeriesChart(sym1, sym2, hist1, hist2);
                                Image tsImg = new Image(ImageDataFactory.create(timeSeriesBytes));
                                tsImg.setAutoScale(true);
                                document.add(tsImg);
                                document.add(new Paragraph("\n"));
                        } catch (Exception e) {
                                System.err.println("Error generating time series chart: " + e.getMessage());
                        }

                        document.add(new Paragraph("1. Análisis Visual y Textual de los Métodos\n")
                                        .setBold().setFontSize(12).setFontColor(primaryColor));

                        document.add(new Paragraph(
                                        "El análisis comparativo se basa en cuatro algoritmos principales para determinar la similitud y correlación "
                                                        +
                                                        "entre las dos series de tiempo. Cada uno proporciona una perspectiva matemática diferente sobre la relación de los activos:\n")
                                        .setTextAlignment(TextAlignment.JUSTIFIED));

                        com.itextpdf.layout.element.List bulletList = new com.itextpdf.layout.element.List();
                        bulletList.add(new com.itextpdf.layout.element.ListItem(
                                        "Distancia Euclidiana: Calcula la distancia lineal directa entre dos puntos en el espacio n-dimensional. "
                                                        +
                                                        "Valores más bajos indican mayor similitud en los precios absolutos."));
                        bulletList.add(new com.itextpdf.layout.element.ListItem(
                                        "Correlación de Pearson: Mide la relación lineal entre las dos series. " +
                                                        "Un valor cercano a 1 indica que se mueven en la misma dirección fuerte, 0 significa poca correlación y -1 movimiento opuesto."));
                        bulletList.add(new com.itextpdf.layout.element.ListItem(
                                        "Similitud Coseno: Mide el ángulo entre las dos series tratadas como vectores. "
                                                        +
                                                        "Ignora la magnitud y se centra en la dirección. Valores cercanos a 1 indican que los activos tienden a moverse en direcciones similares."));
                        bulletList.add(new com.itextpdf.layout.element.ListItem(
                                        "Dynamic Time Warping (DTW): Un algoritmo robusto que alinea de forma no lineal "
                                                        +
                                                        "dos secuencias. Es útil para medir similitud incluso cuando hay desfasajes de tiempo temporales (e.g. un activo reacciona días después que el otro). "
                                                        +
                                                        "Valores más bajos implican mayor similitud considerando distorsiones temporales."));
                        document.add(bulletList);
                        document.add(new Paragraph("\n"));

                        document.add(new Paragraph("2. Resultados y Métricas de Similitud\n")
                                        .setBold().setFontSize(12).setFontColor(primaryColor));

                        // Custom Table
                        Table table = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }))
                                        .useAllAvailableWidth();

                        // Table Headers
                        table.addHeaderCell(createHeaderCell("Métrica Escalar (1-vs-1)", primaryColor));
                        table.addHeaderCell(createHeaderCell("Valor Calculado", primaryColor));

                        // Rows
                        boolean alternateRow = false;
                        DeviceRgb lightGray = new DeviceRgb(245, 245, 245);
                        DeviceRgb white = new DeviceRgb(255, 255, 255);

                        for (Map.Entry<String, Double> entry : similarityMetrics.entrySet()) {
                                DeviceRgb bg = alternateRow ? lightGray : white;
                                table.addCell(createCell(entry.getKey().toUpperCase(), bg, darkColor).setBold());
                                table.addCell(createCell(String.format("%.4f", entry.getValue()), bg, darkColor));
                                alternateRow = !alternateRow;
                        }

                        document.add(table);

                        document.add(new Paragraph(
                                        "\n\n* Las estadísticas mostradas son netamente de propósitos educativos e ilustrativos para el análisis de complejidad algorítmica.")
                                        .setFontSize(9).setItalic());

                        document.close();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return baos.toByteArray();
        }

        private byte[] generateVolatilityChart(List<Map<String, Object>> assets) throws Exception {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                // Limit to top 10 for readability
                int limit = Math.min(10, assets.size());
                for (int i = 0; i < limit; i++) {
                        Map<String, Object> a = assets.get(i);
                        dataset.addValue((Double) a.get("volatility"), "Volatilidad", (String) a.get("symbol"));
                }

                JFreeChart barChart = ChartFactory.createBarChart(
                                "Top Activos por Volatilidad",
                                "Activo",
                                "Volatilidad Anualizada",
                                dataset,
                                PlotOrientation.VERTICAL,
                                false, true, false);

                CategoryPlot plot = barChart.getCategoryPlot();
                plot.setBackgroundPaint(Color.white);
                plot.setRangeGridlinePaint(Color.gray);

                BufferedImage bufferedImage = barChart.createBufferedImage(600, 300);
                ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", imgBaos);
                return imgBaos.toByteArray();
        }

        private byte[] generateTimeSeriesChart(String sym1, String sym2, List<FinancialRecord> hist1,
                        List<FinancialRecord> hist2) throws Exception {
                XYSeries series1 = new XYSeries(sym1);
                XYSeries series2 = new XYSeries(sym2);

                // We assume both histories have somewhat aligned sizes, but let's just plot the
                // overlapping recent points, e.g., last 100 days
                int points = Math.min(Math.min(100, hist1.size()), hist2.size());

                // Reverse iteration to show chronological if records are sorted desc by date,
                // so we take from the end if they are desc.
                // Assuming records might be recent first
                for (int i = 0; i < points; i++) {
                        // we use index as x-axis representing time backwards, or reverse for forward
                        // time
                        series1.add(points - i, hist1.get(hist1.size() - 1 - i).close());
                        series2.add(points - i, hist2.get(hist2.size() - 1 - i).close());
                }

                XYSeriesCollection dataset = new XYSeriesCollection();
                dataset.addSeries(series1);
                dataset.addSeries(series2);

                JFreeChart xylineChart = ChartFactory.createXYLineChart(
                                "Comparación de Precios de Cierre",
                                "Tiempo (Días)",
                                "Precio USD",
                                dataset,
                                PlotOrientation.VERTICAL,
                                true, true, false);

                XYPlot plot = xylineChart.getXYPlot();
                plot.setBackgroundPaint(Color.white);
                plot.setRangeGridlinePaint(Color.lightGray);
                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
                renderer.setSeriesPaint(0, new Color(41, 121, 255));
                renderer.setSeriesPaint(1, new Color(255, 61, 0));
                renderer.setSeriesStroke(0, new BasicStroke(2.0f));
                renderer.setSeriesStroke(1, new BasicStroke(2.0f));
                plot.setRenderer(renderer);

                BufferedImage bufferedImage = xylineChart.createBufferedImage(600, 350);
                ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", imgBaos);
                return imgBaos.toByteArray();
        }
}
