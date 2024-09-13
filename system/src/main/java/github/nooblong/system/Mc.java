package github.nooblong.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import github.nooblong.download.utils.OkUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Component
@Slf4j
public class Mc {

    public static final String TOKEN_URL = "https://bots.qq.com";
    public static final String BASE_URL = "https://api.sgroup.qq.com";
    public static OkHttpClient client = new OkHttpClient();
    public static ObjectMapper mapper = new ObjectMapper();
    public static String accessToken = null;

    public static void main(String[] args) throws IOException, InterruptedException {
//        Mc.StatusResponse(description=Mc.Description(text=All the Mods 9),
//        players=Mc.Players(max=20, online=1, sample=[Mc.Player(name=nooblong, id=aa3d2f33-39dd-3074-b676-c243b240a809)]),
//        version=Mc.Version(name=1.20.1, protocol=763),
//        favicon=data:image/png;base64,it/==,
//        time=0)

        for (int i = 0; i < 100; i++) {
            Mc mc = new Mc();
            mc.setHost(new InetSocketAddress("cn-sz-yd-plustmp2.natfrp.cloud", 21942));
            StatusResponse statusResponse = mc.fetchData();
            System.out.println(statusResponse);
            mc.updateAccessToken();
            mc.sendMsg("hello world");
            Thread.sleep(50000);
        }
    }

    public String getAccessToken() {
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("appId", "???");
        objectNode.put("clientSecret", "???");
        Request post = OkUtil.post(objectNode, TOKEN_URL + "/app/getAppAccessToken");
        JsonNode jsonResponse = OkUtil.getJsonResponse(post, client);
        System.out.println(jsonResponse.toPrettyString());
        return jsonResponse.get("access_token").asText();
    }

    public void sendMsg(String text) {
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("content", text);
        objectNode.put("msg_type", 0);
        HashMap<String, String> header = new HashMap<>();
        header.put("Authorization", "QQBot " + accessToken);
        Request post = OkUtil.post(objectNode, BASE_URL + "/v2/users/" + "292862284" + "/messages", header);

        JsonNode jsonResponse = OkUtil.getJsonResponse(post, client);
        System.out.println(jsonResponse.toPrettyString());
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void check() {

    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void updateAccessToken() {
        accessToken = getAccessToken();
    }

    private InetSocketAddress host;
    private int timeout = 7000;
    private Gson gson = new Gson();
    public Map<String, Date> playTime = new HashMap<>();

    public int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    public void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public StatusResponse fetchData() throws IOException {

        Socket socket = new Socket();
        OutputStream outputStream;
        DataOutputStream dataOutputStream;
        InputStream inputStream;
        InputStreamReader inputStreamReader;

        socket.setSoTimeout(this.timeout);

        socket.connect(host, timeout);

        outputStream = socket.getOutputStream();
        dataOutputStream = new DataOutputStream(outputStream);

        inputStream = socket.getInputStream();
        inputStreamReader = new InputStreamReader(inputStream);

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(b);
        handshake.writeByte(0x00); //packet id for handshake
        writeVarInt(handshake, 4); //protocol version
        writeVarInt(handshake, this.host.getHostString().length()); //host length
        handshake.writeBytes(this.host.getHostString()); //host string
        handshake.writeShort(host.getPort()); //port
        writeVarInt(handshake, 1); //state (1 for handshake)

        writeVarInt(dataOutputStream, b.size()); //prepend size
        dataOutputStream.write(b.toByteArray()); //write handshake packet


        dataOutputStream.writeByte(0x01); //size is only 1
        dataOutputStream.writeByte(0x00); //packet id for ping
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int size = readVarInt(dataInputStream); //size of packet
        int id = readVarInt(dataInputStream); //packet id

        if (id == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (id != 0x00) { //we want a status response
            throw new IOException("Invalid packetID");
        }
        int length = readVarInt(dataInputStream); //length of json string

        if (length == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (length == 0) {
            throw new IOException("Invalid string length.");
        }

        byte[] in = new byte[length];
        dataInputStream.readFully(in);  //read json string
        String json = new String(in);


        long now = System.currentTimeMillis();
        dataOutputStream.writeByte(0x09); //size of packet
        dataOutputStream.writeByte(0x01); //0x01 for ping
        dataOutputStream.writeLong(now); //time!?

        readVarInt(dataInputStream);
        id = readVarInt(dataInputStream);
        if (id == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (id != 0x01) {
            throw new IOException("Invalid packetID");
        }
        long pingtime = dataInputStream.readLong(); //read response

        StatusResponse response = gson.fromJson(json, StatusResponse.class);
        response.setTime((int) (now - pingtime));

        dataOutputStream.close();
        outputStream.close();
        inputStreamReader.close();
        inputStream.close();
        socket.close();

        return response;
    }

    @Data
    public static class Description {
        String text;
    }

    @Data
    public static class StatusResponse {
        private Description description;
        private Players players;
        private Version version;
        private String favicon;
        private int time;

    }

    @Data
    public static class Players {
        private int max;
        private int online;
        private List<Player> sample;

    }

    @Data
    public static class Player {
        private String name;
        private String id;

    }

    @Data
    public static class Version {
        private String name;
        private String protocol;

    }
}
