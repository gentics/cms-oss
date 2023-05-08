/*
 * @author herbert
 * @date May 24, 2007
 * @version $Id: PublishETAGraphGenerator.java,v 1.2 2007-08-17 10:37:12 norbert Exp $
 */
package com.gentics.contentnode.publish;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.gentics.lib.log.NodeLogger;

/**
 * A very simple class to generate graphs which display the accruacy of the ETA
 * calculation .. the CSV is created by {@link PublishETAStatisticGenerator}
 * @author herbert
 */
public class PublishETAGraphGenerator extends Thread {

	private PublishETAStatisticGenerator statsGenerator;

	private File file;

	private static NodeLogger logger = NodeLogger.getNodeLogger(PublishETAGraphGenerator.class);

	/**
	 * The width of the generated graph image.
	 */
	private int width = 300;

	/**
	 * The height of the generated graph image.
	 */
	private int height = 100;
    
	/**
	 * This is set in the construct - defines the milli seconds to wait between
	 * creating the graphic.
	 */
	private long delayMilliSeconds = 30 * 1000;

	/**
	 * Image used as the "Finish"-Line
	 */
	private BufferedImage finishImage;
    
	/**
	 * the line for finish and start line.
	 */
	private BufferedImage finishLineImage;
    
	private int GRAPH_TYPE_CIRCLES = 1;
	private int GRAPH_TYPE_LINES = 2;
    
	private int GRAPH_TYPE = GRAPH_TYPE_LINES;
    
	/**
	 * Defines if the graphic should be inverted...
	 * (by default - ist above, + below the middle line...)
	 */
	private static boolean invert = true;
    
	private static Color etaColor = new Color(150, 150, 255);

	public PublishETAGraphGenerator() {
		super("Publish ETA Graph Generator");
		try {
			finishImage = ImageIO.read(PublishETAGraphGenerator.class.getResourceAsStream("finish.png"));
			finishLineImage = ImageIO.read(PublishETAGraphGenerator.class.getResourceAsStream("finish_line.png"));
		} catch (IOException e) {
			logger.error("Error while loading image.", e);
		}
	}

