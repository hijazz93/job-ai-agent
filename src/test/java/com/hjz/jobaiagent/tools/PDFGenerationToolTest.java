package com.hjz.jobaiagent.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "Gitee主页.pdf";
        String content = "作者Gitee主页 https://gitee.com/hao-jiazhang";
        List<String> imagePaths = List.of("D:/Hijazz/File/final/2.code/job-ai-agent/src/main/resources/images/author.jpg");
        String result = tool.generatePDF(fileName, content, imagePaths);
        assertNotNull(result);
    }


}