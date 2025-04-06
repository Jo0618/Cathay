package com.cathay.test.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WaitUtils {

    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    public static void waitForPageLoad(WebDriver driver, String expectedPath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        wait.until(ExpectedConditions.urlContains(expectedPath));
    }


    // 通用 wait: 等待頁面含某個元素
    public static void waitForElementVisible(WebDriver driver, String cssSelector) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)));
    }

    // 打開房貸試算頁面
    public static void openMortgagePage(WebDriver driver, String url) {
        driver.get(url);
        waitForPageLoad(driver, "/mortgage-budget");
    }

}