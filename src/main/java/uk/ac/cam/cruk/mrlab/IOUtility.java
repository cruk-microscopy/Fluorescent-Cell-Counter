package uk.ac.cam.cruk.mrlab;

import fiji.util.gui.GenericDialogPlus;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import loci.common.services.ServiceException;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLServiceImpl;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.LociPrefs;
import ome.units.UNITS;
import org.apache.commons.io.FileUtils;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.scijava.prefs.DefaultPrefService;

public class IOUtility {
  protected static final String setupFileTitle = "hzqfox-CellCounterSettings";
  
  protected static final String filterFileTitle = "hzqfox-FilterSettings";
  
  protected static final String environmentNodeTitle = "Environment";
  
  protected static final String imageNodeTitle = "Image";
  
  protected static final String roiNodeTitle = "ROI";
  
  protected static final String detectionNodeTitle = "Detection";
  
  protected static final String filterNodeTitle = "Filters";
  
  protected static String batchFilePath;
  
  protected static String ext = ".czi";
  
  protected static Boolean doSubDir = Boolean.valueOf(false);
  
  protected static int minSize = 5000;
  
  protected static String batchSetupPath = "         use current setup";
  
  protected static boolean doZScore = true;
  
  protected static String batchResultDirPath = "             same as input";
  
  protected static boolean doAutoROI = false;
  
  protected static boolean exportImage = true;
  
  protected static boolean exportSpotROI = true;
  
  protected static boolean exportSpotTable = true;
  
  protected static boolean exportFilterTable = true;
  
  public static void main(String[] args) throws IOException {}
  
