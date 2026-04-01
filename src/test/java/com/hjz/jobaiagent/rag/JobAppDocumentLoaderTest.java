package com.hjz.jobaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JobAppDocumentLoaderTest {


    @Resource
    private JobAppDocumentLoader jobAppDocumentLoader;

    @Test
    void loadMarkdowns() {
        jobAppDocumentLoader.loadMarkdowns();
    }
}