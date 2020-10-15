package uk.ac.cam.cruk.mrlab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FileUtils;
import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.LUT;

import loci.common.services.ServiceException;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLServiceImpl;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.LociPrefs;
import ome.units.UNITS;



public class ResultPanel {
	
	protected static ImagePlus sourceImage;
	protected static Overlay spotOverlay;
	
	protected static final String noImage = "Image not set.";
	
	//protected static JTextArea sourceInfo;
	protected static JTextArea resultInfo;
	
	protected static String imageInfo = "";
	protected static String spotInfo = "";
	protected static String filterInfo = "";

	
	protected static JButton btnShowTable;
	protected static JButton btnShowHistogram;
	protected static JButton btnShowROI;
	protected static JButton btnSaveSetup;
	protected static JButton btnLoadSetup;
	protected static JButton btnProcessFile;
	
	protected static String setupDir = System.getProperty("user.home");
	protected static String setupFilePath = setupDir + File.separator + "setup.xml";

	// GUI parameters
	protected final int lineWidth = 90;
	protected static Color panelColor = new Color(204, 229, 255);
	protected static JPanel resultPanel;
	protected final static Font textFont = new Font("Helvetica", Font.BOLD, 13);
	protected final static Color fontColor = Color.BLACK;
	protected final Color textAreaColor = new Color(204, 229 , 255);
	protected final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected final Color panelTitleColor = Color.BLUE;
	protected final static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	
	protected final Dimension buttonSize = new Dimension(90, 10);
	
	protected final Dimension numberMax = new Dimension(100, 20);
	
	
	
	// add filter panel to parent panel
	protected static void addResultPanel (JPanel parentPanel) {
		// update panel color
		panelColor = GUIUtility.panelColor;
		
		// result panel for reporting result: add real measurement of mean and stdDev of each channel in the future
		resultPanel = new JPanel();
		resultPanel.setName("resultPanel");
		resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
		
		
		resultInfo = new JTextArea(550, 70);
		//spotCount.setMaximumSize(new Dimension(550, 800));
		//spotCount.setMinimumSize(new Dimension(550, 800));
		resultInfo.setEditable(false);
		resultInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		resultInfo.setFont(textFont);
		resultInfo.setForeground(fontColor);
		resultInfo.setBorder(border);
		resultInfo.setBackground(new Color(255, 255, 204));
		
		resultInfo.setText(noImage);
			
		/*
			// create a button panel, horizontal layout
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			// create buttons of the button panel
			btnShowTable = new JButton("result table");
		    btnShowHistogram = new JButton("histogram");
		    btnShowROI = new JButton("save spot");
			btnSaveSetup = new JButton("save setup");
			btnProcessFile = new JButton("process file");
			//btnApplyFilter = new JButton("apply filter");	    
		    //btnSaveFilter = new JButton("save filter");
		    //btnLoadFilter = new JButton("load filter");
			//btnApplyFilter.setPreferredSize(buttonSize);
		    // configure the functions of the buttons
			//configureFilterPanelButtonFunctions();
			
			// add buttons to button panel
			buttonPanel.add(btnShowTable);
			buttonPanel.add(btnShowHistogram);
			buttonPanel.add(btnShowROI);
			buttonPanel.add(btnSaveSetup);
			buttonPanel.add(btnProcessFile);

			
			//GUIUtility.arrangePanel(buttonPanel);
			//buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			//filterPanel.add(buttonPanel);
			//GUIUtility.arrangePanel(filterPanel);
			//filterPanel.add(buttonPanel);
			
			configureResultPanelButtonFunctions();

			buttonPanel.setBorder(border);
			buttonPanel.setBackground(panelColor);
			buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		*/
		
		
		
		resultPanel.setBackground(panelColor);
		resultPanel.add(resultInfo);
		//resultPanel.add(buttonPanel);
		addButtonPanel(resultPanel);
		configureResultPanelButtonFunctions();
		resultPanel.setBorder(border);
		//resultPanel.setBackground(panelColor);
		resultPanel.setMaximumSize(GUIUtility.panelMax);
		
		/*
		Timer timer = new Timer(550, new ActionListener() {
	        public void actionPerformed(ActionEvent evt) {
	        	
	        	sourceImage = DetectionUtility.getSource();
	        	spotOverlay = DetectionUtility.spotOverlay;
	        	if (sourceImage==null) {
	        		resultInfo.setText(noImage);
	        	} else {
	        		resultInfo.setText(InfoUtility.getImageInfo(sourceImage)+ "\n" + InfoUtility.getSpotInfo(sourceImage, spotOverlay));
	        	}
	        	resultInfo.repaint();	
	        }
	    });
		timer.start();
		*/
		
    	//updateInfo();
		// add result panel to parent panel
		parentPanel.add(resultPanel);
		parentPanel.validate();
		parentPanel.revalidate();

	}
	