  protected static void batchProcessFile() {
    DefaultPrefService prefs = new DefaultPrefService();
    batchFilePath = prefs.get(String.class, "MaikeCellCounter-batch-batchFilePath", batchFilePath);
    ext = prefs.get(String.class, "MaikeCellCounter-batch-ext", ext);
    minSize = prefs.getInt(Integer.class, "MaikeCellCounter-batch-minSize", minSize);
    doZScore = prefs.getBoolean(Boolean.class, "MaikeCellCounter-batch-doZScore", doZScore);
    doAutoROI = prefs.getBoolean(Boolean.class, "MaikeCellCounter-batch-doAutoROI", doAutoROI);
    exportImage = prefs.getBoolean(Boolean.class, "MaikeCellCounter-batch-exportImage", exportImage);
    exportSpotROI = prefs.getBoolean(Boolean.class, "MaikeCellCounter-batch-exportSpotROI", exportSpotROI);
    exportSpotTable = prefs.getBoolean(Boolean.class, "MaikeCellCounter-batch-exportSpotTable", exportSpotTable);
    exportFilterTable = prefs.getBoolean(Boolean.class, "MaikeCellCounter-batch-exportFilterTable", exportFilterTable);
    GenericDialogPlus gd = new GenericDialogPlus("Batch Process File");
    gd.setInsets(5, 100, 5);
    gd.addMessage("Select Image File or Folder");
    gd.addDirectoryOrFileField("", batchFilePath, 24);
    String[] extensions = { ".czi", ".tif" };
    gd.setInsets(5, -5, 5);
    gd.addChoice("file extension", extensions, ext);
    gd.addNumericField("image size >", minSize, 0, 6, "pixel (czi file only)");
    gd.addDirectoryOrFileField("Setup File", batchSetupPath, 24);
    gd.setInsets(5, -5, 5);
    String[] relativeOrAbsoluteValue = { "use relative filter value", "use absolute filter value" };
    gd.addChoice("", relativeOrAbsoluteValue, relativeOrAbsoluteValue[doZScore ? 0 : 1]);
    gd.addDirectoryField("Result Folder", batchResultDirPath, 24);
    gd.setInsets(5, 80, 5);
    gd.addCheckbox("create auto ROI", doAutoROI);
    gd.setInsets(5, 80, 5);
    gd.addCheckbox("export image while processing", exportImage);
    gd.setInsets(5, 80, 5);
    gd.addCheckbox("save cell detection ROI", exportSpotROI);
    gd.setInsets(5, 80, 5);
    gd.addCheckbox("save detection result", exportSpotTable);
    gd.setInsets(5, 80, 5);
    gd.addCheckbox("save filter result", exportFilterTable);
    gd.showDialog();
    if (gd.wasCanceled())
      return; 
    batchFilePath = gd.getNextString();
    ext = gd.getNextChoice();
    minSize = (int)gd.getNextNumber();
    batchSetupPath = gd.getNextString();
    doZScore = (gd.getNextChoiceIndex() == 0);
    batchResultDirPath = gd.getNextString();
    doAutoROI = gd.getNextBoolean();
    exportImage = gd.getNextBoolean();
    exportSpotROI = gd.getNextBoolean();
    exportSpotTable = gd.getNextBoolean();
    exportFilterTable = gd.getNextBoolean();
    prefs.put(String.class, "MaikeCellCounter-batch-batchFilePath", batchFilePath);
    prefs.put(String.class, "MaikeCellCounter-batch-ext", ext);
    prefs.put(Integer.class, "MaikeCellCounter-batch-minSize", minSize);
    prefs.put(Boolean.class, "MaikeCellCounter-batch-doZScore", doZScore);
    prefs.put(Boolean.class, "MaikeCellCounter-batch-doAutoROI", doAutoROI);
    prefs.put(Boolean.class, "MaikeCellCounter-batch-exportImage", exportImage);
    prefs.put(Boolean.class, "MaikeCellCounter-batch-exportSpotROI", exportSpotROI);
    prefs.put(Boolean.class, "MaikeCellCounter-batch-exportSpotTable", exportSpotTable);
    prefs.put(Boolean.class, "MaikeCellCounter-batch-exportFilterTable", exportFilterTable);
    File batchFile = new File(batchFilePath);
    if (!batchFile.exists())
      return; 
    List<File> fileList = getFileList(batchFile, ext);
    if (fileList == null || fileList.size() == 0)
      return; 
    File resultDir = null;
    if (batchResultDirPath.equals("             same as input")) {
      resultDir = batchFile.isDirectory() ? batchFile : batchFile.getParentFile();
    } else {
      resultDir = new File(batchResultDirPath);
    } 
    if (!resultDir.exists() || !resultDir.isDirectory())
      return; 
    String setupFilePath = String.valueOf(resultDir.getAbsolutePath()) + File.separator + "batch_setup.xml";
    if (batchSetupPath.equals("         use current setup")) {
      if (DetectionUtility.sourceImage == null || FilterPanel.filterList == null || FilterPanel.filterList.size() == 0)
        return; 
      saveSetupToXmlFile(setupFilePath);
    } else {
      try {
        FileUtils.copyFile(new File(batchSetupPath), new File(setupFilePath));
      } catch (IOException e) {
        e.printStackTrace();
        return;
      } 
    } 
    HashMap<String, Object> parameters = batchLoadSetup(setupFilePath);
    if (parameters == null)
      return; 
    try {
      processFileList(fileList, ext, minSize, parameters, resultDir, doZScore, doAutoROI, 
          exportImage, exportSpotROI, exportSpotTable, exportFilterTable);
    } catch (IOException|FormatException|ServiceException e) {
      e.printStackTrace();
    } 
  }
  
  protected static List<File> getFileList(File inputFile, final String ext) {
    if (!inputFile.exists())
      return null; 
    final String ext2 = ext.equals(".tif") ? ".tiff" : ext;
    if (inputFile.isFile()) {
      List<File> list = new ArrayList<>();
      if (inputFile.getName().endsWith(ext) || inputFile.getName().endsWith(ext2))
        list.add(inputFile); 
      return list;
    } 
    File[] files = inputFile.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return !(!name.endsWith(ext) && !name.endsWith(ext2));
          }
        });
    return Arrays.asList(files);
  }
  
  @SuppressWarnings("unchecked")