	/**
	 * Creates a new instance to be used during a publish run. Simply call
	 * {@link #start()} afterwards to create a new graph every
	 * 'refreshIntervall' seconds.
	 * @param statsGenerator
	 * @param file
	 */
	public PublishETAGraphGenerator(PublishETAStatisticGenerator statsGenerator, File file,
			int refreshIntervall) {
		this();
		this.statsGenerator = statsGenerator;
		this.file = file;
		this.delayMilliSeconds = refreshIntervall * 1000;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new PublishETAGraphGenerator().drawImage(new File(args[0]), new File(args[1]));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void drawImage(File input, File output) throws IOException {
		List data = loadData(input);

		drawImage(data, output, false, true);
	}

	/**
	 * Draw a graph from the given data set and store the image
	 * into output.
	 * @param data
	 * @param output
	 * @param isInitialized 
	 * @throws IOException
	 */
	public void drawImage(List data, File output, boolean isDone, boolean isInitialized) throws IOException {
		// Offsets reserved for the caption
		int offsetx = 50;
		int offsety = 18;

		if (logger.isDebugEnabled()) {
			logger.debug("drawImage - isDone: {" + isDone + "} - isInitialized: {" + isInitialized + "}");
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);

		Graphics2D g = image.createGraphics();

		g.setFont(g.getFont().deriveFont(11f));

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		// g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		// g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		// g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        
		int rightMargin = 50;
		int bottomMargin = g.getFontMetrics().getAscent() + 2;

		g.setBackground(Color.WHITE);
		g.fillRect(0, 0, width, height);

		PublishETADataSet lastDataSet = (PublishETADataSet) data.get(data.size() - 1);
		long totaltime = lastDataSet.elapsed + lastDataSet.eta;
		double donePercent = isDone ? 100 : lastDataSet.percent;

		if (!isDone && (PublishWorkPhaseConstants.PHASE_NAME_INITIALIZATION.equals(lastDataSet.phase) || lastDataSet.elapsed < 2000)) {
			donePercent = 1;
		}
        
		GraphRenderingInformation info = new GraphRenderingInformation();

		info.isInitialized = isInitialized;

		drawCaption(g, totaltime, offsetx, offsety, rightMargin, bottomMargin);

		drawGraph(g, data, totaltime, offsetx, offsety, width - rightMargin, height - bottomMargin, donePercent, rightMargin, info);
        
		// draw caption of min/max y-values
		int minDeviation = (int) (info.minDeviation * info.timeFactor);
		int minDeviationValue = info.minDeviation;
		int maxDeviation = (int) (info.maxDeviation * info.timeFactor);
		int maxDeviationValue = info.maxDeviation;
        
		int fontHeight = g.getFontMetrics().getAscent(); {
			// if(minDeviation < -13) {
			if (invert) {
				minDeviation = -minDeviation;
			}
			String str = (invert ? "-" : "+") + timeToString(Math.abs(minDeviationValue));
			int width = g.getFontMetrics().stringWidth(str);

			g.drawString(str, offsetx + info.minDeviationX - width / 2, (invert ? height : fontHeight));
		} // if(maxDeviation > 15) {
		{
			if (invert) {
				maxDeviation = -maxDeviation;
			}
			String str = (invert ? "+" : "-") + timeToString(Math.abs(maxDeviationValue));
			int width = g.getFontMetrics().stringWidth(str);

			g.drawString(str, offsetx + info.maxDeviationX - width / 2, (invert ? fontHeight : height));
		}

		// Write the image
		ImageIO.write(image, "png", output);
	}

	/**
	 * Converts a given timestamp (ms) into a readable time string (hh:mm:ss - where hh: is only displaed with >0)
	 * @param timestamp
	 */
	private String timeToString(int timestamp) {
		if (timestamp < 0) {
			// We do not support negative values.
			return "0";
		}
		// i was to lazy to find a nicer solution using DateFormat or similar.. but . this works too ! ;)
		int secs = (int) Math.round(timestamp / 1000.);
		int mins = secs / 60;

		secs = secs % 60;
        
		int hrs = mins / 60;

		mins = mins % 60;
        
		StringBuffer buf = new StringBuffer();

		if (hrs > 0) {
			buf.append(hrs).append(':');
		}
		if (hrs > 0 && mins < 10) {
			buf.append('0');
		}
		buf.append(mins).append(':');
		if (secs < 10) {
			buf.append('0');
		}
		buf.append(secs);
		return buf.toString();
	}

	/**
	 * Draws the caption for the graph.
	 * @param g
	 * @param totaltime
	 * @param offsetx
	 * @param offsety
	 * @param rightMargin
	 * @param bottomMargin 
	 */
	private void drawCaption(Graphics2D g, long totaltime, int offsetx, int offsety, int rightMargin, int bottomMargin) {
		// TODO finish this.
		Font defaultFont = g.getFont();

		g.setColor(Color.BLACK);
		int centery = offsety + ((height - offsety - bottomMargin) / 2);
		int lineHeight = g.getFontMetrics().getAscent();
		String str = "0:00";
		int strWidth = g.getFontMetrics().stringWidth(str);

		g.drawString(str, offsetx - strWidth - 10, centery + (lineHeight / 2));
        
		String totalTimeString = timeToString((int) totaltime);

		g.drawString(totalTimeString, width - rightMargin + 10, centery + (lineHeight / 2) + 5);
        
		/*
		 AffineTransform fontAT = new AffineTransform();
		 
		 Font font = g.getFont();
		 
		 fontAT.rotate(90 * Math.PI / 180);
		 
		 Font derivedFont = font.deriveFont(fontAT);
		 g.setFont(derivedFont);
		 */
        
		g.drawImage(finishImage, width - rightMargin + 1, centery - finishImage.getHeight(), null);
		g.drawImage(finishLineImage, width - rightMargin + 1, centery, null);
        
		g.drawImage(finishLineImage, offsetx - finishLineImage.getWidth() - 1, centery - 2 * finishLineImage.getHeight(), null);
		g.drawImage(finishLineImage, offsetx - finishLineImage.getWidth() - 1, centery - finishLineImage.getHeight(), null);
		g.drawImage(finishLineImage, offsetx - finishLineImage.getWidth() - 1, centery, null);
		g.drawImage(finishLineImage, offsetx - finishLineImage.getWidth() - 1, centery + finishLineImage.getHeight(), null);
        
		g.setFont(defaultFont);
        
		g.setColor(etaColor);
		String etaString = "ETA";
        
		g.drawString(etaString, width - g.getFontMetrics().stringWidth(etaString), g.getFontMetrics().getAscent());
	}

	/**
	 * Draws the actual graph.
	 * @param g
	 * @param data
	 * @param totaltime
	 * @param offsetx
	 * @param offsety
	 * @param width
	 * @param height
	 * @param donePercent
	 * @param rightMargin
	 * @param info
	 * @throws IOException
	 */
	private void drawGraph(Graphics2D g, List data, long totaltime, int offsetx, int offsety,
			int width, int height, double donePercent, int rightMargin, GraphRenderingInformation info) throws IOException {

		Font font = g.getFont();

		// g.setColor(new Color(200,200,255));
		g.setColor(Color.GREEN);
		Stroke defaultStroke = g.getStroke();
		Stroke fatStroke = new BasicStroke(2);

		g.setStroke(fatStroke);

		int center = (int) (height - offsety) / 2;

		if (donePercent != 100) {
			int newWidth = (int) ((width - offsetx) / 100. * donePercent);

			g.drawLine(offsetx, offsety + center, offsetx + newWidth, offsety + center);
			g.setStroke(defaultStroke);
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(offsetx + newWidth, offsety + center, width, offsety + center);
            
			g.setStroke(fatStroke);
			g.setColor(Color.BLACK);
			g.drawImage(finishLineImage, offsetx + newWidth, offsety + center - 2 * finishLineImage.getHeight(), null);
			g.drawImage(finishLineImage, offsetx + newWidth, offsety + center - finishLineImage.getHeight(), null);
			g.drawImage(finishLineImage, offsetx + newWidth, offsety + center, null);
			g.drawImage(finishLineImage, offsetx + newWidth, offsety + center + finishLineImage.getHeight(), null);
			// g.drawLine(offsetx + newWidth, offsety + center - 5, offsetx + newWidth, offsety + center + 5);
			g.setStroke(defaultStroke);

			String percentString = ((int) donePercent) + "%";
			Rectangle2D stringBounds = font.getStringBounds(percentString, g.getFontRenderContext());

			g.drawString(percentString, offsetx + newWidth - (int) stringBounds.getCenterX(), offsety + center + (int) stringBounds.getHeight() + 5);
			// width = newWidth;
		} else {
			g.drawLine(offsetx, offsety + center, width, offsety + center);
			g.setStroke(defaultStroke);
		}

		if (!info.isInitialized) {
			g.setColor(Color.BLACK);
			return;
		}
		g.setColor(etaColor);

		double timeFactor = (double) (width - offsetx) / (double) totaltime;

		info.timeFactor = timeFactor;

		int maxSize = data.size();
		// Not using iterator here
		int lastx = -1;
		int lasty = -1;

		for (int i = 0; i < maxSize; i++) {
			PublishETADataSet dataSet = (PublishETADataSet) data.get(i);
			int deviation = (int) (dataSet.elapsed + dataSet.eta - totaltime);

			if (PublishWorkPhaseConstants.PHASE_NAME_INITIALIZATION.equals(dataSet.phase) || (dataSet.eta <= 1 && deviation < -100000) || i == 0) { // ETA is too short, let's say there was no deviation.
				deviation = 0;
			}
			int y = (int) ((deviation) * timeFactor);

			if (invert) {
				y = -y;
			}
			int x = (int) (dataSet.elapsed * timeFactor);
            
			// ((dataSet.percent - realPercent) * percentFactor);
            
			if (deviation < info.minDeviation || i == 5) {
				info.minDeviation = deviation;
				info.minDeviationX = x;
			}
			if (deviation > info.maxDeviation || i == 5) {
				info.maxDeviation = deviation;
				info.maxDeviationX = x;
			}
            
			if (y < -center) {
				y = -center;
			}
			if (y > center - 1) {
				y = center - 1;
			}
			int posX = offsetx + x;
			int posY = offsety + center + y;

			if (GRAPH_TYPE == GRAPH_TYPE_CIRCLES) {
				g.drawOval(posX, posY, 2, 2);
			} else if (GRAPH_TYPE == GRAPH_TYPE_LINES) {
				if (lastx != -1 && lasty != -1) {
					g.drawLine(lastx, lasty, posX, posY);
				}
				lastx = posX;
				lasty = posY;
			}
		}
		g.setColor(Color.BLACK);
	}

	/**
	 * Loads the data from a CSV file.
	 * @param input
	 * @throws IOException
	 */
	private List loadData(File input) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String line = null;
		List data = new ArrayList();

		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if ("".equals(line)) {
				continue;
			}
			data.add(new PublishETADataSet(line));
		}

