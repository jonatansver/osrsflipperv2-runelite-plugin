package com.osrsflipperv2.runelite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public final class OsrsFlipperV2Panel extends PluginPanel
{
    public interface Controller
    {
        void onPairRequested(String deviceName, String pairingToken);

        void onHeartbeatRequested();

        void onForgetRequested();

        void onRefreshRequested();

        void onReconcileRequested();

        void onHelperActionRequested(GeAssistAction action);
    }

    private final JLabel environmentValueLabel = new JLabel("-");
    private final JLabel backendValueLabel = new JLabel("-");
    private final JLabel deviceValueLabel = new JLabel("-");
    private final JLabel deviceIdValueLabel = new JLabel("-");
    private final JLabel statusValueLabel = new JLabel("Not paired");
    private final JTextField deviceNameField = new JTextField(20);
    private final JTextArea pairingTokenArea = new JTextArea(4, 20);
    private final JTextArea logArea = new JTextArea(10, 20);
    private final JTextArea flipsArea = new JTextArea(8, 20);
    private final JButton pairButton = new JButton("Pair device");
    private final JButton heartbeatButton = new JButton("Refresh connection");
    private final JButton forgetButton = new JButton("Forget device");
    private final JButton refreshButton = new JButton("Refresh state");
    private final JButton reconcileButton = new JButton("Reconcile now");
    private final JButton searchItemButton = new JButton("Search item");
    private final JButton buyPriceButton = new JButton("Fill buy price");
    private final JButton buyQuantityButton = new JButton("Fill buy quantity");
    private final JButton sellPriceButton = new JButton("Fill sell price");
    private final JButton sellQuantityButton = new JButton("Fill sell quantity");
    private final JButton cancelBuyButton = new JButton("Cancel buy");
    private final JButton concludeBuyButton = new JButton("Conclude buy");
    private final JButton refreshSellButton = new JButton("Refresh sell");
    private final JButton concludeSellButton = new JButton("Conclude sell");

    private Controller controller;
    private boolean paired;

    public OsrsFlipperV2Panel()
    {
        JPanel content = getWrappedPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        deviceNameField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        deviceNameField.setForeground(Color.WHITE);
        deviceNameField.setCaretColor(Color.WHITE);
        pairingTokenArea.setForeground(Color.WHITE);
        pairingTokenArea.setCaretColor(Color.WHITE);

        content.add(title());
        content.add(Box.createVerticalStrut(8));
        content.add(connectionCard());
        content.add(Box.createVerticalStrut(8));
        content.add(helperCard());
        content.add(Box.createVerticalStrut(8));
        content.add(activeFlipsCard());
        content.add(Box.createVerticalStrut(8));
        content.add(logCard());

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        logArea.setForeground(Color.WHITE);
        logArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        flipsArea.setEditable(false);
        flipsArea.setLineWrap(true);
        flipsArea.setWrapStyleWord(true);
        flipsArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        flipsArea.setForeground(Color.WHITE);
        flipsArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        pairingTokenArea.setLineWrap(true);
        pairingTokenArea.setWrapStyleWord(true);
        pairingTokenArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        pairingTokenArea.setForeground(Color.WHITE);
        pairingTokenArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        pairButton.addActionListener(event -> {
            if (controller != null)
            {
                controller.onPairRequested(deviceNameField.getText(), pairingTokenArea.getText());
            }
        });

        heartbeatButton.addActionListener(event -> {
            if (controller != null)
            {
                controller.onHeartbeatRequested();
            }
        });

        forgetButton.addActionListener(event -> {
            if (controller != null)
            {
                controller.onForgetRequested();
            }
        });

        refreshButton.addActionListener(event -> {
            if (controller != null)
            {
                controller.onRefreshRequested();
            }
        });

        reconcileButton.addActionListener(event -> {
            if (controller != null)
            {
                controller.onReconcileRequested();
            }
        });

        searchItemButton.addActionListener(event -> sendAction(GeAssistAction.SEARCH_ITEM));
        buyPriceButton.addActionListener(event -> sendAction(GeAssistAction.FILL_BUY_PRICE));
        buyQuantityButton.addActionListener(event -> sendAction(GeAssistAction.FILL_BUY_QUANTITY));
        sellPriceButton.addActionListener(event -> sendAction(GeAssistAction.FILL_SELL_PRICE));
        sellQuantityButton.addActionListener(event -> sendAction(GeAssistAction.FILL_SELL_QUANTITY));
        cancelBuyButton.addActionListener(event -> sendAction(GeAssistAction.CANCEL_BUY));
        concludeBuyButton.addActionListener(event -> sendAction(GeAssistAction.CONCLUDE_BUY));
        refreshSellButton.addActionListener(event -> sendAction(GeAssistAction.REFRESH_SELL_PRICE));
        concludeSellButton.addActionListener(event -> sendAction(GeAssistAction.CONCLUDE_SELL));
    }

    public void setController(Controller controller)
    {
        this.controller = controller;
    }

    public void renderSnapshot(ConnectionSnapshot snapshot)
    {
        Objects.requireNonNull(snapshot, "snapshot");
        SwingUtilities.invokeLater(() ->
        {
            paired = snapshot.paired();
            environmentValueLabel.setText(snapshot.backendEnvironment());
            backendValueLabel.setText(snapshot.backendBaseUrl());
            deviceValueLabel.setText(snapshot.deviceName().isBlank() ? "-" : snapshot.deviceName());
            deviceIdValueLabel.setText(snapshot.deviceId().isBlank() ? "-" : snapshot.deviceId());
            String statusMessage = snapshot.statusMessage();
            statusValueLabel.setText(statusMessage == null || statusMessage.isBlank() ? "-" : statusMessage);
            heartbeatButton.setEnabled(snapshot.paired());
            forgetButton.setEnabled(snapshot.paired());
            refreshButton.setEnabled(snapshot.paired());
            reconcileButton.setEnabled(snapshot.paired());
            searchItemButton.setEnabled(snapshot.paired());
            buyPriceButton.setEnabled(snapshot.paired());
            buyQuantityButton.setEnabled(snapshot.paired());
            sellPriceButton.setEnabled(snapshot.paired());
            sellQuantityButton.setEnabled(snapshot.paired());
            cancelBuyButton.setEnabled(snapshot.paired());
            concludeBuyButton.setEnabled(snapshot.paired());
            refreshSellButton.setEnabled(snapshot.paired());
            concludeSellButton.setEnabled(snapshot.paired());
        });
    }

    public void appendLog(String message)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (!logArea.getText().isEmpty())
            {
                logArea.append(System.lineSeparator());
            }
            logArea.append(message);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void setBusy(boolean busy)
    {
        SwingUtilities.invokeLater(() ->
        {
            pairButton.setEnabled(!busy);
            heartbeatButton.setEnabled(!busy && paired);
            forgetButton.setEnabled(!busy && paired);
            refreshButton.setEnabled(!busy && paired);
            reconcileButton.setEnabled(!busy && paired);
            searchItemButton.setEnabled(!busy && paired);
            buyPriceButton.setEnabled(!busy && paired);
            buyQuantityButton.setEnabled(!busy && paired);
            sellPriceButton.setEnabled(!busy && paired);
            sellQuantityButton.setEnabled(!busy && paired);
            cancelBuyButton.setEnabled(!busy && paired);
            concludeBuyButton.setEnabled(!busy && paired);
            refreshSellButton.setEnabled(!busy && paired);
            concludeSellButton.setEnabled(!busy && paired);
        });
    }

    public void setDeviceName(String deviceName)
    {
        SwingUtilities.invokeLater(() -> deviceNameField.setText(deviceName));
    }

    public void setPairingToken(String pairingToken)
    {
        SwingUtilities.invokeLater(() -> pairingTokenArea.setText(pairingToken));
    }

    public String getDeviceName()
    {
        return deviceNameField.getText().trim();
    }

    public String getPairingToken()
    {
        return pairingTokenArea.getText().trim();
    }

    public void renderDashboard(DashboardSnapshot dashboard)
    {
        Objects.requireNonNull(dashboard, "dashboard");
        SwingUtilities.invokeLater(() ->
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Captured: ").append(dashboard.capturedAtEpochMs()).append(System.lineSeparator());
            builder.append("Default budget: ").append(dashboard.requestDefaultBudgetGp()).append(" gp").append(System.lineSeparator());
            builder.append("Buy limit windows: ").append(dashboard.requestDefaultBuyLimitWindows()).append(System.lineSeparator());
            builder.append(System.lineSeparator());
            for (ActiveFlipSnapshot flip : dashboard.activeFlips())
            {
                builder.append('#').append(flip.slotIndex()).append(' ')
                    .append(flip.itemName()).append(" [").append(flip.effectiveStatusLabel()).append("]")
                    .append(" buy=").append(flip.plannedBuyPriceGp())
                    .append(" sell=").append(flip.plannedSellPriceGp())
                    .append(" qty=").append(flip.plannedQuantity())
                    .append(System.lineSeparator());
            }
            if (dashboard.activeFlips().isEmpty())
            {
                builder.append("No active flips.").append(System.lineSeparator());
            }
            flipsArea.setText(builder.toString());
        });
    }

    private JPanel title()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JLabel title = new JLabel("Osrs Flipper V2");
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Pair the plugin, watch status, and keep a local device token ready.");
        subtitle.setForeground(Color.LIGHT_GRAY);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(ColorScheme.DARK_GRAY_COLOR);
        text.add(title);
        text.add(subtitle);

        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private JPanel infoCard()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "Connection"));

        card.add(row("Environment", environmentValueLabel));
        card.add(row("Backend", backendValueLabel));
        card.add(row("Paired device", deviceValueLabel));
        card.add(row("Device ID", deviceIdValueLabel));
        card.add(row("Status", statusValueLabel));

        return card;
    }

    private JPanel connectionCard()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "Connection and pairing"));

        card.add(row("Device name", deviceNameField));
        card.add(Box.createVerticalStrut(4));

        JLabel tokenLabel = new JLabel("Pairing token");
        tokenLabel.setForeground(Color.WHITE);
        card.add(tokenLabel);

        JScrollPane tokenScroll = new JScrollPane(pairingTokenArea);
        tokenScroll.setPreferredSize(new Dimension(PANEL_WIDTH - 24, 72));
        card.add(tokenScroll);
        card.add(Box.createVerticalStrut(6));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttons.add(pairButton);
        buttons.add(heartbeatButton);
        buttons.add(forgetButton);
        card.add(buttons);
        card.add(Box.createVerticalStrut(8));
        card.add(infoCard());

        return card;
    }

    private JPanel helperCard()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "GE helpers"));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        topRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topRow.add(refreshButton);
        topRow.add(reconcileButton);
        card.add(topRow);
        card.add(Box.createVerticalStrut(6));

        JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
        grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
        grid.add(searchItemButton);
        grid.add(buyPriceButton);
        grid.add(buyQuantityButton);
        grid.add(sellPriceButton);
        grid.add(sellQuantityButton);
        grid.add(cancelBuyButton);
        grid.add(concludeBuyButton);
        grid.add(refreshSellButton);
        grid.add(concludeSellButton);
        card.add(grid);
        return card;
    }

    private JPanel activeFlipsCard()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "Active flips"));

        JScrollPane scrollPane = new JScrollPane(flipsArea);
        scrollPane.setPreferredSize(new Dimension(PANEL_WIDTH - 24, 180));
        card.add(scrollPane);
        return card;
    }

    private JPanel logCard()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "Logs"));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(PANEL_WIDTH - 24, 120));
        card.add(scrollPane);
        return card;
    }

    private void sendAction(GeAssistAction action)
    {
        if (controller != null)
        {
            controller.onHelperActionRequested(action);
        }
    }

    private JPanel row(String label, JLabel value)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel left = new JLabel(label + ":");
        left.setForeground(Color.WHITE);
        row.add(left, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private JPanel row(String label, JTextField field)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel left = new JLabel(label + ":");
        left.setForeground(Color.WHITE);
        row.add(left, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }
}
