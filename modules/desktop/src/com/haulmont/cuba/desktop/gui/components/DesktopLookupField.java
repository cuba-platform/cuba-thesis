/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import com.haulmont.chile.core.datatypes.Enumeration;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.ValidationException;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenerAdapter;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopLookupField
    extends DesktopAbstractOptionsField<JComboBox>
    implements LookupField
{
    private BasicEventList<Object> items = new BasicEventList<Object>();
    private boolean optionsInitialized;
    private AutoCompleteSupport<Object> autoComplete;
    private ValueWrapper prevValue;
    private String caption;
    private boolean editable;
    private NewOptionHandler newOptionHandler;
    private boolean newOptionAllowed;

    public DesktopLookupField() {
        impl = new JComboBox();
        impl.setEditable(true);
        impl.setPrototypeDisplayValue("AAAAAAAAAAAA");
        autoComplete = AutoCompleteSupport.install(impl, items);

        for (int i = 0; i < impl.getComponentCount(); i++) {
            java.awt.Component component = impl.getComponent(i);
            component.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    initOptions();
                }
            });
        }
        // set value only on PopupMenu closing to avoid firing listeners on keyboard navigation
        impl.addPopupMenuListener(
                new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    }

                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                        Object selectedItem = impl.getSelectedItem();
                        if (selectedItem instanceof ValueWrapper) {
                            ValueWrapper newValue = (ValueWrapper) selectedItem;
                            if (newValue != prevValue) {
                                updateValue(newValue);
                                fireValueChanged(prevValue == null ? null : prevValue.getValue(), newValue == null ? null : newValue.getValue());
                                prevValue = newValue;
                            }
                        } else if (selectedItem instanceof String && newOptionAllowed && newOptionHandler != null) {
                            newOptionHandler.addNewOption((String) selectedItem);
                        } else {
                            impl.setSelectedItem(prevValue);
                        }
                    }

                    @Override
                    public void popupMenuCanceled(PopupMenuEvent e) {
                    }
                }
        );
        impl.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Object selectedItem = impl.getSelectedItem();
                        if (selectedItem instanceof String && newOptionAllowed && newOptionHandler != null) {
                            newOptionHandler.addNewOption((String) selectedItem);
                        }
                    }
                }
        );

        setFilterMode(FilterMode.CONTAINS);

        DesktopComponentsHelper.adjustSize(impl);
    }

    private void updateValue(ValueWrapper selectedItem) {
        if (datasource != null && metaProperty != null) {
            updatingInstance = true;
            try {
                if (datasource.getItem() != null) {
                    InstanceUtils.setValueEx(datasource.getItem(), metaPropertyPath.getPath(),
                            selectedItem == null ? null : selectedItem.getValue());
                }
            } finally {
                updatingInstance = false;
            }
        }
    }

    private void initOptions() {
        if (optionsInitialized)
            return;

        items.clear();

        if (!isRequired()) {
            items.add(new ObjectWrapper(null));
        }

        if (optionsDatasource != null) {
            if (!optionsDatasource.getState().equals(Datasource.State.VALID)) {
                optionsDatasource.refresh();
            }
            for (Object id : optionsDatasource.getItemIds()) {
                items.add(new EntityWrapper(optionsDatasource.getItem(id)));
            }

            optionsDatasource.addListener(
                    new CollectionDsListenerAdapter<Entity<Object>>() {
                        @Override
                        public void collectionChanged(CollectionDatasource ds, Operation operation) {
                            items.clear();
                            for (Object id : optionsDatasource.getItemIds()) {
                                items.add(new EntityWrapper(optionsDatasource.getItem(id)));
                            }
                        }
                    }
            );
        } else if (optionsMap != null) {
            for (String key : optionsMap.keySet()) {
                items.add(new MapKeyWrapper(key));
            }
        } else if (optionsList != null) {
            for (Object obj : optionsList) {
                items.add(new ObjectWrapper(obj));
            }
        } else if (datasource != null && metaProperty != null && metaProperty.getRange().isEnum()) {
            Enumeration<Enum> enumeration = metaProperty.getRange().asEnumeration();
            for (Enum en : enumeration.getValues()) {
                items.add(new ObjectWrapper(en));
            }
        }

        optionsInitialized = true;
    }

    @Override
    public Object getNullOption() {
        return null;
    }

    @Override
    public void setNullOption(Object nullOption) {
    }

    @Override
    public FilterMode getFilterMode() {
        return autoComplete.getFilterMode() == TextMatcherEditor.CONTAINS
                ? FilterMode.CONTAINS : FilterMode.STARTS_WITH;
    }

    @Override
    public void setFilterMode(FilterMode mode) {
        autoComplete.setFilterMode(FilterMode.CONTAINS.equals(mode)
                ? TextMatcherEditor.CONTAINS : TextMatcherEditor.STARTS_WITH);
    }

    @Override
    public boolean isNewOptionAllowed() {
        return newOptionAllowed;
    }

    @Override
    public void setNewOptionAllowed(boolean newOptionAllowed) {
        this.newOptionAllowed = newOptionAllowed;
    }

    @Override
    public NewOptionHandler getNewOptionHandler() {
        return newOptionHandler;
    }

    @Override
    public void setNewOptionHandler(NewOptionHandler newOptionHandler) {
        this.newOptionHandler = newOptionHandler;
    }

    @Override
    public void disablePaging() {
    }

    @Override
    public boolean isMultiSelect() {
        return false;
    }

    @Override
    public void setMultiSelect(boolean multiselect) {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void validate() throws ValidationException {
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String description) {
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
        impl.setEnabled(editable);
    }

    @Override
    protected Object getSelectedItem() {
        return impl.getSelectedItem();
    }

    @Override
    protected void setSelectedItem(Object item) {
        impl.setSelectedItem(item);
    }
}