		return data;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return this.width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return this.height;
	}

	public void run() {
		boolean done = false;
		File tmpFile = new File(file.getAbsolutePath() + ".new");
		File oldFile = new File(file.getAbsolutePath() + ".old");

		try {
			while (!done && !isInterrupted()) {
				if (tmpFile.exists()) {
					tmpFile.delete();
				}
				generateImage(tmpFile, oldFile);

				Thread.sleep(delayMilliSeconds);
			}
            
		} catch (InterruptedException e) {
			logger.debug("Thread was interrupted.", e);
		}
		// Create another image, to make sure we got the 100% image :)
		generateImage(tmpFile, oldFile);
	}

	private void generateImage(File tmpFile, File oldFile) {
		List data = statsGenerator.getPublishEtaStatistics();

		if (data != null && !data.isEmpty()){
			try {
				drawImage(data, tmpFile, statsGenerator.getRootPhase().isDone(), statsGenerator.getInitPhase().isDone());

				oldFile.delete();
				file.renameTo(oldFile);
				tmpFile.renameTo(file);
				oldFile.delete();
			} catch (IOException e) {
				logger.error("Error while writing statistic graph.", e);
			}
		}
	}

	public static class GraphRenderingInformation {
		public int maxDeviationX;
		public int minDeviationX;
		public double timeFactor;
		public int maxDeviation = Integer.MIN_VALUE;
		public int minDeviation = Integer.MAX_VALUE;
        
		public boolean isInitialized = false;
	}
}
