import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import managers.ConfigManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

@SuppressWarnings("ALL")
public class MainClass extends JFrame {

    private final ConfigManager config = new ConfigManager();

    // --- COLORS ---
    private final Color COLOR_BG = new Color(32, 33, 36);
    private final Color COLOR_PANEL = new Color(45, 47, 49);
    private final Color COLOR_TEXT = new Color(230, 230, 230);
    private final Color COLOR_BTN = new Color(0, 150, 136);
    private final Color COLOR_BTN_TEXT = Color.WHITE;
    private final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);

    // GUI Components
    private JTextField userField = new JTextField(20);
    private JPasswordField passField = new JPasswordField(20);
    private JTextField idUserField = new JTextField("username", 10);
    private JTextField idPassField = new JTextField("password", 10);
    private JTextField idBtnField = new JTextField("login-button", 10);

    private JTextArea logArea = new JTextArea(10, 40);
    private JButton startButton = new JButton("Start Auto-Login");
    private JButton stopButton = new JButton("Stop");

    private JCheckBox loopCheckBox = new JCheckBox("Loop every 5 mins?");
    private JCheckBox checkNetBox = new JCheckBox("Smart Check (Ping first?)", true);
    private JCheckBox autoResetBox = new JCheckBox("Reset WiFi on Error?", true);
    private JCheckBox hiddenBox = new JCheckBox("Debug: Hide Browser?", false);

    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;

    public MainClass() {
        setTitle("EE WiFi Auto-Login Tool");
        setSize(500, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);

        // Load config (Safe order to avoid {} files)
        config.loadConfig();

        // --- TITLE ---
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(COLOR_BG);
        JLabel titleLabel = new JLabel("EE WiFi Auto-Login");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(COLOR_BTN);
        titleLabel.setBorder(new EmptyBorder(15, 0, 10, 0));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // --- INPUTS ---
        JPanel inputPanel = new JPanel(new GridLayout(10, 2, 8, 8));
        inputPanel.setBackground(COLOR_PANEL);
        inputPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        addCustomLabel(inputPanel, "EE Username/Email:");
        styleInput(userField);
        inputPanel.add(userField);

        addCustomLabel(inputPanel, "EE Password:");
        styleInput(passField);
        inputPanel.add(passField);

        addCustomLabel(inputPanel, "ID (User):");
        styleInput(idUserField);
        inputPanel.add(idUserField);

        addCustomLabel(inputPanel, "ID (Pass):");
        styleInput(idPassField);
        inputPanel.add(idPassField);

        addCustomLabel(inputPanel, "ID (Button):");
        styleInput(idBtnField);
        inputPanel.add(idBtnField);

        addCustomLabel(inputPanel, "Looping:");
        styleCheckBox(loopCheckBox);
        inputPanel.add(loopCheckBox);

        addCustomLabel(inputPanel, "Logic:");
        styleCheckBox(checkNetBox);
        inputPanel.add(checkNetBox);

        addCustomLabel(inputPanel, "Fixes:");
        styleCheckBox(autoResetBox);
        inputPanel.add(autoResetBox);

        addCustomLabel(inputPanel, "Debug:");
        styleCheckBox(hiddenBox);
        inputPanel.add(hiddenBox);

        // --- BUTTONS ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(COLOR_BG);
        styleButton(startButton, COLOR_BTN);
        styleButton(stopButton, new Color(180, 50, 50));
        stopButton.setEnabled(false);
        btnPanel.add(startButton);
        btnPanel.add(stopButton);

        // --- LOGS ---
        logArea.setEditable(false);
        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(new Color(0, 255, 100));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(null);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(inputPanel, BorderLayout.NORTH);
        centerContainer.add(btnPanel, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        loadDataFromConfig();

        startButton.addActionListener(e -> startProcess());
        stopButton.addActionListener(e -> stopProcess());
    }

    // --- HELPERS ---
    private void addCustomLabel(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setForeground(COLOR_TEXT);
        l.setFont(FONT_MAIN);
        panel.add(l);
    }
    private void styleInput(JTextField field) {
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));
        field.setFont(FONT_MAIN);
    }
    private void styleCheckBox(JCheckBox box) {
        box.setBackground(COLOR_PANEL);
        box.setForeground(COLOR_TEXT);
        box.setFont(FONT_MAIN);
        box.setFocusPainted(false);
    }
    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(COLOR_BTN_TEXT);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(160, 35));
    }

    private void loadDataFromConfig() {
        idUserField.setText(config.getString("id_user"));
        idPassField.setText(config.getString("id_pass"));
        idBtnField.setText(config.getString("id_button"));
        userField.setText(config.getString("username"));
        passField.setText(config.getString("password"));
        loopCheckBox.setSelected(config.getBoolean("auto_loop"));
        checkNetBox.setSelected(config.getBoolean("check_internet"));
        if(config.getString("auto_reset").equals("")) config.set("auto_reset", true);
        autoResetBox.setSelected(config.getBoolean("auto_reset"));
        hiddenBox.setSelected(config.getBoolean("hidden_mode"));
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private void saveCrashLog(String context, Exception e) {
        File folder = new File("EE-Auto-Login");
        if (!folder.exists()) folder.mkdirs();

        File logFile = new File(folder, "log.yml");
        try (FileWriter fw = new FileWriter(logFile, true); PrintWriter pw = new PrintWriter(fw)) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            pw.println("---");
            pw.println("timestamp: \"" + dtf.format(LocalDateTime.now()) + "\"");
            pw.println("context: \"" + context + "\"");
            pw.println("error_message: \"" + e.getMessage().replace("\"", "'") + "\"");
            pw.println("stack_trace: |");
            e.printStackTrace(pw);
            pw.println("");
        } catch (IOException ioException) {
            log("CRITICAL: Could not write to log.yml");
        }
    }

    private void startProcess() {
        config.set("username", userField.getText());
        config.set("password", new String(passField.getPassword()));
        config.set("id_user", idUserField.getText());
        config.set("id_pass", idPassField.getText());
        config.set("id_button", idBtnField.getText());
        config.set("auto_loop", loopCheckBox.isSelected());
        config.set("check_internet", checkNetBox.isSelected());
        config.set("auto_reset", autoResetBox.isSelected());
        config.set("hidden_mode", hiddenBox.isSelected());
        config.saveConfig();

        isRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        log("--- Service Started ---");

        Runnable task = () -> {
            if (!isRunning) return;
            boolean shouldLogin = false;

            if (checkNetBox.isSelected()) {
                log("Checking internet connectivity...");
                if (isConnected()) {
                    log("Internet is ONLINE. No action needed.");
                } else {
                    log("Internet is DOWN. Launching Login Sequence...");
                    shouldLogin = true;
                }
            } else {
                log("Skipping connection check (Forced).");
                shouldLogin = true;
            }

            if (shouldLogin) performLogin();
            if (!loopCheckBox.isSelected()) stopProcess();
        };

        if (loopCheckBox.isSelected()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
        } else {
            new Thread(task).start();
        }
    }

    private void stopProcess() {
        isRunning = false;
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        log("--- Service Stopped ---");
    }

    private boolean isConnected() {
        try {
            URL url = new URL("http://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private void resetWifiAdapter() {
        if (!autoResetBox.isSelected()) return;
        log("!!! CRITICAL ERROR DETECTED !!!");
        log("Attempting to RESET WiFi Adapter...");

        try {
            String cmdDisable = "netsh interface set interface \"Wi-Fi\" admin=disable";
            String cmdEnable = "netsh interface set interface \"Wi-Fi\" admin=enable";

            log("Disabling adapter...");
            Runtime.getRuntime().exec(cmdDisable).waitFor();
            Thread.sleep(3000);

            log("Enabling adapter...");
            Runtime.getRuntime().exec(cmdEnable).waitFor();
            log("Adapter reset complete. Waiting 15s...");
            Thread.sleep(15000);

        } catch (Exception e) {
            log("RESET FAILED: " + e.getMessage());
            saveCrashLog("WiFi_Reset_Failure", e);
        }
    }

    private void performLogin() {
        WebDriver driver = null;
        try {
            String localDriverPath = "EE-Auto-Login" + File.separator + "chromedriver.exe";
            File driverFile = new File(localDriverPath);

            if (driverFile.exists()) {
                System.setProperty("webdriver.chrome.driver", driverFile.getAbsolutePath());
                log("Offline Mode: Using driver from sub-folder.");
            } else {
                log("Local driver missing. Trying auto-download...");
                try { WebDriverManager.chromedriver().setup(); }
                catch (Exception e) { log("Driver download failed."); }
            }

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            if (hiddenBox.isSelected()) {
                options.addArguments("--headless=new");
                options.addArguments("--window-size=1920,1080");
                log("Debug: Browser is running HIDDEN.");
            } else {
                options.addArguments("--start-maximized");
            }

            log("Launching Chrome...");
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            try {
                driver.get("http://neverssl.com");
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("ERR_INTERNET_DISCONNECTED") || e.getMessage().contains("ERR_NAME_NOT_RESOLVED"))) {
                    log("Error: Internet Disconnected. Resetting adapter...");
                    driver.quit();
                    resetWifiAdapter();
                    return;
                }
                throw e;
            }

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("ee.co.uk"),
                    ExpectedConditions.urlContains("bt.com"),
                    ExpectedConditions.urlContains("b2c")
            ));
            log("Portal loaded.");

            // Clear Cookies
            try {
                String cookieXpath = "//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'accept')]";
                List<WebElement> cookieBtns = driver.findElements(By.xpath(cookieXpath));
                if(!cookieBtns.isEmpty()) { cookieBtns.get(0).click(); log("Cookies accepted."); }
            } catch (Exception ignored) {}

            // Landing Page
            try {
                WebElement loginNow = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), 'Log in now')]")));
                loginNow.click();
            } catch (Exception ignored) {}

            // Account Selection Popup
            try {
                WebElement accountType = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), 'EE Broadband account')]")));
                accountType.click();
                WebElement subBtn = driver.findElement(By.id("submit-eeb"));
                js.executeScript("arguments[0].click();", subBtn);
                Thread.sleep(3000);
            } catch (Exception ignored) {}

            // Credentials
            WebElement emailBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='signInName' or @type='email']")));
            emailBox.sendKeys(userField.getText());
            try { driver.findElement(By.id("next")).click(); } catch (Exception e) { clickAnyElementWithText(wait, "Next"); }

            WebElement passBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@type='password']")));
            passBox.sendKeys(new String(passField.getPassword()));
            try { driver.findElement(By.id("next")).click(); } catch (Exception e) { clickAnyElementWithText(wait, "Sign in"); }

            log("Login sequence finished. Verifying...");
            Thread.sleep(8000);
            if (isConnected()) log("SUCCESS: Internet restored!");
            else log("FAILED: Connection check failed.");

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            saveCrashLog("Selenium_Login_Failure", e);
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private void clickAnyElementWithText(WebDriverWait wait, String text) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), '" + text + "')]")));
            el.click();
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainClass().setVisible(true));
    }
}