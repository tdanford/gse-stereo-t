package edu.mit.csail.cgs.sigma.blots;

import java.awt.*;
import java.awt.image.*;

public interface ImageIntensity {
	public int intensity(BufferedImage im, Point p);
	public int getRange();
}

class DefaultImageIntensity implements ImageIntensity {
	
	public DefaultImageIntensity() { 
	}

	public int getRange() {
		return 256;
	}

	public int intensity(BufferedImage im, Point p) {
		int x = p.x, y = p.y;
		ColorModel m = ColorModel.getRGBdefault();
		int rgb = im.getRGB(x, y);
		int r = m.getRed(rgb);
		int g = m.getGreen(rgb);
		int b = m.getBlue(rgb);
		
		return (int)Math.round((double)(r + g + b) / 3.0);
	} 
}
