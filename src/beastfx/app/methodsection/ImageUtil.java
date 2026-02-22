package beastfx.app.methodsection;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import beast.base.core.Log;
import beast.pkgmgmt.BEASTClassLoader;


public class ImageUtil {

	
	public static String getIcon(String iconLocation, String type) {
	    try {
	        URL url = BEASTClassLoader.classLoader.getResource(iconLocation);
	        if (url == null) {
	            return null;
	        }
	        ImageIcon icon = new ImageIcon(url);
	        BufferedImage image = new BufferedImage(
	        	    icon.getIconWidth(),
	        	    icon.getIconHeight(),
	        	    BufferedImage.TYPE_INT_RGB);
        	Graphics g = image.createGraphics();
        	icon.paintIcon(null, g, 0,0);
        	g.dispose();
	        return encodeToString(image, type);
	    } catch (Exception e) {
	    	Log.warning.println("Cannot load icon " + iconLocation + " " + e.getMessage());
	        return null;
	    }
	}
	
	/**
     * Encode image to Base64 string
     * @param image The image to Base64 encode
     * @param type jpeg, bmp, ...
     * @return Base64 encoded string
     */
    public static String encodeToString(BufferedImage image, String type) {
        String imageString = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();        
        try {
            ImageIO.write(image, type, bos);
            byte[] imageBytes = bos.toByteArray();

            Encoder encoder = Base64.getEncoder();
            imageString = encoder.encodeToString(imageBytes);

            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
    }

}
