package com.github.nickpesce;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;

public class ScreenViewer implements ActionListener, WindowListener{

	public static final int NUM_LEDS = 60;
	private Robot r;
	private Sender sender;
	private boolean running;
	private Rectangle screenRect;
	private BufferedImage screen;
	private TrayIcon icon;
	private JFrame frame;
	private JButton bStart, bStop, bExit;
	private Thread thread;
	private String hostname;
	private int port;
	
	
	public static void main(String[] args) throws IOException
	{
		ScreenViewer sv = new ScreenViewer();
		sv.start();
	}
	
	/**
	 * constructs and starts a new screenviewer. Continiously 
	 * sends screen pixel data to a specified host.
	 * @throws IOException 
	 */
	public ScreenViewer() throws IOException
	{
		running = true;
		readConfig();
		sender = new Sender(hostname, port);
		screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		try {
			r = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		bStart = new JButton("Start");
		bStart.addActionListener(this);
		bStop = new JButton("Stop");
		bStop.addActionListener(this);
		bExit = new JButton("Exit");
		bExit.addActionListener(this);
		frame = new JFrame();
		frame.getContentPane().add(bStart, BorderLayout.WEST);
		frame.getContentPane().add(bStop, BorderLayout.CENTER);
		frame.getContentPane().add(bExit, BorderLayout.EAST);
		frame.pack();
		frame.addWindowListener(this);
		if(SystemTray.isSupported())
		{
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			try {
				String path;
				if(SystemTray.getSystemTray().getTrayIconSize().getHeight()>16)
					path="/NeopixelsTrayIcon32x32.png";
				else
					path = "/NeopixelsTrayIcon16x16.png";
				BufferedImage image = ImageIO.read(getClass().getResourceAsStream(path));
				icon = new TrayIcon(image);
				icon.setToolTip("NeoMirror");
				SystemTray.getSystemTray().add(icon);
				icon.addActionListener(this);
			} catch (AWTException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				stop();
                sender.close();
			}
		}));
	}
	
	/**
	 * Reads hostname, port, and password from the config file. makes config if needed.
	 * @throws IOException
	 */
	private void readConfig() throws IOException
	{
		File file = new File("config.txt");
		if(!file.exists())
		{
			createConfig(file);
		}
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while((line = reader.readLine()) != null)
		{
			//Does not support ipv6 and some urls
			String[] parts = line.split(" ");
			if(parts[0].equals("hostname"))
				hostname = parts[1].trim();
			else if(parts[0].equals("port"))
				port = Integer.parseInt(parts[1].trim());
		}
		reader.close();
	}
	
	private void createConfig(File file) throws IOException 
	{
		file.createNewFile();
		FileWriter writer = new FileWriter(file);
		writer.write("hostname MY.HOST.NAME\n");
		writer.write("port 12345\n");
		writer.close();
	}

	/**
	 * Get the screen pixel data, find averages, and send the data.
	 */
	public void update()
	{
		//screen = r.createScreenCapture(screenRect);
		byte[] leds = new byte[NUM_LEDS * 3];
		int[] pixel;
		int redBucket = 0;
		int greenBucket = 0;
		int blueBucket = 0;
		int pixelsInBucket = 0;
		double colWidth = ((double)screenRect.getWidth())/NUM_LEDS;
		for (int x = 0; x < 60; x++) {
			screen = r.createScreenCapture(new Rectangle((int)(x*colWidth), 0, 1, (int)screenRect.getHeight()));
		    for (int y = 0; y < screenRect.getHeight(); y++) {
		        //pixel = screen.getRaster().getPixel((int)(x*colWidth), y, new int[3]);
		        pixel = screen.getRaster().getPixel(0, y, new int[3]);

		        redBucket += pixel[0];
		        greenBucket += pixel[1];
		        blueBucket += pixel[2];
		        pixelsInBucket++;
		    }
		    leds[3*x] = (byte)(redBucket/pixelsInBucket);
		    leds[3*x + 1] = (byte)(greenBucket/pixelsInBucket);
		    leds[3*x + 2] = (byte)(blueBucket/pixelsInBucket);
		    pixelsInBucket = 0;
		    redBucket = 0;
		    blueBucket = 0;
		    greenBucket = 0;
		}
		sender.sendPixels(leds);
	}
	
	/**
	 * Starts the thread that analyzes the screen and sends the packets. 
	 * Continues until stop() is called
	 */
	private void start()
	{
		running = true;
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				long time = System.currentTimeMillis();
				while(running)
				{
					update();
					System.out.println(System.currentTimeMillis() - time);
					time = System.currentTimeMillis();
//					try {
//						Thread.sleep(50);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
				}
			}
		});
		thread.start();
	}
	
	/**
	 * Stops the program.
	 */
	private void stop()
	{
		running = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sender.sendOff();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		if(arg0.getSource().equals(icon))
			frame.setVisible(!frame.isVisible());
		else if(arg0.getSource().equals(bStart)){
			if(!running){
				start();
			}
		}
		else if(arg0.getSource().equals(bStop)){
			stop();
		}else if(arg0.getSource().equals(bExit)){
			stop();
			System.exit(0);
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		frame.setVisible(false);
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {		
	}

	@Override
	public void windowOpened(WindowEvent e) {		
	}
	
	

}
