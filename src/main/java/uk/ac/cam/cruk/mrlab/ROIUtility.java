package uk.ac.cam.cruk.mrlab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.Duplicator;
import ij.plugin.RoiScaler;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class ROIUtility {

	
	protected static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	
	protected static Color panelColor = new Color(204, 229, 255);
	protected String frameName = "Cell Counter";
	//public Color panelColor = new Color(255, 204, 229);
	//protected static Dimension panelMax = new Dimension(600, 400);
	//protected static Dimension numberMax = new Dimension(100, 20);
	//protected static JTextArea spotCount;
	
	protected static ImagePlus imp = null;
	protected static Roi roi = null;

	public static void addROIPanel(JPanel parentPanel) {
		
		panelColor = GUIUtility.panelColor;
		
		imp = WindowManager.getCurrentImage();
		
		//PlugInFrame pf = new PlugInFrame(frameName);
		//parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));

		// create and configure the content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(border);
		contentPanel.setBackground(panelColor);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

		
		//JPanel buttonPanel = new JPanel();
		JButton btnAutoRoi = new JButton("auto ROI");
		JButton btnRoiSave = new JButton("save ROI");
		JButton btnRoiHide = new JButton("show/hide ROI");
		JButton btnRoiAdd = new JButton("add ROI");
		JButton btnRoiDel = new JButton("delete ROI");

		contentPanel.add(btnAutoRoi);
		contentPanel.add(btnRoiSave);
		contentPanel.add(btnRoiHide);
		contentPanel.add(btnRoiAdd);
		contentPanel.add(btnRoiDel);

		
		//buttonPanel.add(btnFilter);
		//buttonPanel.setBorder(border);
		//buttonPanel.setBackground(panelColor);
		//buttonPanel.setMaximumSize(panelMax);

		//contentPanel.add(buttonPanel);
		
		// configure button
		btnAutoRoi.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
			  imp = WindowManager.getCurrentImage();
			  if (imp==null) return;
			  roi = getRoi(imp, 
					  DetectionUtility.autoROIChannel, 
					  DetectionUtility.autoMethods[DetectionUtility.autoROIMethod], 
					  DetectionUtility.autoROIColor);
			  imp.setRoi(roi);
		  	}
		});
		// configure button
		btnRoiSave.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
			  imp = WindowManager.getCurrentImage();
			  if (imp==null) return;
			  roi = imp.getRoi();
		  	}
		});
		// configure button
		btnRoiHide.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
			  imp = WindowManager.getCurrentImage();
			  if (imp==null) return;
			  if (imp.getRoi()==null) imp.setRoi(roi);
			  else imp.deleteRoi();
		  	}
		});
		//
		btnRoiAdd.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
			  imp = WindowManager.getCurrentImage();
			  if (imp==null) return;
			  Roi oldRoi = imp.getRoi();
			  if (!oldRoi.isArea()) return;
			  roi = new ShapeRoi(roi);
			  roi = ((ShapeRoi) roi).or(new ShapeRoi(oldRoi));
			  roi.setStrokeColor(DetectionUtility.autoROIColor);
			  imp.setRoi(roi);
		  	}
		});
		//
		btnRoiDel.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
			  imp = WindowManager.getCurrentImage();
			  if (imp==null) return;
			  Roi oldRoi = imp.getRoi();	  		
			  if (!oldRoi.isArea()) return;	  		
			  roi = new ShapeRoi(roi);				
			  roi = ((ShapeRoi) roi).not(new ShapeRoi(oldRoi));
			  roi.setStrokeColor(DetectionUtility.autoROIColor);
			  imp.setRoi(roi);
		  	}
		});
		
		
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		parentPanel.add(contentPanel);
		parentPanel.validate();
		parentPanel.revalidate();

	}
	
	public static Roi getRoi(ImagePlus impOri, int channel, String method, Color roiColor) {
		int[] dims = impOri.getDimensions();
		if (dims[2]<channel) return null;
		
		int width =dims[0]; int height = dims[1];
		double xScale = Math.round((double)width/(double)1000);
		double yScale = Math.round((double)height/(double)1000);
		
		int posC = channel;
		int posZ = impOri.getZ();
		int posT = impOri.getT();

		Roi oriRoi = impOri.getRoi(); impOri.deleteRoi();
		ImagePlus imp = new Duplicator().run(impOri, posC, posC, posZ, posZ, posT, posT);
		impOri.setRoi(oriRoi);
		
		int widthDownSized = width/(int)xScale;
		int heightDownSized = height/(int)yScale;
		
		IJ.run(imp, "Size...", "width=["+ widthDownSized +"] height=["+ heightDownSized +"] depth=1 average interpolation=Bilinear");
		IJ.run(imp, "8-bit", "");
		imp.getProcessor().setAutoThreshold(method, true, ImageProcessor.NO_LUT_UPDATE);
		ByteProcessor bp = imp.createThresholdMask();
		ImagePlus mask = new ImagePlus("mask", bp);
		IJ.run(mask, "Median...", "radius=10");
		IJ.run(mask, "Fill Holes", "");
		mask.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true, ImageProcessor.NO_LUT_UPDATE);
		Roi maskRoi = ThresholdToSelection.run(mask);
		if (maskRoi==null) return null;
		Roi newRoi = RoiScaler.scale(maskRoi, xScale, yScale, false);
		
		newRoi.setStrokeColor(DetectionUtility.autoROIColor);

		imp.close(); mask.close();
		System.gc();
		
		return newRoi;
	}
	
	public static Roi trimBorder (Roi roi, ImagePlus imp, double borderSize) { // might be slow!!!
		int width = imp.getWidth(); int height = imp.getHeight();
		ShapeRoi imageBound = new ShapeRoi(new Roi(borderSize, borderSize, width-2*borderSize, height-2*borderSize));
		roi = new ShapeRoi(roi);
		roi = ((ShapeRoi) roi).and(imageBound);
		return roi;
	}
	
}
