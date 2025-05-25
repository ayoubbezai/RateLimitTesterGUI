import java.awt.*;
import java.awt.event.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

public class RateLimitTesterGUI extends JFrame {

    // Colors and styling
    private static final Color PRIMARY_COLOR = new Color(48, 63, 159);
    private static final Color SECONDARY_COLOR = new Color(255, 255, 255);
    private static final Color ACCENT_COLOR = new Color(255, 152, 0);
    private static final Color ERROR_COLOR = new Color(198, 40, 40);
    private static final Color SUCCESS_COLOR = new Color(56, 142, 60);
    private static final Color BUTTON_BG = new Color(66, 165, 245);
    private static final Color BUTTON_HOVER = new Color(33, 150, 243);
    private static final Color BUTTON_PRESSED = new Color(30, 136, 229);

    // UI Components
    private JTextField urlField;
    private JComboBox<String> methodCombo;
    private JTextField threadsField;
    private JTextField requestsField;
    private JTextField delayField;
    private JTextArea payloadArea;
    private JButton startButton;
    private JTextArea outputArea;
    private JPanel jsonEditorPanel;
    private JButton addFieldButton;
    private JButton toggleJsonInput;
    private JScrollPane payloadScroll;
    private boolean jsonEditorMode = false;

    // Test tracking variables
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger rateLimitedCount = new AtomicInteger(0);
    private AtomicInteger otherErrors = new AtomicInteger(0);
    private final List<Long> successTimestamps = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean stopAll = false;
    private volatile Long first429Time = null;

    public RateLimitTesterGUI() {
        setTitle("Rate Limit Tester - Stop on 429 + Estimate RPM");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        applyStyles();
    }

    private void initComponents() {
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Input panel with GridBagLayout for precise control
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();

        // URL components
        JLabel urlLabel = new JLabel("Target URL:");
        urlField = new JTextField(30);
        urlField.setToolTipText("Enter the target endpoint URL");

        // Method selection
        JLabel methodLabel = new JLabel("HTTP Method:");
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        methodCombo.setToolTipText("Select HTTP method");

        // Test configuration
        JLabel threadsLabel = new JLabel("Number of Threads:");
        threadsField = new JTextField("5", 5);
        threadsField.setToolTipText("Number of concurrent threads to use");

        JLabel requestsLabel = new JLabel("Max Requests per Thread:");
        requestsField = new JTextField("100", 5);
        requestsField.setToolTipText("Maximum requests each thread will send");

        JLabel delayLabel = new JLabel("Delay between Requests (ms):");
        delayField = new JTextField("100", 5);
        delayField.setToolTipText("Delay between consecutive requests in milliseconds");

        // JSON Payload components
        JLabel payloadLabel = new JLabel("POST JSON Payload:");
        payloadArea = new JTextArea(4, 30);
        payloadArea.setEnabled(false);
        payloadScroll = new JScrollPane(payloadArea);
        payloadScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // JSON Editor Panel
        jsonEditorPanel = new JPanel();
        jsonEditorPanel.setLayout(new BoxLayout(jsonEditorPanel, BoxLayout.Y_AXIS));
        jsonEditorPanel.setBorder(BorderFactory.createTitledBorder("JSON Key-Value Editor"));
        jsonEditorPanel.setVisible(false);

        // Add field button
        addFieldButton = new JButton("Add Field");
        addFieldButton.addActionListener(e -> addJsonField("", ""));
        addFieldButton.setVisible(false);

        // Toggle between JSON modes
        toggleJsonInput = new JButton("Switch to JSON Editor");
        toggleJsonInput.addActionListener(e -> toggleJsonEditor());

        // Start test button
        startButton = new JButton("Start Test");
        startButton.setToolTipText("Begin the rate limit test");
        startButton.addActionListener(new StartButtonListener());

        // Method change listener
        methodCombo.addActionListener(e -> {
            String method = (String) methodCombo.getSelectedItem();
            boolean isPayloadMethod = !"GET".equalsIgnoreCase(method);
            payloadArea.setEnabled(isPayloadMethod);
            toggleJsonInput.setEnabled(isPayloadMethod);
            addFieldButton.setEnabled(isPayloadMethod);
            
            if (!isPayloadMethod) {
                jsonEditorMode = false;
                jsonEditorPanel.setVisible(false);
                toggleJsonInput.setText("Switch to JSON Editor");
                payloadScroll.setVisible(true);
            }
        });

        // Layout configuration
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        // URL row
        c.gridx = 0; c.gridy = 0; inputPanel.add(urlLabel, c);
        c.gridx = 1; c.gridy = 0; c.gridwidth = 3; inputPanel.add(urlField, c);

        // Method and threads row
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 1; inputPanel.add(methodLabel, c);
        c.gridx = 1; c.gridy = 1; inputPanel.add(methodCombo, c);
        c.gridx = 2; c.gridy = 1; inputPanel.add(threadsLabel, c);
        c.gridx = 3; c.gridy = 1; inputPanel.add(threadsField, c);

        // Requests and delay row
        c.gridx = 0; c.gridy = 2; inputPanel.add(requestsLabel, c);
        c.gridx = 1; c.gridy = 2; inputPanel.add(requestsField, c);
        c.gridx = 2; c.gridy = 2; inputPanel.add(delayLabel, c);
        c.gridx = 3; c.gridy = 2; inputPanel.add(delayField, c);

        // Payload row
        c.gridx = 0; c.gridy = 3; inputPanel.add(payloadLabel, c);
        c.gridx = 1; c.gridy = 3; c.gridwidth = 3; inputPanel.add(payloadScroll, c);
        
        // JSON mode toggle
        c.gridx = 0; c.gridy = 4; c.gridwidth = 4; inputPanel.add(toggleJsonInput, c);
        
        // JSON editor panel
        c.gridx = 0; c.gridy = 5; c.gridwidth = 4; inputPanel.add(jsonEditorPanel, c);
        
        // Add field button
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; inputPanel.add(addFieldButton, c);
        
        // Start button
        c.gridx = 0; c.gridy = 7; c.gridwidth = 4; inputPanel.add(startButton, c);

        // Output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Test Output"));

        // Add components to main panel
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(mainPanel);
    }

