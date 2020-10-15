package uk.ac.cam.cruk.mrlab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.prefs.DefaultPrefService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.WaitForUserDialog;
import ij.gui.YesNoCancelDialog;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class CellCounterOld implements PlugIn {

	// parameters for source image
	protected ImagePlus sourceImage = null;
	protected int targetChannel;
	protected int numC;
	protected int numZ;
	protected int numT;
	protected int posC;
	protected int posZ;
	protected int posT;
	
	protected double spotRadius = 4.0d;
	protected double qualityThreshold = 40;
	protected SpotCollection tmSpots;
	
	// parameters for GUI
	protected PlugInFrame pf;
	
	protected String frameName = "Cell Counter";
	protected final int lineWidth = 90;
	protected Color panelColor = new Color(204, 229, 255);
	protected final Font textFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color fontColor = Color.BLACK;
	protected final Font errorFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color errorFontColor = Color.RED;
	protected final Color textAreaColor = new Color(204, 229 , 255);
	protected final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected final Color panelTitleColor = Color.BLUE;
	protected final EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	protected final Dimension textAreaMax = new Dimension(260, 150);
	protected final Dimension tablePreferred = new Dimension(260, 100);
	protected final Dimension tableMax = new Dimension(260, 150);
	protected final Dimension panelTitleMax = new Dimension(500, 30);
	protected final Dimension panelMax = new Dimension(620, 300);
	//protected final Dimension panelMin = new Dimension(600, 200);
	protected final Dimension buttonSize = new Dimension(90, 10);
	
	protected final Dimension numberMax = new Dimension(100, 20);
	
	protected JButton btnManualTrack;
	protected JButton btnAutoTrack;
	protected JButton btnCfgAuto;
	
	protected JButton btnChangeRoiShape;
	protected JButton btnDelBefore;
	protected JButton btnDelAfter;
	
	protected JButton btnViewPlot;
	protected JButton btnViewTable;
	
	protected JButton btnFindGap;
	
	protected JTextArea sourceInfo;
	protected JTextArea spotCount;

	
	protected void updateFrameByUser(String userName) {	// make the frame gaylly for Hung-Chang
		String homeDir = System.getProperty("user.home");
		if (homeDir.endsWith(userName)) {
			panelColor = new Color(255, 204, 229);
			frameName = "\u5b8f\u660c\u5144\u7684(\u57fa\u4f6c\u7c89)\u63d2\u4ef6";
		}
	}

	protected void createFrame() {

		//sourceImage = WindowManager.getCurrentImage();

		updateFrameByUser("chen02");
		
		
		PlugInFrame pf = new PlugInFrame(frameName);
		pf.setLayout(new BoxLayout(pf, BoxLayout.Y_AXIS));

		JPanel parentPanel = new JPanel();
		parentPanel.setBorder(border);
		parentPanel.setBackground(pf.getBackground());
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		// create and configure the content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(border);
		contentPanel.setBackground(panelColor);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

		// result panel for reporting result: add real measurement of mean and stdDev of each channel in the future
		JPanel resultPanel = new JPanel();
		spotCount = new JTextArea();
		spotCount.setMaximumSize(new Dimension(150, 400));
		spotCount.setEditable(false);
		spotCount.setAlignmentX(Component.CENTER_ALIGNMENT);
		spotCount.setFont(textFont);
		spotCount.setText("not yet counted.");
		spotCount.setBackground(new Color(255, 255, 204));
		resultPanel.setBackground(new Color(255, 255, 204));
		resultPanel.add(spotCount);
		
		
		//Create the spot detection panel.
		JPanel detectionPanel = new JPanel();
			JPanel channelPanel = new JPanel();
			JPanel radiusPanel = new JPanel();
			JPanel qualityPanel = new JPanel();
		// channel detection: label with slider
	        JLabel sliderLabel = new JLabel("Detection Channel", JLabel.CENTER);
	        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	        JSlider channelSlider = new JSlider(JSlider.HORIZONTAL, 1, 3, 1);
	        channelSlider.setMajorTickSpacing(1);
	        channelSlider.setMinorTickSpacing(1);
	        channelSlider.setPaintTicks(true);
	        channelSlider.setPaintLabels(true);
	        channelSlider.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
			channelSlider.setBackground(panelColor);
		// diameter of detection: label with spinner
			JLabel radiusLabel = new JLabel("Cell Diameter (µm)", JLabel.CENTER);
		    radiusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	        SpinnerNumberModel model = new SpinnerNumberModel(spotRadius*2, 0.1d, 1000.0d, 0.5d);
	        JSpinner diameterSpinner = new JSpinner(model);
	        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)diameterSpinner.getEditor();
	        DecimalFormat format = editor.getFormat();
	        format.setMinimumFractionDigits(2);
	        editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
			diameterSpinner.setPreferredSize(numberMax);
			diameterSpinner.setMinimumSize(numberMax);
		// quality threshold of detection: label with spinner
			JLabel qualityLabel = new JLabel("Quality Threshold", JLabel.CENTER);
		    qualityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			SpinnerNumberModel model2 = new SpinnerNumberModel(qualityThreshold, 0.001d, 1000.0d, 5.0d);
	        JSpinner qualityThresholdSpinner = new JSpinner(model2);
	        JSpinner.NumberEditor editor2 = (JSpinner.NumberEditor)qualityThresholdSpinner.getEditor();
	        DecimalFormat format2 = editor2.getFormat();
	        format2.setMinimumFractionDigits(3);
	        editor2.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
			qualityThresholdSpinner.setPreferredSize(numberMax);
			qualityThresholdSpinner.setMinimumSize(numberMax);
		// add each components to its panel
			channelPanel.setLayout(new BoxLayout(channelPanel, BoxLayout.Y_AXIS));
			channelPanel.add(sliderLabel);
			channelPanel.add(channelSlider);
			channelPanel.setBorder(border);
			channelPanel.setBackground(panelColor);
			channelPanel.setMaximumSize(new Dimension(500, 75));
			channelPanel.setPreferredSize(new Dimension(500, 75));
			radiusPanel.setLayout(new BoxLayout(radiusPanel, BoxLayout.Y_AXIS));
			radiusPanel.add(radiusLabel);
			radiusPanel.add(diameterSpinner);
			radiusPanel.setBorder(border);
			radiusPanel.setBackground(panelColor);
			radiusPanel.setMaximumSize(new Dimension(500, 55));
			radiusPanel.setPreferredSize(new Dimension(500, 55));
			qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));
			qualityPanel.add(qualityLabel);
			qualityPanel.add(qualityThresholdSpinner);
			qualityPanel.setBorder(border);
			qualityPanel.setBackground(panelColor);
			qualityPanel.setMaximumSize(new Dimension(500, 55));
			qualityPanel.setPreferredSize(new Dimension(500, 55));
		// add the three sub-panel to detection panel
		detectionPanel.setLayout(new BoxLayout(detectionPanel, BoxLayout.X_AXIS));
		detectionPanel.add(channelPanel);
		detectionPanel.add(radiusPanel);
		detectionPanel.add(qualityPanel);
		detectionPanel.setBorder(border);
		detectionPanel.setBackground(panelColor);
		detectionPanel.setMaximumSize(panelMax);
		
		// add action/change listener to detection components
			channelSlider.addChangeListener(new ChangeListener() {
				@Override 
				public void stateChanged(ChangeEvent e) {
					JSlider source = (JSlider)e.getSource();
					if (!source.getValueIsAdjusting())
					    targetChannel = (int)source.getValue();
					tmSpots = displaySpots();
					int nSpot = filterOverlay (1, 0, true, 0.0d);
					spotCount.setText(getImageInfo()+ "\n" + getSpotInfo());
	        }});
			diameterSpinner.addChangeListener(new ChangeListener() {
				@Override 
				public void stateChanged(ChangeEvent e) {
					JSpinner source = (JSpinner)e.getSource();
					spotRadius = (double)source.getValue() / (double)2;
					tmSpots = displaySpots();
					int nSpot = filterOverlay (1, 0, true, 0.0d);
					spotCount.setText(getImageInfo()+ "\n" + getSpotInfo());
			}});
	        qualityThresholdSpinner.addChangeListener(new ChangeListener() {
				@Override 
				public void stateChanged(ChangeEvent e) {
					JSpinner source = (JSpinner)e.getSource();
					qualityThreshold = (double)source.getValue();
					tmSpots = displaySpots();
					int nSpot = filterOverlay (1, 0, true, 0.0d);
					spotCount.setText(getImageInfo()+ "\n" + getSpotInfo());
			}});

		
		JPanel buttonPanel = new JPanel();
		JButton btnRefresh = new JButton("refresh source");
		JButton btnShow = new JButton("show/hide spot");
		JButton btnFilter = new JButton("add filter");

		buttonPanel.add(btnRefresh);
		buttonPanel.add(btnShow);
		//buttonPanel.add(btnFilter);
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		buttonPanel.setMaximumSize(panelMax);

		ROIUtility.addROIPanel(contentPanel);
		
		contentPanel.add(detectionPanel);
		contentPanel.add(buttonPanel);
		
		
		//contentPanel.add(thresholdPanel);
		addFilterPanel(contentPanel);
		
		contentPanel.add(resultPanel);
		
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		// configure the JTextArea to display source image info
		//sourceInfo.setBackground(textAreaColor);

		// configure refresh button
		btnRefresh.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
		  		sourceImage = WindowManager.getCurrentImage();
		  		if (sourceImage==null) return;
		  		tmSpots = displaySpots();
				int nSpot = filterOverlay (1, 0, true, 0.0d);
				spotCount.setText(getImageInfo()+ "\n" + getSpotInfo());
		  	}
		});
		// configure refresh button
		btnShow.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
		  		sourceImage.setHideOverlay(!sourceImage.getHideOverlay());
		  	}
		});
		btnFilter.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) {
		  		addFilter();
		  	}
		});
		
		
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		//f.add(parentPanel, BorderLayout.NORTH);
		
		pf.add(parentPanel);
		pf.pack();
		pf.setSize(620, 550);
		pf.setMinimumSize(panelMax);
		pf.setVisible(true);
		pf.setLocationRelativeTo(null);
		//pf.setResizable(false);
		GUI.center(pf);
	}
	
	protected SpotCollection displaySpots() {
		if (sourceImage==null) return null;
		Roi r = sourceImage.getRoi();
		if (r!=null) {
			Roi newRoi = ROIUtility.trimBorder(r, sourceImage, 1);
			sourceImage.setRoi(newRoi);
		}
		//imp = sourceImage;
		// configure the Trackmate detector settings
		Settings tmSettings = new Settings();
		tmSettings.detectorFactory = new LogDetectorFactory();
		//tmSettings.addSpotFilter(new FeatureFilter("QUALITY", minSpotQuality, true));
		Map<String, Object> map = tmSettings.detectorFactory.getDefaultSettings();
		map.put("RADIUS", spotRadius);
		map.put("DO_MEDIAN_FILTERING", false);
		map.put("DO_SUBPIXEL_LOCALIZATION", true);
		// set to Dapi channel
		map.put("TARGET_CHANNEL", targetChannel);
		map.put("THRESHOLD", qualityThreshold);
		tmSettings.detectorSettings = map;
		// set Trackmate to the raw image data
		tmSettings.setFrom(sourceImage);
		// detect spots using Trackmate
		SpotCollection dapiSpots = countSpotsTrackmate(tmSettings, false);
		tmSpotToOverlay();
		//sourceImage.setRoi(r);
		
		return dapiSpots;
	}
	
	protected SpotCollection countSpotsTrackmate(Settings settings, boolean display) {

		Model model = new fiji.plugin.trackmate.Model();
		TrackMate trackmate = new TrackMate(model, settings);
		// Check input is ok
		boolean ok = trackmate.checkInput();
		if (ok == false) {
			System.out.println(trackmate.getErrorMessage());
		}
		// Find spots
		ok = trackmate.execDetection();
		if (ok == false) {
			System.out.println(trackmate.getErrorMessage());
		}
		// Compute spot features
		ok = trackmate.computeSpotFeatures(true);
		if (ok == false) {
			System.out.println(trackmate.getErrorMessage());
		}

		// Filter spots
		ok = trackmate.execSpotFiltering(true);
		if (ok == false) {
			System.out.println(trackmate.getErrorMessage());
		}
		// display spot detections
		if (display) {
			SelectionModel selectionModel = new SelectionModel(model);
			HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, settings.imp);
			displayer.render();
			displayer.refresh();
		}
		// Return spot collection
		return model.getSpots();
	}

	protected void addFilter () {
		/*
		 * Check filter panel: if not exist, create; if exist, fetch
		 * In filter panel, add a new filter
		 */


		
	}

	protected int filterOverlay (ArrayList<int[]> filterSet) { // AND/OR, channel, mean/stdDev, higher, value
		if (sourceImage==null || tmSpots==null) return -1;
		if (sourceImage.getOverlay()==null) return -1;
		if (tmOriOverlay()) return -1;
		Roi r = sourceImage.getRoi();
		
		int posC = sourceImage.getC();
		double pixelSize = sourceImage.getCalibration().pixelWidth;
		Overlay overlay = sourceImage.getOverlay();
		Roi[] rois = overlay.toArray();
		
		int numFilter = filterSet.size();
		boolean[] logicalOperations = new boolean[numFilter];

		for (Roi roi : rois) {
			
			sourceImage.setRoi(roi, false);
			boolean passPrevFilters = true;
			
			for (int i=0; i<filterSet.size(); i++) {
				boolean passCurrentFilter = false;
				
				int[] filter = filterSet.get(i);
				if (filter.length!=5) continue;
				boolean logicalAnd = (filter[0]==0);
				int channel = filter[1]+1;
				boolean getMean = (filter[2]==0);
				boolean higher = (filter[3]==0);
				double threshold = (double)filter[4];

				sourceImage.setPositionWithoutUpdate(channel, 1, 1);
				double value = getMean ? sourceImage.getRawStatistics().mean : sourceImage.getRawStatistics().stdDev;
				if (higher ? (value>threshold) : (value<threshold)) {
					passCurrentFilter = true;
				}
				
				if (logicalAnd) {
					passPrevFilters = (passPrevFilters && passCurrentFilter);
				} else { //logical OR
					passPrevFilters = (passPrevFilters || passCurrentFilter);
				}
				
			}
			if (!passPrevFilters)
				overlay.remove(roi);
				
		}

		//sourceImage.deleteRoi();
		//sourceImage.setOverlay(newOverlay);
		sourceImage.setC(posC);
		sourceImage.setRoi(r);
		return overlay.size();
	}

	protected boolean tmOriOverlay () {
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
	
	protected void tmSpotToOverlay () {
		if (sourceImage==null) return;
		if (tmSpots==null) return;
		int posC = sourceImage.getC();
		double pixelSize = sourceImage.getCalibration().pixelWidth;
		Iterator<Spot> spotIterator = tmSpots.iterator(true);
		Overlay newOverlay = new Overlay();
		while (spotIterator.hasNext()) {
			Spot spot = spotIterator.next();
			// get spot position in pixel coordinates
			double x = spot.getFeature("POSITION_X");
			double y = spot.getFeature("POSITION_Y");
			double d = spotRadius*2;
			Roi spotRoi = new OvalRoi((x-d/2)/pixelSize,(y-d/2)/pixelSize, d/pixelSize, d/pixelSize);
			spotRoi.setPosition(0, 0, 0);
			spotRoi.setStrokeColor(Color.MAGENTA);
			newOverlay.add(spotRoi);
		}
		sourceImage.setOverlay(newOverlay);
	}
	
	protected int filterOverlay (int channel, int stat, boolean higher, double threshold) {
		if (sourceImage==null) return -1;
		Roi r = sourceImage.getRoi();
		
		tmSpotToOverlay();
		
		Overlay overlay = sourceImage.getOverlay();
		int posC = sourceImage.getC();
		double pixelSize = sourceImage.getCalibration().pixelWidth;

		Roi[] rois = overlay.toArray();

		for (Roi roi : rois) {
			sourceImage.setRoi(roi, false);
			sourceImage.setPositionWithoutUpdate(channel, 1, 1);
			double value = stat==0 ? sourceImage.getRawStatistics().mean : sourceImage.getRawStatistics().stdDev;
			if (higher ? (value<=threshold) : (value>=threshold)) {
				overlay.remove(roi);
			}
		}
		sourceImage.setRoi(r);
		sourceImage.setOverlay(overlay);
		sourceImage.setC(posC);
		return overlay.size();
	}
	
	
	// add filter panel to root panel
	protected void addFilterPanel (JPanel rootPanel) {
		// create a parent filter panel
		JPanel filterPanel = new JPanel();
		filterPanel.setName("filterPanel");
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

		JPanel buttonpanel = new JPanel();
		buttonpanel.setLayout(new BoxLayout(buttonpanel, BoxLayout.X_AXIS));
		JButton btnAddFilter = new JButton("add filter");
		btnAddFilter.setPreferredSize(new Dimension(120, 50));
		btnAddFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		        addFilter(filterPanel);
			}
	    });

	    JButton btnGetFilter = new JButton("apply filter");
		btnGetFilter.setPreferredSize(new Dimension(120, 50));
		btnGetFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int nSpot = filterOverlay (1, 0, true, 0.0d); // start from SpotCollection
				Component[] panels = filterPanel.getComponents();
				for (Component panel : panels) {
					if (panel instanceof JPanel) {	// filter panel 1, 2, 3, 4...
						//String filterName = ((JPanel)panel).getName();
						//if (filterName.toLowerCase().contains(keywords))
						String filterName = ((JPanel)panel).getName();
						if (filterName==null) continue;
						if (filterName.toLowerCase().contains("filter")) {
							nSpot = filterOverlay(getFilters((JPanel) panel));
							spotCount.setText(getImageInfo()+ "\n" + getSpotInfo());
						}
							
					}
				}
		       
			}
	    });
	    
		buttonpanel.add(btnAddFilter);
		buttonpanel.add(btnGetFilter);
		buttonpanel.setBorder(border);
		buttonpanel.setBackground(panelColor);
		buttonpanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		filterPanel.add(buttonpanel);
		filterPanel.setBorder(border);
		filterPanel.setBackground(panelColor);
		filterPanel.setMaximumSize(panelMax);

		rootPanel.add(filterPanel);
	}

	// add a new filter (as panel) to the filter panel
	protected void addFilter (JPanel filterPanel) {
		// check filters
		JPanel newFilter = new JPanel();
		int numFilters = checkFilter(filterPanel, "filter ");
		//println(numFilters);
		newFilter.setName("Filter "+ String.valueOf(numFilters+1));
		newFilter.setLayout(new BoxLayout(newFilter, BoxLayout.Y_AXIS));
		newFilter.setBackground(panelColor);
		addElement(newFilter, true);
		filterPanel.add(newFilter);
		filterPanel.revalidate();
	    filterPanel.validate();
	}

	// check in parent panel, how many filters have already exist
	protected int checkFilter(JPanel parentPanel, String keywords) {
		int filterNumber = 0;
		Component[] panels = parentPanel.getComponents();
		for (Component panel : panels) {
			if (panel instanceof JPanel) {	// filter panel 1, 2, 3, 4...
				String filterName = ((JPanel)panel).getName();
				if (filterName==null) continue;
				if (filterName.toLowerCase().contains(keywords))
					filterNumber++;
			}
		}
		return filterNumber;
	}

	protected ArrayList<int[]> getFilters (JPanel filterPanel) {
		ArrayList<int[]> filterSet = new ArrayList<int[]>();
		Component[] filters = filterPanel.getComponents();
		for (Component filter : filters) {
			if (!(filter instanceof Box)) continue;
			Component[] elements = ((Container) filter).getComponents();
			int[] newElementArray = new int[5];
			for (Component element : elements) {
				String name = element.getName();
				if (name==null) continue;
				int idx = name.indexOf("element");
				if (idx==-1) continue;
				int elementIdx = Integer.valueOf(name.substring(idx+7, name.length()));
				if (elementIdx==5) {
					double value = (double) ((JSpinner)element).getValue();
					newElementArray[elementIdx-1] = (int)(Math.round(value));
				} else {
					newElementArray[elementIdx-1] = ((JComboBox)element).getSelectedIndex();
				}			
			}
		filterSet.add(newElementArray);
		}
		return filterSet;
	}
	
	// populate a new filter (panel) with elements		
	protected void addElement(JPanel filter, boolean firstTime) {
		Box newFilterElement = Box.createHorizontalBox();
		newFilterElement.setName("a box");
		
		String[] logicalStrings = {"AND","OR"};
	    JComboBox logicalOperation = new JComboBox(logicalStrings);
	    logicalOperation.setName("element1");

		String[] channelStrings = {"C1","C2", "C3"};
	    JComboBox channelSeletion = new JComboBox(channelStrings);
	    channelSeletion.setName("element2");
	    
		String[] statStrings = {"mean","stdDev"};
	    JComboBox statSeletion = new JComboBox(statStrings);
		statSeletion.setName("element3");
	    
		String[] compareStrings = {"higher than","lower than"};
	    JComboBox compare = new JComboBox(compareStrings);
	    compare.setName("element4");
	    
	    //JSpinner diameterSpinner = new JSpinner();
        SpinnerNumberModel model3 = new SpinnerNumberModel(0, 0.0d, 65536.0d, 100.0d);
        JSpinner thresholdSpinner = new JSpinner(model3);
        JSpinner.NumberEditor editor3 = (JSpinner.NumberEditor)thresholdSpinner.getEditor();
        DecimalFormat format3 = editor3.getFormat();
        format3.setMinimumFractionDigits(1);
        editor3.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
		thresholdSpinner.setName("element5");
		
		JButton btnAddElement = new JButton("+");
		btnAddElement.setMinimumSize(new Dimension(20, 20));
		btnAddElement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// add another element to the panel
				addElement(filter, false);
			}
	    });
	    JButton btnDelElement = new JButton("-");
		btnDelElement.setMinimumSize(new Dimension(20, 20));
		btnDelElement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// add another element to the panel
				delElement(filter, newFilterElement, firstTime);
			}
	    });
	    
	    if (firstTime) {
	    	JComboBox filterName = new JComboBox(new String[]{filter.getName()});
	    	filterName.setName("element1");
	    	newFilterElement.add(filterName);
	    } else {
	    	newFilterElement.add(logicalOperation);
	    }
	    
		newFilterElement.add(channelSeletion); 
		newFilterElement.add(statSeletion); 
		newFilterElement.add(compare);
		newFilterElement.add(thresholdSpinner); 
		newFilterElement.add(btnAddElement);
		newFilterElement.add(btnDelElement);
		channelSeletion.setAlignmentX(Component.LEFT_ALIGNMENT);
		filter.add(newFilterElement);
		filter.revalidate();
	    filter.validate();
    }

	// delete a filter or a filter element
	protected void delElement(JPanel panel, Box box, boolean deleteAll) {
		if (deleteAll) {
			Component parent = panel.getParent();
			if (parent instanceof JPanel) {
			((JPanel)parent).remove(panel);
			parent.revalidate();
			parent.validate();
			}
		} else {
			panel.remove(box);
			panel.revalidate();
		    panel.validate();
		}
	}

	// get image information into string

