package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Semaphore;

@Service
public class OcrServiceImpl implements OcrService {

    @Autowired
    private FieldExtractionService fieldExtractionService;

    private final Semaphore ocrSemaphore = new Semaphore(2, true);

    @Override
    public ExtractedComplaintData extractTextFromPdf(MultipartFile file) throws Exception {

        ocrSemaphore.acquire();

        try {
            Tesseract tesseract = new Tesseract();

            File localTessData = new File("tessdata");
            if (localTessData.exists()) {
                tesseract.setDatapath(localTessData.getAbsolutePath());
            } else {
                tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            }

            File hinFile = new File(localTessData, "hin.traineddata");
            if (hinFile.exists()) {
                tesseract.setLanguage("eng+hin");
            } else {
                tesseract.setLanguage("eng");
            }
            tesseract.setPageSegMode(3);

            StringBuilder extractedText = new StringBuilder();

            try (PDDocument document = Loader.loadPDF(file.getBytes())) {

                PDFRenderer pdfRenderer = new PDFRenderer(document);
                PDFTextStripper textStripper = new PDFTextStripper();

                int totalPages = document.getNumberOfPages();
                int pagesToProcess = totalPages;

                boolean rotatePages = false;

                if (totalPages > 1) {
                    System.out.println("Detecting orientation using page 2...");

                    try {
                        BufferedImage referenceImage =
                                pdfRenderer.renderImageWithDPI(1, 150);

                        rotatePages = shouldRotate(referenceImage, tesseract);

                        System.out.println(
                                "Orientation decision: "
                                        + (rotatePages ? "180 degrees" : "normal")
                        );
                    } catch (Exception ex) {
                        System.err.println("Orientation check skipped: " + ex.getMessage());
                    }
                }

                for (int page = 0; page < pagesToProcess; page++) {

                    System.out.println(
                            "Processing page " + (page + 1)
                                    + " of " + pagesToProcess
                    );

                    String pageText = null;

                    try {
                        textStripper.setStartPage(page + 1);
                        textStripper.setEndPage(page + 1);

                        String digitalText =
                                textStripper.getText(document).trim();

                        if (digitalText.length() > 50) {
                            System.out.println(
                                    "Page " + (page + 1)
                                            + ": Digital text detected. Skipping OCR."
                            );

                            pageText = digitalText;

                        } else {
                            System.out.println(
                                    "Page " + (page + 1)
                                            + ": Scanned page. Running OCR..."
                            );

                            BufferedImage rawImage =
                                    pdfRenderer.renderImageWithDPI(page, 150);

                            if (rotatePages) {
                                rawImage = rotate180(rawImage);
                            }

                            BufferedImage grayscaleImage =
                                    convertToGrayscale(rawImage);

                            BufferedImage binarizedImage =
                                    applyOtsuThreshold(grayscaleImage);

                            pageText = tesseract.doOCR(binarizedImage);
                        }

                    } catch (Exception ex) {
                        System.err.println(
                                "Error processing page " + (page + 1)
                                        + ": " + ex.getMessage()
                        );

                        ex.printStackTrace();
                    }

                    if (pageText != null && !pageText.isBlank()) {
                        extractedText.append("\n--- PAGE ")
                                .append(page + 1)
                                .append(" ---\n");

                        extractedText.append(pageText.trim());
                        extractedText.append("\n\n");
                    }
                }
            }

            String text = extractedText.toString();
            String fileName = file.getOriginalFilename();

            return fieldExtractionService.extractFields(fileName, text);

        } finally {
            ocrSemaphore.release();
        }
    }

    private boolean shouldRotate(
            BufferedImage image,
            Tesseract tesseract) {

        try {
            BufferedImage normalImage = convertToGrayscale(image);
            BufferedImage normalBinary = applyOtsuThreshold(normalImage);

            String normalText = tesseract.doOCR(normalBinary);

            BufferedImage rotatedImage = rotate180(image);
            BufferedImage rotatedGray = convertToGrayscale(rotatedImage);
            BufferedImage rotatedBinary = applyOtsuThreshold(rotatedGray);

            String rotatedText = tesseract.doOCR(rotatedBinary);

            int normalScore = calculateOcrScore(normalText);
            int rotatedScore = calculateOcrScore(rotatedText);

            System.out.println("Normal OCR score: " + normalScore);
            System.out.println("Rotated OCR score: " + rotatedScore);

            int scoreDifference = rotatedScore - normalScore;

            if (scoreDifference >= 5) {
                System.out.println(
                        "Rotation required. Score difference: " + scoreDifference
                );
                return true;
            }

            System.out.println(
                    "Keeping normal orientation. Score difference: "
                            + scoreDifference
            );

            return false;

        } catch (Exception ex) {
            System.err.println(
                    "Orientation detection failed: " + ex.getMessage()
            );

            return false;
        }
    }

    private int calculateOcrScore(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        int score = 0;
        String[] words = text.trim().split("\\s+");

        for (String word : words) {
            if (word.matches("[A-Za-z]{2,}")) {
                score++;
            }
        }

        score += Math.min(text.length() / 100, 20);

        return score;
    }

    private BufferedImage rotate180(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rotated = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = rotated.createGraphics();

        AffineTransform transform = new AffineTransform();
        transform.translate(width, height);
        transform.rotate(Math.PI);

        graphics.drawImage(image, transform, null);
        graphics.dispose();

        return rotated;
    }

    private BufferedImage convertToGrayscale(BufferedImage original) {

        BufferedImage grayscale = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D graphics = grayscale.createGraphics();
        graphics.drawImage(original, 0, 0, null);
        graphics.dispose();

        return grayscale;
    }

    private BufferedImage applyOtsuThreshold(BufferedImage grayscale) {

        int width = grayscale.getWidth();
        int height = grayscale.getHeight();

        int[] histogram = new int[256];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayscale.getRaster().getSample(x, y, 0);
                histogram[pixel]++;
            }
        }

        int totalPixels = width * height;
        double sum = 0;

        for (int i = 0; i < 256; i++) {
            sum += (double) i * histogram[i];
        }

        double sumBackground = 0;
        int weightBackground = 0;
        double maximumVariance = 0;
        int threshold = 128;

        for (int t = 0; t < 256; t++) {

            weightBackground += histogram[t];

            if (weightBackground == 0) {
                continue;
            }

            int weightForeground = totalPixels - weightBackground;

            if (weightForeground == 0) {
                break;
            }

            sumBackground += (double) t * histogram[t];

            double meanBackground =
                    sumBackground / weightBackground;

            double meanForeground =
                    (sum - sumBackground) / weightForeground;

            double betweenClassVariance =
                    (double) weightBackground * weightForeground
                            * Math.pow(
                            meanBackground - meanForeground,
                            2
                    );

            if (betweenClassVariance > maximumVariance) {
                maximumVariance = betweenClassVariance;
                threshold = t;
            }
        }

        System.out.println("Otsu threshold = " + threshold);

        BufferedImage binarized = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_BINARY
        );

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int pixel = grayscale.getRaster().getSample(x, y, 0);
                int newPixel = pixel > threshold ? 1 : 0;

                binarized.getRaster().setSample(x, y, 0, newPixel);
            }
        }

        return binarized;
    }
}