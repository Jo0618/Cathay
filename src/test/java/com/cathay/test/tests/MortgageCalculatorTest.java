package com.cathay.test.tests;

import com.cathay.test.config.AppiumConfig;
import com.cathay.test.config.ElementConfig;
import com.cathay.test.config.TestDataConfig;
import com.cathay.test.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.openqa.selenium.support.ui.Select;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.lang.Runtime;
import java.lang.Process;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.ArrayList;



public class MortgageCalculatorTest extends AppiumConfig {
    private static final Logger logger = LoggerFactory.getLogger(MortgageCalculatorTest.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_INTERVAL = 2000; // 2秒
    private static final int PAGE_LOAD_TIMEOUT = 30; // 30秒
    private static final int ELEMENT_WAIT_TIMEOUT = 15; // 15秒
    private static final int SETUP_RETRY_COUNT = 3;
    private static final int SETUP_RETRY_INTERVAL = 5000; // 5秒
    private static final int DRIVER_INIT_WAIT = 10000; // 10秒

    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp(Method method, ITestResult result) {
        super.setUp(method, result); // 父類 AppiumConfig 處理初始化與 retry

        try {
            logger.info("驗證 driver session 與重新導頁...");
            if (driver == null || driver.getSessionId() == null) {
                throw new RuntimeException("Driver 尚未初始化或 sessionId 為 null");
            }

            // 驗證進入房貸試算頁面
            WaitUtils.openMortgagePage(driver, APP_URL);

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));

            logger.info("MortgageCalculatorTest 驗證完成");
        } catch (Exception e) {
            logger.error("MortgageCalculatorTest 初始化驗證失敗: {}", e.getMessage(), e);
            throw new RuntimeException("MortgageCalculatorTest 驗證失敗", e);
        }
    }

    @AfterMethod(alwaysRun = true)
    @Override
    protected void tearDown() {
        try {
            if (driver != null) {
                String sessionId = driver.getSessionId().toString();
                logger.info("正在清理 driver session: {}", sessionId);
                
                super.tearDown();
                logger.info("MortgageCalculatorTest 清理完成");
            }
        } catch (Exception e) {
            logger.error("MortgageCalculatorTest 清理失敗: {}", e.getMessage(), e);
        } finally {
            driver = null;
        }
    }

    @DataProvider(name = "mortgageData")
    public Object[][] provideMortgageData() {
        return new Object[][] {
            {"10000000", "20", "2.5", "53000"},
            {"20000000", "30", "2.8", "82000"},
            {"15000000", "25", "2.6", "64000"}
        };
    }

    private static final String ALL_FIELD_DESC = "UI驗證 - 房貸試算欄位檢查";
    @Test(description = ALL_FIELD_DESC, groups = {"UI", "ColumnCheck"})
    public void testMortgageAllFieldVisibility() {
        Reporter.log(ALL_FIELD_DESC, true);
        try {
            logger.info("開始執行：testMortgageAllFieldVisibility");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            // WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // 1. 標題與說明文字
            WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ElementConfig.getElementSelectors("page_title").get(0))));
            WebElement description = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ElementConfig.getElementSelectors("page_description").get(0))));
            WebElement mortgageLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ElementConfig.getElementSelectors("mortgage_link").get(0))));

            Assert.assertTrue(title.isDisplayed(), "標題應顯示");
            Assert.assertTrue(description.isDisplayed(), "描述文字應顯示");
            Assert.assertEquals(mortgageLink.getAttribute("href"), "https://www.cathaybk.com.tw/cathaybk/personal/loan/product/mortgage/", "購屋貸款方案連結錯誤");

            // 2. 房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            Assert.assertNotNull(citySelect, "縣市下拉選單應該出現");

            // 3. 每月可負擔房貸金額
            String amountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(amountSelector)));
            // Assert.assertTrue(amountInput.isDisplayed(), "應顯示房貸金額欄位");
            Assert.assertNotNull(amountInput, "縣市下拉選單應該出現");

            // 4. 貸款年限（選項）
            String loanTermSelector = ElementConfig.getElementSelectors("loan_term").get(0);
            WebElement loanTermRadio = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanTermSelector)));
            Assert.assertTrue(loanTermRadio.isDisplayed(), "應顯示貸款年限選項");

            // 5. 貸款利率欄位
            String interestSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(interestSelector)));
            Assert.assertTrue(interestInput.isDisplayed(), "應顯示貸款利率欄位");

            // 6. 執行按鈕
            String calculateSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calculateBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(calculateSelector)));
            Assert.assertTrue(calculateBtn.isDisplayed(), "應顯示開始試算按鈕");

            String resetSelector = ElementConfig.getElementSelectors("calculate_reset_button").get(0);
            WebElement resetBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resetSelector)));
            Assert.assertTrue(resetBtn.isDisplayed(), "應顯示重新計算按鈕");

            // 7. 其他試算連結
            WebElement monthlyCalc = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ElementConfig.getElementSelectors("monthly_calc_link").get(0))));
            WebElement ltvCalc = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ElementConfig.getElementSelectors("ltv_calc_link").get(0))));

            Assert.assertTrue(monthlyCalc.isDisplayed(), "每月還款金額試算應顯示");
            Assert.assertTrue(ltvCalc.isDisplayed(), "貸款成數試算應顯示");

            System.out.println("所有欄位與文字、按鈕均正確顯示");

        } catch (Exception e) {
            logger.error("欄位檢查測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }


    private static final String LOCATION_SELECTION_DESC = "UI驗證 - 房屋座落區域欄位檢查";
    @Test(description = LOCATION_SELECTION_DESC, groups = {"UI", "ColumnCheck"})
    public void testLocationSelection() {
        Reporter.log(LOCATION_SELECTION_DESC, true);
        try {
            logger.info("開始執行：testLocationSelection");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            List<String> citySelectors = ElementConfig.getElementSelectors("city_select");
            String citySelector = citySelectors.get(0);
            By citySelectLocator = By.cssSelector(citySelector);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(citySelectLocator));

            Assert.assertNotNull(citySelect, "縣市下拉選單應該出現");

            List<WebElement> cityOptions = citySelect.findElements(By.tagName("option"));
            List<String> optionTexts = cityOptions.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());

            List<String> expectedCities = new ArrayList<>(TestDataConfig.CITY_DISTRICT_MAP.keySet());

            for (String city : expectedCities) {
                Assert.assertTrue(optionTexts.contains(city), "下拉選單中應包含城市: " + city);

                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = '" + city + "'; arguments[0].dispatchEvent(new Event('change'))",
                    citySelect
                );

                String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
                WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
                Thread.sleep(100);

                List<WebElement> areaOptions = areaSelect.findElements(By.tagName("option"));
                List<String> areaNames = new ArrayList<>();
                for (WebElement areaOption : areaOptions) {
                    String area = areaOption.getText().trim();
                    if (!area.isEmpty() && !area.equals("請選擇區域")) {
                        areaNames.add(area);
                    }
                }

                List<String> expectedDistricts = TestDataConfig.CITY_DISTRICT_MAP.get(city);
                if (expectedDistricts != null) {
                    for (String district : expectedDistricts) {
                        Assert.assertTrue(areaNames.contains(district),
                            "縣市 " + city + " 的行政區應包含: " + district);
                    }
                } else {
                    System.out.println("沒有設定縣市 " + city + " 對應的行政區，略過檢查");
                }

                System.out.println("驗證縣市「" + city + "」與其行政區成功！");
            }

            System.out.println("縣市下拉選單內容驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }

    private static final String MORTGAGE_CALCULATION_DESC = "房貸試算功能測試 - 輸入完整資訊，檢查試算結果";
    @Test(description = MORTGAGE_CALCULATION_DESC, groups = {"Function", "Calculator"})
    public void testMortgageCalculation() {
        Reporter.log(MORTGAGE_CALCULATION_DESC, true);
        try {
            logger.info("開始執行：testMortgageCalculation");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );

            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            // ((JavascriptExecutor) driver).executeScript("arguments[0].click();", calcBtn);
            calcBtn.click();

            Thread.sleep(300); // 等待結果動畫或載入

            // 6. 驗證結果是否正確顯示
            String resultLoanSelector = ElementConfig.getElementSelectors("result_loan").get(0);
            String resultPriceSelector = ElementConfig.getElementSelectors("result_price").get(0);
            WebElement loanAmountResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resultLoanSelector)));
            WebElement housePriceResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resultPriceSelector)));

            Assert.assertFalse(loanAmountResult.getText().isEmpty(), "應顯示貸款總額");
            Assert.assertFalse(housePriceResult.getText().isEmpty(), "應顯示購屋總價");
            logger.info("顯示貸款總額與購屋總價成功");

            // 7. 驗證欄位輸入值是否存在（頁面呈現）
            Select cityDropdown = new Select(citySelect);
            Select areaDropdown = new Select(areaSelect);
           Assert.assertEquals(cityDropdown.getFirstSelectedOption().getText(), "台北市", "房屋座落縣市應為台北市");
            Assert.assertEquals(areaDropdown.getFirstSelectedOption().getText(), "大安區", "房屋座落區域應為大安區");
            String amountValue = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", amountInput);
            Assert.assertEquals(amountValue, "3", "每月可負擔房貸金額應為3萬");
            String interestValue = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", interestInput);
            Assert.assertEquals(interestValue, "2.3", "貸款利率應為 2.3");

            // 8. 驗證「馬上預約諮詢」按鈕存在
            String consultButtonSelector = ElementConfig.getElementSelectors("consult_button").get(0);
            WebElement consultBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(consultButtonSelector)));
            Assert.assertTrue(consultBtn.isDisplayed(), "應顯示馬上預約諮詢按鈕");

            logger.info("試算功能驗證成功");

        } catch (Exception e) {
            logger.error("試算測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static final String MORTGAGE_CALCULATION_RESET_DESC = "房貸試算功能測試 - 點選重新試算";
    @Test(description = MORTGAGE_CALCULATION_RESET_DESC, groups = {"Function", "Calculator"})
    public void testMortgageCalculationReset() {
        Reporter.log(MORTGAGE_CALCULATION_RESET_DESC, true);
        try {
            logger.info("testMortgageCalculationReset");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );

            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300); // 等待結果動畫或載入

            // 6. 驗證結果是否正確顯示
            String resultLoanSelector = ElementConfig.getElementSelectors("result_loan").get(0);
            String resultPriceSelector = ElementConfig.getElementSelectors("result_price").get(0);
            WebElement loanAmountResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resultLoanSelector)));
            WebElement housePriceResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resultPriceSelector)));

            Assert.assertFalse(loanAmountResult.getText().isEmpty(), "應顯示貸款總額");
            Assert.assertFalse(housePriceResult.getText().isEmpty(), "應顯示購屋總價");
            logger.info("顯示貸款總額與購屋總價成功");

            // 8. 驗證「馬上預約諮詢」按鈕存在
            String consultButtonSelector = ElementConfig.getElementSelectors("consult_button").get(0);
            WebElement consultBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(consultButtonSelector)));
            Assert.assertTrue(consultBtn.isDisplayed(), "應顯示馬上預約諮詢按鈕");

            // 9. 點選「重新試算」
            String calculateResetButtonSelector = ElementConfig.getElementSelectors("calculate_reset_button").get(0);
            WebElement calcResetBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateResetButtonSelector)));
            // calcResetBtn.click();
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", calcResetBtn);

            Thread.sleep(300);

            // 驗證縣市下拉為預設值
            String selectedCityValue = citySelect.getAttribute("value");
            String selectedCityText = new Select(citySelect).getFirstSelectedOption().getText();
            Assert.assertTrue(selectedCityValue == null || selectedCityValue.isEmpty() || selectedCityText.contains("請選擇"),
                "重新試算後，縣市應該未選擇");

            // 驗證行政區下拉為預設值
            String selectedAreaValue = areaSelect.getAttribute("value");
            String selectedAreaText = new Select(areaSelect).getFirstSelectedOption().getText();
            Assert.assertTrue(selectedAreaValue == null || selectedAreaValue.isEmpty() || selectedAreaText.contains("請選擇"),
                "重新試算後，行政區應該未選擇");

            // 驗證房貸金額 input 是否為空
            Assert.assertEquals(amountInput.getAttribute("value"), "", "每月可負擔房貸金額應為空");

            // 驗證貸款利率 input 是否為空
            Assert.assertEquals(interestInput.getAttribute("value"), "", "貸款利率應為空");

            //貸款總額與購屋總價,皆須消失
            Assert.assertTrue(loanAmountResult.getText().isEmpty() || !loanAmountResult.isDisplayed(),"重新試算後應不顯示貸款總額");
            Assert.assertTrue(housePriceResult.getText().isEmpty() || !housePriceResult.isDisplayed(),"重新試算後應不顯示購屋總價");

            logger.info("重新試算功能驗證成功");
        } catch (Exception e) {
            logger.error("重新試算測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private static final String MORTGAGE_CALCULATION_TWICE_DESC = "房貸試算功能測試 - 重新試算，檢查試算結果";
    @Test(description = MORTGAGE_CALCULATION_TWICE_DESC, groups = {"Function", "Calculator"})
    public void testMortgageCalculationTwice() {
        Reporter.log(MORTGAGE_CALCULATION_TWICE_DESC, true);
        try {
            logger.info("testMortgageCalculationTwice");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );

            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300); // 等待結果動畫或載入

            // 6. 驗證結果是否正確顯示
            String resultLoanSelector = ElementConfig.getElementSelectors("result_loan").get(0);
            String resultPriceSelector = ElementConfig.getElementSelectors("result_price").get(0);
            WebElement loanAmountResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resultLoanSelector)));
            WebElement housePriceResult = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(resultPriceSelector)));

            Assert.assertFalse(loanAmountResult.getText().isEmpty(), "應顯示貸款總額");
            Assert.assertFalse(housePriceResult.getText().isEmpty(), "應顯示購屋總價");
            logger.info("顯示貸款總額與購屋總價成功");

            // 9. 點選「重新試算」
            String calculateResetButtonSelector = ElementConfig.getElementSelectors("calculate_reset_button").get(0);
            WebElement calcResetBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateResetButtonSelector)));
            // calcResetBtn.click();
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", calcResetBtn);

            Thread.sleep(300);

            //貸款總額與購屋總價,皆須消失
            Assert.assertTrue(loanAmountResult.getText().isEmpty() || !loanAmountResult.isDisplayed(),"重新試算後應不顯示貸款總額");
            Assert.assertTrue(housePriceResult.getText().isEmpty() || !housePriceResult.isDisplayed(),"重新試算後應不顯示購屋總價");

            // 1. 選擇房屋座落區域
           ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台南市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );

           ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '北區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("5");

            // 3. 點選貸款年限（30年）
            String loanTerm30ySelector = ElementConfig.getElementSelectors("loan_term").get(1);
            clickLoanTermLabel(loanTerm30ySelector);

            // 4. 輸入貸款利率
            interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.0");

            calcBtn.click();

            Assert.assertFalse(loanAmountResult.getText().isEmpty(), "應顯示貸款總額");
            Assert.assertFalse(housePriceResult.getText().isEmpty(), "應顯示購屋總價");
            logger.info("顯示貸款總額與購屋總價成功");

            // 7. 驗證欄位輸入值是否存在（頁面呈現）
            Select cityDropdown = new Select(citySelect);
            Select areaDropdown = new Select(areaSelect);
            Assert.assertEquals(cityDropdown.getFirstSelectedOption().getText(), "台南市", "房屋座落縣市應為台南市");
            Assert.assertEquals(areaDropdown.getFirstSelectedOption().getText(), "北區", "房屋座落區域應為北區");
            String amountValue = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", amountInput);
            Assert.assertEquals(amountValue, "5", "每月可負擔房貸金額應為3萬");
            String interestValue = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", interestInput);
            Assert.assertEquals(interestValue, "2.0", "貸款利率應為 2.0");

            // 8. 驗證「馬上預約諮詢」按鈕存在
            String consultButtonSelector = ElementConfig.getElementSelectors("consult_button").get(0);
            WebElement consultBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(consultButtonSelector)));
            Assert.assertTrue(consultBtn.isDisplayed(), "應顯示馬上預約諮詢按鈕");


            logger.info("重新試算功能驗證成功");
        } catch (Exception e) {
            logger.error("重新試算測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private static final String ERROR_LOCATION_SELECTION_DESC = "房貸試算功能測試 - 房屋座落區域皆未選擇";
    @Test(description = ERROR_LOCATION_SELECTION_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingLocationSelectionAll() {
        Reporter.log(ERROR_LOCATION_SELECTION_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingLocationSelectionAll");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 縣市
            String cityErrorXPath = ElementConfig.getElementSelectors("city_select").get(1);
            WebElement cityErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(cityErrorXPath)));
            Assert.assertTrue(cityErrorMsg.isDisplayed(), "未顯示「請選擇縣市」錯誤訊息");

            // 驗證錯誤訊息 - 行政區
            String districtErrorXPath = ElementConfig.getElementSelectors("district_select").get(1);
            WebElement districtErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(districtErrorXPath)));
            Assert.assertTrue(districtErrorMsg.isDisplayed(), "未顯示「請選擇行政區」錯誤訊息");

            System.out.println("房屋座落區域ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }

    private static final String ERROR_LOCATION_SELECTION_ONE_DESC = "房貸試算功能測試 - 房屋座落區域擇一未選擇";
    @Test(description = ERROR_LOCATION_SELECTION_ONE_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingLocationSelectionOne() {
        Reporter.log(ERROR_LOCATION_SELECTION_ONE_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingLocationSelectionOne");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 行政區
            String districtErrorXPath = ElementConfig.getElementSelectors("district_select").get(1);
            WebElement districtErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(districtErrorXPath)));
            Assert.assertTrue(districtErrorMsg.isDisplayed(), "未顯示「請選擇行政區」錯誤訊息");

            System.out.println("房屋座落區域ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }


    private static final String ERROR_LOAN_AMOUNT_DESC = "房貸試算功能測試 - 房貸金額未填寫";
    @Test(description = ERROR_LOAN_AMOUNT_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingLoadAmount() {
        Reporter.log(ERROR_LOAN_AMOUNT_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingLoadAmount");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );
            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            // String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            // WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            // amountInput.clear();
            // amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 每月可負擔房貸金額
            String loanAmountErrorXPath = ElementConfig.getElementSelectors("loan_amount").get(1);
            WebElement loanAmountErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(loanAmountErrorXPath)));
            Assert.assertTrue(loanAmountErrorMsg.isDisplayed(), "未顯示「請輸入整數貸款金額」錯誤訊息");

            System.out.println("每月可負擔房貸金額ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }

    private static final String ERROR_LOAN_AMOUNT_TYPE_DESC = "房貸試算功能測試 - 房貸金額輸入非數字";
    @Test(description = ERROR_LOAN_AMOUNT_TYPE_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingLoadAmountNonInt() {
        Reporter.log(ERROR_LOAN_AMOUNT_TYPE_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingLoadAmountNonInt");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );
            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3.5");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 每月可負擔房貸金額
            String loanAmountErrorXPath = ElementConfig.getElementSelectors("loan_amount").get(1);
            WebElement loanAmountErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(loanAmountErrorXPath)));
            Assert.assertTrue(loanAmountErrorMsg.isDisplayed(), "未顯示「請輸入整數貸款金額」錯誤訊息");

            System.out.println("每月可負擔房貸金額-非整數ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }


    private static final String ERROR_LOAN_TERM_DESC = "房貸試算功能測試 - 貸款年限其他選項未輸入";
    @Test(description = ERROR_LOAN_TERM_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingLoadTerm() {
        Reporter.log(ERROR_LOAN_TERM_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingLoadTerm");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );
            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 3. 點選貸款年限（其他）
            String loanTermOtherSelector = ElementConfig.getElementSelectors("loan_term").get(2);
            clickLoanTermLabel(loanTermOtherSelector);
            // String loanTermOtherInputSelector = ElementConfig.getElementSelectors("loan_term").get(3);
            // WebElement loanTermOtherInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(loanTermOtherInputSelector)));
            // loanTermOtherInput.clear();
            // loanTermOtherInput.sendKeys("10.5");


            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 貸款年限（其他）未填
            String loanTermErrorXPath = ElementConfig.getElementSelectors("loan_term").get(4);
            WebElement loanTermErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(loanTermErrorXPath)));
            Assert.assertTrue(loanTermErrorMsg.isDisplayed(), "未顯示「請輸入貸款年限」錯誤訊息");

            System.out.println("貸款年限-其他選項未輸入ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }

    private static final String ERROR_LOAN_TERM_TYPE_DESC = "房貸試算功能測試 - 貸款年限其他選項輸入非整數";
    @Test(description = ERROR_LOAN_TERM_TYPE_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingLoadTermNonInt() {
        Reporter.log(ERROR_LOAN_TERM_TYPE_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingLoadTermNonInt");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );
            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 3. 點選貸款年限（其他）
            String loanTermOtherSelector = ElementConfig.getElementSelectors("loan_term").get(2);
            clickLoanTermLabel(loanTermOtherSelector);
            String loanTermOtherInputSelector = ElementConfig.getElementSelectors("loan_term").get(3);
            WebElement loanTermOtherInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(loanTermOtherInputSelector)));
            loanTermOtherInput.clear();
            loanTermOtherInput.sendKeys("10.5");


            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 貸款年限（其他）輸入非整數
            String loanTermErrorXPath = ElementConfig.getElementSelectors("loan_term").get(4);
            WebElement loanTermErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(loanTermErrorXPath)));
            Assert.assertTrue(loanTermErrorMsg.isDisplayed(), "未顯示「請輸入貸款年限」錯誤訊息");

            System.out.println("貸款年限-其他選項輸入非整數ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }

    private static final String ERROR_INTEREST_RATE_DESC = "房貸試算功能測試 - 貸款利率未輸入";
    @Test(description = ERROR_INTEREST_RATE_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingInterestRate() {
        Reporter.log(ERROR_INTEREST_RATE_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingInterestRate");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );
            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            // String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            // WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            // interestInput.clear();
            // interestInput.sendKeys("2.3");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 貸款利率未輸入
            String interestRateErrorXPath = ElementConfig.getElementSelectors("interest_rate").get(1);
            WebElement interestRateErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(interestRateErrorXPath)));
            Assert.assertTrue(interestRateErrorMsg.isDisplayed(), "未顯示「請輸入數字」錯誤訊息");

            System.out.println("貸款利率未輸入ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }

    private static final String ERROR_INTEREST_RATE_TYPE_DESC = "房貸試算功能測試 - 貸款利率輸入非數字";
    @Test(description = ERROR_INTEREST_RATE_TYPE_DESC, groups = {"Function", "ErrorHandling"})
    public void testErrorHandlingInterestRateNonNumber() {
        Reporter.log(ERROR_INTEREST_RATE_TYPE_DESC, true);
        try {
            logger.info("開始執行：testErrorHandlingInterestRateNonNumber");

            // driver.get(APP_URL);
            WaitUtils.openMortgagePage(driver, APP_URL);
            logger.info("已開啟房貸試算頁面: {}", APP_URL);

            // WaitUtils.waitForMortgagePageLoad(driver);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
            // wait.until(webDriver -> {
            //     try {
            //         webDriver.getCurrentUrl();
            //         return true;
            //     } catch (Exception e) {
            //         return false;
            //     }
            // });

            // 1. 選擇房屋座落區域
            String citySelector = ElementConfig.getElementSelectors("city_select").get(0);
            WebElement citySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(citySelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '台北市'; arguments[0].dispatchEvent(new Event('change'))",
                citySelect
            );
            String areaSelector = ElementConfig.getElementSelectors("district_select").get(0);
            WebElement areaSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(areaSelector)));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '大安區'; arguments[0].dispatchEvent(new Event('change'))",
                areaSelect
            );

            // 2. 輸入每月可負擔房貸金額
            String loanAmountSelector = ElementConfig.getElementSelectors("loan_amount").get(0);
            WebElement amountInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(loanAmountSelector)));
            amountInput.clear();
            amountInput.sendKeys("3");

            // 4. 輸入貸款利率
            String interestRateSelector = ElementConfig.getElementSelectors("interest_rate").get(0);
            WebElement interestInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(interestRateSelector)));
            interestInput.clear();
            interestInput.sendKeys("www");


            // 5. 點擊「開始試算」
            String calculateButtonSelector = ElementConfig.getElementSelectors("calculate_button").get(0);
            WebElement calcBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(calculateButtonSelector)));
            calcBtn.click();

            Thread.sleep(300);

            // 驗證錯誤訊息 - 貸款利率未輸入
            String interestRateErrorXPath = ElementConfig.getElementSelectors("interest_rate").get(1);
            WebElement interestRateErrorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(interestRateErrorXPath)));
            Assert.assertTrue(interestRateErrorMsg.isDisplayed(), "未顯示「請輸入數字」錯誤訊息");

            System.out.println("貸款利率輸入非數字ErrorHandling驗證通過！");

        } catch (Exception e) {
            logger.error("測試失敗: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {}
        }
    }



    private WebElement findElementWithRetry(List<String> selectors, String elementName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
        Exception lastException = null;
        
        for (int i = 1; i <= MAX_RETRIES; i++) {
            try {
                logger.info("開始查找元素: {}, 嘗試第 {} 次", elementName, i);
                
                for (String selector : selectors) {
                    try {
                        logger.info("嘗試使用選擇器: {}", selector);
                        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
                        if (element != null && element.isDisplayed()) {
                            logger.info("成功找到元素: {} 使用選擇器: {}", elementName, selector);
                            return element;
                        }
                    } catch (Exception e) {
                        logger.warn("使用選擇器 {} 查找失敗: {}", selector, e.getMessage());
                    }
                }
                
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            } catch (Exception e) {
                lastException = e;
                logger.error("第 {} 次查找失敗: {}", i, e.getMessage());
            }
        }
        
        // 如果所有重試都失敗，記錄頁面源代碼
        logger.error("當前頁面源代碼：\n{}", driver.getPageSource());
        throw new NoSuchElementException(String.format("第 %d 次查找失敗: 未找到元素 %s", MAX_RETRIES, elementName));
    }

    public void clickLoanTermLabel(String loanTermFor) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ELEMENT_WAIT_TIMEOUT));
        // String selector = "label[for='" + loanTermFor + "']";
        String selector = loanTermFor;
        WebElement label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));

        if (label.isDisplayed()) {
            System.out.println("貸款年限「" + loanTermFor + "」選項有出現！");
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", label);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", label);
        } else {
            throw new RuntimeException("貸款年限 label 不可見: " + loanTermFor);
        }
    }
    
} 