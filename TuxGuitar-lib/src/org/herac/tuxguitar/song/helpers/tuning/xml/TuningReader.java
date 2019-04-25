package org.herac.tuxguitar.song.helpers.tuning.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.herac.tuxguitar.song.models.TGTuning;
import org.herac.tuxguitar.song.helpers.tuning.TuningGroup;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TuningReader {
	private static final String TUNING_TAG = "tuning";
	private static final String GROUP_TAG = "group";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String NOTES_ATTRIBUTE = "notes";
	private static final String KEY_SEPARATOR = ",";
	
	public void loadTunings(TuningGroup group, InputStream stream){
		try{
			if ( stream != null ){
				Document doc = getDocument(stream);
				loadTunings(group, doc.getFirstChild());
			}
		}catch(Throwable e){
			e.printStackTrace();
		}
	}
	
	private static Document getDocument(InputStream stream) throws ParserConfigurationException, SAXException, IOException {
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse(stream);
		
		return document;
	}
	
	private static void loadTunings(TuningGroup group, Node node){
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node child = nodeList.item(i);
			String nodeName = child.getNodeName();
			
			if (nodeName.equals(TUNING_TAG)) {
				NamedNodeMap params = child.getAttributes();
				
				String name = params.getNamedItem(NAME_ATTRIBUTE).getNodeValue();
				String notes = params.getNamedItem(NOTES_ATTRIBUTE).getNodeValue();
				
				if (name == null || notes == null || name.trim().equals("") || notes.trim().equals("")){
					throw new RuntimeException("Invalid Tuning file format.");
				}
				String[] noteStrings = notes.split(KEY_SEPARATOR);
				int[] noteValues = new int[noteStrings.length];
				for (int j = 0; j < noteStrings.length; j++){
					int note = Integer.parseInt(noteStrings[j]);
					if( note >= 0 && note < 128 ){
						noteValues[j] = note;
					} else {
						throw new RuntimeException("Invalid Tuning note: " + noteStrings[j]);
					}
				}
				
				TGTuning tuning = new TGTuning(name, noteValues);
				group.getTunings().add(tuning);
			} else if (nodeName.equals(GROUP_TAG)) {
				NamedNodeMap params = child.getAttributes();
				String name = params.getNamedItem(NAME_ATTRIBUTE).getNodeValue();

				TuningGroup subGroup = new TuningGroup(name, new ArrayList<TGTuning>(), new ArrayList<TuningGroup>());
				loadTunings(subGroup, child);
				group.getGroups().add(subGroup);
			}
		}
	}
}
