package com.cathay.test.config;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Socket;
import java.time.Duration;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.cathay.test.utils.WaitUtils;

public class AppiumConfig {
    protected AndroidDriver driver;
    protected static final String APP_URL = "https://www.cathaybk.com.tw/cathaybk/personal/loan/calculator/mortgage-budget/";
    protected static final Logger logger = LogManager.getLogger(AppiumConfig.class);

    protected static final int SETUP_RETRY_COUNT = 3;
    protected static final int SETUP_RETRY_INTERVAL = 5000;
    protected static final int DRIVER_INIT_WAIT = 3000;
    protected static final int ELEMENT_WAIT_TIMEOUT = 15;
    protected static final int PAGE_LOAD_TIMEOUT = 60;

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method, ITestResult result) {
        int retryCount = 0;
        Exception lastException = null;
        System.out.println("*******MortgageCalculatorTest.setUp() 執行！");

        boolean SHOULD_QUIT_ON_SETUP_FAIL = false;

        if (driver != null) {
            try {
                WaitUtils.openMortgagePage(driver, APP_URL);
                logger.info("使用已存在的 driver，重新導入房貸試算頁面");
                return;
            } catch (Exception e) {
                logger.warn("重導頁面失敗，將重新初始化 driver: {}", e.getMessage());
                if (SHOULD_QUIT_ON_SETUP_FAIL) {
                    try {
                        driver.quit();
                    } catch (Exception ex) {
                        logger.warn("關閉舊 driver 失敗: {}", ex.getMessage());
                    }
                }
                driver = null;
            }
        }

        while (retryCount < SETUP_RETRY_COUNT) {
            try {
                logger.info("開始初始化 MortgageCalculatorTest (嘗試 {}/{})", retryCount + 1, SETUP_RETRY_COUNT);

                try (Socket socket = new Socket("127.0.0.1", 4723)) {
                    logger.info("Appium 服務器可用");
                } catch (Exception e) {
                    throw new RuntimeException("無法連接到 Appium 服務器，請確保 Appium 服務器正在運行", e);
                }

                try {
                    Process process = Runtime.getRuntime().exec("adb devices");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    boolean deviceFound = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("emulator") && line.contains("device")) {
                            deviceFound = true;
                            break;
                        }
                    }
                    if (!deviceFound) {
                        throw new RuntimeException("找不到可用的 Android 模擬器，請確保模擬器正在運行");
                    }
                    logger.info("Android 模擬器可用");
                } catch (Exception e) {
                    throw new RuntimeException("檢查 Android 模擬器狀態時發生錯誤", e);
                }

                DesiredCapabilities caps = new DesiredCapabilities();
                caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
                caps.setCapability(MobileCapabilityType.PLATFORM_VERSION, "16.0");
                caps.setCapability(MobileCapabilityType.DEVICE_NAME, "Pixel_3a_API_34_extension_level_7_x86_64");
                caps.setCapability(MobileCapabilityType.BROWSER_NAME, "chrome");
                caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");
                caps.setCapability(MobileCapabilityType.NO_RESET, true);
                caps.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 120);
                caps.setCapability("autoGrantPermissions", true);
                caps.setCapability("autoAcceptAlerts", true);

                Map<String, Object> chromeOptions = new HashMap<>();
                chromeOptions.put("w3c", true);

                // String userDataDir = System.getProperty("java.io.tmpdir") + "/temp-chrome-profile";
                // String userDataDir = "/data/data/com.android.chrome/app_chrome";

                chromeOptions.put("args", Arrays.asList(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--ignore-certificate-errors",
                    "--remote-allow-origins=*",
                    "--disable-features=SameSiteByDefaultCookies,CookiesWithoutSameSiteMustBeSecure",
                    "--enable-features=NetworkService,AllowThirdPartyCookies"
                ));

                caps.setCapability("goog:chromeOptions", chromeOptions);
                caps.setCapability("chromedriverExecutable", "/opt/homebrew/bin/chromedriver");

                URL appiumServerUrl = new URL("http://127.0.0.1:4723");
                driver = new AndroidDriver(appiumServerUrl, caps);

                // ⚡ 加入 CDP 指令來允許第三方 cookie
                try {
                    Map<String, Object> enableCookies = new HashMap<>();
                    enableCookies.put("enabled", false);  // false = 不阻擋
                    driver.executeCdpCommand("Network.setCookieBlockingEnabled", enableCookies);
                    logger.info("✅ 已透過 CDP 設定允許第三方 Cookie");
                } catch (Exception cdpEx) {
                    logger.warn("⚠️ 設定第三方 Cookie 支援時發生例外: {}", cdpEx.getMessage());
                }

                Thread.sleep(DRIVER_INIT_WAIT);

                if (driver == null) {
                    throw new RuntimeException("Driver 初始化失敗: driver 為 null");
                }

                String sessionId = driver.getSessionId().toString();
                logger.info("Driver session ID: {}", sessionId);

                WaitUtils.openMortgagePage(driver, APP_URL);

                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));

                logger.info("MortgageCalculatorTest 初始化完成");
                return;
            } catch (Exception e) {
                lastException = e;
                logger.error("MortgageCalculatorTest 初始化失敗 (嘗試 {}/{}): {}",
                        retryCount + 1, SETUP_RETRY_COUNT, e.getMessage(), e);

                if (driver != null) {
                    try {
                        logger.warn("Driver 初始化失敗，保留 session 供除錯參考 (未自動 quit)");
                        if (SHOULD_QUIT_ON_SETUP_FAIL) {
                            driver.quit();
                            logger.info("Driver 已主動關閉");
                        }
                    } catch (Exception quitEx) {
                        logger.error("清理 driver 時發生錯誤: {}", quitEx.getMessage());
                    }
                    driver = null;
                }

                retryCount++;
                if (retryCount < SETUP_RETRY_COUNT) {
                    logger.info("等待 {} 毫秒後重試...", SETUP_RETRY_INTERVAL);
                    try {
                        Thread.sleep(SETUP_RETRY_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重試等待被中斷", ie);
                    }
                }
            }
        }

        String errorMsg = String.format("MortgageCalculatorTest 初始化失敗，已重試 %d 次。最後一個錯誤: %s",
                SETUP_RETRY_COUNT, lastException != null ? lastException.getMessage() : "未知錯誤");
        logger.error(errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }

    @AfterMethod(alwaysRun = true)
    protected void tearDown() {
        try {
            if (driver != null) {
                logger.info("正在關閉 Driver...");
                driver.quit();
                logger.info("Driver 已成功關閉");
            }
        } catch (Exception e) {
            logger.error("關閉 Driver 時發生錯誤: {}", e.getMessage());
        } finally {
            driver = null;
        }
    }
}
