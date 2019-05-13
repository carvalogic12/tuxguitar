package org.herac.tuxguitar.app.view.dialog.track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.herac.tuxguitar.app.TuxGuitar;
import org.herac.tuxguitar.app.system.icons.TGIconManager;
import org.herac.tuxguitar.app.ui.TGApplication;
import org.herac.tuxguitar.app.util.TGMessageDialogUtil;
import org.herac.tuxguitar.app.util.TGMusicKeyUtils;
import org.herac.tuxguitar.app.view.controller.TGViewContext;
import org.herac.tuxguitar.app.view.util.TGDialogUtil;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionProcessor;
import org.herac.tuxguitar.editor.action.track.TGChangeTrackTuningAction;
import org.herac.tuxguitar.song.helpers.tuning.TuningGroup;
import org.herac.tuxguitar.song.helpers.tuning.TuningPreset;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGString;
import org.herac.tuxguitar.song.models.TGTrack;
import org.herac.tuxguitar.ui.UIFactory;
import org.herac.tuxguitar.ui.event.UIMouseDoubleClickListener;
import org.herac.tuxguitar.ui.event.UIMouseEvent;
import org.herac.tuxguitar.ui.event.UISelectionEvent;
import org.herac.tuxguitar.ui.event.UISelectionListener;
import org.herac.tuxguitar.ui.layout.UITableLayout;
import org.herac.tuxguitar.ui.widget.*;

public class TGTrackTuningDialog {
	
	private static final String[] NOTE_NAMES = TGMusicKeyUtils.getSharpKeyNames(TGMusicKeyUtils.PREFIX_TUNING);
	private static final float MINIMUM_BUTTON_WIDTH = 80;
	private static final float MINIMUM_BUTTON_HEIGHT = 25;
	private static final int MAX_OCTAVES = 10;
	private static final int MAX_NOTES = 12;
	
	private TGViewContext context;
	private UIWindow dialog;

	private List<TGTrackTuningModel> initialTuning;
	private List<TGTrackTuningModel> tuning;
	private List<TGTrackTuningPresetModel> tuningPresets;
	private UITable<TGTrackTuningModel> tuningTable;
	private List<UIDropDownSelect<TGTrackTuningGroupEntryModel>> tuningPresetSelects;
	private UICheckBox stringTransposition;
	private UICheckBox stringTranspositionTryKeepString;
	private UICheckBox stringTranspositionApplyToChords;
	private UISpinner offsetSpinner;
	private UISelectItem<TGTrackTuningGroupEntryModel> customPresetItem;
	private UIButton buttonEdit;
	private UIButton buttonDelete;
	private UIButton buttonMoveUp;
	private UIButton buttonMoveDown;
	private TGTrackTuningPresetModel currentPreset;

	public TGTrackTuningDialog(TGViewContext context) {
		this.context = context;
		this.tuningPresets = new ArrayList<TGTrackTuningPresetModel>();
		this.tuningPresetSelects = new ArrayList<UIDropDownSelect<TGTrackTuningGroupEntryModel>>();
	}
	
