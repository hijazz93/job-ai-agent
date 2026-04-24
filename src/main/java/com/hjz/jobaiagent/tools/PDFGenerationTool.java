package com.hjz.jobaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.hjz.jobaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.util.List;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content and optional images", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content,
            @ToolParam(description = "List of image file paths to embed in the PDF (use absolute paths)") List<String> imagePaths) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = PdfFontFactory.createFont("C:/Windows/Fonts/msyh.ttc,0", "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                document.setFont(font);

                Paragraph textParagraph = new Paragraph(content);
                document.add(textParagraph);

                if (imagePaths != null && !imagePaths.isEmpty()) {
                    for (String imagePath : imagePaths) {
                        try {
                            ImageDataFactory.create(imagePath);
                            Image image = new Image(ImageDataFactory.create(imagePath));
                            image.setWidth(com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 80);
                            document.add(image);
                        } catch (Exception e) {
                            document.add(new Paragraph("无法加载图片: " + imagePath));
                        }
                    }
                }
            }
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }
}