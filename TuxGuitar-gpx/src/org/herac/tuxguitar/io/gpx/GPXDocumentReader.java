package org.herac.tuxguitar.io.gpx;

import java.io.InputStream;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilderFactory;

import org.herac.tuxguitar.io.gpx.score.GPXAutomation;
import org.herac.tuxguitar.io.gpx.score.GPXBar;
import org.herac.tuxguitar.io.gpx.score.GPXBeat;
import org.herac.tuxguitar.io.gpx.score.GPXChord;
import org.herac.tuxguitar.io.gpx.score.GPXDocument;
import org.herac.tuxguitar.io.gpx.score.GPXMasterBar;
import org.herac.tuxguitar.io.gpx.score.GPXNote;
import org.herac.tuxguitar.io.gpx.score.GPXRhythm;
import org.herac.tuxguitar.io.gpx.score.GPXTrack;
import org.herac.tuxguitar.io.gpx.score.GPXVoice;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.herac.tuxguitar.song.factory.TGFactory;
import org.herac.tuxguitar.song.models.TGNoteSpelling;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

public class GPXDocumentReader {
	
	public static final Integer GP6 = 6;
	public static final Integer GP7 = 7;
	
	private Integer version;
	private Document xmlDocument;
	private GPXDocument gpxDocument;
	
	public GPXDocumentReader(InputStream stream, Integer version) {
		this.version = version;
		this.xmlDocument = getDocument(stream);
		this.gpxDocument = new GPXDocument();
	}
	
