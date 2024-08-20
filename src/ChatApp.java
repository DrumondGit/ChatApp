import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

public class ChatApp {

    private MulticastSocket mSocket;
    private InetAddress groupIp;
    private InetSocketAddress group;
    private String userName;
    private volatile boolean running = true;

    public ChatApp(String userName, String groupAddress) throws IOException {
        this.userName = userName;
        groupIp = InetAddress.getByName(groupAddress);
        group = new InetSocketAddress(groupIp, 6789);
        mSocket = new MulticastSocket(6789);
        mSocket.joinGroup(group.getAddress());
        sendMessage(userName + " entrou na sala");
    }

    public void sendMessage(String message) {
        if (mSocket == null || mSocket.isClosed()) {
            return;
        }
        try {
            byte[] messageBytes = message.getBytes();
            DatagramPacket messageOut = new DatagramPacket(messageBytes, messageBytes.length, groupIp, 6789);
            mSocket.send(messageOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessages() {
        byte[] buffer = new byte[1000];
        while (running) {
            try {
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                mSocket.receive(messageIn);
                System.out.println("Recebido: " + new String(messageIn.getData()).trim());
                buffer = new byte[1000];
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public boolean leaveRoom() {
        if (mSocket == null || mSocket.isClosed()) {
            return false;
        }
        try {
            sendMessage(userName + " saiu da sala");
            mSocket.leaveGroup(group.getAddress());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (mSocket != null && !mSocket.isClosed()) {
                mSocket.close();
            }
        }
    }


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Instruções de uso:");
            System.out.println("Digite no terminal: cd .\\out\\production\\ChatApp\\");
            System.out.println("Uso: java ChatApp <nome> <endereço multicast>");
            return;
        }

        ChatApp chatApp = null;
        try {
            chatApp = new ChatApp(args[0], args[1]);

            ChatApp finalChatApp1 = chatApp;
            Thread receiveThread = new Thread(() -> finalChatApp1.receiveMessages());
            receiveThread.start();

            Scanner scanner = new Scanner(System.in);
            String message;
            while (true) {
                if (scanner.hasNextLine()) {
                    message = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(message)) {
                        break;
                    }
                    chatApp.sendMessage(chatApp.userName + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (chatApp != null) {
                chatApp.running = false;
                ChatApp finalChatApp = chatApp;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> finalChatApp.leaveRoom()));
                try {
                    chatApp.leaveRoom();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
