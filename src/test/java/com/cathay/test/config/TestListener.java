package com.cathay.test.config;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestListener implements ITestListener {
    private static final Logger logger = LogManager.getLogger(TestListener.class);
    private static final String REPORT_DIR = "test-reports";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    @Override
    public void onTestStart(ITestResult result) {
        logger.info("開始執行測試: {}", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("測試通過: {}", result.getName());
        writeToReport(result, "通過");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logger.error("測試失敗: {}", result.getName());
        Throwable throwable = result.getThrowable();
        
        if (throwable instanceof NoSuchElementException) {
            String errorMessage = throwable.getMessage();
            logger.error("元素未找到錯誤: {}", errorMessage);
            
            // 解析錯誤信息以獲取更多細節
            if (errorMessage.contains("no such element")) {
                String elementInfo = extractElementInfo(errorMessage);
                logger.error("未找到的元素信息: {}", elementInfo);
            }
        } else if (throwable instanceof TimeoutException) {
            logger.error("等待元素超時: {}", throwable.getMessage());
        }
        
        writeToReport(result, "失敗");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("測試跳過: {}", result.getName());
        writeToReport(result, "跳過");
    }

    private String extractElementInfo(String errorMessage) {
        // 嘗試從錯誤信息中提取元素定位信息
        if (errorMessage.contains("xpath")) {
            return "XPath: " + errorMessage.substring(errorMessage.indexOf("xpath"));
        } else if (errorMessage.contains("id")) {
            return "ID: " + errorMessage.substring(errorMessage.indexOf("id"));
        } else if (errorMessage.contains("class")) {
            return "Class: " + errorMessage.substring(errorMessage.indexOf("class"));
        }
        return errorMessage;
    }

    private void writeToReport(ITestResult result, String status) {
        try {
            File dir = new File(REPORT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = REPORT_DIR + "/test_report_" + DATE_FORMAT.format(new Date()) + ".txt";
            FileWriter writer = new FileWriter(fileName, true);
            
            writer.write("測試時間: " + new Date() + "\n");
            writer.write("測試名稱: " + result.getName() + "\n");
            writer.write("測試狀態: " + status + "\n");
            
            if (result.getThrowable() != null) {
                writer.write("錯誤信息: " + result.getThrowable().getMessage() + "\n");
                
                // 添加更詳細的錯誤信息
                if (result.getThrowable() instanceof NoSuchElementException) {
                    writer.write("錯誤類型: 元素未找到\n");
                    String elementInfo = extractElementInfo(result.getThrowable().getMessage());
                    writer.write("未找到的元素信息: " + elementInfo + "\n");
                } else if (result.getThrowable() instanceof TimeoutException) {
                    writer.write("錯誤類型: 等待超時\n");
                }
                
                writer.write("堆疊追蹤: \n");
                for (StackTraceElement element : result.getThrowable().getStackTrace()) {
                    writer.write("\t" + element.toString() + "\n");
                }
            }
            
            writer.write("----------------------------------------\n");
            writer.close();
            
        } catch (IOException e) {
            logger.error("無法寫入測試報告: {}", e.getMessage(), e);
        }
    }
} 