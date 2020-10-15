package uk.ac.cam.cruk.mrlab;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;
import ij.ImagePlus;

import ij.gui.Overlay;


import uk.ac.cam.cruk.mrlab.Filter;
/*
 * Filter Panel:
 * 	filter a multi-channel image (current Z, current T), by fluorescent intensity in given channels
 * 		- ImagePlus sourceImage: the image to operate on
 * 		- Overlay spotDetected: contains all the cell ROIs
 * 		- 
 * 		- take overlay from trackmate spot detection 
 */
//import uk.ac.cam.cruk.mrlab.Filter.Rule;

public class FilterPanel {
	

	protected static ImagePlus sourceImage = null;
	protected static Overlay spotOverlay = null;
	
	protected static String[] channelStrings;
	
	protected static JPanel filterPanel;
	protected static JPanel buttonPanel;
	
	protected static final String filterPanelName = "Filter ";
	//protected static final String elementName = "element";
	
	protected static JButton btnAddFilter;
	protected static JButton btnApplyFilter;
	protected static JButton btnNameFilter;
	protected static JButton btnSaveFilter;
	protected static JButton btnLoadFilter;
	protected static Dimension buttonSize = new Dimension(120, 50);
	

	//protected final static Dimension panelMax = new Dimension(650, 400);

