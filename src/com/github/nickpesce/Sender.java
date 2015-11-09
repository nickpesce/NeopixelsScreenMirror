package com.github.nickpesce;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Sender {
	private InetAddress host;
	private int port;
	private DatagramSocket socket;
	
	/**
	 * Constructs a Sender object. It opens a socket and allows sending of pixel data.
	 * @param hostName The host name to open the socket to
	 * @param port The port to send to.
	 */
	public Sender(String hostName, int port) {
		this.port = port;
		 try
         {
             socket = new DatagramSocket(port);
             host = InetAddress.getByName(hostName);
         } catch (SocketException | UnknownHostException e)
         {
             e.printStackTrace();
         }
	}
	
	/**
	 * Sends a packet with the specified text to the host:port defined in the constructor.
	 * Packet is a UDP datagram
	 * @param text The message to send
	 */
	private void sendPacket(final String text)
    {
        new Thread(new Runnable(){
            public void run() {
                try {
                    byte[] buf = text.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, host, port);
                    socket.send(packet);
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }
	
	/**
	 * Close the connection
	 */
	public void close()
	{
		socket.close();
	}

	/**
	 * Converts and send an array of pixel arrays as a command string to the server.
	 * @param leds The array of rgb arrays to send.
	 */
	public void sendPixels(int[][] leds) {
		StringBuilder command = new StringBuilder("each -c ");
		for(int[] led : leds)
		{
			command.append("(").append(led[0]).append(",").append(led[1]).append(",").append(led[2]).append(");");
		}
		command.deleteCharAt(command.length()-1);
		sendPacket(command.toString());
				
	}
}