protected static void processImage(File file, ImagePlus sourceImage, HashMap<String, Object> parameters, File outputDir, boolean doZScore, boolean doROI, boolean exportImage, boolean exportSpotROI, boolean exportSpotTable, boolean exportFilterTable, ResultsTable batchTable) throws IOException {
    if (!file.exists() || sourceImage == null || parameters == null || !outputDir.exists() || !outputDir.isDirectory())
      return; 
    ResultsTable filterTable = new ResultsTable();
    filterTable.setPrecision(3);
    batchTable.incrementCounter();
    filterTable.incrementCounter();
    batchTable.addValue("file", file.getName());
    filterTable.addValue("file", file.getName());
    batchTable.addValue("image", sourceImage.getTitle());
    filterTable.addValue("image", sourceImage.getTitle());
    Roi roi = new Roi(1, 1, sourceImage.getWidth() - 2, sourceImage.getHeight() - 2);
    if (doROI) {
      int channel = ((Integer)parameters.get("autoROIChannel")).intValue();
      String method = DetectionUtility.autoMethods[((Integer)parameters.get("autoROIMethod")).intValue()];
      Color color = (Color)parameters.get("autoROIColor");
      roi = ROIUtility.getRoi(sourceImage, channel, method, color);
    } 
    sourceImage.setRoi(roi);
    double area = (sourceImage.getStatistics()).area / 1000000.0D;
    batchTable.addValue("area (mm2)", area);
    filterTable.addValue("area (mm)", area);
    if (exportImage) {
      int[] channelColors = (int[])parameters.get("channelColors");
      int j = sourceImage.getNChannels();
      int numColor = channelColors.length;
      for (int k = 0; k < j; k++) {
        if (k < numColor)
          ((CompositeImage)sourceImage).setChannelLut(LUT.createLutFromColor(DetectionUtility.colors[channelColors[k]]), k + 1); 
      } 
      sourceImage.setOverlay(new Overlay(roi));
      String imagePath = outputDir + File.separator + sourceImage.getTitle();
      if (!imagePath.endsWith(".tif"))
        imagePath = String.valueOf(imagePath) + ".tif"; 
      IJ.save(sourceImage, imagePath);
    } 
    if (!exportSpotROI || !exportSpotTable || !exportFilterTable)
      return; 
    Overlay spotOverlay = DetectionUtility.getSpotOverlay(sourceImage, parameters);
    if (spotOverlay == null || spotOverlay.size() == 0)
      return; 
    int spotCount = spotOverlay.size();
    Object obj = parameters.get("qualityThreshold-" + sourceImage.getTitle());
    double newThreshold = (obj == null) ? 0.0D : ((Double)obj).doubleValue();
    batchTable.addValue("quality threshold", newThreshold);
    filterTable.addValue("quality threshold", newThreshold);
    batchTable.addValue("detected cell count", spotCount);
    filterTable.addValue("detected cell count", spotCount);
    batchTable.addValue("cell density (count/mm²)", spotCount / area);
    filterTable.addValue("cell density (count/mm²)", spotCount / area);
    int numC = sourceImage.getNChannels(), numSpot = spotOverlay.size();
    double[][] spotMean = new double[numC][numSpot];
    double[][] spotStdDev = new double[numC][numSpot];
    Roi[] spotRois = spotOverlay.toArray();
    RoiManager rm = RoiManager.getInstance();
    if (rm == null) {
      rm = new RoiManager();
    } else {
      rm.reset();
    } 
    rm.setVisible(false);
    for (int c = 0; c < numC; c++) {
      sourceImage.setPositionWithoutUpdate(c + 1, 1, 1);
      for (int j = 0; j < spotRois.length; j++) {
        if (c == 0)
          rm.addRoi(spotRois[j]); 
        sourceImage.setRoi(spotRois[j], false);
        spotMean[c][j] = (sourceImage.getRawStatistics()).mean;
        spotStdDev[c][j] = (sourceImage.getRawStatistics()).stdDev;
      } 
    } 
    double[][][] data = DetectionUtility.computeSpotData(sourceImage, spotOverlay);
    double[][] dataMean = data[0], dataStdDev = data[1];
    if (exportSpotROI && rm.getCount() != 0)
      rm.runCommand("Save", outputDir + File.separator + sourceImage.getTitle() + " cell detection ROI.zip"); 
    if (exportSpotTable) {
      ResultsTable spotTable = new ResultsTable();
      spotTable.setPrecision(3);
      for (int j = 0; j < numSpot; j++) {
        spotTable.incrementCounter();
        spotTable.addValue("Cell", (j + 1));
        spotTable.addValue("X", spotRois[j].getContourCentroid()[0]);
        spotTable.addValue("Y", spotRois[j].getContourCentroid()[1]);
        for (int k = 0; k < numC; k++) {
          spotTable.addValue("C" + (k + 1) + " Mean", spotMean[k][j]);
          spotTable.addValue("C" + (k + 1) + " StdDev", spotMean[k][j]);
        } 
      } 
      spotTable.saveAs(outputDir + File.separator + sourceImage.getTitle() + " cell detection result.csv");
    } 
    ArrayList<Filter> filterList = (ArrayList<Filter>)parameters.get("filterList");
    if (filterList == null || filterList.size() == 0)
      return; 
    for (int i = 0; i < filterList.size(); i++) {
      String name = ((Filter)filterList.get(i)).getName();
      Filter filter = Filter.filterOverlay(filterList.get(i), sourceImage, spotOverlay, (i == 0), false, dataMean, dataStdDev);
      int preCount = filter.getPreCount();
      int postCount = filter.getPostCount();
      batchTable.addValue(String.valueOf(name) + " count", postCount);
      batchTable.addValue(String.valueOf(name) + " density (count/mm²)", postCount / area);
      batchTable.addValue(String.valueOf(name) + " percentage (%)", postCount * 100.0D / preCount);
      batchTable.addValue(String.valueOf(name) + " cumulative percentage (%)", postCount * 100.0D / spotCount);
      filterTable.addValue(String.valueOf(name) + " count", postCount);
      filterTable.addValue(String.valueOf(name) + " density (count/mm²)", postCount / area);
      filterTable.addValue(String.valueOf(name) + " percentage (%)", postCount * 100.0D / preCount);
      filterTable.addValue(String.valueOf(name) + " cumulative percentage (%)", postCount * 100.0D / spotCount);
      if (exportSpotROI) {
        rm.reset();
        Roi[] rois = sourceImage.getOverlay().toArray();
        for (int ii = 0; ii < rois.length; ii++)
          rm.addRoi(rois[ii]); 
        if (rm.getCount() != 0)
          rm.runCommand("Save", outputDir + File.separator + sourceImage.getTitle() + " " + name + " ROI.zip"); 
      } 
    } 
    if (exportFilterTable)
      filterTable.saveAs(outputDir + File.separator + sourceImage.getTitle() + " filter result.csv"); 
    rm.close();
    IJ.run("Collect Garbage", "");
    System.gc();
  }
  
  @SuppressWarnings("resource")