	public void show() {
		TGSongManager songManager = this.findSongManager();
		TGTrack track = this.findTrack();
		
		if(!songManager.isPercussionChannel(track.getSong(), track.getChannelId())) {
			this.tuning = getTuningFromTrack(track);
			this.initialTuning = getTuningFromTrack(track);

			UIFactory factory = this.getUIFactory();
			UIWindow parent = this.context.getAttribute(TGViewContext.ATTRIBUTE_PARENT);
			UITableLayout dialogLayout = new UITableLayout();
			
			this.dialog = factory.createWindow(parent, true, true);
			this.dialog.setLayout(dialogLayout);
			this.dialog.setText(TuxGuitar.getProperty("tuning"));
			
			UITableLayout leftPanelLayout = new UITableLayout();
			UIPanel leftPanel = factory.createPanel(this.dialog, false);
			leftPanel.setLayout(leftPanelLayout);
			dialogLayout.set(leftPanel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);
			
			UITableLayout rightPanelLayout = new UITableLayout();
			UIPanel rightPanel = factory.createPanel(this.dialog, false);
			rightPanel.setLayout(rightPanelLayout);
			dialogLayout.set(rightPanel, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, false, true);
			
			UITableLayout bottomPanelLayout = new UITableLayout(0f);
			UIPanel bottomPanel = factory.createPanel(this.dialog, false);
			bottomPanel.setLayout(bottomPanelLayout);
			dialogLayout.set(bottomPanel, 2, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, false, 1, 2);
			
			this.initTuningTable(leftPanel);
			
			this.initTuningOptions(rightPanel, track);
			
			this.initButtons(bottomPanel);

			this.updateTuningControls();

			TGDialogUtil.openDialog(this.dialog, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
		}
	}

	private TGTrackTuningGroupModel createTuningGroupModels(TuningGroup group) {
	    if (group == null) {
	    	return null;
		}
	    TGTrackTuningGroupModel model = new TGTrackTuningGroupModel();
	    model.setName(group.getName());
	    TGTrackTuningGroupEntryModel[] entries = new TGTrackTuningGroupEntryModel[group.getGroups().size() + group.getTunings().size()];
	    int i = 0;
		for (TuningGroup subGroup : group.getGroups()) {
			TGTrackTuningGroupEntryModel entry = new TGTrackTuningGroupEntryModel();
			TGTrackTuningGroupModel subGroupModel = createTuningGroupModels(subGroup);
			subGroupModel.setEntry(entry);
			entry.setGroup(subGroupModel);
			entry.setParent(model);
			entries[i++] = entry;
		}
		for (TuningPreset tuning : group.getTunings()) {
			TGTrackTuningGroupEntryModel entry = new TGTrackTuningGroupEntryModel();
			TGTrackTuningPresetModel preset = this.createTuningPreset(tuning);
			preset.setEntry(entry);
			entry.setPreset(preset);
			entry.setParent(model);
			entries[i++] = entry;
			this.tuningPresets.add(preset);
		}
		model.setChildren(entries);
		return model;
    }

	private void populatePresetDropDown(UIDropDownSelect<TGTrackTuningGroupEntryModel> select, TGTrackTuningGroupModel group) {
		select.setIgnoreEvents(true);
		select.removeItems();
		if (group != null) {
			if (group.getEntry() == null) {
				select.addItem(this.customPresetItem);
			}
			for (TGTrackTuningGroupEntryModel entry : group.getChildren()) {
				boolean wasEmpty = select.getItemCount() == (group.getEntry() == null ? 1 : 0);

				String name = "";
				if (entry.getGroup() != null) {
					name = entry.getGroup().getName();
				} else if (entry.getPreset() != null) {
					name = this.createTuningPresetLabel(entry.getPreset());
				}
				select.addItem(new UISelectItem<TGTrackTuningGroupEntryModel>(name, entry));
				if (wasEmpty) {
					select.setSelectedValue(entry);
					if (entry.getPreset() != null) {
						this.currentPreset = entry.getPreset();
					}
				}
			}
		}
		select.setEnabled(select.getItemCount() > 0);
		select.setIgnoreEvents(false);
	}

	private void initTuningTable(UILayoutContainer parent) {
		UIFactory factory = this.getUIFactory();
		UITableLayout parentLayout = (UITableLayout) parent.getLayout();
		
		UITableLayout panelLayout = new UITableLayout();
		UIPanel panel = factory.createPanel(parent, false);
		panel.setLayout(panelLayout);
		parentLayout.set(panel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		UITableLayout presetsLayout = new UITableLayout(0f);
		UIPanel presetsPanel = factory.createPanel(panel, false);
		presetsPanel.setLayout(presetsLayout);
		panelLayout.set(presetsPanel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);

		this.tuningPresets.clear();
		this.customPresetItem = new UISelectItem<TGTrackTuningGroupEntryModel>(TuxGuitar.getProperty("tuning.preset.custom"));
		int treeDepth = TuxGuitar.getInstance().getTuningManager().getTreeDepth();
		TGTrackTuningGroupModel tuningLeaf = createTuningGroupModels(TuxGuitar.getInstance().getTuningManager().getTuningsRoot());
        for (int i = 0; i < treeDepth; i++) {
            UIDropDownSelect<TGTrackTuningGroupEntryModel> presetSelect = factory.createDropDownSelect(presetsPanel);
			this.tuningPresetSelects.add(presetSelect);
			presetsLayout.set(presetSelect, 1+i, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);
			if (tuningLeaf != null) {
				this.populatePresetDropDown(presetSelect, tuningLeaf);

				TGTrackTuningGroupModel nextLeaf = null;
				for (TGTrackTuningGroupEntryModel entry : tuningLeaf.getChildren()) {
					if (entry.getGroup() != null) {
						nextLeaf = entry.getGroup();
						break;
					}
				}
				tuningLeaf = nextLeaf;
			}
			presetSelect.addSelectionListener(new UISelectionListener() {
				public void onSelect(UISelectionEvent event) {
					TGTrackTuningDialog.this.onSelectPreset((UIDropDownSelect<TGTrackTuningGroupEntryModel>) event.getComponent());
				}
			});
		}

		this.tuningTable = factory.createTable(panel, true);
		this.tuningTable.setColumns(2);
		this.tuningTable.setColumnName(0, TuxGuitar.getProperty("tuning.label"));
		this.tuningTable.setColumnName(1, TuxGuitar.getProperty("tuning.value"));
		this.tuningTable.addMouseDoubleClickListener(new UIMouseDoubleClickListener() {
			public void onMouseDoubleClick(UIMouseEvent event) {
				TGTrackTuningDialog.this.onEditTuningModel();
			}
		});
		panelLayout.set(this.tuningTable, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);
		panelLayout.set(this.tuningTable, UITableLayout.PACKED_WIDTH, 250f);
		panelLayout.set(this.tuningTable, UITableLayout.PACKED_HEIGHT, 200f);

		UITableLayout buttonsLayout = new UITableLayout(0f);
		UIPanel buttonsPanel = factory.createPanel(panel, false);
		buttonsPanel.setLayout(buttonsLayout);
		panelLayout.set(buttonsPanel, 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		
		UIButton buttonAdd = factory.createButton(buttonsPanel);
		buttonAdd.setImage(TGIconManager.getInstance(this.context.getContext()).getListAdd());
		buttonAdd.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.onAddTuningModel();
			}
		});
		
		buttonEdit = factory.createButton(buttonsPanel);
		buttonEdit.setImage(TGIconManager.getInstance(this.context.getContext()).getListEdit());
		buttonEdit.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.onEditTuningModel();
			}
		});


		buttonMoveUp = factory.createButton(buttonsPanel);
		buttonMoveUp.setImage(TGIconManager.getInstance(this.context.getContext()).getArrowUp());
		buttonMoveUp.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.moveString(-1);
			}
		});

		buttonMoveDown = factory.createButton(buttonsPanel);
		buttonMoveDown.setImage(TGIconManager.getInstance(this.context.getContext()).getArrowDown());
		buttonMoveDown.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.moveString(1);
			}
		});

		buttonDelete = factory.createButton(buttonsPanel);
		buttonDelete.setImage(TGIconManager.getInstance(this.context.getContext()).getListRemove());
		buttonDelete.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.onRemoveTuningModel();
			}
		});

		this.tuningTable.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.updateButtons();
			}
		});

		buttonsLayout.set(buttonAdd, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_FILL, false, false);
		buttonsLayout.set(buttonDelete, 1, 2, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_FILL, false, false);
		buttonsLayout.set(buttonMoveUp, 1, 3, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, false);
		buttonsLayout.set(buttonMoveDown, 1, 4, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false);
		buttonsLayout.set(buttonEdit, 1, 5, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false);
	}

	private void initTuningOptions(UILayoutContainer parent, TGTrack track) {
		UIFactory factory = this.getUIFactory();
		UITableLayout parentLayout = (UITableLayout) parent.getLayout();
		
		UITableLayout panelLayout = new UITableLayout();
		UIPanel panel = factory.createPanel(parent, false);
		panel.setLayout(panelLayout);
		parentLayout.set(panel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);
		
		UITableLayout topLayout = new UITableLayout(0f);
		UIPanel top = factory.createPanel(panel, false);
		top.setLayout(topLayout);
		panelLayout.set(top, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, true, 1, 1, null, null, 0f);
		
		UITableLayout bottomLayout = new UITableLayout(0f);
		UIPanel bottom = factory.createPanel(panel, false);
		bottom.setLayout(bottomLayout);
		panelLayout.set(bottom, 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_BOTTOM, true, true, 1, 1, null, null, 0f);
		
		//---------------------------------OFFSET--------------------------------
		UILabel offsetLabel = factory.createLabel(top);
		offsetLabel.setText(TuxGuitar.getProperty("tuning.offset") + ":");
		topLayout.set(offsetLabel, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, true);
		
		this.offsetSpinner = factory.createSpinner(top);
		this.offsetSpinner.setMinimum(TGTrack.MIN_OFFSET);
		this.offsetSpinner.setMaximum(TGTrack.MAX_OFFSET);
		this.offsetSpinner.setValue(track.getOffset());
		topLayout.set(this.offsetSpinner, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);
		
		//---------------------------------OPTIONS----------------------------------
		this.stringTransposition = factory.createCheckBox(bottom);
		this.stringTransposition.setText(TuxGuitar.getProperty("tuning.strings.transpose"));
		this.stringTransposition.setSelected( true );
		bottomLayout.set(this.stringTransposition, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);
		
		this.stringTranspositionApplyToChords = factory.createCheckBox(bottom);
		this.stringTranspositionApplyToChords.setText(TuxGuitar.getProperty("tuning.strings.transpose.apply-to-chords"));
		this.stringTranspositionApplyToChords.setSelected( true );
		bottomLayout.set(this.stringTranspositionApplyToChords, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);
		
		this.stringTranspositionTryKeepString = factory.createCheckBox(bottom);
		this.stringTranspositionTryKeepString.setText(TuxGuitar.getProperty("tuning.strings.transpose.try-keep-strings"));
		this.stringTranspositionTryKeepString.setSelected( true );
		bottomLayout.set(this.stringTranspositionTryKeepString, 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);
		
		this.stringTransposition.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				UICheckBox stringTransposition = TGTrackTuningDialog.this.stringTransposition;
				UICheckBox stringTranspositionApplyToChords = TGTrackTuningDialog.this.stringTranspositionApplyToChords;
				UICheckBox stringTranspositionTryKeepString = TGTrackTuningDialog.this.stringTranspositionTryKeepString;
				stringTranspositionApplyToChords.setEnabled((stringTransposition.isEnabled() && stringTransposition.isSelected()));
				stringTranspositionTryKeepString.setEnabled((stringTransposition.isEnabled() && stringTransposition.isSelected()));
			}
		});
	}
	
	private void initButtons(UILayoutContainer parent) {
		UIFactory factory = this.getUIFactory();
		UITableLayout parentLayout = (UITableLayout) parent.getLayout();
		
		UIButton buttonOK = factory.createButton(parent);
		buttonOK.setText(TuxGuitar.getProperty("ok"));
		buttonOK.setDefaultButton();
		buttonOK.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				if( TGTrackTuningDialog.this.updateTrackTuning() ) {
					TGTrackTuningDialog.this.dialog.dispose();
				}
			}
		});
		parentLayout.set(buttonOK, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, MINIMUM_BUTTON_WIDTH, MINIMUM_BUTTON_HEIGHT, null);
		
		UIButton buttonCancel = factory.createButton(parent);
		buttonCancel.setText(TuxGuitar.getProperty("cancel"));
		buttonCancel.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningDialog.this.dialog.dispose();
			}
		});
		parentLayout.set(buttonCancel, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, MINIMUM_BUTTON_WIDTH, MINIMUM_BUTTON_HEIGHT, null);
		parentLayout.set(buttonCancel, UITableLayout.MARGIN_RIGHT, 0f);
	}
	
	private void onSelectPreset(UIDropDownSelect<TGTrackTuningGroupEntryModel> select) {
		TGTrackTuningGroupEntryModel model = select.getSelectedValue();
		// need to loop one past in order to get the preset
		for (int i = this.tuningPresetSelects.indexOf(select) + 1; i < this.tuningPresetSelects.size() + 1; i++) {
			if( model == null ) {
				if (i > 0 && i < this.tuningPresetSelects.size()) {
					UIDropDownSelect<TGTrackTuningGroupEntryModel> child = this.tuningPresetSelects.get(i);
					this.populatePresetDropDown(child, null);
				}
			} else {
				if (model.getPreset() != null) {
					this.updateTuningFromPreset(model.getPreset());
					this.currentPreset = model.getPreset();
					model = null;
				} else if (model.getGroup() != null && i < this.tuningPresetSelects.size()) {
					UIDropDownSelect<TGTrackTuningGroupEntryModel> child = this.tuningPresetSelects.get(i);
					this.populatePresetDropDown(child, model.getGroup());
					TGTrackTuningGroupEntryModel[] entries = model.getGroup().getChildren();
					if (entries.length > 0) {
						model = entries[0];
					} else {
						model = null;
					}
				}
			}
		}
	}
	
	private void onAddTuningModel() {
		new TGTrackTuningChooserDialog(this).select(new TGTrackTuningChooserHandler() {
			public void handleSelection(TGTrackTuningModel model) {
				addTuningModel(model);
			}
		});
	}
	
	private void onEditTuningModel() {
		final TGTrackTuningModel editingModel = this.tuningTable.getSelectedValue();
		if( editingModel != null ) {
			new TGTrackTuningChooserDialog(this).select(new TGTrackTuningChooserHandler() {
				public void handleSelection(TGTrackTuningModel model) {
					editingModel.setValue(model.getValue());
					updateTuningControls();
				}
			}, editingModel);
		}
	}
	
	private void onRemoveTuningModel() {
		TGTrackTuningModel model = this.tuningTable.getSelectedValue();
		if( model != null ) {
			removeTuningModel(model);
		}
	}

	private void moveString(int delta) {
		final TGTrackTuningModel model = this.tuningTable.getSelectedValue();
		if (model != null) {
		    int index = this.tuning.indexOf(model);
		    this.tuning.remove(index);
		    this.tuning.add(index + delta, model);
			this.updateTuningControls();
		}
	}

	private static boolean areTuningsEqual(List<TGTrackTuningModel> a, List<TGTrackTuningModel> b) {
		if(a.size() == b.size()) {
			for(int i = 0 ; i < a.size(); i ++) {
				if(!a.get(i).getValue().equals(b.get(i).getValue())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static boolean areTuningsEqual(TGTrackTuningPresetModel a, TGTrackTuningPresetModel b) {
	    return areTuningsEqual(Arrays.asList(a.getValues()), Arrays.asList(b.getValues()));
    }

	private boolean isUsingPreset(TGTrackTuningPresetModel preset) {
		TGTrackTuningModel[] values = preset.getValues();
		return areTuningsEqual(this.tuning, Arrays.asList(preset.getValues()));
	}
	
	private void updateTuningPresetSelection() {
		TGTrackTuningPresetModel selection = null;
		for(TGTrackTuningPresetModel preset : this.tuningPresets) {
			if( this.isUsingPreset(preset)) {
				selection = preset;
				break;
			}
		}
		// special case for when different presets have the same tuning
		if (selection != null && currentPreset != null && areTuningsEqual(selection, currentPreset)) {
			return;
		}
		if (selection == null) {
			int depth = 0;
			for (UIDropDownSelect<TGTrackTuningGroupEntryModel> select : this.tuningPresetSelects) {
				if (depth == 0) {
					select.setIgnoreEvents(true);
					select.setSelectedItem(this.customPresetItem);
					select.setIgnoreEvents(false);
				} else {
				    this.populatePresetDropDown(select, null);
				}
				depth++;
			}
			this.currentPreset = null;
		} else {
			List<TGTrackTuningGroupModel> path = new ArrayList<TGTrackTuningGroupModel>();
			TGTrackTuningGroupModel leaf = selection.getEntry().getParent();
			while (leaf != null) {
				path.add(0, leaf);
				if (leaf.getEntry() != null) {
					leaf = leaf.getEntry().getParent();
				} else {
					leaf = null;
				}
			}
			int depth = 0;
			for (UIDropDownSelect<TGTrackTuningGroupEntryModel> select : this.tuningPresetSelects) {
			    if (depth < path.size()) {
					this.populatePresetDropDown(select, path.get(depth));
					select.setIgnoreEvents(true);
					TGTrackTuningGroupEntryModel entry = depth == path.size() - 1 ? selection.getEntry() : path.get(depth + 1).getEntry();
                    select.setSelectedValue(entry);
                    if (entry.getPreset() != null) {
						this.currentPreset = entry.getPreset();
					}
					select.setIgnoreEvents(false);
				} else {
					this.populatePresetDropDown(select, null);
				}
				depth++;
			}
		}
	}
	
	private void updateTuningTable() {
		TGTrackTuningModel selection = this.tuningTable.getSelectedValue();
		
		this.tuningTable.removeItems();
		for(TGTrackTuningModel model : this.tuning) {
			UITableItem<TGTrackTuningModel> item = new UITableItem<TGTrackTuningModel>(model);
			item.setText(0, this.getValueLabel(model.getValue()));
			item.setText(1, this.getValueLabel(model.getValue(), true));
			
			this.tuningTable.addItem(item);
		}
		
		if( selection != null ) {
			this.tuningTable.setSelectedValue(selection);
		}
	}

	private void updateButtons() {
		TGTrackTuningModel model = this.tuningTable.getSelectedValue();
		int index = model != null ? this.tuning.indexOf(model) : -1;
		buttonEdit.setEnabled(model != null);
		buttonDelete.setEnabled(model != null);
		buttonMoveUp.setEnabled(model != null && index > 0);
		buttonMoveDown.setEnabled(model != null && index < this.tuning.size() - 1);

		boolean isDefault = areTuningsEqual(this.tuning, this.initialTuning);
		stringTransposition.setEnabled(!isDefault);
		stringTranspositionApplyToChords.setEnabled(!isDefault);
		stringTranspositionTryKeepString.setEnabled(!isDefault);
	}
	
	private void updateTuningControls() {
		this.updateTuningTable();
		this.updateTuningPresetSelection();
		this.updateButtons();
	}
	
	private static List<TGTrackTuningModel> getTuningFromTrack(TGTrack track) {
		List<TGTrackTuningModel> tuning = new ArrayList<>();
		for(int i = 0; i < track.stringCount(); i ++) {
			TGString string = track.getString(i + 1);
			TGTrackTuningModel model = new TGTrackTuningModel();
			model.setValue(string.getValue());
			tuning.add(model);
		}
		return tuning;
	}
	
	private void addTuningModel(TGTrackTuningModel model) {
		if( this.tuning.add(model)) {
			this.updateTuningControls();
		}
	}
	
	private void removeTuningModel(TGTrackTuningModel model) {
		if( this.tuning.remove(model)) {
			this.updateTuningControls();
		}
	}
	
	private void updateTuningModels(List<TGTrackTuningModel> models) {
		this.tuning.clear();
		if( this.tuning.addAll(models)) {
			this.updateTuningControls();
		}
	}
	
	private void updateTuningFromPreset(TGTrackTuningPresetModel preset) {
		List<TGTrackTuningModel> models = new ArrayList<TGTrackTuningModel>();
		for(TGTrackTuningModel presetModel : preset.getValues()) {
			TGTrackTuningModel model = new TGTrackTuningModel();
			model.setValue(presetModel.getValue());
			models.add(model);
		}
		this.updateTuningModels(models);
	}
	
	private boolean updateTrackTuning() {
		final TGSongManager songManager = this.findSongManager();
		final TGSong song = this.findSong();
		final TGTrack track = this.findTrack();
		
		final List<TGString> strings = new ArrayList<TGString>();
		for(int i = 0; i < this.tuning.size(); i ++) {
			strings.add(TGSongManager.newString(findSongManager().getFactory(),(i + 1), this.tuning.get(i).getValue()));
		}
		
		final Integer offset = ((songManager.isPercussionChannel(song, track.getChannelId())) ? 0 : this.offsetSpinner.getValue());
		final boolean offsetChanges = (offset != null && !offset.equals(track.getOffset()));
		final boolean tuningChanges = hasTuningChanges(track, strings);
		final boolean transposeStrings = shouldTransposeStrings(track, track.getChannelId());
		final boolean transposeApplyToChords = (transposeStrings && this.stringTranspositionApplyToChords.isSelected());
		final boolean transposeTryKeepString = (transposeStrings && this.stringTranspositionTryKeepString.isSelected());
		
		if( this.validateTrackTuning(strings)) {
			if( tuningChanges || offsetChanges ){
				TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context.getContext(), TGChangeTrackTuningAction.NAME);
				tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, song);
				tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, track);
				
				if( tuningChanges ) {
					tgActionProcessor.setAttribute(TGChangeTrackTuningAction.ATTRIBUTE_STRINGS, strings);
					tgActionProcessor.setAttribute(TGChangeTrackTuningAction.ATTRIBUTE_TRANSPOSE_STRINGS, transposeStrings);
					tgActionProcessor.setAttribute(TGChangeTrackTuningAction.ATTRIBUTE_TRANSPOSE_TRY_KEEP_STRINGS, transposeTryKeepString);
					tgActionProcessor.setAttribute(TGChangeTrackTuningAction.ATTRIBUTE_TRANSPOSE_APPLY_TO_CHORDS, transposeApplyToChords);
				}
				if( offsetChanges ) {
					tgActionProcessor.setAttribute(TGChangeTrackTuningAction.ATTRIBUTE_OFFSET, offset);
				}
				tgActionProcessor.process();
			}
			return true;
		}
		return false;
	}
	
	private boolean validateTrackTuning(List<TGString> strings) {
		if( strings.size() < TGTrack.MIN_STRINGS || strings.size() > TGTrack.MAX_STRINGS ) {
			TGMessageDialogUtil.errorMessage(this.getContext().getContext(), this.dialog, TuxGuitar.getProperty("tuning.strings.range-error", new String[] {Integer.toString(TGTrack.MIN_STRINGS), Integer.toString(TGTrack.MAX_STRINGS)}));
			
			return false;
		}
		return true;
	}
	
	private boolean shouldTransposeStrings(TGTrack track, int selectedChannelId){
		if( this.stringTransposition.isSelected()){
			boolean percussionChannelNew = findSongManager().isPercussionChannel(track.getSong(), selectedChannelId);
			boolean percussionChannelOld = findSongManager().isPercussionChannel(track.getSong(), track.getChannelId());
			
			return (!percussionChannelNew && !percussionChannelOld);
		}
		return false;
	}
	
	private boolean hasTuningChanges(TGTrack track, List<TGString> newStrings){
		List<TGString> oldStrings = track.getStrings();
		//check the number of strings
		if(oldStrings.size() != newStrings.size()){
			return true;
		}
		//check the tuning of strings
		for(int i = 0;i < oldStrings.size();i++){
			TGString oldString = (TGString)oldStrings.get(i);
			boolean stringExists = false;
			for(int j = 0;j < newStrings.size();j++){
				TGString newString = (TGString)newStrings.get(j);
				if(newString.isEqual(oldString)){
					stringExists = true;
				}
			}
			if(!stringExists){
				return true;
			}
		}
		return false;
	}

	public String createTuningPresetLabel(TGTrackTuningPresetModel tuningPreset) {
		StringBuilder label = new StringBuilder();
		label.append(tuningPreset.getName() + " - ");
		TGTrackTuningModel[] values = tuningPreset.getValues();
		for(int i = 0 ; i < values.length; i ++) {
			if( i > 0 ) {
				label.append(" ");
			}
			label.append(this.getValueLabel(values[values.length - i - 1].getValue()));
		}
		return label.toString();
	}

	public TGTrackTuningPresetModel createTuningPreset(TuningPreset tuning) {
		int[] values = tuning.getValues();
		TGTrackTuningModel[] models = new TGTrackTuningModel[values.length];
		for(int i = 0 ; i < models.length; i ++) {
			models[i] = new TGTrackTuningModel();
			models[i].setValue(values[i]);
		}
		TGTrackTuningPresetModel preset = new TGTrackTuningPresetModel();
		preset.setName(tuning.getName());
		preset.setValues(models);
		preset.setProgram(tuning.getProgram());
		return preset;
	}
	
	public String[] getValueLabels() {
		String[] valueNames = new String[MAX_NOTES * MAX_OCTAVES];
		for (int i = 0; i < valueNames.length; i++) {
			valueNames[i] = this.getValueLabel(i, true);
		}
		return valueNames;
	}
	
	public String getValueLabel(Integer value) {
		return this.getValueLabel(value, false);
	}
	
	public String getValueLabel(Integer value, boolean octave) {
		StringBuilder sb = new StringBuilder();
		if( value != null ) {
			sb.append(NOTE_NAMES[value % NOTE_NAMES.length]);
			
			if( octave ) {
				sb.append(value / MAX_NOTES);
			}
		}
		return sb.toString();
	}
	
	public TGSongManager findSongManager() {
		return this.context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG_MANAGER);
	}
	
	public TGSong findSong() {
		return this.context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
	}
	
	public TGTrack findTrack() {
		return this.context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK);
	}
	
	public TGViewContext getContext() {
		return this.context;
	}
	
	public UIFactory getUIFactory() {
		return TGApplication.getInstance(this.context.getContext()).getFactory();
	}
	
	public UIWindow getDialog() {
		return this.dialog;
	}
}
