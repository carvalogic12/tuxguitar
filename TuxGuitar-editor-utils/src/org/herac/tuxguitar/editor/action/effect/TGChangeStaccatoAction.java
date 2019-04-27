package org.herac.tuxguitar.editor.action.effect;

import org.herac.tuxguitar.action.TGActionContext;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionBase;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGNote;
import org.herac.tuxguitar.song.models.TGString;
import org.herac.tuxguitar.util.TGContext;
import org.herac.tuxguitar.util.TGNoteRange;

public class TGChangeStaccatoAction extends TGActionBase {
	
	public static final String NAME = "action.note.effect.change-staccato";
	
	public TGChangeStaccatoAction(TGContext context) {
		super(context, NAME);
	}
	
	protected void processAction(TGActionContext context){
		TGNoteRange noteRange = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_NOTE_RANGE);

		for (TGNote note : noteRange.getNotes()) {
			getSongManager(context).getMeasureManager().changeStaccato(note);
		}
	}
}
