package org.herac.tuxguitar.app.action.impl.caret;

import org.herac.tuxguitar.action.TGActionContext;
import org.herac.tuxguitar.app.view.component.tab.Tablature;
import org.herac.tuxguitar.app.view.component.tab.TablatureEditor;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionBase;
import org.herac.tuxguitar.util.TGContext;

public class TGGoUpAction extends TGActionBase{
	
	public static final String NAME = "action.caret.go-up";
	
	public TGGoUpAction(TGContext context) {
		super(context, NAME);
	}
	
	protected void processAction(TGActionContext context){
		Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		tablature.getCaret().moveUp();
		if (!context.hasAttributeEqualsTrue(TGDocumentContextAttributes.ATTRIBUTE_KEEP_SELECTION)) {
			tablature.getSelector().clearSelection();
		}
	}
}
