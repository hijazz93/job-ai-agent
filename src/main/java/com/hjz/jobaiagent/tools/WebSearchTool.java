package com.hjz.jobaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "使用百度搜索引擎查找信息")
    public String searchWeb(
            @ToolParam(description = "搜索关键词") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);

            // 检查 API 是否返回错误
            if (jsonObject.containsKey("error")) {
                return "百度搜索失败，API 返回错误：" + jsonObject.getStr("error");
            }

            // 提取 organic_results 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "未搜索到相关结果（关键词：" + query + "）";
            }

            int count = Math.min(organicResults.size(), 5);
            List<Object> objects = organicResults.subList(0, count);
            String result = objects.stream().map(Object::toString)
                    .collect(Collectors.joining("\n"));
            return result;
        } catch (Exception e) {
            return "百度搜索出错: " + e.getMessage();
        }
    }
}