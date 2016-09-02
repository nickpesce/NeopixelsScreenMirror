package com.github.nickpesce;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Sender {
	private String hostname;
	private DatagramSocket socket;
	private InetAddress host;
	private int port;
	
	/**
	 * Constructs a Sender object. It opens a socket and allows sending of pixel data.
	 * @param hostName The host name to open the socket to
	 * @param port The port to send to.
	 * @throws UnknownHostException 
	 * @throws SocketException 
	 */
	public Sender(String hostName, int port) throws UnknownHostException, SocketException {
		this.port = port;
		this.hostname = hostName;
		host = InetAddress.getByName(hostname);
		socket = new DatagramSocket();
	}
	
	/**
	 * Sends a packet with the specified byte array to the host:port defined in the constructor.
	 * @param byte array in the form [r1, g1, b1, r2, g2, b2, ..., rn, gb, bn]
	 */
	public void sendPixels(final byte[] leds)
    {
        new Thread(new Runnable(){
            public void run() {
                	DatagramPacket packet = new DatagramPacket(leds, leds.length, host, port);
                    //Writes the led data to the socket output stream
                	try {
						socket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}
            }
        }).start();
    }
	
	public void sendOff()
	{
		sendPixels(new byte[ScreenViewer.NUM_LEDS]);
	}
	
	public void close() {
        socket.close();
	}
}
