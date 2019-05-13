package org.herac.tuxguitar.editor.action.note;

import org.herac.tuxguitar.action.TGActionContext;
import org.herac.tuxguitar.action.TGActionManager;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionBase;
import org.herac.tuxguitar.editor.action.tools.TGTransposeAction;
import org.herac.tuxguitar.editor.clipboard.TGClipboard;
import org.herac.tuxguitar.song.helpers.TGStoredBeatList;
import org.herac.tuxguitar.song.managers.TGMeasureManager;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.managers.TGTrackManager;
import org.herac.tuxguitar.song.models.*;
import org.herac.tuxguitar.util.TGContext;

import java.util.List;

public class TGPasteNoteAction extends TGActionBase{

	public static final String NAME = "action.note.paste";

	public static final String ATTRIBUTE_PASTE_MODE = "pasteMode";
	public static final String ATTRIBUTE_PASTE_COUNT = "pasteCount";

	public static final Integer TRANSFER_TYPE_REPLACE = 1;
	public static final Integer TRANSFER_TYPE_INSERT = 2;

	public TGPasteNoteAction(TGContext context) {
		super(context, NAME);
	}
	
	protected void processAction(TGActionContext context){
		Integer pasteMode = context.getAttribute(ATTRIBUTE_PASTE_MODE);
		Integer pasteCount = context.getAttribute(ATTRIBUTE_PASTE_COUNT);

		if (pasteMode == null) {
			pasteMode = TRANSFER_TYPE_REPLACE;
		}
		if (pasteCount == null) {
			pasteCount = 1;
		}

		if( pasteMode > 0 && pasteCount > 0 ) {
			TGStoredBeatList beatList = TGClipboard.getInstance(this.getContext()).getBeats();
			if (beatList != null && beatList.getLength() > 0) {
				TGSongManager songManager = this.getSongManager(context);
				TGTrackManager trackManager = songManager.getTrackManager();
				TGBeat start = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT);
				TGTrack track = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK);

				TGActionManager tgActionManager = TGActionManager.getInstance(getContext());
				if (pasteMode.equals(TRANSFER_TYPE_REPLACE)) {
					context.setAttribute(TGMoveBeatsAction.ATTRIBUTE_MOVE, -beatList.getLength());
					tgActionManager.execute(TGMoveBeatsAction.NAME, context);
				}
				transpose(context, trackManager.addBeats(track, beatList, start.getStart()));
				trackManager.moveOutOfBoundsBeatsToNewMeasure(track, start.getStart());
			}
		}
	}
	private void transpose(TGActionContext context, List<TGBeat> beats) {
		TGMeasureManager measureManager = this.getSongManager(context).getMeasureManager();
		TGMeasure measure = beats.get(0).getMeasure();

		Integer transposition = context.getAttribute(TGTransposeAction.ATTRIBUTE_TRANSPOSITION);
		if (transposition != null && transposition != 0) {
            Boolean tryKeepString = Boolean.TRUE.equals(context.getAttribute(TGTransposeAction.ATTRIBUTE_TRY_KEEP_STRING));
            boolean applyToChords = Boolean.TRUE.equals(context.getAttribute(TGTransposeAction.ATTRIBUTE_APPLY_TO_CHORDS));
            List<TGString> strings = measureManager.getSortedStringsByValue(measure.getTrack(), ( transposition > 0 ? 1 : -1 ) ) ;
            for (TGBeat beat : beats) {
                measureManager.transposeNotes( beat , strings, transposition , tryKeepString , applyToChords , -1 );
            }
		}
		measureManager.removeNotesAfterString(measure, measure.getTrack().stringCount());
	}
}
