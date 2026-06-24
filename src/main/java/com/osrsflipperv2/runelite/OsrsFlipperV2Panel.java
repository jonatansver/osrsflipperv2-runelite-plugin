package com.osrsflipperv2.runelite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
    }

    private final JLabel environmentValueLabel = new JLabel("-");
    private final JLabel backendValueLabel = new JLabel("-");
    private final JLabel deviceValueLabel = new JLabel("-");
    private final JLabel deviceIdValueLabel = new JLabel("-");
    private final JLabel statusValueLabel = new JLabel("Not paired");
    private final JTextField deviceNameField = new JTextField(20);
    private final JTextArea pairingTokenArea = new JTextArea(4, 20);
    private final JTextArea logArea = new JTextArea(10, 20);
    private final JButton pairButton = new JButton("Pair device");
    private final JButton heartbeatButton = new JButton("Heartbeat now");
    private final JButton forgetButton = new JButton("Forget device");

    private Controller controller;
    private boolean paired;

    public OsrsFlipperV2Panel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(title());
        add(Box.createVerticalStrut(8));
        add(infoCard());
        add(Box.createVerticalStrut(8));
        add(pairingCard());
        add(Box.createVerticalStrut(8));
        add(logCard());

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        logArea.setForeground(Color.WHITE);
        logArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

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

    private JPanel pairingCard()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "Pairing"));

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
