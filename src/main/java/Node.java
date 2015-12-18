import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.kotlin.ExtensionsKt;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by igushs on 12/19/15.
 */
public class Node {

    public static final int MULTICAST_PORT = 12345;
    public static final String MULTICAST_HOST_NAME = "230.1.1.1";

    private final InetAddress multicastAddress;

    private ObjectMapper mapper = ExtensionsKt.jacksonObjectMapper()
            .registerModule(new KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)
            .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

    private final Board board = new Board();

    public Board getBoard() {
        return board;
    }

    private final MulticastSocket receiveSocket;
    private final MulticastSocket sendSocket;
    private final Consumer<VersionedMessage> onVersionedMessageListener;

    public Node(Consumer<VersionedMessage> onVersionedMessageListener) throws IOException {
        this.onVersionedMessageListener = onVersionedMessageListener;
        multicastAddress = InetAddress.getByName(MULTICAST_HOST_NAME);
        receiveSocket = new MulticastSocket(MULTICAST_PORT);
        sendSocket = new MulticastSocket();
    }

    private void joinOnAllIfaces() {
        try {
            InetSocketAddress inetSocketAddr = new InetSocketAddress(
                    MULTICAST_HOST_NAME,
                    MULTICAST_PORT
            );
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                receiveSocket.joinGroup(inetSocketAddr, iface);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Set<Message> sendQueue = Collections.synchronizedSet(new LinkedHashSet<Message>());

    public void sendMessage(Message msg) {
        sendQueue.add(msg);
        if (msg instanceof VersionedMessage) {
            board.setVersion(Math.max(board.getVersion(), ((VersionedMessage) msg).getVersion()));
        }
    }

    private void handleMessage(Message msg) {
        if (msg instanceof VersionedMessage && !board.saveMessage((VersionedMessage) msg)) {
            return;
        }

        System.out.println("--> " + msg);

        if (sendQueue.contains(msg)) {
            sendQueue.remove(msg);
        }

        if (msg instanceof GetMessage) {
            ((GetMessage) msg).getVersions()
                    .stream()
                    .map(it -> board.getMessages().get(it))
                    .filter(it -> it != null)
                    .flatMap(Collection::stream)
                    .forEach(this::sendMessage);
        } else if (msg instanceof GetAllMessage) {
            board.getMessages().values().stream().flatMap(Collection::stream).forEach(this::sendMessage);
        } else if (msg instanceof VersionedMessage) {
            onVersionedMessageListener.accept((VersionedMessage) msg);
        }
    }

    public void start() {
        joinOnAllIfaces();

        sendMessage(new GetAllMessage());

        new Thread(() -> { //receive loop
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[81920], 81920);
                    receiveSocket.receive(packet);
                    Message msg = mapper.readValue(packet.getData(), Message.class);
                    handleMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> { //send messages loop
            while (true) {
                ArrayList<Message> queueAsList = new ArrayList<>(sendQueue);
                if (queueAsList.size() > 0) {
                    Message msg = queueAsList.get(0);
                    System.out.println("<-- " + msg);
                    sendQueue.remove(msg);
                    try {
                        byte[] bytes = mapper.writeValueAsBytes(msg);
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                        packet.setAddress(multicastAddress);
                        packet.setPort(MULTICAST_PORT);
                        try {
                            sendSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();

        new Thread(() -> { //check for missed versions
            while (true) {
                if (!board.getMissedVersions().isEmpty()) {
                    sendMessage(new GetMessage(new ArrayList<Long>(board.getMissedVersions())));
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }
}
