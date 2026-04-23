package com.hjz.jobaiagent.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class JobAppTest {

    @Resource
    private JobApp jobApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是嘉章";
        String answer = jobApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想找Agent开发工作";
        answer = jobApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我要找什么工作？刚跟你说过，帮我回忆一下";
        answer = jobApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = java.util.UUID.randomUUID().toString();
        String message = "你好，我是嘉章。我想找Agent开发的工作岗位，但我不知道怎么做";
        JobApp.JobReport jobReport = jobApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(jobReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = java.util.UUID.randomUUID().toString();
        String message = "软件工程校招的岗位方向众多，应届生该怎么选择适合自己的岗位？";
        String answer = jobApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("推荐几个2026年近期的招聘会");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近在北京找工作，看看（https://www.bjbys.net.cn/）上有哪些就业岗位");

        // 测试资源下载：图片下载
        testMessage("直接下载一张简历模板图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成就业数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的就业档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘就业计划’PDF，包含招聘公司、招聘岗位和岗位要求");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = jobApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
