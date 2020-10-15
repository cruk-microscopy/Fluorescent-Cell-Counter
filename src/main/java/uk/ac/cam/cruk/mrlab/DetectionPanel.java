package uk.ac.cam.cruk.mrlab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.SpotCollection;
import ij.ImagePlus;
import ij.gui.Overlay;


/*
 * Detection Panel:
 * 
 * 	[auto ROI] [save ROI] [show/hide ROI] [add] [delete] [setup]
 * 
 * 	Detection Channel		Cell Diameter (µm)		Quality Threshold
 * 	+-------+-------+		[		8		]:		[		40		]:
 *  1		2		3
 * 
 * 	[refresh source]	[prepare source] 	[show/hide spot] 	[setup]
 * 
 * 
 * 	detect cell (spot) in a multi-channel image (current Z, current T), by fluorescent intensity in target channels
 * 	@ GUI component:
 * 		- detection channel: target channel for detection
 * 		- cell diameter: spot diameter for LoG detector
 * 		- quality threshold: LoG detector SNR based quality threshold (in trackmate)
 * 		- refresh source: button to refresh source image, change detection channel slider
 * 		- prepare source: change color represent for multi-channel image, 
 * 		- show/hide spot: display the LoG spot detected (directly from TrackMate, convereted to overlay)
 * 		- setup: generate dialog for: prepare source color, ROI color, autoROI
 * 
 *  @ Parameters:
 * 		- ImagePlus sourceImage: the image to operate on;
 * 		- targetChannel: target channel for LoG spot detection;
 * 		- spotRadius: LoG spot radius;
 * 		- qualityThreshold: LoG spot quality threshold;
 * 
 * 		- Overlay spotOverlay: contains all the cell ROIs
 */

public class DetectionPanel {
	
	// LoG spot detection parameters:
	protected static ImagePlus sourceImage = null;
	protected static Overlay spotOverlay = null;
	

	protected static int targetChannel;
	protected static double spotRadius = 4.0d;
	protected static double qualityThreshold = 40;
	
	
	protected static SpotCollection tmSpots;
	
	// GUI component
	protected static JPanel detectionPanel;
	protected static JPanel spotDetectionPanel;
	
	protected static final JLabel sliderLabel = new JLabel("Detection Channel", JLabel.CENTER);
	protected static final JLabel diameterLabel = new JLabel("Cell Diameter (µm)", JLabel.CENTER);
	protected static final JLabel qualityThresholdLabel = new JLabel("Quality Threshold", JLabel.CENTER);
	protected static JSlider channelSlider;
	protected static JSpinner diameterSpinner;
	protected static JSpinner qualityThresholdSpinner;
	
	protected static JButton btnRefresh = new JButton("refresh source");
	protected static JButton btnPrepare = new JButton("prepare source");
	protected static JButton btnShow = new JButton("show/hide detection");
	protected static JButton btnSetup = new JButton("setup");
	
	protected final int lineWidth = 60;
	protected static Color panelColor = new Color(204, 229, 255);
	protected final Font textFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color fontColor = Color.BLACK;
	protected final Font errorFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color errorFontColor = Color.RED;
	protected final Color textAreaColor = new Color(204, 229 , 255);
	protected final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected final Color panelTitleColor = Color.BLUE;
	protected final static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	protected final Dimension textAreaMax = new Dimension(260, 150);
	protected final Dimension tablePreferred = new Dimension(260, 100);
	protected final Dimension tableMax = new Dimension(260, 150);
	protected final Dimension panelTitleMax = new Dimension(500, 30);
	//protected final static Dimension panelMax = new Dimension(600, 400);
	protected final Dimension panelMin = new Dimension(280, 200);
	protected final static Dimension buttonSize = new Dimension(120, 10);
	
	protected final static Dimension numberMax = new Dimension(100, 20);
	
	protected static void setImage (ImagePlus imp) {
		sourceImage = imp;
		return;
	}
	protected static void setSpot (Overlay overlay) {
		spotOverlay = overlay;
		return;
	}
	
	protected static void addDetectionPanel(JPanel parentPanel) {
		panelColor = GUIUtility.panelColor;
		DetectionUtility.loadParameters();
		ROIUtility.addROIPanel(parentPanel);
		addSpotPanel(parentPanel);
		addSourcePanel(parentPanel);
		
		parentPanel.validate();
		parentPanel.revalidate();
		
	}
	
	// configure channel slider
	protected static void configureChannelSlider () {
        channelSlider = new JSlider(JSlider.HORIZONTAL, 1, targetChannel, 1);
        channelSlider.setMajorTickSpacing(1);
        channelSlider.setMinorTickSpacing(1);
        channelSlider.setPaintTicks(true);
        channelSlider.setPaintLabels(true);
        channelSlider.setValue(targetChannel);
        channelSlider.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		channelSlider.setBackground(panelColor);
	}
	
