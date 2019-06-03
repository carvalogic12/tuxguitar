package org.herac.tuxguitar.app.view.component.tab;

import java.util.List;

import org.herac.tuxguitar.app.TuxGuitar;
import org.herac.tuxguitar.app.document.TGDocument;
import org.herac.tuxguitar.app.system.config.TGConfigKeys;
import org.herac.tuxguitar.app.system.config.TGConfigManager;
import org.herac.tuxguitar.app.tools.percussion.PercussionEntry;
import org.herac.tuxguitar.app.tools.percussion.PercussionManager;
import org.herac.tuxguitar.app.ui.TGApplication;
import org.herac.tuxguitar.app.view.component.tab.edit.EditorKit;
import org.herac.tuxguitar.app.view.component.tabfolder.TGTabFolder;
import org.herac.tuxguitar.app.view.main.TGWindow;
import org.herac.tuxguitar.app.view.util.TGSyncProcess;
import org.herac.tuxguitar.document.TGDocumentManager;
import org.herac.tuxguitar.graphics.control.*;
import org.herac.tuxguitar.player.base.MidiPlayerMode;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.models.*;
import org.herac.tuxguitar.ui.resource.UIPainter;
import org.herac.tuxguitar.ui.resource.UIRectangle;
import org.herac.tuxguitar.ui.resource.UIResourceFactory;
import org.herac.tuxguitar.util.TGBeatRange;
import org.herac.tuxguitar.util.TGContext;
import org.herac.tuxguitar.util.TGNoteRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tablature implements TGController {

	public static final float DEFAULT_SCALE = 1.f;

	private TGContext context; 
	private UIResourceFactory resourceFactory;
	private TGDocumentManager documentManager;
	private TGResourceBuffer resourceBuffer;
	private TGSyncProcess disposeUnregisteredResources;
	
	private Caret caret;
	private Selector selector;
	private TGLayout viewLayout;
	private EditorKit editorKit;
	private Float scale;
	
	public Tablature(TGContext context, TGDocumentManager documentManager) {
		this.context = context;
		this.documentManager = documentManager;
		TGConfigManager config = TGConfigManager.getInstance(this.context);
		this.scale = config.getFloatValue(TGConfigKeys.LAYOUT_ZOOM, DEFAULT_SCALE);
		this.caret = new Caret(this);
		this.selector = new Selector(this);
		this.editorKit = new EditorKit(this);
		this.createSyncProcesses();
	}
	
	public void createSyncProcesses() {
		this.disposeUnregisteredResources = new TGSyncProcess(this.context, new Runnable() {
			public void run() {
				getResourceBuffer().disposeUnregisteredResources();
			}
		});
	}
	
	public void updateTablature(){
		this.getViewLayout().updateSong();
		this.disposeUnregisteredResources.process();
	}
	
	public void updateMeasures(List<Integer> numbers){
		this.getViewLayout().updateMeasureNumbers(numbers);
		this.getCaret().update();
		this.disposeUnregisteredResources.process();
	}
	
	public void resetCaret(){
		this.caret.update(1, TGDuration.QUARTER_TIME, 1);
	}
	
	public void paintTablature(UIPainter painter, UIRectangle area, float fromX, float fromY){
		this.getViewLayout().fillBackground(painter, area);
		this.getViewLayout().paint(painter, area, fromX, fromY);
		this.getCaret().paintCaret(this.getViewLayout(), painter);
		this.getEditorKit().paintSelection(this.getViewLayout(), painter);
		this.getSelector().paintSelectedArea(this.getViewLayout(), painter);
	}
	
	public Float getScale() {
		return scale;
	}

	public Caret getCaret(){
		return this.caret;
	}

	public Selector getSelector() {
		return selector;
	}

	public TGBeatRange getCurrentBeatRange() {
		if (getSelector().isActive()) {
			return getSelector().getBeatRange();
		}
        TGBeat beat = getCaret().getSelectedBeat();
        if (beat != null) {
            return TGBeatRange.single(beat);
        }
		return TGBeatRange.empty();
	}

	public TGNoteRange getCurrentNoteRange() {
	    int voice = getCaret().getVoice();
		if (getSelector().isActive()) {
			return getSelector().getNoteRange(Collections.singletonList(voice));
		} else {
			TGNote defaultNote = getCaret().getSelectedNote();
			if (defaultNote != null && defaultNote.getVoice().getIndex() == voice) {
				return TGNoteRange.single(defaultNote);
			}
		}
		return TGNoteRange.empty();
	}

	public void restoreStateFrom(TGDocument document) {
		this.getCaret().restoreStateFrom(document);
	    this.getSelector().restoreStateFrom(document);
	}

	public EditorKit getEditorKit() {
		return this.editorKit;
	}
	
	public TGContext getContext() {
		return this.context;
	}
	
	public TGSongManager getSongManager() {
		return this.documentManager.getSongManager();
	}
	
	public TGSong getSong() {
		return this.documentManager.getSong();
	}
	
	public TGLayout getViewLayout(){
		return this.viewLayout;
	}
	
	public void setViewLayout(TGLayout viewLayout){
		if( getViewLayout() != null ){
			getViewLayout().disposeLayout();
		}
		this.viewLayout = viewLayout;
		this.reloadStyles();
	}
	
	public void reloadStyles() {
		if( this.getViewLayout() != null ){
			float deviceScale = TGWindow.getInstance(context).getWindow().getDeviceZoom() / 100f;
			this.getViewLayout().loadStyles(this.scale * deviceScale);
			this.loadPercussionMap();
		}
		this.loadCaretStyles();
	}

	private void loadPercussionMap() {
		PercussionManager percussionManager = PercussionManager.getInstance(getContext());
		setPercussionMap(percussionManager.getEntries());
	}

	public void setPercussionMap(PercussionEntry[] entries) {
		TGPercussionNote[] map = new TGPercussionNote[entries.length];
		for (int i = 0; i < entries.length; i++) {
			PercussionEntry entry = entries[i];
			map[i] = new TGPercussionNote(entry.getNote(), entry.getKind());
		}
		this.getViewLayout().setPercussionMap(map);
	}
	
	public void reloadViewLayout(){
		TGConfigManager config = TGConfigManager.getInstance(this.context);
		
		this.loadViewLayout(config.getIntegerValue(TGConfigKeys.LAYOUT_STYLE), config.getIntegerValue(TGConfigKeys.LAYOUT_MODE));
	}
	
	private void loadViewLayout( int style, int mode ){
		switch(mode){
			case TGLayout.MODE_VERTICAL:
				setViewLayout(new TGLayoutVertical(this, style));
			break;
			case TGLayout.MODE_HORIZONTAL:
				setViewLayout(new TGLayoutHorizontal(this, style));
			break;
			default:
				if( mode != TGLayout.DEFAULT_MODE ){
					this.loadViewLayout( style, TGLayout.DEFAULT_MODE );
				}
			break;
		}
	}
	
	public void loadCaretStyles() {
		TGConfigManager config = TGConfigManager.getInstance(this.context);
		
		getCaret().setColor1(config.getColorModelConfigValue(TGConfigKeys.COLOR_CARET_1));
		getCaret().setColor1Fill(config.getColorModelConfigValue(TGConfigKeys.COLOR_CARET_1_FILL));
		getCaret().setColor2(config.getColorModelConfigValue(TGConfigKeys.COLOR_CARET_2));
		getCaret().setColor2Fill(config.getColorModelConfigValue(TGConfigKeys.COLOR_CARET_2_FILL));
	}
	
	public void scale(Float scale) {
		if(!this.scale.equals(scale)) {
			this.scale = (scale != null ? scale : DEFAULT_SCALE);
			this.reloadStyles();

			TGConfigManager config = TGConfigManager.getInstance(getContext());
			config.setValue(TGConfigKeys.LAYOUT_ZOOM, this.scale);
		}
	}
	
	public void dispose(){
		this.getCaret().dispose();
		this.getViewLayout().disposeLayout();
		this.getResourceBuffer().disposeAllResources();
	}
	
	public UIResourceFactory getResourceFactory(){
		if( this.resourceFactory == null ){
			this.resourceFactory = TGApplication.getInstance(this.context).getFactory();
		}
		return this.resourceFactory;
	}
	
	public TGResourceBuffer getResourceBuffer(){
		if( this.resourceBuffer == null ){
			this.resourceBuffer = new TGResourceBuffer();
		}
		return this.resourceBuffer;
	}
	
	public List<TGTrack> getTrackSelection(){
		return getSongManager().getVisibleTracks(getSong());
	}

	public boolean isRunning(TGBeat beat) {
		return ( isRunning( beat.getMeasure() ) && TuxGuitar.getInstance().getEditorCache().isPlaying(beat.getMeasure(),beat) );
	}
	
	public boolean isRunning(TGMeasure measure) {
		return ( measure.getTrack().equals(getCaret().getTrack()) && TuxGuitar.getInstance().getEditorCache().isPlaying( measure ) );
	}
	
	public boolean isLoopSHeader(TGMeasureHeader measureHeader){
		MidiPlayerMode pm = TuxGuitar.getInstance().getPlayer().getMode();
		return ( pm.isLoop() && pm.getLoopSHeader() == measureHeader.getNumber() );
	}
	
	public boolean isLoopEHeader(TGMeasureHeader measureHeader){
		MidiPlayerMode pm = TuxGuitar.getInstance().getPlayer().getMode();
		return ( pm.isLoop() && pm.getLoopEHeader() == measureHeader.getNumber() );
	}
	
	public TGLayoutStyles getStyles() {
		return new TablatureStyles(TGConfigManager.getInstance(this.context));
	}

}
