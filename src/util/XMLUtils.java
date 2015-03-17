package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class XMLUtils {
	private static final Logger LOGGER = Logger.getLogger(XMLUtils.class);
	
	/**
     * Returns the value of an element selected via an xpath expression from
     * a Plugin's plugin.xml file.
     *
     * @param plugin the plugin.
     * @param xpath  the xpath expression.
     * @return the value of the element selected by the xpath expression.
     */
    public static String getElementValue(File config, String xpath) {
        try {
            if (config.exists()) {
                SAXReader saxReader = new SAXReader();
                saxReader.setEncoding("UTF-8");
                Document pluginXML = saxReader.read(config);
                Element element = (Element)pluginXML.selectSingleNode(xpath);
                if (element != null) {
                    return element.getTextTrim();
                }
            }
        } catch (Exception e) {
        	LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
    
    public static void setElementValue(File config, String xpath, String value) {
    	XMLWriter writer = null;
    	try {
            if (config.exists()) {
                SAXReader saxReader = new SAXReader();
                saxReader.setEncoding("UTF-8");
                Document pluginXML = saxReader.read(config);
                Element element = (Element)pluginXML.selectSingleNode(xpath);
                if (element != null) {
                    element.setText(value);
                }
                writer = new XMLWriter(new FileWriter(config));
                writer.write(pluginXML);
            }
        } catch (Exception e) {
        	LOGGER.error(e.getMessage(), e);
        } finally {
        	try {
        		if (null != writer)
        			writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
}
