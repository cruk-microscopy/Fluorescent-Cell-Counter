package uk.ac.cam.cruk.mrlab;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.ArrayUtils;
import org.jdom2.Element;

public class Rule { // Rule : as a filter element
	

	protected static final Dimension boxSize = new Dimension(550, 55);
	
	protected static final String firstRuleName = "FirstRule";
	protected static final String ruleName = "Rule";
	protected static final String elementName = "element ";
	protected static final String[] statString = {"Mean", "StdDev"};	// median
	
	protected int id;	// rule ID
	protected boolean logicalAND;	// logical AND/OR for inter-rule relationship
	protected int channel;	// channel of source image
	protected int statParam;	// statistical parameter {mean, stdDev}
	protected boolean higher;	// comparison direction
	protected double value;	// value from measurement of given ROI in the image
	protected double valueInZScore;	// value in z-score, from the whole data distribution (parametric)
	
	public Rule () {}
	
	public String toString() {
		String str = "ID " + this.id + ": ";
		str += this.logicalAND ? " AND " : " OR ";
		str += "C" + this.channel;
		str += " " + statString[this.statParam] + " ";
		str += this.higher ? " higher than " : " lower than ";
		str += " " + this.value;
		str += " (z-score: " + this.valueInZScore + ")";
		return str;
	}