	private Document getDocument(InputStream stream) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		return null;
	}
	
	public void xml2file() {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Result output = new StreamResult(new File(System.getProperty("user.home")+"/output.xml"));
			Source input = new DOMSource(this.xmlDocument);

			transformer.transform(input, output);
		} catch (TransformerException e) {}
	}
	
	public GPXDocument read(){
		if( this.xmlDocument != null ){
			//xml2file(); // for debug (saves xml file in home directory)
			this.readScore();
			this.readAutomations();
			this.readTracks();
			this.readMasterBars();
			this.readBars();
			this.readVoices();
			this.readBeats();
			this.readNotes();
			this.readRhythms();
		}
		return this.gpxDocument;
	}
	
	public void readScore(){
		if( this.xmlDocument != null ){
			Node scoreNode = getChildNode(this.xmlDocument.getFirstChild(), "Score");
			if( scoreNode != null ){
				this.gpxDocument.getScore().setTitle( getChildNodeContent(scoreNode, "Title"));
				this.gpxDocument.getScore().setSubTitle( getChildNodeContent(scoreNode, "SubTitle"));
				this.gpxDocument.getScore().setArtist( getChildNodeContent(scoreNode, "Artist"));
				this.gpxDocument.getScore().setAlbum( getChildNodeContent(scoreNode, "Album"));
				this.gpxDocument.getScore().setWords( getChildNodeContent(scoreNode, "Words"));
				this.gpxDocument.getScore().setMusic( getChildNodeContent(scoreNode, "Music"));
				this.gpxDocument.getScore().setWordsAndMusic( getChildNodeContent(scoreNode, "WordsAndMusic"));
				this.gpxDocument.getScore().setCopyright( getChildNodeContent(scoreNode, "Copyright"));
				this.gpxDocument.getScore().setTabber( getChildNodeContent(scoreNode, "Tabber"));
				this.gpxDocument.getScore().setInstructions( getChildNodeContent(scoreNode, "Instructions"));
				this.gpxDocument.getScore().setNotices( getChildNodeContent(scoreNode, "Notices"));
			}
		}
	}
	
	public void readAutomations(){
		if( this.xmlDocument != null ){
			Node masterTrackNode = getChildNode(this.xmlDocument.getFirstChild(), "MasterTrack");
			if( masterTrackNode != null ){
				NodeList automationNodes = getChildNodeList(masterTrackNode, "Automations");
				for (Node automationNode : iterable(automationNodes)) {
					if( automationNode.getNodeName().equals("Automation") ){
						GPXAutomation automation = new GPXAutomation();
						automation.setType( getChildNodeContent(automationNode, "Type"));
						automation.setBarId( getChildNodeIntegerContent(automationNode, "Bar"));
						automation.setValue( getChildNodeIntegerContentArray(automationNode, "Value"));
						automation.setLinear( getChildNodeBooleanContent(automationNode, "Linear"));
						automation.setPosition( getChildNodeIntegerContent(automationNode, "Position"));
						automation.setVisible( getChildNodeBooleanContent(automationNode, "Visible"));
						
						this.gpxDocument.getAutomations().add( automation );
					}
				}
			}
		}
	}
	
	public void readTracks(){
		if( this.xmlDocument != null ){
			NodeList trackNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Tracks");
			for (Node trackNode : iterable(trackNodes)) {
				if( trackNode.getNodeName().equals("Track") ){
					GPXTrack track = new GPXTrack();
					track.setId( getAttributeIntegerValue(trackNode, "id") );
					track.setName(getChildNodeContent(trackNode, "Name" ));
					track.setColor(getChildNodeIntegerContentArray(trackNode, "Color"));
					track.setLetRing(getChildNode(trackNode, "LetRingThroughout" )!=null);

					if( this.version == GP6 ) {
						Node gmNode = getChildNode(trackNode, "GeneralMidi");
						if( gmNode != null ){
							track.setGmProgram(getChildNodeIntegerContent(gmNode, "Program"));
							track.setGmChannel1(getChildNodeIntegerContent(gmNode, "PrimaryChannel"));
							track.setGmChannel2(getChildNodeIntegerContent(gmNode, "SecondaryChannel"));
						}
						Node partSounding = getChildNode(trackNode, "PartSounding");
						if( partSounding != null ){
							TGNoteSpelling spelling = new TGFactory().newNoteSpelling();
							String key = getChildNodeContent(partSounding, "NominalKey");
							int transposition = getChildNodeIntegerContent(partSounding, "TranspositionPitch");
							int keyValue = spelling.fromString(key);
							// TODO: set key.  Seems to be in the GPXMasterBar
							// transposition -1 is assumed, deal with anything else
							// MasterBar // Key // AccidentalCount overrides this
						}
					} else if (this.version == GP7) {
						Node midiConnectionNode = getChildNode(trackNode, "MidiConnection");
						if( midiConnectionNode != null ){
							track.setGmChannel1(getChildNodeIntegerContent(midiConnectionNode, "PrimaryChannel"));
							track.setGmChannel2(getChildNodeIntegerContent(midiConnectionNode, "SecondaryChannel"));
						}
						NodeList soundsNodes = getChildNodeList(trackNode, "Sounds");
						if( soundsNodes != null ){
							for (Node soundNode : iterable(soundsNodes)) {
								if( soundNode.getNodeName().equals("Sound")) {
									Node midiNode = getChildNode(soundNode, "MIDI");
									if( midiNode != null ) {
										track.setGmProgram(getChildNodeIntegerContent(midiNode, "Program"));
									}
								}
							}
						}
					}
					
					NodeList propertiesNode = null;
					if( this.version == GP6 ) {
						propertiesNode = getChildNodeList(trackNode, "Properties");
					} else if (this.version == GP7) {
						Node stavesNode = getChildNode(trackNode, "Staves");
						if( stavesNode != null ) {
							Node staffNode = getChildNode(stavesNode, "Staff");
							if( staffNode != null ) {
								propertiesNode = getChildNodeList(staffNode, "Properties");
							}
						}
					}
					
					if( propertiesNode != null ){
						for (Node propertyNode : iterable(propertiesNode)) {
							if (propertyNode.getNodeName().equals("Property") ) {
								if (getAttributeValue(propertyNode, "name").equals("Tuning")) {
									track.setTunningPitches(getChildNodeIntegerContentArray(propertyNode, "Pitches"));
								} else if( getAttributeValue(propertyNode, "name").equals("CapoFret") ) {
									track.setCapo(getChildNodeIntegerContent(propertyNode, "Fret"));
								}
							}
						}
					}
					
					this.gpxDocument.getTracks().add( track );
					this.readChords(propertiesNode);
				}
			}
		}
	}
	
	public void readChords(NodeList propertiesNode) {
		if( propertiesNode != null ){
			for (Node propertyNode : iterable(propertiesNode)) {
				if (propertyNode.getNodeName().equals("Property") ){
					if( getAttributeValue(propertyNode, "name").equals("DiagramCollection") ) {
						NodeList itemsNode = getChildNodeList(propertyNode, "Items");
						if( itemsNode != null ) {
							for (Node itemNode : iterable(itemsNode)) {
								if (itemNode.getNodeName().equals("Item")) {
									Node diagramNode = getChildNode(itemNode, "Diagram");
									NodeList fretsNode = getChildNodeList(itemNode, "Diagram");
									if( diagramNode != null && fretsNode != null ) {
										GPXChord chord = new GPXChord();
										
										chord.setId(getAttributeIntegerValue(itemNode, "id"));
										chord.setName(getAttributeValue(itemNode, "name"));
										chord.setStringCount(getAttributeIntegerValue(diagramNode, "stringCount"));
										chord.setFretCount(getAttributeIntegerValue(diagramNode, "fretCount"));
										chord.setBaseFret(getAttributeIntegerValue(diagramNode, "baseFret"));
										if( chord.getFretCount() != null ) {
											chord.setFrets(new Integer[chord.getFretCount()]);
											for (Node fretNode : iterable(fretsNode)) {
												if (fretNode.getNodeName().equals("Fret")) {
													Integer string = getAttributeIntegerValue(fretNode, "string");
													if( string != null && string > 0 && string <= chord.getFretCount() ) {
														chord.getFrets()[string - 1] = getAttributeIntegerValue(fretNode, "fret");
													}
												}
											}
											this.gpxDocument.getChords().add(chord);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void readMasterBars(){
		if( this.xmlDocument != null ){
			NodeList masterBarNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "MasterBars");
			for (Node masterBarNode : iterable(masterBarNodes)) {
				if( masterBarNode.getNodeName().equals("MasterBar") ){
					GPXMasterBar masterBar = new GPXMasterBar();
					masterBar.setBarIds( getChildNodeIntegerContentArray(masterBarNode, "Bars"));
					masterBar.setTime( getChildNodeIntegerContentArray(masterBarNode, "Time", "/"));
					masterBar.setTripletFeel(getChildNodeContent(masterBarNode, "TripletFeel"));
					
					Node repeatNode = getChildNode(masterBarNode, "Repeat");
					if( repeatNode != null ){
						masterBar.setRepeatStart(getAttributeBooleanValue(repeatNode, "start"));
						if( getAttributeBooleanValue(repeatNode, "end") ){
							masterBar.setRepeatCount( getAttributeIntegerValue(repeatNode, "count") - 1);
						}
					}
					Node alternativeNode = getChildNode(masterBarNode, "AlternateEndings");
					if( alternativeNode!=null){
						masterBar.setRepeatAlternative( getChildNodeIntegerContentArray(masterBarNode, "AlternateEndings"));
					}
					
					Node sectionNode = getChildNode(masterBarNode, "Section");
					if( sectionNode!=null){
						masterBar.setSectionLetter(getChildNodeContent(sectionNode, "Letter"));	
						masterBar.setSectionText(getChildNodeContent(sectionNode, "Text"));						
					}
					
					Node keyNode = getChildNode(masterBarNode, "Key");
					if (keyNode != null) {
						masterBar.setAccidentalCount(this.getChildNodeIntegerContent(keyNode, "AccidentalCount") ); 
						masterBar.setMode(this.getChildNodeContent(keyNode, "Mode") ); 
					}
					
					this.gpxDocument.getMasterBars().add( masterBar );
				}
			}
		}
	}
	
	public void readBars(){
		if( this.xmlDocument != null ){
			NodeList barNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Bars");
			for (Node barNode : iterable(barNodes)) {
				if( barNode.getNodeName().equals("Bar") ){
					GPXBar bar = new GPXBar();
					bar.setId(getAttributeIntegerValue(barNode, "id"));
					bar.setVoiceIds( getChildNodeIntegerContentArray(barNode, "Voices"));
					bar.setClef(getChildNodeContent(barNode, "Clef"));
					bar.setSimileMark(getChildNodeContent(barNode,"SimileMark"));
					
					this.gpxDocument.getBars().add( bar );
				}
			}
		}
	}
	
	public void readVoices(){
		if( this.xmlDocument != null ){
			NodeList voiceNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Voices");
			for (Node voiceNode : iterable(voiceNodes)) {
				if( voiceNode.getNodeName().equals("Voice") ){
					GPXVoice voice = new GPXVoice();
					voice.setId(getAttributeIntegerValue(voiceNode, "id"));
					voice.setBeatIds( getChildNodeIntegerContentArray(voiceNode, "Beats"));
					
					this.gpxDocument.getVoices().add( voice );
				}
			}
		}
	}
	
	public void readBeats(){
		if( this.xmlDocument != null ){
			NodeList beatNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Beats");
			for (Node beatNode : iterable(beatNodes)) {
				if( beatNode.getNodeName().equals("Beat") ){
					GPXBeat beat = new GPXBeat();
					beat.setId(getAttributeIntegerValue(beatNode, "id"));
					beat.setDynamic(getChildNodeContent(beatNode, "Dynamic"));
					beat.setRhythmId(getAttributeIntegerValue(getChildNode(beatNode, "Rhythm"), "ref"));
					beat.setTremolo( getChildNodeIntegerContentArray(beatNode, "Tremolo", "/"));
					// TODO: <Legato destination="false" origin="true"/>
					// TODO: <Hairpin>Crescendo</Hairpin>
					beat.setNoteIds( getChildNodeIntegerContentArray(beatNode, "Notes"));
					beat.setTremolo( getChildNodeIntegerContentArray(beatNode, "Tremolo", "/"));
					beat.setChordId( getChildNodeIntegerContent(beatNode, "Chord", null));
					beat.setFading( getChildNodeContent(beatNode, "Fadding"));
					beat.setGrace( getChildNodeContent(beatNode, "GraceNotes"));

					String text = getChildNodeContent(beatNode, "FreeText");
					if ( text != null ) {
						beat.setText(text);
					}		
					
					NodeList propertyNodes = getChildNodeList(beatNode, "Properties");
					if( propertyNodes != null ){
						for (Node propertyNode : iterable(propertyNodes)) {
							if (propertyNode.getNodeName().equals("Property") ){
								String propertyName = getAttributeValue(propertyNode, "name");
								
								if( propertyName.equals("WhammyBar") ){
									beat.setWhammyBarEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("WhammyBarOriginValue") ){
									beat.setWhammyBarOriginValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarMiddleValue") ){
									beat.setWhammyBarMiddleValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarDestinationValue") ){
									beat.setWhammyBarDestinationValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarOriginOffset") ){
									beat.setWhammyBarOriginOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarMiddleOffset1") ){
									beat.setWhammyBarMiddleOffset1( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarMiddleOffset2") ){
									beat.setWhammyBarMiddleOffset2( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarDestinationOffset") ){
									beat.setWhammyBarDestinationOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("Brush") ){
									beat.setBrush( getChildNodeContent(propertyNode, "Direction") );
								}
								if( propertyName.equals("Slapped") ){
									beat.setSlapped( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("Popped") ){
									beat.setPopped( getChildNode(propertyNode, "Enable") != null );
								}
								if (propertyName.equals("VibratoWTremBar")) // has a child Strength with String content Wide or Slight
									beat.setVibrato( true );
								if (propertyName.equals("Brush"))
									beat.setBrush( getChildNodeContent(propertyNode, "Direction"));
							}
						}
					}
					
					NodeList xpropertyNodes = getChildNodeList(beatNode, "XProperties");
					if( xpropertyNodes != null ){
						for (Node xpropertyNode : iterable(xpropertyNodes)) {
							if (xpropertyNode.getNodeName().equals("XProperty") ){
								int propertyId = getAttributeIntegerValue(xpropertyNode, "id");
								if( propertyId == 687935489 ){									
									beat.setBrushDuration(getChildNodeIntegerContent(xpropertyNode, "Int"));
								}
							}
						}
					}
					
					this.gpxDocument.getBeats().add( beat );
				}
			}
		}
	}
	
	public void readNotes(){
		if( this.xmlDocument != null ){
			NodeList noteNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Notes");
			for (Node noteNode : iterable(noteNodes)) {
				if( noteNode.getNodeName().equals("Note") ){
					GPXNote note = new GPXNote();
					note.setId( getAttributeIntegerValue(noteNode, "id") );
					
					Node tieNode = getChildNode(noteNode, "Tie");
					note.setTieDestination( tieNode != null ? getAttributeValue(tieNode, "destination").equals("true") : false);

					//not sure if this is better or worse than TG 1.5.2
					// committing fork r27
					Node ghostNode = getChildNode(noteNode, "AntiAccent");
					if ( ghostNode != null ) {
						String ghost = ghostNode.getTextContent();
						if ( ghost.equals("Normal"))
							note.setGhost(true);
					}

					int accent = getChildNodeIntegerContent(noteNode, "Accent");
					if (accent > 0)
						note.setAccent(accent);
					
					note.setAccent(getChildNodeIntegerContent(noteNode, "Accent"));
					note.setTrill(getChildNodeIntegerContent(noteNode, "Trill"));
                                        note.setLetRing( getChildNode(noteNode, "LetRing") != null );
					note.setVibrato( getChildNode(noteNode, "Vibrato") != null );
					
					NodeList propertyNodes = getChildNodeList(noteNode, "Properties");
					if( propertyNodes != null ){
						for (Node propertyNode : iterable(propertyNodes)) {
							if (propertyNode.getNodeName().equals("Property") ){
								String propertyName = getAttributeValue(propertyNode, "name");
								if( propertyName.equals("String") ){
									note.setString( getChildNodeIntegerContent(propertyNode, "String") );
								}
								if( propertyName.equals("Fret") ){
									note.setFret( getChildNodeIntegerContent(propertyNode, "Fret") );
								}
								if( propertyName.equals("ConcertPitch") ){
									Node pitchNode = getChildNode(propertyNode, "Pitch");
									if (pitchNode != null)
									{
										note.setStep(getChildNodeContent(pitchNode, "Step")); 
										note.setAccidental(getChildNodeContent(pitchNode, "Accidental")); 
										note.setOctave(getChildNodeIntegerContent(pitchNode, "Octave")); 
									}
								}
								if( propertyName.equals("Midi") ){
									note.setMidiNumber( getChildNodeIntegerContent(propertyNode, "Number") );
								}
								if( propertyName.equals("Tone") ){
									note.setTone( getChildNodeIntegerContent(propertyNode, "Step") );
								}
								if( propertyName.equals("Octave") ){
									note.setOctave( getChildNodeIntegerContent(propertyNode, "Number") );
								}
								if( propertyName.equals("Element") ){
									note.setElement( getChildNodeIntegerContent(propertyNode, "Element") );
								}
								if( propertyName.equals("Variation") ){
									note.setVariation( getChildNodeIntegerContent(propertyNode, "Variation") );
								}
								if( propertyName.equals("Muted") ){
									note.setMutedEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("PalmMuted") ){
									note.setPalmMutedEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("Slide") ){
									note.setSlide( true );
									note.setSlideFlags( getChildNodeIntegerContent(propertyNode, "Flags") );
								}
								if( propertyName.equals("Tapped") ){
									note.setTapped( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("Bended") ){
									note.setBendEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("BendOriginValue") ){
									note.setBendOriginValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendMiddleValue") ){
									note.setBendMiddleValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendDestinationValue") ){
									note.setBendDestinationValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendOriginOffset") ){
									note.setBendOriginOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendMiddleOffset1") ){
									note.setBendMiddleOffset1( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendMiddleOffset2") ){
									note.setBendMiddleOffset2( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendDestinationOffset") ){
									note.setBendDestinationOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("HopoOrigin") ){
									note.setHammer(true);
								}
								if( propertyName.equals("HopoDestination") ){
									note.setPullOff(true);
								}
								if( propertyName.equals("HarmonicFret") ){
									note.setHarmonicFret( ( getChildNodeIntegerContent(propertyNode, "HFret") ) );
								}
								if( propertyName.equals("HarmonicType") ){
									note.setHarmonicType( getChildNodeContent (propertyNode, "HType"));
								}
							}
						}
					}
					
					NodeList xpropertyNodes = getChildNodeList(noteNode, "XProperties");
					if( xpropertyNodes != null ){
						for (Node xpropertyNode : iterable(xpropertyNodes)) {
							if (xpropertyNode.getNodeName().equals("XProperty") ){
								int propertyId = getAttributeIntegerValue(xpropertyNode, "id");
								if( propertyId == 688062467 ){									
									note.setTrillDuration(getChildNodeIntegerContent(xpropertyNode, "Int"));
								}
							}
						}
					}
					
					this.gpxDocument.getNotes().add( note );
				}
			}
		}
	}
	
	public void readRhythms(){
		if( this.xmlDocument != null ){
			NodeList rhythmNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Rhythms");
			for (Node rhythmNode : iterable(rhythmNodes)) {
				if( rhythmNode.getNodeName().equals("Rhythm") ){
					Node primaryTupletNode = getChildNode(rhythmNode, "PrimaryTuplet");
					Node augmentationDotNode = getChildNode(rhythmNode, "AugmentationDot");
					
					GPXRhythm rhythm = new GPXRhythm();
					rhythm.setId( getAttributeIntegerValue(rhythmNode, "id") );
					rhythm.setNoteValue(getChildNodeContent(rhythmNode, "NoteValue") );
					rhythm.setPrimaryTupletDen(primaryTupletNode != null ? getAttributeIntegerValue(primaryTupletNode, "den") : 1);
					rhythm.setPrimaryTupletNum(primaryTupletNode != null ? getAttributeIntegerValue(primaryTupletNode, "num") : 1);
					rhythm.setAugmentationDotCount(augmentationDotNode != null ? getAttributeIntegerValue(augmentationDotNode, "count") : 0);
					
					this.gpxDocument.getRhythms().add( rhythm );
				}
			}
		}
	}
	
	private String getAttributeValue(Node node, String attribute ){
		if( node != null ){
			return node.getAttributes().getNamedItem( attribute ).getNodeValue();
		}
		return null;
	}
	
	private int getAttributeIntegerValue(Node node, String attribute ){
		try {
			return new BigDecimal(this.getAttributeValue(node, attribute)).intValue();
		} catch( Throwable throwable ){ 
			return 0;
		}
	}
	
	private boolean getAttributeBooleanValue(Node node, String attribute ){
		String value = this.getAttributeValue(node, attribute);
		if( value != null ){
			return value.equals("true");
		}
		return false;
	}
	
	private Node getChildNode(Node node, String name ){
		NodeList childNodes = node.getChildNodes();
		for (Node childNode : iterable(childNodes)) {
			if( childNode.getNodeName().equals( name ) ){
				return childNode;
			}
		}
		return null;
	}
	
	private NodeList getChildNodeList(Node node, String name ){
		Node childNode = getChildNode(node, name);
		if( childNode != null ){
			return childNode.getChildNodes();
		}
		return null;
	}
	
	private String getChildNodeContent(Node node, String name ){
		Node childNode = getChildNode(node, name);
		if( childNode != null ){
			return childNode.getTextContent();
		}
		return null;
	}
	
	private boolean getChildNodeBooleanContent(Node node, String name ){
		String value = this.getChildNodeContent(node, name);
		if( value != null ){
			return value.equals("true");
		}
		return false;
	}
	
	private int getChildNodeIntegerContent(Node node, String name){
		return this.getChildNodeIntegerContent(node, name, 0);
	}
	
	private Integer getChildNodeIntegerContent(Node node, String name, Integer defaultValue){
		try {
			return new BigDecimal(this.getChildNodeContent(node, name)).intValue();
		} catch( Throwable throwable ){
			return defaultValue;
		}
	}
	
	private int[] getChildNodeIntegerContentArray(Node node, String name , String regex){
		String rawContents = this.getChildNodeContent(node, name);
		if( rawContents != null ){
			String[] contents = rawContents.trim().split(regex);
			int[] intContents = new int[contents.length];
			for( int i = 0 ; i < intContents.length; i ++ ){
				try {
					intContents[i] = new BigDecimal( contents[i].trim() ).intValue();
				} catch( Throwable throwable ){
					intContents[i] = 0;
				}
			}
			return intContents;
		}
		return null;
	}
	
	private int[] getChildNodeIntegerContentArray(Node node, String name ){
		return getChildNodeIntegerContentArray(node, name, (" ") );
	}

	private static Iterable<Node> iterable(final NodeList nodeList) {
		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return new Iterator<Node>() {
					private Node node = nodeList.getLength() > 0 ? nodeList.item(0) : null;
					public boolean hasNext() {
						return node != null;
					}
					public Node next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						Node current = node;
						this.node = this.node.getNextSibling();
						return current;
					}
				};
			}
		};
	}
}
