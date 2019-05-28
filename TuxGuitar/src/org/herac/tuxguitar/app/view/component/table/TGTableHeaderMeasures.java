package org.herac.tuxguitar.app.view.component.table;

import org.herac.tuxguitar.app.action.impl.caret.TGMoveToAction;
import org.herac.tuxguitar.app.action.impl.marker.TGOpenMarkerEditorAction;
import org.herac.tuxguitar.app.view.component.tab.Caret;
import org.herac.tuxguitar.app.view.component.tab.Tablature;
import org.herac.tuxguitar.app.view.util.TGBufferedPainterListenerLocked;
import org.herac.tuxguitar.app.view.util.TGBufferedPainterLocked.TGBufferedPainterHandle;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionProcessor;
import org.herac.tuxguitar.song.models.*;
import org.herac.tuxguitar.ui.event.UIMouseEvent;
import org.herac.tuxguitar.ui.resource.UIColor;
import org.herac.tuxguitar.ui.resource.UIPainter;
import org.herac.tuxguitar.ui.resource.UIPosition;
import org.herac.tuxguitar.ui.widget.UICanvas;

public class TGTableHeaderMeasures implements TGTableHeader, TGBufferedPainterHandle  {

	private TGTable table;
	private UICanvas canvas;
	private UIPosition mousePosition;

	public TGTableHeaderMeasures(TGTable table){
		this.table = table;
		this.canvas = this.table.getUIFactory().createCanvas(this.table.getColumnControl(), false);
		this.canvas.addPaintListener(new TGBufferedPainterListenerLocked(this.table.getContext(), this));
		this.canvas.addMouseMoveListener(this::headerMouseOver);
		this.canvas.addMouseExitListener(this::headerMouseExit);
		this.canvas.addMouseDownListener(this::headerClicked);
		this.canvas.addMouseDoubleClickListener(this::headerDoubleClicked);

		this.table.appendListeners(this.canvas);
	}

	public UICanvas getControl() {
		return this.canvas;
	}

	private void headerMouseOver(UIMouseEvent event) {
	    this.mousePosition = event.getPosition().clone();
		TGMeasureHeader header = getMeasureHeaderAt(event.getPosition().getX());
		if (header != null && header.hasMarker()) {
			this.canvas.setToolTipText(header.getMarker().getTitle());
		} else {
			this.canvas.setToolTipText(null);
		}
		this.canvas.redraw();
	}

	private void headerMouseExit(UIMouseEvent event) {
	    this.mousePosition = null;
		this.canvas.redraw();
	}

	private void headerClicked(UIMouseEvent event) {
		if (event.getButton() == 1) {
			TGMeasureHeader header = getMeasureHeaderAt(event.getPosition().getX());
			if (header != null) {
				Tablature tablature = table.getViewer().getEditor().getTablature();
				Caret caret = tablature.getCaret();
				TGMeasure measure = caret.getTrack().getMeasure(header.getNumber() - 1);
				TGBeat beat = tablature.getSongManager().getMeasureManager().getFirstBeat(measure.getBeats());

				TGActionProcessor processor = new TGActionProcessor(table.getContext(), TGMoveToAction.NAME);
				processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, caret.getTrack());
				processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, measure);
				processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT, beat);
				processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, caret.getSelectedString());
				processor.process();
            }
		}
	}

	private void headerDoubleClicked(UIMouseEvent event) {
		if (event.getButton() == 1) {
			TGMeasureHeader header = getMeasureHeaderAt(event.getPosition().getX());
			if (header != null) {
				TGMarker marker = header.getMarker();

				TGActionProcessor processor = new TGActionProcessor(table.getContext(), TGOpenMarkerEditorAction.NAME);
				if (marker != null) {
                    processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MARKER, marker);
				}
				processor.process();
			}
		}
	}

	private TGMeasureHeader getMeasureHeaderAt(float x) {
		Tablature tablature = table.getViewer().getEditor().getTablature();

		int scrollX = this.table.getViewer().getHScrollSelection();
		float cellSize = this.table.getRowHeight();

		int index = (int) Math.floor((x + scrollX) / cellSize);
		return tablature.getSongManager().getMeasureHeader(tablature.getSong(), index + 1);
	}

	public void paintControl(UIPainter painter) {
		int scrollX = this.table.getViewer().getHScrollSelection();
		float cellSize = this.table.getRowHeight();
		float width = this.canvas.getBounds().getWidth();
		Tablature tablature = this.table.getViewer().getEditor().getTablature();
		TGSong song = tablature.getSong();

		TGTableColorModel colorModel = this.table.getViewer().getColorModel();
		UIColor colorBackground = colorModel.createBackground(this.table.getViewer().getContext(), 0);
		UIColor colorForeground = colorModel.createForeground(this.table.getViewer().getContext(), 0);

		painter.setLineWidth(UIPainter.THINNEST_LINE_WIDTH);
		painter.setBackground(colorBackground);
		painter.initPath(UIPainter.PATH_FILL);
		painter.setAntialias(false);
		painter.addRectangle(0, 0, width, cellSize);
		painter.closePath();

		painter.setAntialias(true);

		int count = song.countMeasureHeaders();
		int j = (int) Math.floor(scrollX / cellSize);
		for(float x = -scrollX + j * cellSize; j < count && x < width; j++, x += cellSize) {
		    TGMeasureHeader header = song.getMeasureHeader(j);

			float xt = x + 2f;
			float yt = cellSize / 2f - 2f;
			float wt = cellSize - 4f;
			float ht = cellSize / 2f;

		    if (header.hasMarker()) {

		    	TGMarker marker = header.getMarker();
		    	UIColor color = table.getUIFactory().createColor(marker.getColor().toColorModel());
		    	painter.setBackground(color);
		    	painter.initPath(UIPainter.PATH_FILL);
		    	drawTriangle(painter, xt, yt, wt, ht);
		    	painter.closePath();
		    	color.dispose();
			}

			if (this.mousePosition != null && this.mousePosition.getX() >= x && this.mousePosition.getX() < x + cellSize) {
			    painter.setAlpha(64);
				painter.setBackground(colorForeground);
				painter.initPath(UIPainter.PATH_FILL);
				drawTriangle(painter, xt, yt, wt, ht);
				painter.closePath();
				painter.setAlpha(255);
			}
		}

		colorBackground.dispose();
		colorForeground.dispose();
	}

	private void drawTriangle(UIPainter painter, float x, float y, float w, float h) {
		painter.moveTo(x, y);
		painter.lineTo(x + w, y);
		painter.lineTo(x + w / 2f, y + h);
		painter.lineTo(x, y);
	}

	public UICanvas getPaintableControl() {
		return getControl();
	}
}