	// create button panel, and add to result panel
	protected static void addButtonPanel (JPanel resultPanel) {
		// add button groups
		JPanel buttonPanel = new JPanel();
		btnShowTable = new JButton("result table");
	    btnShowHistogram = new JButton("histogram");
	    btnShowROI = new JButton("to ROI Manager");
		btnSaveSetup = new JButton("save setup");
		btnLoadSetup = new JButton("load setup");
		btnProcessFile = new JButton("process file");
		//btnShowTable.setPreferredSize(buttonSize);
		// and 3 buttons horizontally aligned: refresh, load, resize
		GroupLayout buttonLayout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(buttonLayout);
		buttonLayout.setAutoCreateGaps(true);
		buttonLayout.setAutoCreateContainerGaps(true);

		buttonLayout.setHorizontalGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			 .addGroup(buttonLayout.createSequentialGroup()
			    .addComponent(btnShowTable)
			    .addComponent(btnShowHistogram)
			    .addComponent(btnShowROI))
			 .addGroup(buttonLayout.createSequentialGroup()
		        .addComponent(btnSaveSetup)
		        .addComponent(btnLoadSetup)
		        .addComponent(btnProcessFile)));
			                
		buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnShowTable, btnShowHistogram, btnShowROI, btnSaveSetup, btnLoadSetup, btnProcessFile);	
		
		buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
			.addGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addGroup(buttonLayout.createSequentialGroup()
			                .addComponent(btnShowTable)
			                .addComponent(btnSaveSetup))
					.addGroup(buttonLayout.createSequentialGroup()
			                .addComponent(btnShowHistogram)
			                .addComponent(btnLoadSetup))
					.addGroup(buttonLayout.createSequentialGroup()
			                .addComponent(btnShowROI)
			                .addComponent(btnProcessFile))));
		
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		resultPanel.add(buttonPanel);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	}
	
	// configure button functions of filter panel
	protected static void configureResultPanelButtonFunctions () {
		
		btnShowTable.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { showResultTable(); }
		});
		
		btnShowHistogram.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { showHistogram(); }
		});
		
		btnShowROI.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { showROI(); }
		});
		
		btnSaveSetup.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { saveSetup(); }
		});
		
		btnLoadSetup.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { loadSetup(); }
		});
		
		btnProcessFile.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { batchProcessFile(); }
		});
		
		
	}
	
	









	protected static void showResultTable() {
		sourceImage = DetectionUtility.sourceImage;
		spotOverlay = DetectionUtility.spotOverlay;
		if (sourceImage==null || spotOverlay==null || spotOverlay.size()==0) return;
		Roi[] spotRois = spotOverlay.toArray();
		
		DetectionUtility.computeSpotData();
		double[][] dataMean = DetectionUtility.spotMean;
		double[][] dataStdDev = DetectionUtility.spotStdDev;
		if (dataMean==null || dataStdDev==null) return;
		
		int numC = dataMean.length;
		int numSpot = dataMean[0].length;
		
		ResultsTable spotTable = new ResultsTable();
		spotTable.setPrecision(3);
		for (int i=0; i<numSpot; i++) {
			spotTable.incrementCounter();
			spotTable.addValue("Spot", i+1);
			spotTable.addValue("X", spotRois[i].getContourCentroid()[0]);
			spotTable.addValue("Y", spotRois[i].getContourCentroid()[1]);
			for (int j=0; j<numC; j++) {
				spotTable.addValue("C"+(j+1)+" Mean", dataMean[j][i]);
				spotTable.addValue("C"+(j+1)+" StdDev", dataStdDev[j][i]);
			}
		}
		spotTable.show("Detected Spots");
		
		ArrayList<Filter> filterList = FilterPanel.filterList;
		if (filterList==null || filterList.size()==0) return;
		ResultsTable filterTable = new ResultsTable();
		filterTable.setPrecision(3);
		for (Filter filter : filterList) {
			filterTable.incrementCounter();
			filterTable.addValue("ID", filter.getId());
			filterTable.addValue("name", filter.getName());
			filterTable.addValue("pre-filter count", filter.getPreCount());
			filterTable.addValue("post-filter count", filter.getPostCount());
			filterTable.addValue("percentage(%)", (double)filter.getPostCount() * (double)100 / (double)filter.getPreCount());
			filterTable.addValue("cumulative percentage(%)", (double)filter.getPostCount() * (double)100 / (double)numSpot);
		}
		filterTable.show("Filter Result");
		//filterTable.saveAs(savePath + "_result.csv");
	}

	
	protected static void showHistogram() {
		sourceImage = DetectionUtility.sourceImage;
		spotOverlay = DetectionUtility.spotOverlay;
		if (sourceImage==null || spotOverlay==null || spotOverlay.size()==0) return;
		
		DetectionUtility.computeSpotData();
		double[][] dataMean = DetectionUtility.spotMean;
		double[][] dataStdDev = DetectionUtility.spotStdDev;
		if (dataMean==null || dataStdDev==null) return;
		
		int numC = dataMean.length;
		//int numSpot = dataMean[0].length;
		
		Plot plotMean = new Plot("Histogram of Spots Mean", "Spot Mean Value", "Count");
		Plot plotStdDev = new Plot("Histogram of Spots StdDev", "Spot StdDev Value", "Count");
		for (int c=0; c<numC; c++) {
			plotMean.setColor(DetectionUtility.colors[DetectionUtility.channelColors[c]]);
			plotStdDev.setColor(DetectionUtility.colors[DetectionUtility.channelColors[c]]);
			
			plotMean.addHistogram(dataMean[c]);
			plotStdDev.addHistogram(dataStdDev[c]);
			
			String name = DetectionUtility.colorStrings[DetectionUtility.channelColors[c]];
			String style = name + "," + name + ",filled";
			
			plotMean.setStyle(c, style);
			plotStdDev.setStyle(c, style);
		}
		//PlotWindow window = plot.show();
		plotMean.show(); plotStdDev.show();
	}
	
	
	// get current Overlay, translate to ROI, export to ROI Manager
	protected static void showROI() {
		sourceImage = DetectionUtility.sourceImage;
		if (sourceImage==null || sourceImage.getOverlay()==null || sourceImage.getOverlay().size()==0) return;
		Overlay overlay = DetectionUtility.pointToOvalOverlay(sourceImage.getOverlay());
		Roi[] rois = overlay.toArray();
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) rm = new RoiManager();
		else rm.reset();
		for (int i=0; i<rois.length; i++) {
			rm.addRoi(rois[i]);
		}
	}


	protected static void saveSetup() {
		// check source image
		if (DetectionUtility.sourceImage==null) return;
		// create file saving dialog
		DefaultPrefService prefs = new DefaultPrefService();
		setupDir = prefs.get(String.class, "MaikeCellCounter-setupDir", setupDir);
		setupFilePath = prefs.get(String.class, "MaikeCellCounter-setupFilePath", setupFilePath);
		GenericDialogPlus gd = new GenericDialogPlus("Save Setup to File");
		gd.addDirectoryField("Folder", setupDir);
		gd.addStringField("File Name", "setup.xml", 20);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		setupFilePath = gd.getNextString() + File.separator + gd.getNextString();
		prefs.put(String.class, "MaikeCellCounter-setupDir", setupDir);
		if (!setupFilePath.endsWith(".xml")) setupFilePath += ".xml";
		prefs.put(String.class, "MaikeCellCounter-setupFilePath", setupFilePath);
		// save setup to xml file
		IOUtility.saveSetupToXmlFile(setupFilePath);
	}
	
	
	protected static void loadSetup() {
		// set source image, restore spot overlay from TrackMate result
		if (DetectionUtility.sourceImage==null) return;
		// create file loading dialog		
		DefaultPrefService prefs = new DefaultPrefService();
		setupFilePath = prefs.get(String.class, "MaikeCellCounter-setupFilePath", setupFilePath);
		GenericDialogPlus gd = new GenericDialogPlus("Load Setup from File");
		gd.addFileField("File", setupFilePath);
		String[] choice = {" relative ", " absolute "};
		gd.addChoice("use", choice, choice[0]);
		gd.addToSameRow(); gd.addMessage("value");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		setupFilePath = gd.getNextString();
		boolean doZScore = (gd.getNextChoiceIndex()==0);
		prefs.put(String.class, "MaikeCellCounter-setupFilePath", setupFilePath);
		// load setup from xml file
		IOUtility.loadSetupFromXmlFile(setupFilePath, doZScore);
		GUIUtility.updateFrame();
	}
	
	
	protected static void batchProcessFile() {
		
		IOUtility.batchProcessFile();
	}

	
	protected static void updateInfo () {
		sourceImage = DetectionUtility.getSource();
		if (sourceImage==null) {
    		resultInfo.setText(noImage);
    	} else {
    		resultInfo.setText(imageInfo + "\n" + spotInfo + "\n" +  filterInfo);
    	}
		resultInfo.revalidate();
		resultInfo.repaint();
		resultPanel.revalidate();
		resultPanel.repaint();
		//GUIUtility.updateFrame();
	}

}
