package com.hjz.jobaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JobManusTest {

    @Resource
    private JobManus jobManus;

    @Test
    public void run() {
        String userPrompt = """
                我居住在山西太原小店区，请帮我找到2026年,且离我5公里以内的招聘会地点.
                放一些配图，制定一份详细的招聘会计划，
                并用中文以 PDF 格式输出""";
        String answer = jobManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}