    private void toggleJsonEditor() {
        jsonEditorMode = !jsonEditorMode;
        
        if (jsonEditorMode) {
            // Switch to editor mode
            jsonEditorPanel.setVisible(true);
            addFieldButton.setVisible(true);
            payloadScroll.setVisible(false);
            toggleJsonInput.setText("Switch to Raw JSON");
            
            // Clear existing fields
            jsonEditorPanel.removeAll();
            
            // Parse existing JSON if any
            String currentJson = payloadArea.getText().trim();
            if (!currentJson.isEmpty() && currentJson.startsWith("{") && currentJson.endsWith("}")) {
                try {
                    String content = currentJson.substring(1, currentJson.length() - 1).trim();
                    if (!content.isEmpty()) {
                        String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                        for (String pair : pairs) {
                            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                            if (kv.length == 2) {
                                addJsonField(
                                    kv[0].trim().replaceAll("^\"|\"$", ""), 
                                    kv[1].trim().replaceAll("^\"|\"$", "")
                                );
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // If parsing fails, start with one empty field
                    addJsonField("", "");
                }
            } else {
                // Start with one empty field
                addJsonField("", "");
            }
        } else {
            // Switch to raw mode
            jsonEditorPanel.setVisible(false);
            addFieldButton.setVisible(false);
            payloadScroll.setVisible(true);
            toggleJsonInput.setText("Switch to JSON Editor");
            
            // Generate JSON from fields and update payload area
            String generatedJson = generateJsonFromEditor();
            payloadArea.setText(generatedJson);
        }
        
        revalidate();
        repaint();
    }

    private String generateJsonFromEditor() {
        try {
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            
            for (Component comp : jsonEditorPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel fieldPanel = (JPanel) comp;
                    JTextField keyField = (JTextField) fieldPanel.getComponent(0);
                    JTextField valueField = (JTextField) fieldPanel.getComponent(2);
                    
                    String key = keyField.getText().trim();
                    String value = valueField.getText().trim();
                    
                    if (!key.isEmpty()) {
                        if (!first) {
                            jsonBuilder.append(",");
                        }
                        jsonBuilder.append("\"").append(escapeJson(key)).append("\":\"").append(escapeJson(value)).append("\"");
                        first = false;
                    }
                }
            }
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating JSON: " + e.getMessage(), 
                "JSON Error", 
                JOptionPane.ERROR_MESSAGE);
            return "{}";
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void addJsonField(String key, String value) {
        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.X_AXIS));
        fieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        JTextField keyField = new JTextField(key);
        keyField.setBorder(BorderFactory.createTitledBorder("Key"));
        keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, keyField.getPreferredSize().height));
        
        JTextField valueField = new JTextField(value);
        valueField.setBorder(BorderFactory.createTitledBorder("Value"));
        valueField.setMaximumSize(new Dimension(Integer.MAX_VALUE, valueField.getPreferredSize().height));
        
        JButton removeButton = createRemoveButton(fieldPanel);
        
        fieldPanel.add(keyField);
        fieldPanel.add(Box.createHorizontalStrut(10));
        fieldPanel.add(valueField);
        fieldPanel.add(Box.createHorizontalStrut(5));
        fieldPanel.add(removeButton);
        
