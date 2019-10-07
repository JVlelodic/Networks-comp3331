package lab02;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class PingClient {
    
    public static void main(String[] args) throws Exception{
        
        if (args.length != 2) {
            System.out.println("Required arguments: hostIP port");
            return;
        }
        
        String host = args[0]; 
        int port = Integer.parseInt(args[1]);
        
        DatagramSocket socket = new DatagramSocket();
        int i = 0; 
        
        InetAddress clientHost = InetAddress.getByName(host); 
        socket.setSoTimeout(1000);              

        while (i < 10) {
            
            long startTime = System.currentTimeMillis(); 
            String msg = "PING " + i + " " + startTime + " \r\n"; 
            byte[] buf = new byte[1024];
            buf = msg.getBytes();
            DatagramPacket send = new DatagramPacket(buf, buf.length, clientHost, port);
            socket.send(send);
            
            try {
                
                DatagramPacket response = new DatagramPacket(new byte[1024],1024);
                socket.receive(response); 
                
                long rtt = System.currentTimeMillis() - startTime; 
                System.out.println("ping to " + host + ", seq = " +
                i + ", rtt = " + rtt + " ms");
                
            }catch (SocketTimeoutException e){
                System.out.println("ping to " + host + ", seq = " +
                        i + ", rtt = Timeout"); 
            }
            i++; 
        }
        socket.close();
    }
}

