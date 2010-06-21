/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.12.2008 15:12:41
 * $Id$
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.gui.data.ValueListener;
import org.dom4j.Element;

import java.util.Collection;

/**
 * Root of the GenericUI components hierarchy
 */
public interface Component {

    enum Alignment {
        TOP_RIGHT,
        TOP_LEFT,
        TOP_CENTER,
        MIDDLE_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_CENTER,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER
    }

    int UNITS_PIXELS = 0;
    int UNITS_PERCENTAGE = 8;

    /** Component ID as defined in <code>id</code> attribute */
    String getId();
    /** Set component ID */
    void setId(String id);

    String getDebugId();
    void setDebugId(String id);

    /** Is component in enabled state? */
    boolean isEnabled();
    /** Set component enabled state */
    void setEnabled(boolean enabled);

    /** Is component visible? */
    boolean isVisible();
    /** Set component visibility */
    void setVisible(boolean visible);

    /** Set focus to this component */
    void requestFocus();

    /** Get component height in {@link #getHeightUnits()} */
    float getHeight();

    /** Height units: {@link #UNITS_PIXELS}, {@link #UNITS_PERCENTAGE} */
    int getHeightUnits();

    /** Set component height in {@link #getHeightUnits()} */
    void setHeight(String height);

    /** Get component width in {@link #getWidthUnits()} */
    float getWidth();

    /** Width units: {@link #UNITS_PIXELS}, {@link #UNITS_PERCENTAGE} */
    int getWidthUnits();

    /** Set component width in {@link #getWidthUnits()} */
    void setWidth(String width);

    Alignment getAlignment();
    void setAlignment(Alignment alignment);

    /** Current style name. Styles implementation is client-type-specific */
    String getStyleName();
    /** Set style name. Styles implementation is client-type-specific */
    void setStyleName(String name);

    /**
     * Component which can contain other components
     */
    interface Container extends Component {
        void add(Component component);
        void remove(Component component);

        /** Get component directly owned by this container */
        <T extends Component> T getOwnComponent(String id);

        /** Get component belonging to the whole components tree below this container */
        <T extends Component> T getComponent(String id);

        /** Get all components directly owned by this container */
        Collection<Component> getOwnComponents();

        /** Get all components belonging to the whole components tree below this container */
        Collection<Component> getComponents();
    }

    /**
     * Component delegating work to some "wrapped" client-specific implementation
     */
    interface Wrapper extends Component {
        <T> T getComponent();
        Object getComposition();
    }

    /**
     * Component containing a layout
     */
    interface HasLayout extends Component {
        void expandLayout(boolean expandLayout);
    }

    /**
     * Component belonging to a frame
     */
    interface BelongToFrame extends Component {
        <A extends IFrame> A getFrame();
        void setFrame(IFrame frame);
    }

    /**
     * Object having a caption
     */
    interface HasCaption {
        String getCaption();
        void setCaption(String caption);

        String getDescription();
        void setDescription(String description);
    }

    /**
     * Object having a value
     */
    interface HasValue extends Editable, BelongToFrame {
        <T> T getValue();
        void setValue(Object value);

        void addListener(ValueListener listener);
        void removeListener(ValueListener listener);
    }

    /**
     * Object having a formatter
     */
    interface HasFomatter {
        Formatter getFormatter();
        void setFormatter(Formatter formatter);
    }

    /**
     * Object having an XML descriptor attached
     */
    interface HasXmlDescriptor {
        Element getXmlDescriptor();
        void setXmlDescriptor(Element element);
    }

    /**
     * Component containing {@link Action}s
     */
    interface ActionsHolder extends Component {
        void addAction(Action action);
        void removeAction(Action action);

        Collection<Action> getActions();

        Action getAction(String id);
    }

    /**
     * Component supporting "editable" state
     */
    interface Editable extends Component {
        boolean isEditable();
        void setEditable(boolean editable);
    }

    /**
     * Object supporting save/restore of user settings.
     * See also {@link com.haulmont.cuba.security.app.UserSettingServiceBean}
     */
    interface HasSettings {
        void applySettings(Element element);
        boolean saveSettings(Element element);
    }

    /**
     * Component supporting "expandable" state
     */
    interface Expandable extends Component {
        void setExpandable(boolean expandable);
        boolean isExpandable();
    }

    interface Disposable {
        void dispose();
        boolean isDisposed();
    }

    /**
     * Component supporting an action
     */
    interface ActionOwner {
        Action getAction();
        void setAction(Action action);
    }

    /**
     * Component having an icon
     */
    interface HasIcon {
        String getIcon();
        void setIcon(String icon);
    }

    interface HasButtonsPanel {
        ButtonsPanel getButtonsPanel();
        void setButtonsPanel(ButtonsPanel panel);
    }
}