        jsonEditorPanel.add(fieldPanel);
        jsonEditorPanel.revalidate();
        jsonEditorPanel.repaint();
    }

    private JButton createRemoveButton(JPanel fieldPanel) {
        JButton removeButton = new JButton("×");
        removeButton.setFont(new Font("Arial", Font.BOLD, 14));
        removeButton.setForeground(ERROR_COLOR);
        removeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        removeButton.setContentAreaFilled(false);
        removeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                removeButton.setForeground(ERROR_COLOR.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                removeButton.setForeground(ERROR_COLOR);
            }
        });
        removeButton.addActionListener(e -> {
            jsonEditorPanel.remove(fieldPanel);
            jsonEditorPanel.revalidate();
            jsonEditorPanel.repaint();
        });
        return removeButton;
    }

    private void applyStyles() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default if system L&F fails
        }

        // Frame style
        getContentPane().setBackground(SECONDARY_COLOR);
        
        // Style all buttons
        styleButton(startButton, BUTTON_BG);
        styleButton(toggleJsonInput, BUTTON_BG);
        styleButton(addFieldButton, BUTTON_BG);
        
        // Input fields styling
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        methodCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        threadsField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        requestsField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        delayField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        payloadArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        // Output area styling
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputArea.setBackground(new Color(245, 245, 245));
        
        // Labels styling
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        }
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_PRESSED);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }
        });
    }

    class RequestWorker extends Thread {
        private final String targetUrl;
        private final int maxRequests;
        private final int delayMs;
        private final String method;
        private final String jsonPayload;

        public RequestWorker(String targetUrl, int maxRequests, int delayMs, String method, String jsonPayload) {
            this.targetUrl = targetUrl;
            this.maxRequests = maxRequests;
            this.delayMs = delayMs;
            this.method = method;
            this.jsonPayload = jsonPayload;
        }

        @Override
        public void run() {
            for (int i = 0; i < maxRequests; i++) {
                if (stopAll) {
                    appendOutput(String.format("[%s] Stopped due to global rate limit.\n", getName()));
                    break;
                }

                try {
                    long start = System.currentTimeMillis();

                    URL url = new URL(targetUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    if (!"GET".equalsIgnoreCase(method)) {
                        conn.setDoOutput(true);
                        byte[] payloadBytes = jsonPayload.getBytes("UTF-8");
                        conn.setFixedLengthStreamingMode(payloadBytes.length);
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(payloadBytes);
                        }
                    }

                    int responseCode = conn.getResponseCode();
                    String responseMessage = conn.getResponseMessage();
                    long duration = System.currentTimeMillis() - start;

                    if (responseCode == 200) {
                        successCount.incrementAndGet();
                        successTimestamps.add(System.currentTimeMillis());
                        appendOutput(String.format("[✓] %s - 200 OK (%d ms)\n", getName(), duration));
                    } else if (responseCode == 429) {
                        rateLimitedCount.incrementAndGet();
                        long now = System.currentTimeMillis();
                        synchronized (RateLimitTesterGUI.this) {
                            if (first429Time == null) {
                                first429Time = now;
                            }
                        }
                        appendOutput(String.format("[⚠] %s - 429 Rate Limited - Stopping all threads (%d ms)\n", getName(), duration));
                        stopAll = true;
                        break;
                    } else {
                        otherErrors.incrementAndGet();
                        appendOutput(String.format("[X] %s - HTTP %d %s (%d ms)\n", getName(), responseCode, responseMessage, duration));
                    }

                } catch (Exception e) {
                    otherErrors.incrementAndGet();
                    appendOutput(String.format("[!] %s - Error: %s\n", getName(), e.getMessage()));
                }

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                }
            }
            appendOutput(String.format("[%s] Thread finished.\n", getName()));
        }
    }

    class StartButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Reset test state
            successCount.set(0);
            rateLimitedCount.set(0);
            otherErrors.set(0);
            stopAll = false;
            first429Time = null;
            successTimestamps.clear();
            outputArea.setText("");

            final String url = urlField.getText().trim();
            final String method = (String) methodCombo.getSelectedItem();
            
            // Initialize payload
            String payload = "";
            if (!"GET".equalsIgnoreCase(method)) {
                if (jsonEditorMode) {
                    payload = generateJsonFromEditor();
                    System.out.println("Generated JSON: " + payload);
                    
                    if (!isValidJson(payload)) {
                        JOptionPane.showMessageDialog(RateLimitTesterGUI.this,
                                "Invalid JSON format generated from editor",
                                "JSON Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    payload = payloadArea.getText().trim();
                }
            }

            // Create final copies for lambda
            final String finalPayload = payload;
            final int numThreads;
            final int maxRequests;
            final int delay;

            try {
                numThreads = Integer.parseInt(threadsField.getText().trim());
                maxRequests = Integer.parseInt(requestsField.getText().trim());
                delay = Integer.parseInt(delayField.getText().trim());

                // Validation checks
                if (numThreads <= 0 || maxRequests <= 0 || delay < 0) {
                    JOptionPane.showMessageDialog(RateLimitTesterGUI.this,
                            "Please enter positive numbers for threads, requests, and non-negative delay.",
                            "Invalid input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (url.isEmpty()) {
                    JOptionPane.showMessageDialog(RateLimitTesterGUI.this,
                            "Please enter a valid URL.",
                            "Invalid input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!"GET".equalsIgnoreCase(method)) {
                    if (finalPayload.isEmpty()) {
                        int option = JOptionPane.showConfirmDialog(RateLimitTesterGUI.this,
                                "JSON payload is empty. Continue with empty payload?",
                                "Empty Payload", JOptionPane.YES_NO_OPTION);
                        if (option != JOptionPane.YES_OPTION) return;
                    } else if (!isValidJson(finalPayload)) {
                        JOptionPane.showMessageDialog(RateLimitTesterGUI.this,
                                "Payload must be a valid JSON object (enclosed in {} with \"key\":\"value\" pairs)",
                                "Invalid JSON", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(RateLimitTesterGUI.this,
                        "Please enter valid numeric values.",
                        "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            startButton.setEnabled(false);
            appendOutput("Starting test...\n");
            appendOutput("Using payload: " + finalPayload + "\n");

            new Thread(() -> {
                Thread[] threads = new Thread[numThreads];
                long testStart = System.currentTimeMillis();

                for (int i = 0; i < numThreads; i++) {
                    threads[i] = new RequestWorker(url, maxRequests, delay, method, finalPayload);
                    threads[i].setName("User-" + (i + 1));
                    threads[i].start();
                }

                for (Thread t : threads) {
                    try {
                        t.join();
                    } catch (InterruptedException ignored) {
                    }
                }

                long testEnd = System.currentTimeMillis();

                // Calculate estimated RPM
                long endTimeForCalc = first429Time != null ? first429Time : testEnd;
                int requestsBefore429;
                synchronized (successTimestamps) {
                    requestsBefore429 = (int) successTimestamps.stream().filter(ts -> ts <= endTimeForCalc).count();
                }

                double durationMinutes = (endTimeForCalc - testStart) / 60000.0;
                double estimatedRPM = 0;
                if (durationMinutes > 0 && requestsBefore429 > 0) {
                    estimatedRPM = requestsBefore429 / durationMinutes;
                }

                appendOutput("\n=== Test Summary ===\n");
                appendOutput("Total Requests Attempted (threads * max requests): " + (numThreads * maxRequests) + "\n");
                appendOutput("Successful Requests (200): " + successCount.get() + "\n");
                appendOutput("Rate Limited Responses (429): " + rateLimitedCount.get() + "\n");
                appendOutput("Other Errors: " + otherErrors.get() + "\n");
                appendOutput(String.format("Estimated Rate Limit: %.2f requests per minute\n", estimatedRPM));
                appendOutput(String.format("Total Test Duration: %.2f seconds\n", (testEnd - testStart) / 1000.0));

                startButton.setEnabled(true);
            }).start();
        }
        
        private boolean isValidJson(String json) {
            json = json.trim();
            // Basic structure check
            if (!json.startsWith("{") || !json.endsWith("}")) {
                return false;
            }
            
            try {
                String content = json.substring(1, json.length()-1).trim();
                if (content.isEmpty()) return true;
                
                // Simple check for key-value pairs
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length != 2) return false;
                    String key = kv[0].trim();
                    if (!key.startsWith("\"") || !key.endsWith("\"")) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void appendOutput(String text) {
        final String outputText = text;
        SwingUtilities.invokeLater(() -> {
            // Color coding for different message types
            if (outputText.contains("[✓]")) {
                outputArea.setForeground(SUCCESS_COLOR);
            } else if (outputText.contains("[⚠]") || outputText.contains("[X]")) {
                outputArea.setForeground(ERROR_COLOR);
            } else if (outputText.contains("[!]")) {
                outputArea.setForeground(ACCENT_COLOR);
            } else {
                outputArea.setForeground(Color.BLACK);
            }
            
            outputArea.append(outputText);
            
            // Auto-scroll to bottom
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RateLimitTesterGUI gui = new RateLimitTesterGUI();
            gui.setVisible(true);
        });
    }
}