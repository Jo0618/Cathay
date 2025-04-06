package com.cathay.test.config;

import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ElementConfig {
    private static final Logger logger = LoggerFactory.getLogger(ElementConfig.class);
    private static final String CONFIG_FILE = "elements.yaml";
    private static Map<String, Object> config;
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = ElementConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            config = yaml.load(inputStream);
            logger.info("成功載入元素配置文件");
        } catch (Exception e) {
            logger.error("載入元素配置文件失敗: {}", e.getMessage());
            throw new RuntimeException("無法載入元素配置文件", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getElementConfig(String elementKey) {
        try {
            Map<String, Object> mortgageCalculator = (Map<String, Object>) config.get("mortgage_calculator");
            return (Map<String, Object>) mortgageCalculator.get(elementKey);
        } catch (Exception e) {
            logger.error("獲取元素配置失敗: {}", e.getMessage());
            throw new RuntimeException("無法獲取元素配置: " + elementKey, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static String getElementName(String elementKey) {
        Map<String, Object> elementConfig = getElementConfig(elementKey);
        return (String) elementConfig.get("name");
    }
    
    @SuppressWarnings("unchecked")
    public static List<String> getElementSelectors(String elementKey) {
        Map<String, Object> elementConfig = getElementConfig(elementKey);
        return (List<String>) elementConfig.get("selectors");
    }
    
    public static String getIframeTag() {
        Map<String, Object> iframeConfig = getElementConfig("iframe");
        return (String) iframeConfig.get("tag");
    }
    
    public static String getIframeSelector() {
        Map<String, Object> iframeConfig = getElementConfig("iframe");
        return (String) iframeConfig.get("selector");
    }
} 