	/*
	 * GUI utility of a Rule object: as a horizontal Box element in JPanel
	 */
	// populate a new filter (panel) with elements		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static void addElement (JPanel filter, boolean firstTime, String[] channelStrings) {
		Box newFilterElement = Box.createHorizontalBox();
		newFilterElement.setName(firstTime ? firstRuleName : ruleName);
		// logical AND / OR
		String[] logicalStrings = {"AND","OR"};
	    JComboBox logicalOperation = new JComboBox(logicalStrings);
	    logicalOperation.setName(elementName + "1");
	    // channel
	    JComboBox channelSeletion = new JComboBox(channelStrings);
	    channelSeletion.setName(elementName + "2");
	    // statistical parameters: mean, stdDev, ...
		String[] statStrings = {"mean","stdDev"};
	    JComboBox statSeletion = new JComboBox(statStrings);
		statSeletion.setName(elementName + "3");
	    // higher than / lower than
		String[] compareStrings = {"higher than","lower than"};
	    JComboBox compare = new JComboBox(compareStrings);
	    compare.setName(elementName + "4");
	    // threshold value
        SpinnerNumberModel model3 = new SpinnerNumberModel(0, 0.0d, 65536.0d, 100.0d);
        JSpinner thresholdSpinner = new JSpinner(model3);
        JSpinner.NumberEditor editor3 = (JSpinner.NumberEditor)thresholdSpinner.getEditor();
        DecimalFormat format3 = editor3.getFormat();
        format3.setMinimumFractionDigits(1);
        editor3.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
		thresholdSpinner.setName(elementName + "5");
		// add a new rule
		JButton btnAddElement = new JButton("+");
		btnAddElement.setMinimumSize(new Dimension(25, 25));
		btnAddElement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { addElement(filter, false, channelStrings); }
	    });
		// delete the current rule
	    JButton btnDelElement = new JButton("-");
		btnDelElement.setMinimumSize(new Dimension(25, 25));
		btnDelElement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { delElement(filter, newFilterElement, firstTime); }
	    });
		
	    if (firstTime) {
	    	JComboBox filterName = new JComboBox(new String[]{filter.getName()});
	    	filterName.setName(elementName + "1");
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
		filter.add(newFilterElement);
		filter.revalidate();
	    filter.validate();
    }
	// delete a filter or a filter element
	protected static void delElement (JPanel panel, Box box, boolean deleteAll) {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static void addElements (JPanel filterPanel, Filter filter, String[] channelStrings) {
		
		for (Rule rule : filter.getRules()) {
			boolean firstTime = (rule.id==1);
			Box newFilterElement = Box.createHorizontalBox();
			newFilterElement.setName(firstTime ? firstRuleName : ruleName);
			// logical AND / OR
			String[] logicalStrings = {"AND","OR"};
		    JComboBox logicalOperation = new JComboBox(logicalStrings);
		    logicalOperation.setName(elementName + "1");
		    // channel
		    JComboBox channelSeletion = new JComboBox(channelStrings);
		    channelSeletion.setName(elementName + "2");
		    // statistical parameters: mean, stdDev, ...
			String[] statStrings = {"mean","stdDev"};
		    JComboBox statSeletion = new JComboBox(statStrings);
			statSeletion.setName(elementName + "3");
		    // higher than / lower than
			String[] compareStrings = {"higher than","lower than"};
		    JComboBox compare = new JComboBox(compareStrings);
		    compare.setName(elementName + "4");
		    // threshold value
	        SpinnerNumberModel model3 = new SpinnerNumberModel(0, 0.0d, 65536.0d, 100.0d);
	        JSpinner thresholdSpinner = new JSpinner(model3);
	        JSpinner.NumberEditor editor3 = (JSpinner.NumberEditor)thresholdSpinner.getEditor();
	        DecimalFormat format3 = editor3.getFormat();
	        format3.setMinimumFractionDigits(1);
	        editor3.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
			thresholdSpinner.setName(elementName + "5");
			// add a new rule
			JButton btnAddElement = new JButton("+");
			btnAddElement.setMinimumSize(new Dimension(25, 25));
			btnAddElement.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) { addElement(filterPanel, false, channelStrings); }
		    });
			// delete the current rule
		    JButton btnDelElement = new JButton("-");
			btnDelElement.setMinimumSize(new Dimension(25, 25));
			btnDelElement.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) { delElement(filterPanel, newFilterElement, firstTime); }
		    });
			
			logicalOperation.setSelectedIndex( rule.logicalAND?0:1 );
			channelSeletion.setSelectedIndex( rule.channel-1 );
			statSeletion.setSelectedIndex( rule.statParam );
			compare.setSelectedIndex( rule.higher?0:1 );
			thresholdSpinner.setValue( rule.value );
			
		    if (firstTime) {
		    	JComboBox filterName = new JComboBox(new String[]{filter.getName()});
		    	filterName.setName(elementName + "1");
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
			filterPanel.add(newFilterElement);
		}
		filterPanel.revalidate();
	    filterPanel.validate();
    }
	
	// parse a Rule panel to populate a Rule object
	@SuppressWarnings("rawtypes")
	protected static Rule getRule (Box box) {
		// create a Rule object, from a Box element in the filter panel
		Rule rule = new Rule();
		// get all elements as Box sub-components
		Component[] elements = ((Container) box).getComponents();
		// populate Rule with elements
		for (Component element : elements) {
			String name = element.getName();
			if (name==null) continue;
			int idx = name.indexOf(elementName);
			if (idx==-1) continue;
			int elementIdx = Integer.valueOf(name.substring(idx+8, name.length()));
			switch (elementIdx) { // AND/OR, channel, mean/stdDev, higher, value
			case 1:	// logical AND / OR
				rule.logicalAND = (((JComboBox)element).getSelectedIndex() == 0);
				break;
			case 2:	// channel
				rule.channel = ((JComboBox)element).getSelectedIndex() + 1;
				break;
			case 3:	// mean/stdDev
				rule.statParam = ((JComboBox)element).getSelectedIndex();
				break;
			case 4:	// higher than / lower than
				rule.higher = (((JComboBox)element).getSelectedIndex() == 0);
				break;
			case 5:	// threshold value
				rule.value = (double) ((JSpinner)element).getValue();
				break;
			default:
				System.out.println("filter panel error, found more than 5 elements.");
			}	
		}
		return rule;
	}
	// parse a Rule node in xml format to populate a Rule object
	protected static Rule getRule (Element ruleNode) {
		// create a Rule object, from a Box element in the filter panel
		Rule rule = new Rule();
		// get all elements as Box sub-components
		rule.id = Integer.valueOf( ruleNode.getChildText("ID") );	      
		rule.logicalAND = ruleNode.getChildText("log").equals("AND");
		rule.channel = Integer.valueOf( ruleNode.getChildText("channel") );
		rule.statParam = ArrayUtils.indexOf(statString, ruleNode.getChildText("parameter"));
		rule.higher = ruleNode.getChildText("comparison").equals("higher than");
		rule.value = Double.valueOf( ruleNode.getChildText("value") );
		rule.valueInZScore = Double.valueOf( ruleNode.getChildText("z-score") );
		return rule;
	}
	
}
