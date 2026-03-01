package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.payment.dto.PaymentReferenceRequest;
import ao.co.oportunidade.payment.dto.PaymentReferenceQrResponse;
import ao.co.oportunidade.payment.dto.PaymentReferenceResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * Service that issues MultiCaixa payment references as QR codes.
 * The QR encodes the same reference text as the text service.
 * Compatible with QR-enabled MultiCaixa POS/agents in Angola.
 */
@ApplicationScoped
public class PaymentReferenceQrService {

    private static final Logger LOG = Logger.getLogger(PaymentReferenceQrService.class);
    private static final int QR_SIZE = 256;
    private static final String PNG_MIME = "image/png";

    @Inject
    PaymentReferenceTextService textService;

    /**
     * Issues a MultiCaixa payment reference as QR code image (base64 PNG).
     *
     * @param request the reference request
     * @return QR code as base64 PNG plus reference metadata
     */
    public PaymentReferenceQrResponse issueReferenceQr(PaymentReferenceRequest request) {
        PaymentReferenceResponse textRef = textService.issueReference(request);
        String referenceText = textRef.getReferenceText();

        byte[] qrPng = encodeQrToPng(referenceText);
        String base64 = Base64.getEncoder().encodeToString(qrPng);

        LOG.infof("Issued QR reference: ref=%s entity=%s order=%s",
                textRef.getReferenceNumber(), textRef.getEntityCode(), request.getMerchantTransactionId());

        return PaymentReferenceQrResponse.builder()
                .qrCodeBase64("data:" + PNG_MIME + ";base64," + base64)
                .referenceNumber(textRef.getReferenceNumber())
                .entityCode(textRef.getEntityCode())
                .referenceText(referenceText)
                .build();
    }

    byte[] encodeQrToPng(String content) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 2);

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, PNG_MIME, out);
                return out.toByteArray();
            }
        } catch (WriterException | IOException e) {
            LOG.errorf(e, "Failed to generate QR code");
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}
