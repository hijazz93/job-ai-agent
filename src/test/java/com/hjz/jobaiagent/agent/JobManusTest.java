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
                我居住在山西太原小店区，请帮我完成以下任务：
                          1. 搜索2026年、距离我5公里以内的所有招聘会，列出：时间、地点、距离、交通方式、主办方。
                          2. 必须调用 imageSearch 工具，搜索以下真实高清配图（各至少1张）：
                             - 太原昌盛万达广场招聘会现场照片
                             - 山西应用科技学院黄陵校区招聘会现场照片
                             - 太原人力资源服务产业园招聘会现场照片
                             - 招聘会通用场景（求职、企业展位）
                          3. 制定一份详细的招聘会计划，包含：
                             - 每场招聘会详情
                             - 参会准备清单（简历、着装、物品）
                             - 交通路线与时间规划
                             - 现场沟通技巧
                             - 会后跟进策略
                          4. 将所有信息+配图**整合进PDF**，图片必须真实嵌入，不允许用文字描述代替图片。
                          5. 输出PDF文件，文件名：2026太原小店区5公里内招聘会计划.pdf""";
        String answer = jobManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}