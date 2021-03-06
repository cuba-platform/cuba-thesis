/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.groupbox;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasEnabled;
import com.haulmont.cuba.web.toolkit.ui.client.Tools;
import com.vaadin.client.ui.VPanel;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaGroupBoxWidget extends VPanel implements HasEnabled {

    public static final String CLASSNAME = "cuba-groupbox";

    protected boolean expanded = true;

    protected boolean collapsable = false;

    private boolean enabled = true;

    protected ExpandHandler expandHandler;

    public Element captionWrap;

    public Element expander = DOM.createSpan();

    public Element captionStartDeco = DOM.createDiv();
    public Element captionEndDeco = DOM.createDiv();
    public Element captionTextNode;

    protected boolean captionAsHtml;

    public CubaGroupBoxWidget(String primaryStyleName) {
        setStylePrimaryName(primaryStyleName);
        setStyleName(primaryStyleName);

        captionWrap = captionNode.getParentElement().cast();

        captionNode.setClassName(primaryStyleName + "-caption");
        contentNode.setClassName(primaryStyleName + "-content");
        bottomDecoration.setClassName(primaryStyleName + "-deco");
        expander.setClassName(primaryStyleName + "-expander");

        contentNode.getStyle().clearPosition();

        setExpanded(true);

        captionStartDeco.appendChild(DOM.createDiv());
        captionStartDeco.setClassName(primaryStyleName + "-caption-start-deco");
        captionWrap.insertFirst(captionStartDeco);

        captionEndDeco.appendChild(DOM.createDiv());
        captionEndDeco.setClassName(primaryStyleName + "-caption-end-deco");
        captionWrap.appendChild(captionEndDeco);

        captionNode.insertFirst(expander);

        captionTextNode = (Element) captionNode.getChild(1);
        captionTextNode.setClassName(primaryStyleName + "-caption-text");

        DOM.sinkEvents(expander, Event.ONCLICK);
        DOM.sinkEvents(captionTextNode, Event.ONCLICK);
    }

    public CubaGroupBoxWidget() {
        this(CLASSNAME);
    }

    @Override
    public void setCaption(String text) {
        if (text == null || text.equals("")) {
            addStyleDependentName("nocaption");
        } else {
            removeStyleDependentName("nocaption");
        }

        if (captionAsHtml) {
            captionTextNode.setInnerHTML(text);
        } else {
            captionTextNode.setInnerText(text);
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (expanded) {
            expander.addClassName("expanded");
            getElement().removeClassName("collapsed");
            getElement().addClassName("expanded");
        } else {
            expander.removeClassName("expanded");
            getElement().removeClassName("expanded");
            getElement().addClassName("collapsed");
        }

        this.expanded = expanded;
    }

    public boolean isCollapsable() {
        return collapsable;
    }

    public void setCollapsable(boolean collapsable) {
        Style expanderStyle = expander.getStyle();
        if (collapsable) {
            expanderStyle.clearProperty("display");
            removeStyleDependentName("nocollapsable");
        } else {
            addStyleDependentName("nocollapsable");
            expanderStyle.setDisplay(Style.Display.NONE);
        }

        Tools.textSelectionEnable(captionNode, !collapsable);

        this.collapsable = collapsable;
    }

    public void setCaptionAsHtml(boolean captionAsHtml) {
        this.captionAsHtml = captionAsHtml;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            this.enabled = enabled;
        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (collapsable && DOM.eventGetType(event) == Event.ONCLICK && isEnabled()
                && (DOM.eventGetTarget(event) == expander || DOM.eventGetTarget(event) == captionNode.getChild(1))) {
            toggleExpanded(event);
        } else {
            super.onBrowserEvent(event);
        }
    }

    protected void toggleExpanded(Event event) {
        setExpanded(!expanded);

        if (collapsable && expandHandler != null) {
            if (expanded) {
                expandHandler.expand();
            } else {
                expandHandler.collapse();
            }
        }
        DOM.eventCancelBubble(event, true);
    }

    public interface ExpandHandler {
        void expand();

        void collapse();
    }
}