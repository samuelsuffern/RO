import java.io.FileInputStream;
import java.io.File;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ro1920send{
    private  static long rto = 100;
    private  static long rtt = 0;
    private  static long ortt = 0;
    public static void main(String[] args){
        File file = new File(args[0]);
        int serverPort = 0;
        int portRouter = 0;
        int number = 0;

        /*
                          servidor        servidor          router           router
            input_file      dest_ip     dest_port       emulator_IP     emulator_port 

        */ 
        try {
            portRouter = Integer.parseInt(args[4]);
            serverPort = Integer.parseInt(args[2]);
            InputStream in = new FileInputStream(file);
            InetAddress routerIP= InetAddress.getByName(args[3]);
            InetAddress serverIP = InetAddress.getByName(args[1]);
    
            int leidos = 0;
            DatagramSocket ds = new DatagramSocket();
            long inicio = System.currentTimeMillis();
            do{
              ds.setSoTimeout((int)ro1920send.rto);
              byte[] bytes = new byte[1452];
              
              leidos = in.read(bytes);
              
              if(!(leidos == -1)){
                  boolean acked=false;
                  while(!acked){
                      long timestamp = System.currentTimeMillis(); // cojo timestamp para preparar el paquete
                      
                      System.out.print("Inicio tx: ");
                        ByteBuffer header = headerMaker(serverIP, serverPort, timestamp,number,leidos); // construyo cabecera
                        byte[] data = packageConstructor(header,bytes, leidos);  // cabecera + datos = paquete

                        DatagramPacket pkg = new DatagramPacket(data,0, data.length, routerIP, portRouter); // creo paquete

                        System.out.println("Envio paquete: "+number);

                       ds.send(pkg);
                       
                       byte[] ack = new byte[18]; // preparo buffer para recibir ack
                       DatagramPacket ackpkg = new DatagramPacket(ack,ack.length);
                       try {
                           ds.receive(ackpkg);
                           int acknumber = getPkgNumber(ackpkg.getData()); // cojo el nº de ack
        
                            acked=true;
                           if(acknumber == number){  // coincide con paquete esperado
                            long aRTT = getRTT(ackpkg.getData()); // aRTT = horaActual - timeStamp;
                            if(acknumber == 0){                 // paquete inicial
                                ro1920send.rtt = aRTT;          
                                ro1920send.ortt = aRTT/2;
                            }else{                              // paquete k-esimo
                               ro1920send.rtt = (1-1/8)*ro1920send.rtt + (1/8)*aRTT;
                               ro1920send.ortt = (1-1/4)*ro1920send.ortt + (1/4)*Math.abs((ro1920send.rtt-aRTT));
                            }
            
                            ro1920send.rto = ro1920send.rtt + 4*ro1920send.ortt;
                               number++;   // paquete recibido, incremento numero de paquete-
                            System.out.println();
                           }
                       } catch (Exception e) {
                           //TODO: handle exception
                           //System.out.println(e);
                           acked=false;
                       }
                   }

               }
            }while(leidos != -1);
            in.close();       
            long finalt = System.currentTimeMillis();
            long tiempo = (finalt-inicio)/1000;
            System.out.println("Tiempo: "+ tiempo) ;
            
            
            
        } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
        }


        





    }
    public static long getRTT(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.flip();
        bb.position(6);
        long number = bb.getLong();
        long resta =(System.currentTimeMillis() - number);
        //System.out.println("hora de confirmacion" + System.currentTimeMillis()+ " hora que se envio: "+number + " RESTA : " + resta);
        return resta;

    }
    public static int getPkgNumber(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.flip();
        bb.position(14);
        int number = bb.getInt();
        return number;

    }
    public static byte[] packageConstructor(ByteBuffer header,byte[] file, int leidos){
        ByteBuffer bb = ByteBuffer.allocate(leidos+20);
        
        header.flip();
        bb.put(header);
        bb.put(file,0,leidos);
        byte[] data = bb.array();
        return data;
    }
    public static ByteBuffer headerMaker(InetAddress serverIP,int serverPort,long timestamp,int number,int leidos){
        ByteBuffer header = ByteBuffer.allocate(20);
        header.put(serverIP.getAddress());
        header.putShort((short)serverPort);
        header.putLong(timestamp);
        header.putInt(number);
        header.putShort((short)leidos);
        
        return header;
    }

 
}