/*
 * Function groups to manage the source image	
 */


	
	
	
	
	

	

	
	
	@Override
	public void run(String arg) {
		createFrame();
	}
	
	/*
	 * 	Functions to configure and display strings to source info panel
	 */
		// function to generate string of image information
		public String getImageInfo () {
			ImagePlus imp = sourceImage;
			if (imp==null)	return ("No image recognized!"); // if no image recognized, return;
			// get image title info
			String imageTitle = "Image: " + imp.getTitle();
			imageTitle = wrapString(imageTitle, lineWidth, 1) + "\n";
			// get image size in RAM, and bit depth info
			double sizeInMBGB = imp.getSizeInBytes()/1048576; String sizeUnit = "MB";
			if (sizeInMBGB>1024) { sizeInMBGB /= 1024; sizeUnit = "GB";}
			String imageSize = "Size: "
					+ new DecimalFormat("0.#").format(sizeInMBGB)
					+ sizeUnit + " (" + String.valueOf(imp.getBitDepth()) + " bit)";
			if (sizeUnit=="GB" && sizeInMBGB>2) {imageSize += " (image might be too large!)";}
			imageSize = wrapString(imageSize, lineWidth, 1) + "\n";
			// get image dimension info
			int[] dim = imp.getDimensions();
			String imageDimension = "Dimension:  X:" + String.valueOf(dim[0])
			  + ", Y:" + String.valueOf(dim[1])
			  + ", Z:" + String.valueOf(dim[3])
			  + ", C:" + String.valueOf(dim[2])
			  + ", T:" + String.valueOf(dim[4]);
			imageDimension = wrapString(imageDimension, lineWidth, 1) + "\n";
			// get image ROI info
			String imageRoi = imp.getRoi()==null ? "ROI: NO" : "ROI: YES";
			imageRoi = wrapString(imageRoi, lineWidth, 1) + "\n";
			// get image overlay info // omit for now
			//String imgOverlay = imp.getOverlay()==null?"Image does not have overlay.":"Image contains overlay.";
			//imgOverlay = wrapString(imgOverlay, lineWidth, 1) + "\n";
			
			return (imageTitle + imageSize + imageDimension + imageRoi + "\n");
		}
		// function to generate statistic information
		public String getSpotInfo () {
			// extract area info and area unit string
			double countArea = sourceImage.getStatistics().area;
			String areaUnit = "pixel²";
			String unit = sourceImage.getCalibration().getUnit().toLowerCase();
			System.out.println("debug: pixel unit: " + unit);
			if (unit.equals("micron") || unit.equals("µm") || unit.equals("um")) {
				areaUnit = "µm²";
				if (countArea > 1e3) {
					countArea /= 1e6;
					areaUnit = "mm²";
				}
			}
			String areaInfo = sourceImage.getRoi()==null ? "Image area: " : "ROI area: ";
			areaInfo += new DecimalFormat("0.###").format(countArea) + " " + areaUnit;
			areaInfo = wrapString(areaInfo, lineWidth, 1) + "\n";
			// extract spot counting info
			int nSpot = filterOverlay (1, 0, true, 0.0d);
			String cellCountInfo = "Cell count: " + String.valueOf(nSpot);
			String cellDensityInfo = "      Cell density: " + new DecimalFormat("0.###").format(nSpot/countArea) + " cells/" + areaUnit;
			cellCountInfo = wrapString(cellCountInfo+cellDensityInfo, lineWidth, 1) + "\n";

			return (areaInfo + cellCountInfo + "\n");
		}
		// function to wrap string after certain length for display in text area
		public String wrapString(
				String inputLongString,
				int wrapLength,
				int indent
				) {
			String wrappedString = ""; String indentStr = "";
			for (int i=0; i<indent; i++)
				indentStr += " ";
			for (int i=0; i<inputLongString.length(); i++) {
				if (i!=0 && i%lineWidth==0)	wrappedString += ("\n"+indentStr);
				wrappedString += inputLongString.charAt(i);
			}
			return wrappedString;
		}
		
}
