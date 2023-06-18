package com.example.redes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerApp {
    private static final int TCP_PORT = 5475;
    private static final int UDP_PORT = 5565;
    private List<ClientHandler> clients;

    public ServerApp() {
        clients = new ArrayList<>();
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(TCP_PORT);
            DatagramSocket datagramSocket = new DatagramSocket(UDP_PORT);
            System.out.println("Server started on TCP port " + TCP_PORT + " and UDP port " + UDP_PORT);
            TCPListener tcpListener = new TCPListener(serverSocket);
            UDPListener udpListener = new UDPListener(datagramSocket);
            tcpListener.start();
            udpListener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastTCPMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendTCPMessage(message);
            }
        }
    }

    private void broadcastUDPMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendUDPMessage(message);
        }
    }

    private void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private class TCPListener extends Thread {
        private ServerSocket serverSocket;

        public TCPListener(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket);
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class UDPListener extends Thread {
        private DatagramSocket datagramSocket;

        public UDPListener(DatagramSocket datagramSocket) {
            this.datagramSocket = datagramSocket;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(packet);
                    String request = new String(packet.getData(), 0, packet.getLength());

                    if (request.equals("GET_CLIENTS")) {
                        sendClientList(packet.getAddress(), packet.getPort());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                datagramSocket.close();
            }
        }

        private void sendClientList(InetAddress address, int port) {
            StringBuilder clientList = new StringBuilder();
            for (ClientHandler client : clients) {
                clientList.append(client.getClientName()).append("\n");
            }
            byte[] buffer = clientList.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            try {
                datagramSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private DatagramSocket datagramSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                datagramSocket = new DatagramSocket();

                clientName = in.readLine();
                broadcastTCPMessage(clientName + " entrou no chat.", this);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    broadcastTCPMessage(clientMessage, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                    removeClient(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendTCPMessage(String message) {
            out.println(message);
        }

        public void sendUDPMessage(String message) {
            try {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientSocket.getInetAddress(),
                        UDP_PORT);
                datagramSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ServerApp server = new ServerApp();
        server.start();
    }
}
