/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui.client.table;

import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.haulmont.cuba.web.toolkit.ui.client.Tools;
import com.haulmont.cuba.web.toolkit.ui.client.logging.ClientLogger;
import com.haulmont.cuba.web.toolkit.ui.client.logging.ClientLoggerFactory;
import com.vaadin.client.Focusable;
import com.vaadin.client.UIDL;
import com.vaadin.client.Util;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.client.ui.VScrollTable;

import java.util.Iterator;

/**
 * @author devyatkin
 * @version $Id$
 */
public class CubaScrollTableWidget extends VScrollTable implements ShortcutActionHandler.ShortcutActionHandlerOwner {

    protected static final String WIDGET_CELL_CLASSNAME = "widget-container";

    protected ShortcutActionHandler shortcutHandler;

    protected boolean textSelectionEnabled = false;

    protected ClientLogger logger = ClientLoggerFactory.getLogger("CubaScrollTableWidget");

    protected CubaScrollTableWidget() {
        // handle shortcuts
        DOM.sinkEvents(getElement(), Event.ONKEYDOWN);
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        final int type = DOM.eventGetType(event);
        if (type == Event.ONKEYDOWN && shortcutHandler != null) {
            shortcutHandler.handleKeyboardEvent(event);
        }
    }

    public void setShortcutActionHandler(ShortcutActionHandler handler){
        this.shortcutHandler = handler;
    }

    @Override
    public ShortcutActionHandler getShortcutActionHandler() {
        return shortcutHandler;
    }

    @Override
    protected VScrollTableBody createScrollBody() {
        return new CubaScrollTableBody();
    }

    @Override
    public void updateActionMap(UIDL mainUidl) {
        UIDL actionsUidl = mainUidl.getChildByTagName("actions");
        if (actionsUidl == null) {
            return;
        }

        final Iterator<?> it = actionsUidl.getChildIterator();
        while (it.hasNext()) {
            final UIDL action = (UIDL) it.next();
            final String key = action.getStringAttribute("key");
            final String caption = action.getStringAttribute("caption");
            if (!action.hasAttribute("kc")) {
                actionMap.put(key + "_c", caption);
                if (action.hasAttribute("icon")) {
                    // TODO need some uri handling ??
                    actionMap.put(key + "_i", client.translateVaadinUri(action
                            .getStringAttribute("icon")));
                } else {
                    actionMap.remove(key + "_i");
                }
            }
        }
    }

    protected class CubaScrollTableBody extends VScrollTableBody {

        protected Widget lastFocusedWidget = null;

        @Override
        protected VScrollTableRow createRow(UIDL uidl, char[] aligns2) {
            if (uidl.hasAttribute("gen_html")) {
                // This is a generated row.
                return new VScrollTableGeneratedRow(uidl, aligns2);
            }
            return new CubaScrollTableRow(uidl, aligns2);
        }

        protected class CubaScrollTableRow extends VScrollTableRow {

            public CubaScrollTableRow(UIDL uidl, char[] aligns) {
                super(uidl, aligns);
            }

            @Override
            protected void initCellWithWidget(final Widget w, char align,
                                              String style, boolean sorted, TableCellElement td) {
                super.initCellWithWidget(w, align, style, sorted, td);

                td.getFirstChildElement().addClassName(WIDGET_CELL_CLASSNAME);
                if (w instanceof HasFocusHandlers) {
                    ((HasFocusHandlers) w).addFocusHandler(new FocusHandler() {
                        @Override
                        public void onFocus(FocusEvent event) {
                            lastFocusedWidget = w;

                            if (logger.enabled) {
                                logger.log("onFocus: Focus widget in column: " + childWidgets.indexOf(w));
                            }

                            if (!isSelected()) {
                                deselectAll();

                                toggleSelection();
                                setRowFocus(CubaScrollTableRow.this);
                            }
                        }
                    });
                }
            }

            protected void handleFocusForWidget() {
                if (lastFocusedWidget == null)
                    return;

                logger.log("Handle focus");

                if (isSelected()) {
                    if (lastFocusedWidget instanceof Focusable) {
                        ((Focusable) lastFocusedWidget).focus();

                        if (logger.enabled) {
                            logger.log("onSelect: Focus widget");
                        }
                    } else if (lastFocusedWidget instanceof com.google.gwt.user.client.ui.Focusable) {
                        ((com.google.gwt.user.client.ui.Focusable) lastFocusedWidget).setFocus(true);

                        if (logger.enabled) {
                            logger.log("onSelect: Focus GWT widget");
                        }
                    }
                }

                lastFocusedWidget = null;
            }

            @Override
            public void onBrowserEvent(Event event) {
                super.onBrowserEvent(event);

                if (event.getTypeInt() == Event.ONMOUSEUP)
                    handleFocusForWidget();
            }

            @Override
            protected Element getEventTargetTdOrTr(Event event) {
                final Element eventTarget = event.getEventTarget().cast();
                final Element eventTargetParent = DOM.getParent(eventTarget);
                Widget widget = Util.findWidget(eventTarget, null);
                final Element thisTrElement = getElement();

                if (widget != this) {
                    if (event.getTypeInt() == Event.ONMOUSEUP) {
                        if (widget instanceof Focusable || widget instanceof com.google.gwt.user.client.ui.Focusable) {
                            lastFocusedWidget = widget;
                        }
                    }
                    // find cell
                    Element tdElement = eventTargetParent;
                    while (DOM.getParent(tdElement) != thisTrElement) {
                        tdElement = DOM.getParent(tdElement);
                    }
                    return tdElement;
                }
                return getTdOrTr(eventTarget);
            }

            @Override
            protected void initCellWithText(String text, char align, String style, boolean textIsHTML,
                                            boolean sorted, String description, TableCellElement td) {
                super.initCellWithText(text, align, style, textIsHTML, sorted, description, td);

                Element tdElement = td.cast();
                Tools.textSelectionEnable(tdElement, textSelectionEnabled);
            }

            @Override
            protected void updateCellStyleNames(TableCellElement td, String primaryStyleName) {
                Element container = td.getFirstChild().cast();
                boolean isWidget = container.getClassName() != null
                        && container.getClassName().contains(WIDGET_CELL_CLASSNAME);

                super.updateCellStyleNames(td, primaryStyleName);

                if (isWidget)
                    container.addClassName(WIDGET_CELL_CLASSNAME);
            }
        }
    }
}