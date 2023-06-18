package com.example.redes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class ClientApp extends JFrame {
    private static final String SERVER_IP = "172.16.0.1";
    private static final int SERVER_TCP_PORT = 5475;
    private static final int SERVER_UDP_PORT = 5565;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton listButton;

    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ClientApp(String name) {
        this.clientName = name;

        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        messageField = new JTextField();
        bottomPanel.add(messageField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        bottomPanel.add(sendButton, BorderLayout.EAST);

        listButton = new JButton("List");
        listButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestConnectedClients();
            }
        });
        bottomPanel.add(listButton, BorderLayout.WEST);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);

        connectToServer();
        startTCPListening();
        startUDPListening();
    }

    private void connectToServer() {
        try {
            tcpSocket = new Socket(SERVER_IP, SERVER_TCP_PORT);
            udpSocket = new DatagramSocket();
            in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            out = new PrintWriter(tcpSocket.getOutputStream(), true);
            chatArea.append("Connected to server\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startTCPListening() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String serverMessage;
                    out.println(clientName);
                    while ((serverMessage = in.readLine()) != null) {
                        chatArea.append(serverMessage + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                        tcpSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void startUDPListening() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    while (true) {
                        udpSocket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        chatArea.append(message + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    udpSocket.close();
                }
            }
        });
        thread.start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(clientName + ": " + message);
            // sendUDPMessage(clientName + ": " + message);
            messageField.setText("");
            chatArea.append("You: " + message + "\n");
        }
    }

    private void sendUDPMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, tcpSocket.getInetAddress(),
                    SERVER_UDP_PORT);
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestConnectedClients() {
        sendUDPMessage("GET_CLIENTS");
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new ClientApp(args[0]);
                }
            });
        } else {
            System.out.println("Please provide a client name as an argument.");
        }
    }
}
