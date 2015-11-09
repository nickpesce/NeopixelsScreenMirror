package com.github.nickpesce;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class ScreenViewer{

	private Robot r;
	private Sender sender;
	private boolean running;
	private Rectangle screenRect;
	private BufferedImage screen;
	
	public static void main(String[] args)
	{
		ScreenViewer sv = new ScreenViewer();
	}
	
	public ScreenViewer()
	{
		running = true;
		sender = new Sender("nickspi.student.umd.edu", 42297);
		screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		try {
			r = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		while(running)
		{
			update();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		sender.close();
	}
	
	public void update()
	{
		screen = r.createScreenCapture(screenRect);
		int[][] leds = new int[60][3];
		int[] pixel;
		int redBucket = 0;
		int greenBucket = 0;
		int blueBucket = 0;
		int pixelsInBucket = 0;
		double colWidth = screen.getWidth()/60.0;
		for (int x = 0; x < 60; x++) {
		    for (int y = 0; y < screen.getHeight(); y++) {
		        pixel = screen.getRaster().getPixel((int)(x*colWidth), y, new int[3]);
		        redBucket += pixel[0];
		        greenBucket += pixel[1];
		        blueBucket += pixel[2];
		        pixelsInBucket++;
		    }
		    leds[x] = new int[]{redBucket/pixelsInBucket, greenBucket/pixelsInBucket, blueBucket/pixelsInBucket};
		    pixelsInBucket = 0;
		    redBucket = 0;
		    blueBucket = 0;
		    greenBucket = 0;
		}
		sender.sendPixels(leds);
	}
	
	

}
