import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;


public class ro1920recv {
    public static void main(String[] args) {
        int number = 0;
        
        try {
            File file = new File(args[0]);
            file.delete();

            OutputStream out = new FileOutputStream(file,true);
            InetSocketAddress addr = new InetSocketAddress("0.0.0.0", Integer.parseInt(args[1]));
            DatagramSocket serverSocket = new DatagramSocket(addr);
            boolean fin = false;
            while (!fin) {
                byte[] data = new byte[1472];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                
                serverSocket.receive(packet);
                
    
                int received = getPkgNumber(packet.getData());
                if (received == number) {
                    ByteBuffer bb = ByteBuffer.allocate(packet.getData().length);
                    bb.put(packet.getData());
                    byte[] ack = new byte[18];
                   // bb.flip();
                    bb.position(0);
                    bb.get(ack);
                    InetAddress routerIP = packet.getAddress();
                    Integer portRouter = packet.getPort();
                    DatagramPacket ackP = new DatagramPacket(ack,0, ack.length, routerIP, portRouter);
                    serverSocket.send(ackP);
                    bb.position(18);
                    short leidos = bb.getShort();
                    //findfinal(bb);
                    if(leidos < 1452){    
                        serverSocket.send(ackP);
                        serverSocket.send(ackP);
                        fin = true;
                    }

                    byte[] save = new byte[leidos];
                    bb.position(20);
                    bb.get(save);
                    out.write(save);
                    
                    number++;
                    // ByteBuffer ack = headerMaker(dest, portDest, timestamp, received);
                    
                } else {
                    ByteBuffer bb = ByteBuffer.allocate(packet.getData().length);
                    bb.put(packet.getData());
                    bb.flip();

                    InetAddress routerIP = packet.getAddress();
                    Integer portRouter = packet.getPort();
                    

                    byte[] ack = new byte[18];
                    bb.position(0);
                    bb.get(ack);
                    DatagramPacket ackP = new DatagramPacket(ack,0, ack.length, routerIP, portRouter);
                    serverSocket.send(ackP);

                }
            }
            
            out.close();

        } catch (Exception e) {
            //TODO: handle exception
        }
    }
    
    public static short getPort(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.flip();
        bb.position(4);
        short port = bb.getShort();
        return port;

    }
    public static InetAddress getAddress(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.flip();
        byte[] add = new byte[4];
        bb.get(add);
        InetAddress inetAd = null;
        try {
            inetAd = InetAddress.getByAddress(add);
        } catch (Exception e) {
            //TODO: handle exception
        }
        return inetAd;

    }
    public static long getTimeStamp(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.flip();
        bb.position(6);
        long ts = bb.getLong();
        return ts;

    }
    

    public static int getPkgNumber(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.flip();
        bb.position(14);
        int number = bb.getInt();
        return number;

    }
    public static byte[] packageConstructor(ByteBuffer header,byte[] file){
        ByteBuffer bb = ByteBuffer.allocate(1472);
        header.flip();
        bb.put(header);
        bb.put(file);
        byte[] data = bb.array();
        return data;
    }
    public static ByteBuffer headerMaker(InetAddress serverIP,int serverPort,long timestamp,int number){
        ByteBuffer header = ByteBuffer.allocate(18);
        header.put(serverIP.getAddress());
        
        header.putShort((short)serverPort);
        header.putLong(timestamp);
        header.putInt(number);
        return header;
    }


}