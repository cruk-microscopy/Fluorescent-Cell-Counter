package uk.ac.cam.cruk.mrlab;


import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JPanel;

import org.scijava.prefs.DefaultPrefService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

import ij.ImagePlus;
import ij.CompositeImage;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.process.LUT;


public class DetectionUtility {
	
	protected static ImagePlus sourceImage;
	protected static Overlay spotOverlay;
	
	protected static final String[] autoMethods = {"Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError(I)", "Minimum",
			"Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"};
	
	protected static int red = 255;
	protected static int green = 255;
	protected static int blue = 0;
	protected static int alpha = 255;
	protected static Color customColor = new Color(red, green, blue, alpha);
	protected static final Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.GRAY, Color.WHITE, Color.BLACK, customColor};
	protected static final String[] colorStrings = {"RED", "GREEN", "BLUE", "MAGENTA", "CYAN", "YELLOW", "GRAY", "WHITE", "BLACK", "custom"};
	protected static String[] channelStrings = {"C1"};
	protected static final String[] roiShapes = {"circle", "point"};
	
	protected static int autoROIChannel = 2;
	protected static int autoROIMethod = 13;
	protected static int autoROIColorIndex = 5;
	protected static Color autoROIColor = colors[autoROIColorIndex];
	protected static int[] channelColors = {4, 1, 0, 2, 3, 5, 6, 7, 8, 9}; // default color setting: cyan, green, red	
	protected static int spotColorIndex = 3;
	protected static Color spotColor = colors[spotColorIndex];
	protected static double spotAlpha = 1.0;
	protected static int spotShape = 0;
	
	// spot detection parameters
	protected static int targetChannel = 1;
	protected static double spotRadius = 4.0d; // radius in calibration unit (e.g.: micron)
	protected static double qualityThreshold = 40;
	protected static SpotCollection tmSpots;
	
	protected static double[][] spotMean;
	protected static double[][] spotStdDev;
	
	
	protected static JPanel detectionPanel;
	
	protected static void loadParameters () {
		// update number of channels based on the current source image
		int numC = 1;
		if (sourceImage!=null) numC = sourceImage.getNChannels();
		channelStrings = getChannelStrings(numC);
		targetChannel = Math.max(targetChannel, numC);
		// make use of scijava parameter persistence storage
		DefaultPrefService prefs = new DefaultPrefService();
		targetChannel = prefs.getInt(Integer.class, "MaikeCellCounter-targetChannel", targetChannel);
		spotRadius = prefs.getDouble(Double.class, "MaikeCellCounter-spotRadius", spotRadius);
		qualityThreshold = prefs.getDouble(Double.class, "MaikeCellCounter-qualityThreshold", qualityThreshold);
		autoROIChannel = prefs.getInt(Integer.class, "MaikeCellCounter-autoROIChannel", autoROIChannel);
		autoROIMethod = prefs.getInt(Integer.class, "MaikeCellCounter-autoROIMethod", autoROIMethod);
		autoROIColorIndex = prefs.getInt(Integer.class, "MaikeCellCounter-autoROIColor", autoROIColorIndex);
		for (int c=0; c<numC; c++) {
			channelColors[c] = prefs.getInt(Integer.class, "MaikeCellCounter-channelColors"+c, channelColors[c]);
		}
		spotShape = prefs.getInt(Integer.class, "MaikeCellCounter-spotShape", spotShape);
		spotColorIndex = prefs.getInt(Integer.class, "MaikeCellCounter-spotColor", spotColorIndex);
		spotAlpha = prefs.getDouble(Double.class, "MaikeCellCounter-alpha", spotAlpha);
	}
	protected static void saveParameters () {
		// update number of channels based on the current source image
		int numC = 1;
		if (sourceImage!=null) numC = sourceImage.getNChannels();
		// make use of scijava parameter persistence storage
		DefaultPrefService prefs = new DefaultPrefService();
		prefs.put(Integer.class, "MaikeCellCounter-targetChannel", targetChannel);
		prefs.put(Double.class, "MaikeCellCounter-spotRadius", spotRadius);
		prefs.put(Double.class, "MaikeCellCounter-qualityThreshold", qualityThreshold);
		
		prefs.put(Integer.class, "MaikeCellCounter-autoROIChannel", autoROIChannel);
		prefs.put(Integer.class, "MaikeCellCounter-autoROIMethod", autoROIMethod);
		prefs.put(Integer.class, "MaikeCellCounter-autoROIColor", autoROIColorIndex);
		for (int c=0; c<numC; c++) {
			prefs.put(Integer.class, "MaikeCellCounter-channelColors"+c, channelColors[c]);
		}
		prefs.put(Integer.class, "MaikeCellCounter-spotShape", spotShape);
		prefs.put(Integer.class, "MaikeCellCounter-spotColor", spotColorIndex);
		prefs.put(Double.class, "MaikeCellCounter-alpha", spotAlpha);
	}
	
	protected static void setSource(ImagePlus imp) {
		if (imp!=null) sourceImage = imp;
	}
	protected static ImagePlus getSource() {
		return sourceImage;
	}
	protected static String[] getChannelStrings (int nChannels) {
		String[] channelStrings = new String[nChannels];
		for (int c=0; c<nChannels; c++) {
			channelStrings[c] = "C" + (c+1);
		}
		return channelStrings;
	}
	protected static void setChannel(int channel) {
		if (sourceImage==null) return;
		if (channel>0 && channel<=sourceImage.getNChannels()) targetChannel = channel;
	}
	protected static void setRadius (double radius) {
		spotRadius = radius;
	}
	protected static void setQualityThreshold(double threshold) {
		qualityThreshold = threshold;
	}
	
	protected static void refreshSource () {
		setSource(WindowManager.getCurrentImage());
		displaySpots();
		//update GUI accordingly
		ResultPanel.imageInfo = InfoUtility.getImageInfo(sourceImage);
		ResultPanel.updateInfo();
	}
	protected static void prepareSource () { // change source image channel color and 
		if (sourceImage == null || channelColors.length==0) return;
		if (!sourceImage.isComposite()) {
			sourceImage.setLut(LUT.createLutFromColor(colors[channelColors[0]]));
		} else {
			int numC = sourceImage.getNChannels();
			int numColor = channelColors.length;
			for (int c=0; c<numC; c++) {
				if (c < numColor)
					((CompositeImage)sourceImage).setChannelLut(LUT.createLutFromColor(colors[channelColors[c]]), c+1);
			}
			((CompositeImage)sourceImage).updateAndDraw();
		}
	}
	protected static void showHideOverlay () {
		if (sourceImage == null) return;
		sourceImage.setHideOverlay(!sourceImage.getHideOverlay());
	}
	public static void configSourceDisplay() {
		// load saved parameters
		loadParameters();
		// update number of channels based on the current source image
		int numC = 1;
		if (sourceImage!=null) numC = sourceImage.getNChannels();
		channelStrings = getChannelStrings(numC);
		// create setup dialog
		GenericDialog gd = new GenericDialog("Setup");
		// auto ROI setup
		gd.addMessage("ROI");
		gd.addChoice("channel", channelStrings, channelStrings[Math.min(numC, autoROIChannel)-1]);
		gd.addToSameRow();
		gd.addChoice("method", autoMethods, autoMethods[autoROIMethod]);
		gd.addToSameRow();
		gd.addChoice("color", colorStrings, colorStrings[autoROIColorIndex]);
		// source image channel color setup
		gd.addMessage("Channel Colors");
		for (int c=0; c<numC; c++) {
			if (c!=0) gd.addToSameRow();
			gd.addChoice(channelStrings[c], colorStrings, colorStrings[channelColors[c]]);
		}
		// spot shape and color setup
		gd.addMessage("Cell Detecion");
		gd.addChoice("shape", roiShapes, roiShapes[spotShape]);
		gd.addToSameRow();
		gd.addChoice("color", colorStrings, colorStrings[spotColorIndex]);
		gd.addToSameRow();
		gd.addNumericField("transparency", 100*(1-spotAlpha), 0, 3, "%");
		// show dialog
		gd.showDialog();
		if (gd.wasCanceled()) return;
		// get parameters
		autoROIChannel = gd.getNextChoiceIndex()+1;
		autoROIMethod = gd.getNextChoiceIndex();
		autoROIColorIndex = gd.getNextChoiceIndex();
		if (autoROIColorIndex==9) autoROIColor = getCustomColor("ROI");
		else autoROIColor = colors[autoROIColorIndex];
		for (int c=0; c<numC; c++) {
			channelColors[c] = gd.getNextChoiceIndex();		
		}
		spotShape = gd.getNextChoiceIndex();
		spotColorIndex = gd.getNextChoiceIndex();
		
		spotAlpha = 1 - gd.getNextNumber()/100;
		// save parameters to internal storage
		saveParameters();
		// apply parameters to ROI, sourceImage, spot
		if (spotColorIndex==9) spotColor = getCustomColor("Spot");
		else spotColor = new Color(colors[spotColorIndex].getRed(), colors[spotColorIndex].getGreen(), colors[spotColorIndex].getBlue(), (int) (spotAlpha*255));
		//ROIUtility.roi.setStrokeColor(colors[autoROIColor]);
		if (sourceImage!=null) {
			if (ROIUtility.roi!=null) {
				ROIUtility.roi = ROIUtility.getRoi(sourceImage, autoROIChannel+1, autoMethods[autoROIMethod], colors[autoROIColorIndex]);
				//sourceImage.setRoi(ROIUtility.roi);
			}
			prepareSource();
			if (sourceImage.getOverlay()!=null) {
				Overlay overlay = sourceImage.getOverlay();
				overlay = (spotShape==0) ? pointToOvalOverlay(overlay) : ovalToPointOverlay(overlay);
				sourceImage.setOverlay(overlay);
			}
		}
	}
	

	
	// control the spot detection quality threshold, to return a overlay [1000, 6000] cell/mm2, otherwise return null
	protected static Overlay getSpotOverlay (ImagePlus imp, HashMap<String, Object> parameters) {
		return getSpotOverlay(imp, parameters, new double[] {500, 7500});
	}
	protected static Overlay getSpotOverlay (ImagePlus imp, HashMap<String, Object> parameters, double[] densityRange) {
		return getSpotOverlay(imp, parameters, densityRange, new double[] {10, 80});
	}
	protected static Overlay getSpotOverlay (ImagePlus imp, HashMap<String, Object> parameters, double[] densityRange, double[] qualityRange) {
		if (imp==null || parameters==null) return null;
		
		double minDensity = densityRange[0]; double maxDensity = densityRange[1];
		double minQualityThreshold = qualityRange[0]; double maxQualityThreshold = qualityRange[1];
		
		Overlay spotOverlay = new Overlay();
		Roi r = imp.getRoi();
		if (r!=null) {
			Roi newRoi = ROIUtility.trimBorder(r, imp, 1);
			imp.setRoi(newRoi);
		}
		String pixelUnit = imp.getCalibration().getUnit();
		if (!pixelUnit.equals("micron") && !pixelUnit.equals("um") && !pixelUnit.equals("µm"))
			System.out.println("Wrong calibration!!! Image: " + imp.getTitle());
		double pixelSize = imp.getCalibration().pixelWidth;
		double area = imp.getStatistics().area / 1e6; // area in mm2
		
		// configure the Trackmate spot detection from parameter setup
		int spotShape = (int) parameters.get("spotShape");
		Color spotBaseColor = (Color) parameters.get("spotColor");
		double spotAlpha = (double) parameters.get("spotAlpha");
		Color spotColor = new Color(spotBaseColor.getRed(), spotBaseColor.getGreen(), spotBaseColor.getBlue(), (int) (spotAlpha*255));
		int targetChannel = (int) parameters.get("targetChannel");
		if (targetChannel>imp.getNChannels()) targetChannel = 1;
		double spotRadius = (double) parameters.get("spotRadius");
		double qualityThreshold = (double) parameters.get("qualityThreshold");
		
		double density = 0; double qualityStep = 10;
		// generic model and setting of trackmate
		Model model = new Model();
		Settings tmSettings = new Settings();
		tmSettings.detectorFactory = new LogDetectorFactory();
		Map<String, Object> map = tmSettings.detectorFactory.getDefaultSettings();
		map.put("RADIUS", spotRadius);
		map.put("DO_MEDIAN_FILTERING", false);
		map.put("DO_SUBPIXEL_LOCALIZATION", true);
		map.put("TARGET_CHANNEL", targetChannel);
		map.put("THRESHOLD", qualityThreshold);
		tmSettings.detectorSettings = map;
		// do first detection with loaded parameters, if pass density filter, return;
		tmSettings.setFrom(imp);
		TrackMate trackmate = new TrackMate(model, tmSettings);
		boolean ok = trackmate.checkInput();
		ok = trackmate.execDetection();
		if (ok == false) System.out.println(trackmate.getErrorMessage());
		density = model.getSpots().getNSpots(false) / area;
		// check if density in range, otherwise, try different quality threshold
		if (density<minDensity || density>maxDensity) {
			// get direction of change
			qualityStep = (density>maxDensity) ? qualityStep : -1*qualityStep;
			// make sure quality treshold is in range
			qualityThreshold = Math.max(qualityThreshold, minQualityThreshold);
			qualityThreshold = Math.min(qualityThreshold, maxQualityThreshold);
			// increment quality threshold per step, check density in range
			while (density<minDensity || density>maxDensity) {
				if (qualityThreshold>maxQualityThreshold || qualityThreshold<minQualityThreshold) {
					qualityThreshold -= qualityStep;
					break;
				}
				map.put("THRESHOLD", qualityThreshold);
				tmSettings.detectorSettings = map;
				tmSettings.setFrom(imp);
				trackmate = new TrackMate(model, tmSettings);
				ok = trackmate.checkInput();
				ok = trackmate.execDetection();
				if (ok == false) System.out.println(trackmate.getErrorMessage());
				density = model.getSpots().getNSpots(false) / area;
				qualityThreshold += qualityStep;				
			}
		}
		
		if (density<minDensity || density>maxDensity) return null; // didn't pass cell number check
		qualityThreshold = (double) map.get("THRESHOLD");
		parameters.put("qualityThreshold-"+imp.getTitle(), qualityThreshold);
		
		// pass cell number check, then prepare overlay
		Iterator<Spot> spotIterator = model.getSpots().iterator(false);
		while (spotIterator.hasNext()) {
			Spot spot = spotIterator.next();
			// get spot position in pixel coordinates
			double x = spot.getDoublePosition( 0 );// / pixelSize;
			double y = spot.getDoublePosition( 1 );// / pixelSize;
			double d = spotRadius*2;
			Roi spotRoi = new OvalRoi((x-d/2)/pixelSize,(y-d/2)/pixelSize, d/pixelSize, d/pixelSize);
			spotRoi.setPosition(0, 0, 0);
			spotRoi.setStrokeColor(spotColor);
			spotOverlay.add(spotRoi);
		}
		// if point shape, change oval ROI to point ROI
		/*
		if (spotShape == 1) {
			Roi[] rois = spotOverlay.toArray();
			PointRoi pointRoi = new PointRoi();
			for (int i=0; i<rois.length; i++) {
				if (rois[i].getType() != Roi.OVAL) continue;
				double[] centre = rois[i].getContourCentroid();
				pointRoi.addPoint(centre[0], centre[1]);
			}
			pointRoi.setPosition(0, 0, 0);
			pointRoi.setStrokeColor(spotColor);
			spotOverlay = new Overlay();
			spotOverlay.add(pointRoi);
		}
		*/
		return spotOverlay;
	}
	
	/*
	protected static Overlay getSpotOverlay (ImagePlus imp, HashMap<String, Object> parameters, int minSpotCount) {
		if (imp==null || parameters==null) return null;
		Overlay spotOverlay = new Overlay();
		Roi r = imp.getRoi();
		if (r!=null) {
			Roi newRoi = ROIUtility.trimBorder(r, imp, 1);
			imp.setRoi(newRoi);
		}
		// configure the Trackmate spot detection from parameter setup
		int targetChannel = (int) parameters.get("targetChannel");
		if (targetChannel>imp.getNChannels()) targetChannel = 1;
		double spotRadius = (double) parameters.get("spotRadius");
		double qualityThreshold = (double) parameters.get("qualityThreshold");
		Settings tmSettings = new Settings();
		tmSettings.detectorFactory = new LogDetectorFactory();
		Map<String, Object> map = tmSettings.detectorFactory.getDefaultSettings();
		map.put("RADIUS", spotRadius);
		map.put("DO_MEDIAN_FILTERING", false);
		map.put("DO_SUBPIXEL_LOCALIZATION", true);
		map.put("TARGET_CHANNEL", targetChannel);
		map.put("THRESHOLD", qualityThreshold);
		tmSettings.detectorSettings = map;
		// set Trackmate to image data
		tmSettings.setFrom(imp);
		// detect spots using Trackmate
		Model model = new Model();
		TrackMate trackmate = new TrackMate(model, tmSettings);
		// Check input, get spots
		boolean ok = trackmate.checkInput();
		ok = trackmate.execDetection();
		if (ok == false) System.out.println(trackmate.getErrorMessage());
		SpotCollection spots = model.getSpots();
		if (spots==null || spots.getNSpots(false)==0) return spotOverlay;
		// get overlay setting, change spot to overlay
		int spotShape = (int) parameters.get("spotShape");
		Color spotBaseColor = (Color) parameters.get("spotColor");
		double spotAlpha = (double) parameters.get("spotAlpha");
		Color spotColor = new Color(spotBaseColor.getRed(), spotBaseColor.getGreen(), spotBaseColor.getBlue(), (int) (spotAlpha*255));
		
		double pixelSize = imp.getCalibration().pixelWidth;
		Iterator<Spot> spotIterator = spots.iterator(false);
		while (spotIterator.hasNext()) {
			Spot spot = spotIterator.next();
			// get spot position in pixel coordinates
			double x = spot.getDoublePosition( 0 );// / pixelSize;
			double y = spot.getDoublePosition( 1 );// / pixelSize;
			double d = spotRadius*2;
			Roi spotRoi = new OvalRoi((x-d/2)/pixelSize,(y-d/2)/pixelSize, d/pixelSize, d/pixelSize);
			spotRoi.setPosition(0, 0, 0);
			spotRoi.setStrokeColor(spotColor);
			spotOverlay.add(spotRoi);
		}
		if (spotShape == 1) {
			Roi[] rois = spotOverlay.toArray();
			PointRoi pointRoi = new PointRoi();
			for (int i=0; i<rois.length; i++) {
				if (rois[i].getType() != Roi.OVAL) continue;
				double[] centre = rois[i].getContourCentroid();
				pointRoi.addPoint(centre[0], centre[1]);
			}
			pointRoi.setPosition(0, 0, 0);
			pointRoi.setStrokeColor(spotColor);
			spotOverlay = new Overlay();
			spotOverlay.add(pointRoi);
		}

		return spotOverlay;
	}
	*/
	
	@SuppressWarnings("rawtypes")
	protected static void displaySpots() {
		if (sourceImage==null) return;
		Roi r = sourceImage.getRoi();
		if (r!=null) {
			//Roi newRoi = ROIUtility.trimBorder(r, sourceImage, spotRadius/sourceImage.getCalibration().pixelWidth);
			Roi newRoi = ROIUtility.trimBorder(r, sourceImage, 1);
			sourceImage.setRoi(newRoi);
		}
		// configure the Trackmate detector settings
		Settings tmSettings = new Settings();
		tmSettings.detectorFactory = new LogDetectorFactory();
		//tmSettings.addSpotFilter(new FeatureFilter("QUALITY", minSpotQuality, true));
		Map<String, Object> map = tmSettings.detectorFactory.getDefaultSettings();
		map.put("RADIUS", spotRadius);
		map.put("DO_MEDIAN_FILTERING", false);
		map.put("DO_SUBPIXEL_LOCALIZATION", true);
		// set to target channel
		map.put("TARGET_CHANNEL", targetChannel);
		map.put("THRESHOLD", qualityThreshold);
		tmSettings.detectorSettings = map;
		// set Trackmate to the raw image data
		tmSettings.setFrom(sourceImage);
		
		/* debug
		System.out.println("debug sourceImage: " + sourceImage==null?"null":sourceImage.getTitle());
		System.out.println("debug targetChannel: " + targetChannel);
		System.out.println("debug spotRadius: " + spotRadius);
		System.out.println("debug qualityThreshold: " + qualityThreshold);
		*/
		
		// detect spots using Trackmate
		Model model = new Model();
		TrackMate trackmate = new TrackMate(model, tmSettings);
		// Check input, Find spots
		boolean ok = trackmate.checkInput();
		ok = trackmate.execDetection();
		if (ok == false) System.out.println(trackmate.getErrorMessage());
		else saveParameters();
		
		tmSpots = model.getSpots();
		tmSpotToOverlay();
		
		// update spot data
		computeSpotData();
		// update result panel spot info
		ResultPanel.spotInfo = InfoUtility.getSpotInfo(sourceImage, spotOverlay.size(), spotMean, spotStdDev);
		ResultPanel.updateInfo();
		//return spotOverlay.size();
		//return tmSpots.getNSpots(false);
	}
	
	protected static boolean tmOriOverlay () {
		if (sourceImage==null) return false;
		if (tmSpots==null) return false;
		if (sourceImage.getOverlay()==null) return false;
		if (sourceImage.getOverlay().size() != 2) return false;
		Overlay overlay = sourceImage.getOverlay();
		Roi[] rois = overlay.toArray();
		for (int i=0; i<2; i++) {
			if (rois[i].getType() != Roi.RECTANGLE) return false;
			Rectangle r = rois[i].getBounds();
			if (r.width!=0 || r.height!=0) return false;
		}
		return true;
	}
	
	protected static void tmSpotToOverlay () {
		if (sourceImage==null) return;
		if (tmSpots==null) return;
		double pixelSize = sourceImage.getCalibration().pixelWidth;
		Iterator<Spot> spotIterator = tmSpots.iterator(false);
		Overlay newOverlay = new Overlay();
		PointRoi pointRoi = new PointRoi();
		while (spotIterator.hasNext()) {
			Spot spot = spotIterator.next();
			// get spot position in pixel coordinates
			//double x = spot.getFeature("POSITION_X");
			//double y = spot.getFeature("POSITION_Y");
			double x = spot.getDoublePosition( 0 );// / pixelSize;
			double y = spot.getDoublePosition( 1 );// / pixelSize;
			double d = spotRadius*2;
			Roi spotRoi = new OvalRoi((x-d/2)/pixelSize,(y-d/2)/pixelSize, d/pixelSize, d/pixelSize);
			spotRoi.setPosition(0, 0, 0);
			spotRoi.setStrokeColor(spotColor);
			newOverlay.add(spotRoi);
		}
		spotOverlay = newOverlay.duplicate(); // the only place to assign value to spotOverlay
		if (spotShape == 1) newOverlay = ovalToPointOverlay(newOverlay);
		sourceImage.setOverlay(newOverlay);
	}
	
	protected static Overlay pointToOvalOverlay (Overlay overlay) {
		if (overlay==null || overlay.size()==0) return overlay;
		Roi[] rois = overlay.toArray();
 		if (rois.length!=1) {	// check length (multi-point overlay length == 1
 			for (Roi roi : rois) {
 				if (roi.getType() == Roi.OVAL)
 					roi.setStrokeColor(spotColor);
 				else
 					overlay.remove(roi);
 			}
 			return overlay;
 		}
 		
 		Point[] points = ((PointRoi) rois[0]).getContainedPoints();
 		if (points.length==1) return overlay;	// check if single point in the overlay
 		// create new overlay with oval ROIs
 		double r = spotRadius / sourceImage.getCalibration().pixelWidth;
 		
 		Overlay newOverlay = new Overlay();
		for (int i=0; i<points.length; i++) {
			Roi roi = new OvalRoi(points[i].getX()-r, points[i].getY()-r, r*2, r*2);
			roi.setPosition(0, 0, 0);
			roi.setStrokeColor(spotColor);
			newOverlay.add(roi);
		}
 		return newOverlay;
	}
	
	protected static Overlay ovalToPointOverlay (Overlay overlay) {
		if (overlay==null || overlay.size()==0) return overlay;
		Roi[] rois = overlay.toArray();
		if (rois.length==1) {
			if (rois[0].getType() == Roi.POINT) {
				rois[0].setStrokeColor(spotColor);
				return overlay;
			}
		}
		PointRoi pointRoi = new PointRoi();
		for (int i=0; i<rois.length; i++) {
			if (rois[i].getType() != Roi.OVAL) continue;
			double[] centre = rois[i].getContourCentroid();
			pointRoi.addPoint(centre[0], centre[1]);
		}
		pointRoi.setPosition(0, 0, 0);
		pointRoi.setStrokeColor(spotColor);
		Overlay newOverlay = new Overlay();
		newOverlay.add(pointRoi);
 		return newOverlay;
	}
	
	
	protected static void computeSpotData () {
		double[][][] data = computeSpotData(sourceImage, spotOverlay);
		spotMean = data[0];
		spotStdDev = data[1];
	}
	protected static double[][][] computeSpotData (ImagePlus imp, Overlay overlay) {
		if (imp==null || overlay==null || overlay.size()==0) return null;
		Roi roi = imp.getRoi();
		Roi[] spotRois = overlay.toArray();
		int numC = imp.getNChannels();
		int numSpot = spotRois.length;
		double[][] spotMean = new double[numC][numSpot];
		double[][] spotStdDev = new double[numC][numSpot];
		for (int c=0; c<numC; c++) {
			imp.setPositionWithoutUpdate(c+1, 1, 1);
			for (int i=0; i<numSpot; i++) {
				imp.setRoi(spotRois[i], false);
				spotMean[c][i] = imp.getRawStatistics().mean;
				spotStdDev[c][i] = imp.getRawStatistics().stdDev;
			}
		}
		imp.setRoi(roi);
		double[][][] result = new double[2][numC][numSpot];
		result[0] = spotMean;
		result[1] = spotStdDev;
		return result;
	}

	
	protected static Color getColor (String colorName) {
		for (int i=0; i<colorStrings.length; i++) {
			if (colorStrings[i].equals(colorName.toUpperCase()))
				return colors[i];
		}
		return null;
	}
	
	protected static int getColorIndex (String colorName) {
		for (int i=0; i<colorStrings.length; i++) {
			if (colorStrings[i].equals(colorName.toUpperCase()))
				return i;
		}
		return -1;
	}
	protected static Color getCustomColor (String colorComponent) {
		DefaultPrefService prefs = new DefaultPrefService();
		red = prefs.getInt(Integer.class, "MaikeCellCounter-customRed", red);
		green = prefs.getInt(Integer.class, "MaikeCellCounter-customGreen", green);
		blue = prefs.getInt(Integer.class, "MaikeCellCounter-customBlue", blue);
		alpha = prefs.getInt(Integer.class, "MaikeCellCounter-customAlpha", alpha);
		GenericDialog gd = new GenericDialog(colorComponent + " Custom Color");
		gd.addSlider("Red", 0, 255, red);
		gd.addSlider("Green", 0, 255, green);
		gd.addSlider("Blue", 0, 255, blue);
		gd.addSlider("Transparency", 0, 255, 255-alpha);
		gd.showDialog();
		if (gd.wasCanceled()) return customColor;
		red = (int) gd.getNextNumber();
		green = (int) gd.getNextNumber();
		blue = (int) gd.getNextNumber();
		alpha = 255 - (int) gd.getNextNumber();
		prefs.put(Integer.class, "MaikeCellCounter-customRed", red);
		prefs.put(Integer.class, "MaikeCellCounter-customGreen", green);
		prefs.put(Integer.class, "MaikeCellCounter-customBlue", blue);
		prefs.put(Integer.class, "MaikeCellCounter-customAlpha", alpha);
		customColor = new Color(red, green, blue, alpha);
		return customColor;
	}
	
	protected static int getMethod (String method) {
		for (int i=0; i<autoMethods.length; i++) {
			if (autoMethods[i].equals(method)) {
				return i;
			}
		}
		return -1;
	}
}
