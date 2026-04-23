package com.hjz.jobaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 终端操作工具（修复中文乱码）
 */
public class TerminalOperationTool {

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder;
            // Windows 使用 cmd.exe 并指定 GBK 编码
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                builder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                // Linux/Mac 使用 sh
                builder = new ProcessBuilder("/bin/sh", "-c", command);
            }

            Process process = builder.start();

            // Windows 命令行输出是 GBK，必须指定编码解决乱码
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"));
                 // 同时读取错误流，防止命令卡死
                 BufferedReader errorReader = new BufferedReader(
                         new InputStreamReader(process.getErrorStream(), "GBK"))) {

                // 读取正常输出
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }

                // 读取错误输出
                String errorLine;
                boolean hasError = false;
                while ((errorLine = errorReader.readLine()) != null) {
                    output.append("[错误] ").append(errorLine).append(System.lineSeparator());
                    hasError = true;
                }
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("\n命令执行失败，退出码：").append(exitCode);
            }

        } catch (IOException | InterruptedException e) {
            output.append("命令执行异常：").append(e.getMessage());
        }
        return output.toString().trim();
    }
}