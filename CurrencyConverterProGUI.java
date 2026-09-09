import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Project 4 - Tier 2+: "Currency Converter Pro"
 * A feature-rich upgrade of the GUI converter:
 *   - 20 supported currencies with symbols
 *   - Dark / Light mode toggle
 *   - Swap-currencies button
 *   - Conversion history (session-based)
 *   - Favorites bar for quick currency selection
 *   - Copy-result-to-clipboard
 *   - Simulated live rate ticker with up/down trend arrows
 *
 * NOTE: Rates fluctuate via a small simulated random walk (clearly labeled
 * "Live Simulation" in the UI) so the trend arrows and ticker have something
 * to react to without needing an internet connection. Swap this for the
 * real ExchangeRate-API integration (see CurrencyConverterLiveAPI.java)
 * if you want genuinely live numbers.
 */
public class CurrencyConverterProGUI extends JFrame {

    // ---------- Data ----------
    private static final LinkedHashMap<String, BigDecimal> BASE_RATES = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> SYMBOLS = new LinkedHashMap<>();
    static {
        BASE_RATES.put("USD", new BigDecimal("1.00"));
        BASE_RATES.put("EUR", new BigDecimal("0.92"));
        BASE_RATES.put("GBP", new BigDecimal("0.79"));
        BASE_RATES.put("INR", new BigDecimal("83.40"));
        BASE_RATES.put("JPY", new BigDecimal("149.50"));
        BASE_RATES.put("AUD", new BigDecimal("1.52"));
        BASE_RATES.put("CAD", new BigDecimal("1.36"));
        BASE_RATES.put("CHF", new BigDecimal("0.88"));
        BASE_RATES.put("CNY", new BigDecimal("7.24"));
        BASE_RATES.put("SGD", new BigDecimal("1.34"));
        BASE_RATES.put("NZD", new BigDecimal("1.64"));
        BASE_RATES.put("HKD", new BigDecimal("7.82"));
        BASE_RATES.put("ZAR", new BigDecimal("18.30"));
        BASE_RATES.put("AED", new BigDecimal("3.6725"));
        BASE_RATES.put("SEK", new BigDecimal("10.45"));
        BASE_RATES.put("NOK", new BigDecimal("10.55"));
        BASE_RATES.put("KRW", new BigDecimal("1370.00"));
        BASE_RATES.put("MXN", new BigDecimal("17.05"));
        BASE_RATES.put("BRL", new BigDecimal("5.15"));
        BASE_RATES.put("RUB", new BigDecimal("92.50"));

        SYMBOLS.put("USD", "$");  SYMBOLS.put("EUR", "€");  SYMBOLS.put("GBP", "£");
        SYMBOLS.put("INR", "₹");  SYMBOLS.put("JPY", "¥");  SYMBOLS.put("AUD", "A$");
        SYMBOLS.put("CAD", "C$"); SYMBOLS.put("CHF", "Fr"); SYMBOLS.put("CNY", "¥");
        SYMBOLS.put("SGD", "S$"); SYMBOLS.put("NZD", "NZ$");SYMBOLS.put("HKD", "HK$");
        SYMBOLS.put("ZAR", "R");  SYMBOLS.put("AED", "د.إ");SYMBOLS.put("SEK", "kr");
        SYMBOLS.put("NOK", "kr"); SYMBOLS.put("KRW", "₩");  SYMBOLS.put("MXN", "$");
        SYMBOLS.put("BRL", "R$"); SYMBOLS.put("RUB", "₽");
    }

    private final Map<String, BigDecimal> liveRates = new LinkedHashMap<>(BASE_RATES);
    private final Random random = new Random();
    private BigDecimal lastTickerCrossRate = null;

    // ---------- Theme colors ----------
    private boolean darkMode = false;
    private static final Color LIGHT_BG = new Color(244, 246, 250);
    private static final Color LIGHT_PANEL = Color.WHITE;
    private static final Color LIGHT_TEXT = new Color(25, 28, 35);
    private static final Color DARK_BG = new Color(18, 18, 24);
    private static final Color DARK_PANEL = new Color(30, 31, 40);
    private static final Color DARK_TEXT = new Color(225, 228, 235);
    private static final Color ACCENT = new Color(41, 98, 255);
    private static final Color ACCENT_DARK = new Color(0, 224, 255);
    private static final Color GREEN = new Color(34, 170, 90);
    private static final Color RED = new Color(214, 60, 60);
    private static final Color NEUTRAL_GRAY = new Color(130, 130, 140);