	protected final static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));

	protected static Color panelColor = new Color(204, 229, 255);
	
	protected static ArrayList<Filter> filterList;
	
	protected static String filterDir = System.getProperty("user.home");
	protected static String filterPath = filterDir + File.separator + "filter.xml";;
	
	// set source image and spotOverlay from detection
	protected static boolean setSource () {
		return setSource(DetectionUtility.sourceImage);
	}
	protected static boolean setSource (ImagePlus imp) {
		return setSource(imp, DetectionUtility.spotOverlay);
	}
	protected static boolean setSource (ImagePlus imp, Overlay overlay) {
		if (imp==null) return false;
		sourceImage = imp;
		if (overlay==null) return false;
		//spotOverlay = overlay.duplicate();
		spotOverlay = overlay;
		//sourceImage.setOverlay(spotOverlay);
		channelStrings = DetectionUtility.getChannelStrings(imp.getNChannels());
		if (spotOverlay.size()==0) return false;
		return true;
	}
	
	protected static String[] getChannelStrings (ArrayList<Filter> filterList) {
		int nChannels = getNChannels (filterList);
		String[] channelStrings = new String[nChannels];
		for (int c=0; c<nChannels; c++) {
			channelStrings[c] = "C" + (c+1);
		}
		return channelStrings;
	}
	
	
	
	// add filter panel to parent panel
	protected static void addFilterPanel (JPanel parentPanel) {
		filterList = null;
		panelColor = GUIUtility.panelColor;
		// create a parent filter panel
		filterPanel = new JPanel();
		filterPanel.setName("filterPanel");
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
		// create a button panel, horizontal layout
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		// create buttons of the button panel
		btnAddFilter = new JButton("add filter");
	    btnApplyFilter = new JButton("apply filter");
	    btnNameFilter = new JButton("name filter");
	    btnSaveFilter = new JButton("save filter");
	    btnLoadFilter = new JButton("load filter");
		//btnApplyFilter.setPreferredSize(buttonSize);
	    // configure the functions of the buttons
		configureFilterPanelButtonFunctions();
		// add buttons to button panel
		buttonPanel.add(btnAddFilter);
		buttonPanel.add(btnApplyFilter);
		buttonPanel.add(btnNameFilter);
		buttonPanel.add(btnSaveFilter);
		buttonPanel.add(btnLoadFilter);
		//GUIUtility.arrangePanel(buttonPanel);
		//buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		//filterPanel.add(buttonPanel);
		//GUIUtility.arrangePanel(filterPanel);
		//filterPanel.add(buttonPanel);
		

		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		filterPanel.add(buttonPanel);
		filterPanel.setBorder(border);
		filterPanel.setBackground(panelColor);
		filterPanel.setMaximumSize(GUIUtility.panelMax);
		
		// add filter panel to parent panel
		parentPanel.add(filterPanel);
	}

	
	// configure button functions of filter panel
	protected static void configureFilterPanelButtonFunctions () {
		
		btnAddFilter.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { addFilter(filterPanel); }
		});
		
		btnApplyFilter.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { applyFilter(filterPanel); }
		});
		
		btnNameFilter.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { nameFilter(filterPanel); }
		});
		
		btnSaveFilter.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { saveFilter(filterPanel); }
		});
		
		btnLoadFilter.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { loadFilter(filterPanel); }
		});
		
	}

	// button action functions
	// add a new filter (as panel) to the filter panel
	protected static void addFilter (JPanel filterPanel) {
		// set source image
		if (!setSource()) return;
		// check filters
		JPanel newFilter = new JPanel();
		int numFilters = checkFilter(filterPanel);	// get number of filters, N
		newFilter.setName(filterPanelName + String.valueOf(numFilters+1));	//add a new filter with index N+1
		newFilter.setLayout(new BoxLayout(newFilter, BoxLayout.Y_AXIS));
		Rule.addElement(newFilter, true, channelStrings); // add the first Rule to the new Filter
		GUIUtility.arrangePanel(newFilter);
		filterPanel.add(newFilter);
		filterPanel.revalidate();
	    filterPanel.validate();
	}
	// apply filter(s) in the filter panel, to source image
	protected static void applyFilter (JPanel filterPanel) {
		// set source image, restore spot overlay from TrackMate result
		if (!setSource()) return;
		// compute spot data
		DetectionUtility.computeSpotData();
		// get Filter list from filter panel
		filterList = getFilterList(filterPanel);
		for (int i=0; i<filterList.size(); i++) {
			Filter filter = filterList.get(i);
			filter = Filter.filterOverlay(filter, sourceImage, spotOverlay, i==0, (DetectionUtility.spotShape==1));
		}
		ResultPanel.filterInfo = InfoUtility.getFilterInfo(sourceImage, filterList);
		ResultPanel.updateInfo();
	}
	// apply filter(s) in the filter panel, to source image
	protected static void nameFilter (JPanel filterPanel) {
		// set source image
		if (!setSource()) return;
		// apply filter if filter list is empty
		if (filterList==null)
			filterList= getFilterList(filterPanel);//applyFilter(filterPanel);
		// create rename filter dialog
		GenericDialogPlus gd = new GenericDialogPlus("Name Filter(s)");
		for (Filter filter : filterList) {
			gd.addStringField(filter.getName(), filter.getName(), 20);
		}
		gd.showDialog();
		if (gd.wasCanceled()) return;
		HashMap<String, String> namePair = new HashMap<String, String>();
		for (Filter filter : filterList) {
			String newName = gd.getNextString();
			namePair.put(filter.getName(), newName);
			filter.setName(newName);
		}
		renameFilterPanel(namePair);
		ResultPanel.filterInfo = InfoUtility.getFilterInfo(DetectionUtility.getSource(), filterList);
		ResultPanel.updateInfo();
		GUIUtility.updateFrame();
	}
	// save filter(s) in the filter panel to xml file
	protected static void saveFilter(JPanel filterPanel) {
		// set source image
		if (!setSource()) return;
		// apply filter if filter list is empty
		if (filterList==null)
			applyFilter(filterPanel);
		// create file saving dialog
		DefaultPrefService prefs = new DefaultPrefService();
		filterDir = prefs.get(String.class, "MaikeCellCounter-filterDir", filterDir);
		GenericDialogPlus gd = new GenericDialogPlus("Save Filter to File");
		gd.addDirectoryField("Folder", filterDir);
		gd.addStringField("File Name", "filter.xml", 20);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		filterDir = gd.getNextString();
		filterPath = filterDir + File.separator + gd.getNextString();
		prefs.put(String.class, "MaikeCellCounter-filterDir", filterDir);
		prefs.put(String.class, "MaikeCellCounter-filterPath", filterPath);
		if (!filterPath.endsWith(".xml")) filterPath += ".xml";
		// save Filter list to xml file
		IOUtility.saveFilterToXmlFile(filterList, filterPath);
	}
	// load filters from xml file, and update the filter panel
	protected static void loadFilter(JPanel filterPanel) {
		// set source image, restore spot overlay from TrackMate result
		//if (!setSource()) return;
		// create file loading dialog		
		DefaultPrefService prefs = new DefaultPrefService();
		filterPath = prefs.get(String.class, "MaikeCellCounter-filterPath", filterPath);
		GenericDialogPlus gd = new GenericDialogPlus("Load Filter from File");
		gd.addFileField("File", filterPath);
		String[] choice = {" relative ", " absolute "};
		gd.addChoice("use", choice, choice[0]);
		gd.addToSameRow(); gd.addMessage("value");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		filterPath = gd.getNextString();
		boolean doZScore = (gd.getNextChoiceIndex()==0);
		prefs.put(String.class, "MaikeCellCounter-filterPath", filterPath);
		// load Filter list from xml file
		filterList = IOUtility.loadFilterFromXmlFile(filterPath);
		if (filterList==null) return;
		// update Z-score in filters
		if (doZScore && setSource()) { // update Z-score
			DetectionUtility.computeSpotData();
			double[][] mean = DetectionUtility.spotMean; //new double[numC][numSpot];
	 		double[][] stdDev = DetectionUtility.spotStdDev; //new double[numC][numSpot];
	 		for (Filter filter : filterList) {
		 		for (Rule rule : filter.getRules()) {
		 			double[] data = rule.statParam==0 ? mean[rule.channel-1] : stdDev[rule.channel-1];
		 			double[] regularizedData = StatisticUtility.regularizeDataByZscore(data);
		 			double[] MeanAndStd = StatisticUtility.getMeanAndStdFast(regularizedData);
		 			rule.value = StatisticUtility.zScoreToValue(rule.valueInZScore, MeanAndStd);
		 		}
	 		}
		}
		// set source image // known bug, change Detection.spotOverlay size
		/*
		if (setSource()) {
			//sourceImage.setOverlay(spotOverlay);
			spotOverlay = DetectionUtility.spotOverlay;
			sourceImage.setOverlay(spotOverlay);
			//ArrayList<Filter> filterList2 = new ArrayList<Filter>();
			for (int i=0; i<filterList.size(); i++) {
				Filter filter = filterList.get(i);
				if (doZScore)
					filter = Filter.filterOverlay2(filter, sourceImage, spotOverlay, i==0, DetectionUtility.spotShape==1);
				else
					filter = Filter.filterOverlay(filter, sourceImage, spotOverlay, i==0, DetectionUtility.spotShape==1);
			}
			//filterList = filterList2;
		}*/
		updateFilterPanel(filterList);
		GUIUtility.updateFrame();
	}
	
	// check in parent panel, how many filters have already exist
	protected static int checkFilter(JPanel parentPanel) {
		int filterNumber = 0;
		Component[] panels = parentPanel.getComponents();
		for (Component panel : panels) {
			if (panel instanceof JPanel) {	// filter panel 1, 2, 3, 4...
				String name = ((JPanel)panel).getName();
				if (name==null) continue;
				if (name.contains(filterPanelName))
					filterNumber++;
			}
		}
		return filterNumber;
	}
	// get filter ID from sub-filter panel
	/*
	protected static int getFilterID(JPanel filterPanel) {
		String name = filterPanel.getName();
		if (name==null || !name.contains(filterPanelName)) return -1;
		int idx = name.lastIndexOf(filterPanelName);
		if (idx==-1) return -1;
		idx += filterPanelName.length();
		int ID = Integer.valueOf(name.substring(idx, name.length()));
		return ID;
	}*/
	// get filter ID from filter (panel) name
	protected static int getFilterID(String name) {
		if (name==null) return -1;
		int idx = name.lastIndexOf(filterPanelName);
		if (idx==-1) return -1;
		idx += filterPanelName.length();
		int ID = Integer.valueOf(name.substring(idx, name.length()));
		return ID;
	}
	// create a list of Filter, based on the global filter panel
	protected static ArrayList<Filter> getFilterList (JPanel filterPanel) {
		ArrayList<Filter> filterList = new ArrayList<Filter>();
		Component[] panels = filterPanel.getComponents();
		for (Component panel : panels) {
			if (panel instanceof JPanel) {	// filter panel 1, 2, 3, 4...
				Filter filter = getFilter((JPanel) panel);
				if (filter!=null) filterList.add(getFilter((JPanel) panel));
			}
			
		}
		return filterList;
	}
	// create a Filter object based on a filter panel
	protected static Filter getFilter (JPanel panel) {
		// for every filter in panel, create a Filter object
		String name = (panel).getName(); // Filter 1, Filter 2, ...
		if (name==null || !name.contains(filterPanelName)) return null;
		Filter filter = new Filter(getFilterID(name), name);
		Component[] boxes = panel.getComponents();
		for (Component box : boxes) {
			if (!(box instanceof Box)) continue;
			// update filter name with first rule's first element's selected item
			if (box.getName().equals(Rule.firstRuleName)) {
				Component[] elements = ((Container) box).getComponents();
				for (Component element : elements) {
					if (!(element instanceof JComboBox)) continue;
					if (element.getName().equals(Rule.elementName + "1")) {
						filter.setName((String) ((JComboBox) element).getSelectedItem());
					}
				}
			}
			// create a Rule object, from a Box element in the filter panel
			Rule rule = Rule.getRule((Box) box);
			filter.addRule(rule);
		}
		return filter;
	}
	// update filter panel based on Filter list
	protected static void updateFilterPanel (ArrayList<Filter> filterList) {
		// skip source check (risky!!!)
		Component[] panels = filterPanel.getComponents();
		for (Component panel : panels) {
			if (panel instanceof JPanel) {
				if (panel!=null && panel.getName()!=null && panel.getName().contains(filterPanelName))
					filterPanel.remove(panel);
			}
		}
		String[] channelStrings = getChannelStrings(filterList);
		for (Filter filter : filterList) {
			JPanel newFilter = new JPanel();
			newFilter.setName(filterPanelName + String.valueOf(filter.getId()));	//add a new filter with index N+1
			newFilter.setLayout(new BoxLayout(newFilter, BoxLayout.Y_AXIS));
			Rule.addElements(newFilter, filter, channelStrings);
			GUIUtility.arrangePanel(newFilter);
			filterPanel.add(newFilter);   
		}
		filterPanel.revalidate();
		filterPanel.repaint();
	}
	// rename filters in filter panel based on Filter list
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static void renameFilterPanel (HashMap<String, String> namePair) {
		Component[] panels = filterPanel.getComponents();
		for (Component panel : panels) {
			if (!(panel instanceof JPanel)) continue;
			if (panel==null || panel.getName()==null) continue;
			if (!namePair.containsKey(panel.getName())) continue;
			String oldName = panel.getName();
			String newName = namePair.get(oldName);
			// locate the filter panel, and then the first box element
			Component[] boxes = ((Container) panel).getComponents();
			for (Component box : boxes) {
				if (!(box instanceof Box)) continue;
				Component[] elements = ((Container) box).getComponents();
				for (Component element : elements) {
					if (!(element instanceof JComboBox)) continue;
					String name = (String) ((JComboBox) element).getSelectedItem();
					if (name.equals(oldName)) {
						DefaultComboBoxModel model = new DefaultComboBoxModel(new String[]{newName});
						((JComboBox) element).setModel( model );
					}
				}
			}
		}
		filterPanel.revalidate();
		filterPanel.repaint();
	}
	// get maximum number of channels from filter list
	protected static int getNChannels(ArrayList<Filter> filterList) {
		int numC = 0;
		for (Filter filter : filterList) {
			for (Rule rule : filter.getRules()) {
				numC = Math.max(numC, rule.channel);
			}
		}
		return numC;
	}
	
	
}