	//Create the spot detection panel.
	protected static void addSpotPanel (JPanel parentPanel) {
		// load saved parameters
		targetChannel = DetectionUtility.targetChannel;
		spotRadius = DetectionUtility.spotRadius;
		qualityThreshold = DetectionUtility.qualityThreshold;
		// create spot detection panel consist of: channel, radius, and quality panels
		spotDetectionPanel = new JPanel();
		JPanel channelPanel = new JPanel();
		JPanel radiusPanel = new JPanel();
		JPanel qualityPanel = new JPanel();
		// channel detection: label with slider
	        //JLabel sliderLabel = new JLabel("Detection Channel", JLabel.CENTER);
			//int nChannels = sourceImage==null ? 1 : sourceImage.getNChannels();
	        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	        configureChannelSlider();
		// diameter of detection: label with spinner
			//JLabel radiusLabel = new JLabel("Cell Diameter (µm)", JLabel.CENTER);
		    diameterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	        SpinnerNumberModel model = new SpinnerNumberModel(spotRadius*2, 0.1d, 1000.0d, 0.1d);
	        diameterSpinner = new JSpinner(model);
	        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)diameterSpinner.getEditor();
	        editor.getFormat().setMinimumFractionDigits(2);
	        editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
			diameterSpinner.setPreferredSize(numberMax);
			diameterSpinner.setMinimumSize(numberMax);
		// quality threshold of detection: label with spinner
			//JLabel qualityLabel = new JLabel("Quality Threshold", JLabel.CENTER);
			qualityThresholdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			SpinnerNumberModel model2 = new SpinnerNumberModel(qualityThreshold, 0.001d, 1000.0d, 1.0d);
	        qualityThresholdSpinner = new JSpinner(model2);
	        JSpinner.NumberEditor editor2 = (JSpinner.NumberEditor)qualityThresholdSpinner.getEditor();
	        editor2.getFormat().setMinimumFractionDigits(3);
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
			radiusPanel.add(diameterLabel);
			radiusPanel.add(diameterSpinner);
			radiusPanel.setBorder(border);
			radiusPanel.setBackground(panelColor);
			radiusPanel.setMaximumSize(new Dimension(500, 55));
			radiusPanel.setPreferredSize(new Dimension(500, 55));
			qualityPanel.setLayout(new BoxLayout(qualityPanel, BoxLayout.Y_AXIS));
			qualityPanel.add(qualityThresholdLabel);
			qualityPanel.add(qualityThresholdSpinner);
			qualityPanel.setBorder(border);
			qualityPanel.setBackground(panelColor);
			qualityPanel.setMaximumSize(new Dimension(500, 55));
			qualityPanel.setPreferredSize(new Dimension(500, 55));
		// add the three sub-panel to detection panel
		spotDetectionPanel.setLayout(new BoxLayout(spotDetectionPanel, BoxLayout.X_AXIS));
		spotDetectionPanel.add(channelPanel);
		spotDetectionPanel.add(radiusPanel);
		spotDetectionPanel.add(qualityPanel);
		spotDetectionPanel.setBorder(border);
		spotDetectionPanel.setBackground(panelColor);
		spotDetectionPanel.setMaximumSize(GUIUtility.panelMax);
		
		// add detection panel to parent panel
		spotDetectionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		parentPanel.add(spotDetectionPanel);
		
		// add action/change listener to detection components
		configureSpotDetectionFunctions();
	        
	        
	}
	
	protected static void addSourcePanel (JPanel parentPanel) {
		DetectionUtility.sourceImage = null; // might grab the current active image
		JPanel sourcePanel = new JPanel();
		sourcePanel.add(btnRefresh);
		sourcePanel.add(btnPrepare);
		sourcePanel.add(btnShow);
		sourcePanel.add(btnSetup);
		sourcePanel.setBorder(border);
		sourcePanel.setBackground(panelColor);
		sourcePanel.setMaximumSize(GUIUtility.panelMax);
		sourcePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		parentPanel.add(sourcePanel);
		configureSourcePanelButtonFunctions();
	}
	
	protected static void configureSpotDetectionFunctions () {
		channelSlider.addChangeListener(new ChangeListener() {
			@Override 
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting())
				    targetChannel = (int)source.getValue();
				DetectionUtility.setChannel(targetChannel);
				DetectionUtility.displaySpots();
        }});
		diameterSpinner.addChangeListener(new ChangeListener() {
			@Override 
			public void stateChanged(ChangeEvent e) {
				JSpinner source = (JSpinner)e.getSource();
				spotRadius = (double)source.getValue() / (double)2;
				DetectionUtility.setRadius(spotRadius);
				DetectionUtility.displaySpots();
		}});
        qualityThresholdSpinner.addChangeListener(new ChangeListener() {
			@Override 
			public void stateChanged(ChangeEvent e) {
				JSpinner source = (JSpinner)e.getSource();
				qualityThreshold = (double)source.getValue();
				DetectionUtility.setQualityThreshold(qualityThreshold);
				DetectionUtility.displaySpots();
		}});
	}
	
	protected static void configureSourcePanelButtonFunctions () {
		
		btnRefresh.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { 
				DetectionUtility.refreshSource();
				sourceImage = DetectionUtility.sourceImage;
				if (sourceImage!=null) channelSlider.setMaximum(sourceImage.getNChannels());
			}
		});
		
		btnPrepare.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { DetectionUtility.prepareSource();}
		});
		
		btnShow.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { DetectionUtility.showHideOverlay();}
		});
		
		btnSetup.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { DetectionUtility.configSourceDisplay();}
		});
		
	}
	
		
	
}