protected static void processFile(File file, String ext, int minSize, HashMap<String, Object> parameters, File outputDir, boolean doZScore, boolean doROI, boolean exportImage, boolean exportSpotROI, boolean exportSpotTable, boolean exportFilterTable, ResultsTable batchTable) throws FormatException, IOException, ServiceException {
    if (!file.exists() || !file.isFile())
      return; 
    String ext2 = ext.equals(".tif") ? ".tiff" : ext;
    if (!file.getName().endsWith(ext) && !file.getName().endsWith(ext2))
      return; 
    if (ext.equals(".tif")) {
      ImagePlus sourceImage = IJ.openImage(file.getAbsolutePath());
      processImage(file, sourceImage, parameters, outputDir, doZScore, doROI, 
          exportImage, exportSpotROI, exportSpotTable, exportFilterTable, batchTable);
      sourceImage.close();
      IJ.run("Collect Garbage", "");
      return;
    } 
    if (!ext.equals(".czi"))
      return; 
    ImageProcessorReader r = new ImageProcessorReader((IFormatReader)new ChannelSeparator((IFormatReader)LociPrefs.makeImageReader()));
    OMEXMLServiceImpl OMEXMLService = new OMEXMLServiceImpl();
    r.setMetadataStore((MetadataStore)OMEXMLService.createOMEXMLMetadata());
    r.setId(file.getAbsolutePath());
    MetadataRetrieve meta = (MetadataRetrieve)r.getMetadataStore();
    int numSeries = meta.getImageCount();
    for (int id = 0; id < numSeries; id++) {
      r.setSeries(id);
      if (r.getSizeX() >= minSize && 
        !meta.getImageName(id).equals("label image")) {
        ImageStack stack = new ImageStack(r.getSizeX(), r.getSizeY());
        for (int n = 0; n < r.getImageCount(); n++) {
          ImageProcessor ip = r.openProcessors(n)[0];
          stack.addSlice("" + (n + 1), ip);
        } 
        ImagePlus sourceImage = new ImagePlus("", stack);
        sourceImage = HyperStackConverter.toHyperStack(sourceImage, r.getSizeC(), r.getSizeZ(), r.getSizeT());
        Calibration cali = new Calibration();
        cali.pixelWidth = meta.getPixelsPhysicalSizeX(id).value(UNITS.MICROMETER).doubleValue();
        cali.pixelHeight = meta.getPixelsPhysicalSizeY(id).value(UNITS.MICROMETER).doubleValue();
        if (r.getSizeZ() > 1)
          cali.pixelDepth = meta.getPixelsPhysicalSizeZ(id).value(UNITS.MICROMETER).doubleValue(); 
        cali.setUnit("micron");
        sourceImage.setGlobalCalibration(cali);
        sourceImage.setTitle(meta.getImageName(id));
        processImage(file, sourceImage, parameters, outputDir, doZScore, doROI, 
            exportImage, exportSpotROI, exportSpotTable, exportFilterTable, batchTable);
        sourceImage.close();
        IJ.run("Collect Garbage", "");
      } 
    } 
  }
  
  protected static void processFileList(List<File> fileList, String ext, int minSize, HashMap<String, Object> parameters, File outputDir, boolean doZScore, boolean doROI, boolean exportImage, boolean exportSpotROI, boolean exportSpotTable, boolean exportFilterTable) throws FormatException, IOException, ServiceException {
    if (fileList == null || fileList.size() == 0 || parameters == null)
      return; 
    ResultsTable batchTable = new ResultsTable();
    batchTable.setPrecision(3);
    for (File file : fileList) {
      processFile(file, ext, minSize, parameters, outputDir, doZScore, doROI, 
          exportImage, exportSpotROI, exportSpotTable, exportFilterTable, batchTable);
      System.gc();
    } 
    batchTable.show("Batch Result");
  }
  
  protected static void saveElementToXmlFile(Element element, String fileName) {
    Document doc = new Document();
    doc.setRootElement(element);
    XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
    try {
      xmlOutputter.output(doc, new FileOutputStream(fileName));
    } catch (IOException io) {
      System.out.println(io.getMessage());
    } 
  }
  
  protected static Element environmentInfoToElement() {
    Date now = new Date();
    Element rootElement = new Element("Environment");
    rootElement.addContent((Content)(new Element("ImageJ")).setText(getImageJVersion()));
    rootElement.addContent((Content)(new Element("Java")).setText(getJavaVersion()));
    rootElement.addContent((Content)(new Element("OS")).setText(getOS()));
    rootElement.addContent((Content)(new Element("MAC")).setText(getMAC()));
    rootElement.addContent((Content)(new Element("IP")).setText(getIP()));
    rootElement.addContent((Content)(new Element("User")).setText(getUser()));
    rootElement.addContent((Content)(new Element("Date")).setText(getDate(now)));
    rootElement.addContent((Content)(new Element("Time")).setText(getTime(now)));
    return rootElement;
  }
  
  protected static Element imageInfoToElement() {
    if (DetectionUtility.sourceImage == null)
      return null; 
    Element rootElement = new Element("Image");
    rootElement.addContent((Content)(new Element("title")).setText(DetectionUtility.sourceImage.getTitle()));
    DetectionUtility.sourceImage.setActivated();
    rootElement.addContent((Content)(new Element("folder")).setText(IJ.getDirectory("image")));
    rootElement.addContent((Content)(new Element("nChannels")).setText(""+DetectionUtility.sourceImage.getNChannels()));
    Element channelColors = new Element("colors");
    int[] channels = DetectionUtility.channelColors;
    for (int i = 0; i < channels.length; i++)
      channelColors.addContent((Content)(new Element("C" + (i + 1))).setText(DetectionUtility.colorStrings[channels[i]])); 
    rootElement.addContent((Content)channelColors);
    return rootElement;
  }
  
  protected static Element ROIInfoToElement() {
    Element rootElement = new Element("ROI");
    String area = (DetectionUtility.sourceImage.getRoi() == null) ? "image" : "ROI";
    rootElement.addContent((Content)(new Element("area")).setText(area));
    rootElement.addContent((Content)(new Element("channel")).setText(""+DetectionUtility.autoROIChannel));
    rootElement.addContent((Content)(new Element("method")).setText(DetectionUtility.autoMethods[DetectionUtility.autoROIMethod]));
    rootElement.addContent((Content)(new Element("color")).setText(DetectionUtility.colorStrings[DetectionUtility.autoROIColorIndex]));
    return rootElement;
  }
  
  protected static Element detectionInfoToElement() {
    Element rootElement = new Element("Detection");
    rootElement.addContent((Content)(new Element("channel")).setText(""+DetectionUtility.targetChannel));
    rootElement.addContent((Content)(new Element("radius")).setText(""+DetectionUtility.spotRadius));
    rootElement.addContent((Content)(new Element("threshold")).setText(""+DetectionUtility.qualityThreshold));
    rootElement.addContent((Content)(new Element("shape")).setText(DetectionUtility.roiShapes[DetectionUtility.spotShape]));
    rootElement.addContent((Content)(new Element("color")).setText(DetectionUtility.colorStrings[DetectionUtility.spotColorIndex]));
    rootElement.addContent((Content)(new Element("alpha")).setText(""+DetectionUtility.spotAlpha));
    return rootElement;
  }
  
  protected static Element filterInfoToElement(List<Filter> filterList) {
    Element rootElement = new Element("Filters");
    for (Filter filter : filterList) {
      Element filterElement = new Element("Filter");
      filterElement.addContent((Content)(new Element("ID")).setText(""+filter.getId()));
      filterElement.addContent((Content)(new Element("name")).setText(filter.getName()));
      for (Rule rule : filter.getRules()) {
        Element ruleElement = new Element("Rule");
        ruleElement.addContent((Content)(new Element("ID")).setText(""+rule.id));
        ruleElement.addContent((Content)(new Element("log")).setText(rule.logicalAND ? "AND" : "OR"));
        ruleElement.addContent((Content)(new Element("channel")).setText(""+rule.channel));
        ruleElement.addContent((Content)(new Element("parameter")).setText(Rule.statString[rule.statParam]));
        ruleElement.addContent((Content)(new Element("comparison")).setText(rule.higher ? "higher than" : "lower than"));
        ruleElement.addContent((Content)(new Element("value")).setText(""+rule.value));
        ruleElement.addContent((Content)(new Element("z-score")).setText(""+rule.valueInZScore));
        filterElement.addContent((Content)ruleElement);
      } 
      filterElement.addContent((Content)(new Element("pre-count")).setText(""+filter.getPreCount()));
      filterElement.addContent((Content)(new Element("post-count")).setText(""+filter.getPostCount()));
      rootElement.addContent((Content)filterElement);
    } 
    return rootElement;
  }
  
  protected static void saveFilterToXmlFile(List<Filter> filterList, String fileName) {
    Element rootElement = new Element("hzqfox-FilterSettings");
    rootElement.addContent((Content)filterInfoToElement(filterList));
    saveElementToXmlFile(rootElement, fileName);
  }
  
  public static ArrayList<Filter> loadFilterFromXmlFile(String filePath) {
    File xmlFile = new File(filePath);
    if (!xmlFile.exists())
      return null; 
    ArrayList<Filter> filterList = new ArrayList<>();
    SAXBuilder builder = new SAXBuilder();
    try {
      Document document = builder.build(xmlFile);
      Element rootNode = document.getRootElement().getChild("Filters");
      if (rootNode == null)
        return null; 
      List<Element> filterNodes = rootNode.getChildren("Filter");
      if (filterNodes == null)
        return null; 
      for (Element filterNode : filterNodes) {
        Filter filter = new Filter(Integer.valueOf(filterNode.getChildText("ID")).intValue(), filterNode.getChildText("name"));
        List<Element> ruleNodes = filterNode.getChildren("Rule");
        for (Element ruleNode : ruleNodes) {
          Rule rule = Rule.getRule(ruleNode);
          filter.addRule(rule);
        } 
        filterList.add(filter);
      } 
      return filterList;
    } catch (IOException io) {
      System.out.println(io.getMessage());
    } catch (JDOMException jdomex) {
      System.out.println(jdomex.getMessage());
    } 
    return null;
  }
  
  protected static void saveSetupToXmlFile(String fileName) {
    if (DetectionUtility.sourceImage == null)
      return; 
    Element rootElement = new Element("hzqfox-CellCounterSettings");
    rootElement.addContent((Content)environmentInfoToElement());
    rootElement.addContent((Content)imageInfoToElement());
    rootElement.addContent((Content)ROIInfoToElement());
    rootElement.addContent((Content)detectionInfoToElement());
    if (FilterPanel.filterList != null && FilterPanel.filterList.size() != 0)
      rootElement.addContent((Content)filterInfoToElement(FilterPanel.filterList)); 
    saveElementToXmlFile(rootElement, fileName);
  }
  
  protected static void loadSetupFromXmlFile(String setupFilePath, boolean doZScore) {
    if (DetectionUtility.sourceImage == null)
      return; 
    File xmlFile = new File(setupFilePath);
    if (!xmlFile.exists())
      return; 
    SAXBuilder builder = new SAXBuilder();
    try {
      Document document = builder.build(xmlFile);
      Element imageNode = document.getRootElement().getChild("Image");
      int nChannels = Integer.valueOf(imageNode.getChildText("nChannels")).intValue();
      DetectionPanel.channelSlider.setMaximum(nChannels);
      Element colors = imageNode.getChild("colors");
      for (int i = 0; i < 10; i++)
        DetectionUtility.channelColors[i] = DetectionUtility.getColorIndex(colors.getChildText("C" + (i + 1))); 
      Element roiNode = document.getRootElement().getChild("ROI");
      boolean doROI = roiNode.getChildText("area").equals("ROI");
      DetectionUtility.autoROIChannel = Integer.valueOf(roiNode.getChildText("channel")).intValue();
      DetectionUtility.autoROIMethod = DetectionUtility.getMethod(roiNode.getChildText("method"));
      DetectionUtility.autoROIColor = DetectionUtility.getColor(roiNode.getChildText("color"));
      if (doROI)
        DetectionUtility.sourceImage.setRoi(
            ROIUtility.getRoi(
              DetectionUtility.sourceImage, 
              DetectionUtility.autoROIChannel, 
              DetectionUtility.autoMethods[DetectionUtility.autoROIMethod], 
              DetectionUtility.autoROIColor)); 
      Element detectionNode = document.getRootElement().getChild("Detection");
      DetectionUtility.targetChannel = Integer.valueOf(detectionNode.getChildText("channel")).intValue();
      DetectionUtility.spotRadius = Double.valueOf(detectionNode.getChildText("radius")).doubleValue();
      DetectionUtility.qualityThreshold = Double.valueOf(detectionNode.getChildText("threshold")).doubleValue();
      DetectionPanel.channelSlider.setValue(DetectionUtility.targetChannel);
      DetectionPanel.diameterSpinner.setValue(Double.valueOf(2.0D * DetectionUtility.spotRadius));
      DetectionPanel.qualityThresholdSpinner.setValue(Double.valueOf(DetectionUtility.qualityThreshold));
      DetectionUtility.spotShape = detectionNode.getChildText("shape").equals("circle") ? 0 : 1;
      DetectionUtility.spotColor = DetectionUtility.getColor(detectionNode.getChildText("color"));
      DetectionUtility.spotAlpha = Double.valueOf(detectionNode.getChildText("alpha")).doubleValue();
      DetectionUtility.displaySpots();
      Element filterRootNode = document.getRootElement().getChild("Filters");
      if (filterRootNode == null)
        return; 
      List<Element> filterNodes = filterRootNode.getChildren("Filter");
      if (filterNodes == null)
        return; 
      ArrayList<Filter> filterList = new ArrayList<>();
      if (doZScore && DetectionUtility.sourceImage != null) {
        DetectionUtility.computeSpotData();
        double[][] mean = DetectionUtility.spotMean;
        double[][] stdDev = DetectionUtility.spotStdDev;
        for (Filter filter : filterList) {
          for (Rule rule : filter.getRules()) {
            double[] data = (rule.statParam == 0) ? mean[rule.channel - 1] : stdDev[rule.channel - 1];
            double[] regularizedData = StatisticUtility.regularizeDataByZscore(data);
            double[] MeanAndStd = StatisticUtility.getMeanAndStdFast(regularizedData);
            rule.value = StatisticUtility.zScoreToValue(rule.valueInZScore, MeanAndStd);
          } 
        } 
      } 
      for (Element filterNode : filterNodes) {
        Filter filter = new Filter(Integer.valueOf(filterNode.getChildText("ID")).intValue(), filterNode.getChildText("name"));
        List<Element> ruleNodes = filterNode.getChildren("Rule");
        for (Element ruleNode : ruleNodes) {
          Rule rule = Rule.getRule(ruleNode);
          filter.addRule(rule);
        } 
        filterList.add(filter);
      } 
      FilterPanel.filterList = filterList;
      FilterPanel.updateFilterPanel(FilterPanel.filterList);
      GUIUtility.updateFrame();
    } catch (IOException io) {
      System.out.println(io.getMessage());
    } catch (JDOMException jdomex) {
      System.out.println(jdomex.getMessage());
    } 
  }
  
  protected static HashMap<String, Object> batchLoadSetup(String setupFilePath) {
    File xmlFile = new File(setupFilePath);
    if (!xmlFile.exists())
      return null; 
    HashMap<String, Object> parameters = new HashMap<>();
    SAXBuilder builder = new SAXBuilder();
    try {
      Document document = builder.build(xmlFile);
      Element imageNode = document.getRootElement().getChild("Image");
      int nChannels = Integer.valueOf(imageNode.getChildText("nChannels")).intValue();
      parameters.put("nChannels", Integer.valueOf(nChannels));
      Element colors = imageNode.getChild("colors");
      int[] channelColors = new int[10];
      for (int i = 0; i < 10; i++)
        channelColors[i] = DetectionUtility.getColorIndex(colors.getChildText("C" + (i + 1))); 
      parameters.put("channelColors", channelColors);
      Element roiNode = document.getRootElement().getChild("ROI");
      boolean doAutoROI = roiNode.getChildText("area").equals("ROI");
      int autoROIChannel = Integer.valueOf(roiNode.getChildText("channel")).intValue();
      int autoROIMethod = DetectionUtility.getMethod(roiNode.getChildText("method"));
      Color autoROIColor = DetectionUtility.getColor(roiNode.getChildText("color"));
      parameters.put("doAutoROI", Boolean.valueOf(doAutoROI));
      parameters.put("autoROIChannel", Integer.valueOf(autoROIChannel));
      parameters.put("autoROIMethod", Integer.valueOf(autoROIMethod));
      parameters.put("autoROIColor", autoROIColor);
      Element detectionNode = document.getRootElement().getChild("Detection");
      int targetChannel = Integer.valueOf(detectionNode.getChildText("channel")).intValue();
      double spotRadius = Double.valueOf(detectionNode.getChildText("radius")).doubleValue();
      double qualityThreshold = Double.valueOf(detectionNode.getChildText("threshold")).doubleValue();
      int spotShape = detectionNode.getChildText("shape").equals("circle") ? 0 : 1;
      Color spotColor = DetectionUtility.getColor(detectionNode.getChildText("color"));
      double spotAlpha = Double.valueOf(detectionNode.getChildText("alpha")).doubleValue();
      parameters.put("targetChannel", Integer.valueOf(targetChannel));
      parameters.put("spotRadius", Double.valueOf(spotRadius));
      parameters.put("qualityThreshold", Double.valueOf(qualityThreshold));
      parameters.put("spotShape", Integer.valueOf(spotShape));
      parameters.put("spotColor", spotColor);
      parameters.put("spotAlpha", Double.valueOf(spotAlpha));
      ArrayList<Filter> filterList = null;
      parameters.put("filterList", filterList);
      Element filterRootNode = document.getRootElement().getChild("Filters");
      if (filterRootNode == null)
        return parameters; 
      List<Element> filterNodes = filterRootNode.getChildren("Filter");
      if (filterNodes == null)
        return parameters; 
      filterList = new ArrayList<>();
      for (Element filterNode : filterNodes) {
        Filter filter = new Filter(Integer.valueOf(filterNode.getChildText("ID")).intValue(), filterNode.getChildText("name"));
        List<Element> ruleNodes = filterNode.getChildren("Rule");
        for (Element ruleNode : ruleNodes) {
          Rule rule = Rule.getRule(ruleNode);
          filter.addRule(rule);
        } 
        filterList.add(filter);
      } 
      parameters.put("filterList", filterList);
    } catch (IOException io) {
      System.out.println(io.getMessage());
    } catch (JDOMException jdomex) {
      System.out.println(jdomex.getMessage());
    } 
    return parameters;
  }
  
  public static String getImageJVersion() {
    return IJ.getVersion();
  }
  
  public static String getJavaVersion() {
    return System.getProperty("java.version");
  }
  
  public static String getOS() {
    return System.getProperty("os.name");
  }
  
  public static String getMAC() {
    String macAddress = "SocketException";
    try {
      NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
      byte[] mac = network.getHardwareAddress();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < mac.length; i++) {
        sb.append(String.format("%02X%s", new Object[] { Byte.valueOf(mac[i]), (i < mac.length - 1) ? "-" : "" }));
      } 
      macAddress = sb.toString();
    } catch (UnknownHostException|java.net.SocketException e) {
      e.printStackTrace();
    } 
    return macAddress;
  }
  
  public static String getIP() {
    String ipAddress = "UnknownHost";
    try {
      ipAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } 
    return ipAddress;
  }
  
  public static String getUser() {
    return System.getProperty("user.name");
  }
  
  public static String getDate(Date date) {
    return (new SimpleDateFormat("yyyy-MM-dd','E")).format(date);
  }
  
  public static String getTime(Date date) {
    return (new SimpleDateFormat("H:mm:ss.SS','zzz")).format(date);
  }
}