    // ---------- Components needing theme updates ----------
    private final List<JPanel> panels = new ArrayList<>();
    private final List<JLabel> labels = new ArrayList<>();
    private final List<JTextField> fields = new ArrayList<>();
    private final List<JComboBox<String>> combos = new ArrayList<>();
    private final List<JButton> favoriteButtons = new ArrayList<>();

    private JComboBox<String> fromBox, toBox;
    private JTextField amountField, resultField;
    private JLabel titleLabel, subtitleLabel, amountLabel, tickerLabel, historyTitleLabel;
    private JButton convertButton, clearButton, swapButton, copyButton, addFavoriteButton, clearHistoryButton;
    private JToggleButton darkModeToggle;
    private JPanel favoritesBar;
    private DefaultListModel<String> historyModel;
    private JList<String> historyList;

    private final LinkedHashSet<String> favorites = new LinkedHashSet<>(Arrays.asList("USD", "EUR", "INR", "GBP"));

    public CurrencyConverterProGUI() {
        super("Currency Converter Pro");
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        applyTheme();
        startLiveSimulation();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 560);
        setMinimumSize(new Dimension(680, 500));
        setLocationRelativeTo(null);
    }

    // ================= HEADER =================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panels.add(header);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        panels.add(titleBox);
        titleLabel = new JLabel("💱  Currency Converter Pro");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        subtitleLabel = new JLabel("Live rate simulation \u2022 20 currencies \u2022 real-time trends");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labels.add(titleLabel);
        labels.add(subtitleLabel);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        darkModeToggle = new JToggleButton("\uD83C\uDF19 Dark Mode");
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setFont(new Font("SansSerif", Font.BOLD, 13));
        darkModeToggle.addActionListener(e -> {
            darkMode = darkModeToggle.isSelected();
            darkModeToggle.setText(darkMode ? "\u2600 Light Mode" : "\uD83C\uDF19 Dark Mode");
            applyTheme();
        });

        header.add(titleBox, BorderLayout.WEST);
        header.add(darkModeToggle, BorderLayout.EAST);
        return header;
    }

    // ================= CENTER (form + history) =================
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        panels.add(center);

        center.add(buildFormColumn(), BorderLayout.CENTER);
        center.add(buildHistoryColumn(), BorderLayout.EAST);
        return center;
    }

    private JPanel buildFormColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        panels.add(column);

        column.add(buildFavoritesBar());
        column.add(Box.createVerticalStrut(12));
        column.add(buildFormCard());
        column.add(Box.createVerticalStrut(12));
        column.add(buildTickerBar());
        return column;
    }

    private JPanel buildFavoritesBar() {
        JPanel wrapper = new JPanel(new BorderLayout());
        panels.add(wrapper);
        JLabel favLabel = new JLabel("Quick picks:");
        favLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labels.add(favLabel);
        wrapper.add(favLabel, BorderLayout.WEST);

        favoritesBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panels.add(favoritesBar);
        wrapper.add(favoritesBar, BorderLayout.CENTER);
        rebuildFavoritesBar();
        return wrapper;
    }

    private void rebuildFavoritesBar() {
        favoritesBar.removeAll();
        favoriteButtons.clear();
        for (String code : favorites) {
            JButton b = new JButton(SYMBOLS.getOrDefault(code, "") + " " + code);
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif", Font.PLAIN, 12));
            b.addActionListener(e -> {
                toBox.setSelectedItem(code);
                lastTickerCrossRate = null;
                updateTicker();
            });
            favoriteButtons.add(b);
            favoritesBar.add(b);
        }
        stylePillButtons(favoriteButtons);
        favoritesBar.revalidate();
        favoritesBar.repaint();
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panels.add(card);

        String[] codes = BASE_RATES.keySet().toArray(new String[0]);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // From currency
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        JLabel fromLabel = new JLabel("From Currency:");
        labels.add(fromLabel);
        card.add(fromLabel, gbc);
        fromBox = new JComboBox<>(codes);
        combos.add(fromBox);
        gbc.gridx = 1;
        card.add(fromBox, gbc);

        // Swap button
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        swapButton = new JButton("\u21C4");
        swapButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        swapButton.setFocusPainted(false);
        swapButton.setToolTipText("Swap currencies");
        swapButton.addActionListener(e -> {
            String f = (String) fromBox.getSelectedItem();
            String t = (String) toBox.getSelectedItem();
            fromBox.setSelectedItem(t);
            toBox.setSelectedItem(f);
            lastTickerCrossRate = null;
            updateTicker();
        });
        card.add(swapButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // To currency
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel toLabel = new JLabel("To Currency:");
        labels.add(toLabel);
        card.add(toLabel, gbc);
        toBox = new JComboBox<>(codes);
        toBox.setSelectedIndex(3); // INR by default
        combos.add(toBox);
        gbc.gridx = 1;
        card.add(toBox, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        addFavoriteButton = new JButton("\u2606 Save");
        addFavoriteButton.setFocusPainted(false);
        addFavoriteButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        addFavoriteButton.setToolTipText("Add 'To' currency to Quick Picks");
        addFavoriteButton.addActionListener(e -> {
            String code = (String) toBox.getSelectedItem();
            if (favorites.size() < 8 && favorites.add(code)) {
                rebuildFavoritesBar();
            }
        });
        card.add(addFavoriteButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Amount
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        amountLabel = new JLabel("Amount ($):");
        labels.add(amountLabel);
        card.add(amountLabel, gbc);
        amountField = new JTextField();
        fields.add(amountField);
        gbc.gridx = 1; gbc.gridwidth = 2;
        card.add(amountField, gbc);
        gbc.gridwidth = 1;

        fromBox.addActionListener(e -> {
            String code = (String) fromBox.getSelectedItem();
            amountLabel.setText("Amount (" + SYMBOLS.getOrDefault(code, "") + "):");
            lastTickerCrossRate = null;
            updateTicker();
        });
        toBox.addActionListener(e -> {
            lastTickerCrossRate = null;
            updateTicker();
        });

        // Result
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel resultLabel = new JLabel("Converted Amount:");
        labels.add(resultLabel);
        card.add(resultLabel, gbc);
        resultField = new JTextField();
        resultField.setEditable(false);
        resultField.setFont(new Font("SansSerif", Font.BOLD, 15));
        fields.add(resultField);
        gbc.gridx = 1;
        card.add(resultField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        copyButton = new JButton("\u2398 Copy");
        copyButton.setFocusPainted(false);
        copyButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copyButton.addActionListener(e -> {
            String text = resultField.getText();
            if (!text.isEmpty()) {
                StringSelection selection = new StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                copyButton.setText("\u2713 Copied!");
                Timer resetTimer = new Timer(1400, ev -> copyButton.setText("\u2398 Copy"));
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
        });
        card.add(copyButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Buttons row
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 10, 0));
        panels.add(buttonRow);
        clearButton = new JButton("Clear");
        convertButton = new JButton("Convert");
        convertButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        clearButton.setFocusPainted(false);
        convertButton.setFocusPainted(false);
        buttonRow.add(clearButton);
        buttonRow.add(convertButton);
        card.add(buttonRow, gbc);

        convertButton.addActionListener(e -> handleConvert());
        clearButton.addActionListener(e -> {
            amountField.setText("");
            resultField.setText("");
        });

        stylePillButtons(Arrays.asList(swapButton, addFavoriteButton, copyButton, clearButton));
        stylePrimaryButton(convertButton);

        return card;
    }

    private JPanel buildTickerBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panels.add(bar);
        tickerLabel = new JLabel("Loading live rate\u2026", SwingConstants.CENTER);
        tickerLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        labels.add(tickerLabel);
        bar.add(tickerLabel, BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildHistoryColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 8));
        column.setPreferredSize(new Dimension(230, 0));
        panels.add(column);

        JPanel headerRow = new JPanel(new BorderLayout());
        panels.add(headerRow);
        historyTitleLabel = new JLabel("Conversion History");
        historyTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        labels.add(historyTitleLabel);
        headerRow.add(historyTitleLabel, BorderLayout.WEST);

        clearHistoryButton = new JButton("Clear");
        clearHistoryButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
        clearHistoryButton.setFocusPainted(false);
        clearHistoryButton.addActionListener(e -> historyModel.clear());
        headerRow.add(clearHistoryButton, BorderLayout.EAST);
        stylePillButtons(Arrays.asList(clearHistoryButton));

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(historyList);

        column.add(headerRow, BorderLayout.NORTH);
        column.add(scrollPane, BorderLayout.CENTER);
        return column;
    }

    // ================= LOGIC =================
    private void handleConvert() {
        String from = (String) fromBox.getSelectedItem();
        String to = (String) toBox.getSelectedItem();
        String rawAmount = amountField.getText().trim();

        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.",
                    "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            JOptionPane.showMessageDialog(this, "Amount cannot be negative.",
                    "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BigDecimal result = convert(amount, from, to);
        resultField.setText(String.format(Locale.US, "%,.2f", result));
        updateTicker();

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = String.format(Locale.US, "[%s] %,.2f %s -> %,.2f %s",
                time, amount, from, result, to);
        historyModel.add(0, entry);
        if (historyModel.size() > 50) {
            historyModel.remove(historyModel.size() - 1);
        }
    }

    /** Cross-rate conversion routed through USD as the pivot currency. */
    private BigDecimal convert(BigDecimal amount, String from, String to) {
        BigDecimal fromRate = liveRates.get(from);
        BigDecimal toRate = liveRates.get(to);
        BigDecimal amountInUsd = amount.divide(fromRate, 12, RoundingMode.HALF_EVEN);
        return amountInUsd.multiply(toRate).setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal crossRate(String from, String to) {
        BigDecimal fromRate = liveRates.get(from);
        BigDecimal toRate = liveRates.get(to);
        return toRate.divide(fromRate, 8, RoundingMode.HALF_EVEN);
    }

    private void updateTicker() {
        String from = (String) fromBox.getSelectedItem();
        String to = (String) toBox.getSelectedItem();
        BigDecimal rate = crossRate(from, to);

        String arrow = "\u25CF";
        Color color = NEUTRAL_GRAY;
        if (lastTickerCrossRate != null) {
            int cmp = rate.compareTo(lastTickerCrossRate);
            if (cmp > 0) { arrow = "\u25B2"; color = GREEN; }
            else if (cmp < 0) { arrow = "\u25BC"; color = RED; }
        }
        lastTickerCrossRate = rate;
        tickerLabel.setText("1 " + from + " = " + rate.setScale(4, RoundingMode.HALF_EVEN) + " " + to + "   " + arrow);
        tickerLabel.setForeground(color);
    }

    /** Small simulated random walk so the ticker/trend arrows have live movement. */
    private void jitterRates() {
        for (String code : BASE_RATES.keySet()) {
            if (code.equals("USD")) continue; // USD is the fixed pivot
            BigDecimal base = BASE_RATES.get(code);
            BigDecimal current = liveRates.get(code);
            double changePercent = (random.nextDouble() - 0.5) * 0.006; // +/- 0.3%
            BigDecimal changed = current.multiply(BigDecimal.valueOf(1 + changePercent));
            BigDecimal upperBound = base.multiply(new BigDecimal("1.03"));
            BigDecimal lowerBound = base.multiply(new BigDecimal("0.97"));
            if (changed.compareTo(upperBound) > 0) changed = upperBound;
            if (changed.compareTo(lowerBound) < 0) changed = lowerBound;
            liveRates.put(code, changed.setScale(6, RoundingMode.HALF_EVEN));
        }
    }

    private void startLiveSimulation() {
        updateTicker();
        Timer timer = new Timer(2500, e -> {
            jitterRates();
            updateTicker();
        });
        timer.start();
    }

    // ================= THEMING =================
    private void stylePrimaryButton(JButton b) {
        b.setBackground(darkMode ? ACCENT_DARK : ACCENT);
        b.setForeground(darkMode ? Color.BLACK : Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
    }

    private void stylePillButtons(List<JButton> buttons) {
        for (JButton b : buttons) {
            b.setBackground(darkMode ? new Color(50, 52, 65) : new Color(228, 232, 240));
            b.setForeground(darkMode ? DARK_TEXT : LIGHT_TEXT);
            b.setOpaque(true);
            b.setBorderPainted(false);
        }
    }

    private void applyTheme() {
        Color bg = darkMode ? DARK_BG : LIGHT_BG;
        Color panelColor = darkMode ? DARK_PANEL : LIGHT_PANEL;
        Color text = darkMode ? DARK_TEXT : LIGHT_TEXT;

        getContentPane().setBackground(bg);
        for (JPanel p : panels) {
            p.setBackground(bg);
        }
        for (JLabel l : labels) {
            l.setForeground(text);
        }
        for (JTextField f : fields) {
            f.setBackground(panelColor);
            f.setForeground(text);
            f.setCaretColor(text);
        }
        for (JComboBox<String> c : combos) {
            c.setBackground(panelColor);
            c.setForeground(text);
        }
        if (historyList != null) {
            historyList.setBackground(panelColor);
            historyList.setForeground(text);
        }
        if (tickerLabel != null && lastTickerCrossRate == null) {
            tickerLabel.setForeground(darkMode ? DARK_TEXT : LIGHT_TEXT);
        }
        stylePrimaryButton(convertButton);
        stylePillButtons(Arrays.asList(swapButton, addFavoriteButton, copyButton, clearButton, clearHistoryButton));
        stylePillButtons(favoriteButtons);

        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CurrencyConverterProGUI().setVisible(true));
    }
}
