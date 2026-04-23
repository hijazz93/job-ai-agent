package com.hjz.jobaiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "Gitee主页.pdf";
        String content = "作者Gitee主页 https://gitee.com/hao-jiazhang";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}