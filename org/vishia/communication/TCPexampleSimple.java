package org.vishia.communication;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPexampleSimple
{

  byte[] serverAddrBytes = {127,0,0,1};
  
  InetAddress serverAddr;
  
  ServerSocket serverSo;
  
  Socket s2;
  
  boolean bRun = true;
  
  TCPexampleSimple() {
    try {
      serverAddr = InetAddress.getByAddress(serverAddrBytes);
      serverSo = new ServerSocket(0xae00, 10, serverAddr);
    } catch (UnknownHostException e) {
      System.out.println(e.toString());
    } catch (IOException e) {
      System.out.println(e.toString());
    }
      
  }
  
  
  void openAndAccept() {
    do {
      try {
        Socket clientSo = serverSo.accept();
        ServiceForClient srv = new ServiceForClient();
        srv.soToClient = clientSo;
        Thread threadToClient = new Thread(srv);
        threadToClient.start();
          
      } catch (IOException e) {
        System.out.println(e.toString());
      }
    } while(bRun);
  }
  
  
  
  static class ServiceForClient implements Runnable {

    Socket soToClient;
    
    
    ServiceForClient(){}
    
    void setSocket(Socket so) {
      soToClient = so;
    }
    
    
    @Override public void run()
    {
      try {
        BufferedInputStream inStream = new BufferedInputStream(soToClient.getInputStream());
        OutputStream out = soToClient.getOutputStream();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        writeOutput(in);  
      } catch(IOException exc) {
        System.out.println(exc);
      }
      //
      try {
        soToClient.close();
      } catch (IOException exc) {
        System.out.println(exc);
      }
      
    }
    
    
    void writeOutput(BufferedReader in) throws IOException
    {
      boolean bCont = true;
      do {
        String line = in.readLine();
        if(line == null) {
          bCont = false;
        }
        System.out.println(line);
        //System.in.
        
      } while(bCont);
      System.out.println("==Client has closed. ==");
      
    }
    
    
    
  }
  
  
  
  public static void main(String[] args) {
  
    TCPexampleSimple main = new TCPexampleSimple();
    main.openAndAccept();
  
  
  }
  
  
}
