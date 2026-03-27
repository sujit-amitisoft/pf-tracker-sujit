package com.amiti.financetracker.reports.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PdfReportRenderer {
    private PdfReportRenderer() {
    }

    public static byte[] render(List<String> lines) {
        List<List<String>> pages = paginate(lines, 42);
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        int objectCount = 3 + (pages.size() * 2);

        offsets.add(pdf.length());
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(pdf.length());
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [");
        for (int i = 0; i < pages.size(); i += 1) {
            int pageObject = 4 + (i * 2);
            pdf.append(pageObject).append(" 0 R ");
        }
        pdf.append("] /Count ").append(pages.size()).append(" >>\nendobj\n");

        offsets.add(pdf.length());
        pdf.append("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        for (int i = 0; i < pages.size(); i += 1) {
            int pageObject = 4 + (i * 2);
            int contentObject = pageObject + 1;
            offsets.add(pdf.length());
            pdf.append(pageObject).append(" 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents ").append(contentObject).append(" 0 R >>\nendobj\n");

            String stream = buildStream(pages.get(i));
            byte[] streamBytes = stream.getBytes(StandardCharsets.UTF_8);
            offsets.add(pdf.length());
            pdf.append(contentObject).append(" 0 obj\n<< /Length ").append(streamBytes.length).append(" >>\nstream\n").append(stream).append("\nendstream\nendobj\n");
        }

        int xrefStart = pdf.length();
        pdf.append("xref\n0 ").append(objectCount + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xrefStart).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static List<List<String>> paginate(List<String> lines, int pageSize) {
        List<List<String>> pages = new ArrayList<>();
        for (int index = 0; index < lines.size(); index += pageSize) {
            pages.add(lines.subList(index, Math.min(lines.size(), index + pageSize)));
        }
        if (pages.isEmpty()) {
            pages.add(List.of("Finance Report"));
        }
        return pages;
    }

    private static String buildStream(List<String> lines) {
        StringBuilder content = new StringBuilder("BT\n/F1 12 Tf\n50 760 Td\n");
        boolean first = true;
        for (String line : lines) {
            if (!first) {
                content.append("T*\n");
            }
            first = false;
            content.append("(").append(escape(line)).append(") Tj\n");
        }
        content.append("ET");
        return content.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}