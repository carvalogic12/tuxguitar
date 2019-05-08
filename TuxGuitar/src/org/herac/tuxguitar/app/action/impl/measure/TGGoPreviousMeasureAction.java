package org.herac.tuxguitar.app.action.impl.measure;

import org.herac.tuxguitar.action.TGActionContext;
import org.herac.tuxguitar.app.transport.TGTransport;
import org.herac.tuxguitar.app.view.component.tab.Caret;
import org.herac.tuxguitar.app.view.component.tab.Tablature;
import org.herac.tuxguitar.app.view.component.tab.TablatureEditor;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionBase;
import org.herac.tuxguitar.player.base.MidiPlayer;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGTrack;
import org.herac.tuxguitar.util.TGContext;

public class TGGoPreviousMeasureAction extends TGActionBase{
	
	public static final String NAME = "action.measure.go-previous";
	
	public TGGoPreviousMeasureAction(TGContext context) {
		super(context, NAME);
	}
	
	protected void processAction(TGActionContext context){
		if( MidiPlayer.getInstance(getContext()).isRunning() ){
			TGTransport.getInstance(getContext()).gotoPrevious();
		}
		else{
			Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
			if (!context.hasAttributeEqualsTrue(TGDocumentContextAttributes.ATTRIBUTE_KEEP_SELECTION)) {
				tablature.getSelector().clearSelection();
			}
			Caret caret = tablature.getCaret();
			TGTrack track = caret.getTrack();
			TGBeat firstBeat = getSongManager(context).getMeasureManager().getFirstBeat(caret.getMeasure().getBeats());
			TGMeasure measure = caret.getMeasure();
			if (caret.getSelectedBeat() == firstBeat) {
				measure = getSongManager(context).getTrackManager().getPrevMeasure(caret.getMeasure());
			}
			if (track != null && measure != null) {
				caret.update(track.getNumber(), measure.getStart(), caret.getSelectedString().getNumber());
			}
		}
	}
}
