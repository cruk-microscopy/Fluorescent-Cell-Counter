package uk.ac.cam.cruk.mrlab;

import java.awt.Point;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;

public class Filter {
	
	//private final String[] statString = {"Mean", "StdDev"};	// median
	private int id;	// filter ID
    private String name;	// filter name    
    //private ArrayList<int[]> elements; // int[5] {AND/OR, channel, mean/stdDev, higher, value}
    private ArrayList<Rule> rules;	// filter rule list
    private int preCount;	// total count before filtering
    private int postCount;	// total count after filtering
    
    // constructor
    public Filter(){}
    public Filter(int id, String name){
        this.id = id;
        this.name = name;
        this.rules = new ArrayList<Rule>(); // create empty list of rules
    }
    // getter and setter functions of filter parameters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public ArrayList<Rule> getRules() {
    	return rules;
    }
    public void setRules(ArrayList<Rule> rules) {
    	this.rules = rules;
    }
    public int getPreCount() {
        return preCount;
    }
    public void setPreCount(int preCount) {
        this.preCount = preCount;
    }
    public int getPostCount() {
        return postCount;
    }
    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }
    
    // number of rules in the filter
    public int size() {
    	if (this.rules==null) return 0;
    	return this.rules.size();
    }
    // add new rule to filter, return ID of the rule, 1-based
    public int addRule(Rule rule) {
    	int newID = this.size() + 1;
    	if (this.rules==null) this.rules = new ArrayList<Rule>();
    	rule.id = newID;
    	rules.add(rule);
    	return newID;
    }
    // get rule by ID
    public Rule getRule(int id) {
    	if (this.rules==null)
    	for (Rule rule : this.rules) {
    		if (rule.id == id)
    			return rule;
    	}
    	return null;
    }
    // convert Filter object info to String
    @Override
    public String toString() {	
    	String str = "ID " + this.id + ": ";
		str += "name: " + this.name + "\n";
		str += "Rules:\n";
		for (Rule rule : this.rules) {
			str += " " + rule.toString() + "\n";
		}
		str += "preCount: " + this.preCount + ", postCount: " + this.postCount; 		
    	return str;
    }
    
    // filter overlay, and return
    public static Filter filterOverlay (
 			Filter filter,
 			ImagePlus sourceImage,
 			Overlay spotOverlay,
 			boolean firstFilter,
 			boolean pointSpot
 			) { 
    	return filterOverlay(filter, sourceImage, spotOverlay, firstFilter, pointSpot, DetectionUtility.spotMean, DetectionUtility.spotStdDev);
    }
 	public static Filter filterOverlay (
 			Filter filter,
 			ImagePlus sourceImage,
 			Overlay spotOverlay,
 			boolean firstFilter,
 			boolean pointSpot,
 			double[][] mean,
 			double[][] stdDev
 			) { // AND/OR, channel, mean/stdDev, higher, value
 		if (sourceImage==null || sourceImage.getOverlay()==null) return filter;
 		if (filter==null || filter.size()==0) return filter;
 		Roi r = sourceImage.getRoi();
 		int posC = sourceImage.getC();
 		
 		Overlay overlay = firstFilter ? spotOverlay.duplicate() : sourceImage.getOverlay();
 		if (pointSpot)	// change point to oval if neccessary
 			overlay = DetectionUtility.pointToOvalOverlay(overlay);
 		Roi[] rois = overlay.toArray();
 		filter.setPreCount(overlay.size());
 
 		for (int i=0; i<rois.length; i++) {
 			sourceImage.setRoi(rois[i], false);
 			boolean passPrevRules = true;
 			for (int j=0; j<filter.size(); j++) {
 				boolean passCurrentRule = false;
 				Rule rule = filter.getRules().get(j);
 				
 				sourceImage.setPositionWithoutUpdate(rule.channel, 1, 1);
 				double value = rule.statParam==0 ? sourceImage.getRawStatistics().mean : sourceImage.getRawStatistics().stdDev;
 				if (rule.higher ? (value>rule.value) : (value<rule.value)) {
 					passCurrentRule = true;
 				}
 				if (rule.logicalAND) {
 					passPrevRules = (passPrevRules && passCurrentRule);
 				} else { //logical OR
 					passPrevRules = (passPrevRules || passCurrentRule);
 				}
 			}
 			if (!passPrevRules)
 				overlay.remove(rois[i]);
 		}
 		filter.setPostCount(overlay.size());
 		
 		if (pointSpot) // change oval back to point if neccessary
 			overlay = DetectionUtility.ovalToPointOverlay(overlay);
 		sourceImage.setOverlay(overlay);
 		sourceImage.setC(posC);
 		sourceImage.setRoi(r);
 		// update Z-scores;
 		if (mean!=null && stdDev!=null) {
	 		for (Rule rule : filter.getRules()) {
	 			double[] data = rule.statParam==0 ? mean[rule.channel-1] : stdDev[rule.channel-1];
	 			double[] regularizedData = StatisticUtility.regularizeDataByZscore(data);
	 			double[] MeanAndStd = StatisticUtility.getMeanAndStdFast(regularizedData);
	 			rule.valueInZScore = StatisticUtility.valueToZScore(rule.value, MeanAndStd);
	 		}
 		}
 		return filter;
 	}
 	
 	 // filter overlay, use z-score
 	public static Filter filterOverlay2 (
 			Filter filter,
 			ImagePlus sourceImage,
 			Overlay spotOverlay,
 			boolean firstFilter,
 			boolean pointSpot
 			) { // AND/OR, channel, mean/stdDev, higher, value
 		if (sourceImage==null || sourceImage.getOverlay()==null) return filter;
 		if (filter==null || filter.size()==0) return filter;
 		
 		Roi r = sourceImage.getRoi();
 		int posC = sourceImage.getC();
 		
 		Overlay overlay = firstFilter ? spotOverlay : sourceImage.getOverlay();
 		if (pointSpot)	// change point to oval if neccessary
 			overlay = DetectionUtility.pointToOvalOverlay(sourceImage.getOverlay());

 		Roi[] rois = overlay.toArray();
 		filter.setPreCount(overlay.size());
 		// populate data 2-D array
 		double[][] data = new double[filter.size()][rois.length];
 		for (int i=0; i<rois.length; i++) {
 			sourceImage.setRoi(rois[i], false);
 			for (int j=0; j<filter.size(); j++) {
 				Rule rule = filter.getRules().get(j);
 				sourceImage.setPositionWithoutUpdate(rule.channel, 1, 1);
 				double value = rule.statParam==0 ? sourceImage.getRawStatistics().mean : sourceImage.getRawStatistics().stdDev;
 				data[j][i] = value;
 			}
 		}
 		// get z-scores;
 		for (int i=0; i<filter.size(); i++) {
 			double[] regularizedData = StatisticUtility.regularizeDataByZscore(data[i]);
 			double[] MeanAndStd = StatisticUtility.getMeanAndStdFast(regularizedData);
 			//double[] MeanAndStd = StatisticUtility.getMeanAndStdFast(data[i]);
 			filter.getRules().get(i).value = Math.max(0, StatisticUtility.zScoreToValue(filter.getRules().get(i).valueInZScore, MeanAndStd));
 		}
 		// filter data with updated value
 		for (int i=0; i<rois.length; i++) {
 			boolean passPrevRules = true;
 			for (int j=0; j<filter.size(); j++) {
 				boolean passCurrentRule = false;
 				Rule rule = filter.getRules().get(j);
 				double value = data[j][i];
 				if (rule.higher ? (value>rule.value) : (value<rule.value)) {
 					passCurrentRule = true;
 				}
 				if (rule.logicalAND) {
 					passPrevRules = (passPrevRules && passCurrentRule);
 				} else { //logical OR
 					passPrevRules = (passPrevRules || passCurrentRule);
 				}
 			}
 			if (!passPrevRules)
 				overlay.remove(rois[i]);
 		}
 		filter.setPostCount(overlay.size());
 		
 		if (pointSpot) // change oval back to point if neccessary
 			overlay = DetectionUtility.ovalToPointOverlay(overlay);
 		sourceImage.setOverlay(overlay);
 		sourceImage.setC(posC);
 		sourceImage.setRoi(r);
 		
 		return filter;
 	}
	
}
