package com.its.notificationservice.service;

import com.its.notificationservice.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JasperReportService {

    private static final String REPORT_PATH = "/reports/payment_receipt.jrxml";
    private static final String LOGO_PATH = "/reports/shopswift.png";

    public byte[] generatePaymentReceiptPdf(PaymentResponse response) throws JRException {
        if (response == null) {
            log.error("generatePaymentReceiptPdf called with null response");
            throw new IllegalArgumentException("PaymentResponse cannot be null");
        }

        var orderId = response.getOrderId();
        var transactionId = response.getTransactionId();

        log.info("Starting receipt PDF generation: orderId={}, transactionId={}, reportPath={}",
                orderId, transactionId, REPORT_PATH);

        try (InputStream reportStream = getClass().getResourceAsStream(REPORT_PATH);
             InputStream logoStream = getClass().getResourceAsStream(LOGO_PATH)) {

            if (reportStream == null) {
                log.error("Report template not found: orderId={}, transactionId={}, reportPath={}",
                        orderId, transactionId, REPORT_PATH);
                throw new IllegalStateException("Report template not found: " + REPORT_PATH);
            }

            if (logoStream == null) {
                log.error("Logo file not found: {}", LOGO_PATH);
                throw new IllegalStateException("Logo file not found: " + LOGO_PATH);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> params = new HashMap<>();
            params.put("orderId", String.valueOf(response.getOrderId()));
            params.put("transactionId", String.valueOf(response.getTransactionId()));
            params.put("amount", String.valueOf(response.getAmount()));
            params.put("logo", logoStream);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, new JREmptyDataSource()
            );

            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);

            log.info("Receipt PDF generated successfully: orderId={}, transactionId={}, pdfSizeBytes={}",
                    orderId, transactionId, pdf.length);

            return pdf;

        } catch (JRException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            log.error(
                    "Jasper report generation failed: orderId={}, transactionId={}, reportPath={}, error={}, rootError={}",
                    orderId, transactionId, REPORT_PATH, e.getMessage(), root.getMessage(), e
            );
            throw e;
        } catch (IOException e) {
            log.error("IO error reading report resource streams: orderId={}, transactionId={}, error={}",
                    orderId, transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to read report resources", e);
        }
    }
}