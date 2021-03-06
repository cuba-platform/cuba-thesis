/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.desktop.DetachedFrame;
import com.haulmont.cuba.desktop.TopLevelFrame;
import com.haulmont.cuba.desktop.sys.validation.ValidationAwareAction;
import com.haulmont.cuba.desktop.sys.vcl.CollapsiblePanel;
import com.haulmont.cuba.desktop.sys.vcl.Flushable;
import com.haulmont.cuba.gui.components.AbstractFrame;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.KeyCombination;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Collections;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DesktopComponentsHelper {

    public static final int BUTTON_HEIGHT = 30;
    public static final int FIELD_HEIGHT = 28;

    // todo move nimbus constants to theme
    public static final Color defaultBgColor = (Color) UIManager.get("nimbusLightBackground");
    public static final Color requiredBgColor = (Color) UIManager.get("cubaRequiredBackground");

    /**
     * Returns underlying Swing component implementation.
     *
     * @param component GUI component
     * @return          Swing component
     * @see #getComposition(com.haulmont.cuba.gui.components.Component)
     */
    public static <T extends JComponent> T unwrap(Component component) {
        Object comp = component;
        while (comp instanceof Component.Wrapper) {
            comp = ((Component.Wrapper) comp).getComponent();
        }
        return (T) comp;
    }

    /**
     * Returns underlying Swing component, which serves as the outermost container for the supplied GUI component.
     * For simple components like {@link com.haulmont.cuba.gui.components.Button} this method returns the same
     * result as {@link #unwrap(com.haulmont.cuba.gui.components.Component)}.
     *
     * @param component GUI component
     * @return          Swing component
     * @see #unwrap(com.haulmont.cuba.gui.components.Component)
     */
    public static <T extends JComponent> T getComposition(Component component) {
        Object comp = component;
        while (comp instanceof Component.Wrapper) {
            comp = ((Component.Wrapper) comp).getComposition();
        }
        return (T) comp;
    }

    public static int convertMessageType(IFrame.MessageType messageType) {
        switch (messageType) {
            case CONFIRMATION:
            case CONFIRMATION_HTML:
                return JOptionPane.QUESTION_MESSAGE;
            case WARNING:
            case WARNING_HTML:
                return JOptionPane.WARNING_MESSAGE;
            default:
                return JOptionPane.INFORMATION_MESSAGE;
        }
    }

    public static int convertNotificationType(IFrame.NotificationType type) {
        switch (type) {
            case WARNING:
            case WARNING_HTML:
                return JOptionPane.WARNING_MESSAGE;
            case ERROR:
            case ERROR_HTML:
                return JOptionPane.ERROR_MESSAGE;
            case HUMANIZED:
            case HUMANIZED_HTML:
                return JOptionPane.INFORMATION_MESSAGE;
            case TRAY:
            case TRAY_HTML:
                return JOptionPane.WARNING_MESSAGE;
            default:
                return JOptionPane.PLAIN_MESSAGE;
        }
    }

    public static void adjustSize(JButton button) {
        button.setPreferredSize(new Dimension(0, BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT));
    }

    public static void adjustSize(JComboBox comboBox) {
        comboBox.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
    }

    public static void adjustSize(JTextField textField) {
        textField.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
    }

    public static void adjustDateFieldSize(JPanel dateFieldComposition) {
        dateFieldComposition.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
    }

    /**
     * Convert {@link KeyCombination} to {@link KeyStroke}.
     *
     * @param combination Key combination to convert
     * @return KeyStroke
     */
    public static KeyStroke convertKeyCombination(KeyCombination combination) {
        KeyCombination.Modifier[] modifiers = combination.getModifiers();
        int modifiersMask = 0;
        if (modifiers != null && modifiers.length > 0) {
            for (KeyCombination.Modifier modifier : modifiers) {
                modifiersMask = modifiersMask | convertModifier(modifier);
            }
        }
        return KeyStroke.getKeyStroke(combination.getKey().getVirtualKey(), modifiersMask, false);
    }

    /**
     * Convert {@link KeyCombination.Modifier} to {@link InputEvent} modifier constraint.
     *
     * @param modifier modifier to convert
     * @return {@link InputEvent} modifier constraint
     */
    public static int convertModifier(KeyCombination.Modifier modifier) {
        switch (modifier) {
            case CTRL:
                return InputEvent.CTRL_DOWN_MASK;
            case ALT:
                return InputEvent.ALT_DOWN_MASK;
            case SHIFT:
                return InputEvent.SHIFT_DOWN_MASK;
            default:
                throw new IllegalArgumentException("Modifier " + modifier.name() + " not recognized");
        }
    }

    /**
     * Make JTable handle TAB key as all other components - move focus to next/previous components.
     * <p>Default Swing behaviour for table is to move focus to next/previous cell inside the table.</p>
     * @param table table instance
     */
    public static void correctTableFocusTraversal(JTable table) {
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Collections.singleton(AWTKeyStroke.getAWTKeyStroke("TAB")));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Collections.singleton(AWTKeyStroke.getAWTKeyStroke("shift TAB")));
    }

    /**
     * Add shortcut action to any JComponent.
     *
     * @param name name of action that used as action key in {@link InputMap} and {@link ActionMap}.
     * @param component
     * @param key
     * @param action
     */
    public static void addShortcutAction(String name, JComponent component, KeyStroke key, Action action) {
        ActionMap actionMap = component.getActionMap();
        InputMap inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(key, name);
        actionMap.put(name, action);
    }

    public static void decorateMissingValue(JComponent jComponent, boolean missingValueState) {
        jComponent.setBackground(missingValueState ? requiredBgColor : defaultBgColor);
    }

    /**
     * @return {@link TopLevelFrame} of container
     */
    public static TopLevelFrame getTopLevelFrame(Container container) {
        Container prevContainer;
        Container parent = container;
        do {
            prevContainer = parent;
            if (parent instanceof DetachedFrame) {
                parent = ((DetachedFrame) parent).getParentContainer();
            } if (parent instanceof JPopupMenu) {
                parent = ((JPopupMenu) parent).getInvoker().getParent();
            } else {
                parent = parent.getParent();
            }
        } while (parent != null);

        if (!(prevContainer instanceof TopLevelFrame)) {
            if (prevContainer instanceof JComponent) {
                Object tableForEditor = ((JComponent) prevContainer).getClientProperty(DesktopTableCellEditor.CELL_EDITOR_TABLE);
                if (tableForEditor != null) {
                    return getTopLevelFrame((java.awt.Component) tableForEditor);
                }
            }

            return App.getInstance().getMainFrame();
        }

        return (TopLevelFrame) prevContainer;
    }

    /**
     * @return {@link TopLevelFrame} of component
     */
    public static TopLevelFrame getTopLevelFrame(java.awt.Component component) {
        Container prevContainer;
        Container parent = component instanceof Container ? (Container) component : component.getParent();
        do {
            prevContainer = parent;
            if (parent instanceof DetachedFrame) {
                parent = ((DetachedFrame) parent).getParentContainer();
            } if (parent instanceof JPopupMenu) {
                parent = ((JPopupMenu) parent).getInvoker().getParent();
            } else {
                parent = parent.getParent();
            }
        } while (parent != null);

        if (!(prevContainer instanceof TopLevelFrame)) {
            if (prevContainer instanceof JComponent) {
                Object tableForEditor = ((JComponent) prevContainer).getClientProperty(DesktopTableCellEditor.CELL_EDITOR_TABLE);
                if (tableForEditor != null) {
                    return getTopLevelFrame((Container) tableForEditor);
                }
            }

            return App.getInstance().getMainFrame();
        }

        return (TopLevelFrame) prevContainer;
    }

    /**
     * Returns {@link TopLevelFrame} of component.
     *
     * @param component
     * @return {@link TopLevelFrame} of component
     */
    public static TopLevelFrame getTopLevelFrame(DesktopAbstractComponent component) {
        return getTopLevelFrame(component.getComposition());
    }

    /**
     * Returns {@link TopLevelFrame} of frame.
     *
     * @param frame
     * @return {@link TopLevelFrame} of component
     */
    public static TopLevelFrame getTopLevelFrame(IFrame frame) {
        if (frame instanceof DesktopWindow) {
            return ((DesktopWindow) frame).getWindowManager().getFrame();
        } else if (frame instanceof DesktopFrame) {
            return getTopLevelFrame((frame).getFrame());
        } else if (frame instanceof AbstractFrame) {
            Component.Wrapper wrapper = (Component.Wrapper) ((AbstractFrame) frame).getComposition();
            if (wrapper instanceof DesktopWindow) {
                return ((DesktopWindow) wrapper).getWindowManager().getFrame();
            } else if (wrapper instanceof DesktopFrame) {
                return getTopLevelFrame(((DesktopFrame) wrapper).getFrame());
            } else {
                return getTopLevelFrame((Container) wrapper.getComposition());
            }
        } else {
            throw new IllegalArgumentException("Can not get top level frame for " + frame);
        }
    }

    /**
     * Determines whether component will be displayed on the screen.
     *
     * @param component component
     * @return true if the component and all of its ancestors are visible
     */
    public static boolean isRecursivelyVisible(java.awt.Component component) {
        if (component.getParent() instanceof JTabbedPane) {
            JTabbedPane jTabbedPane = (JTabbedPane) component.getParent();

            boolean tabVisible = false;
            for (java.awt.Component childComponent : jTabbedPane.getComponents()) {
                if (childComponent == component) {
                    tabVisible = true;
                    break;
                }
            }

            return tabVisible && isRecursivelyVisible(component.getParent());
        }

        if (component.getParent() instanceof CollapsiblePanel) {
            return isRecursivelyVisible(component.getParent());
        }

        return component.isVisible() && (component.getParent() == null || isRecursivelyVisible(component.getParent()));
    }

    /**
     * Determines whether component will be displayed on the screen.
     *
     * @param component component
     * @return true if the component and all of its ancestors are visible
     */
    public static boolean isRecursivelyEnabled(java.awt.Component component) {
        if (component.getParent() instanceof JTabbedPane) {
            JTabbedPane jTabbedPane = (JTabbedPane) component.getParent();

            boolean tabVisible = false;
            for (java.awt.Component childComponent : jTabbedPane.getComponents()) {
                if (childComponent == component) {
                    tabVisible = true;
                    break;
                }
            }

            return tabVisible && isRecursivelyEnabled(component.getParent());
        }

        return component.isEnabled() && (component.getParent() == null || isRecursivelyEnabled(component.getParent()))
                && isRecursivelyVisible(component);
    }

    /**
     * Determines real size of HTML label with text on screen.
     *
     * @param html text with html markup
     * @return size of label
     */
    public static Dimension measureHtmlText(String html) {
        JFrame testFrame = new JFrame();
        testFrame.setLayout(new BoxLayout(testFrame.getContentPane(), BoxLayout.PAGE_AXIS));
        JLabel testLabel = new JLabel(html);
        testFrame.add(testLabel);
        testFrame.pack();

        Dimension size = testLabel.getSize();

        testFrame.dispose();

        return new Dimension(size);
    }

    /**
     * Flush changes in current focus owner if needed
     */
    public static void flushCurrentInputField() {
        java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner instanceof Flushable) {
            ((Flushable) focusOwner).flushValue();
        } else if (focusOwner != null && focusOwner.getParent() instanceof Flushable) {
            ((Flushable) focusOwner.getParent()).flushValue();
        }
    }

    public static void addEnterShortcut(com.haulmont.cuba.gui.components.TextField textField, final Runnable runnable) {
        JTextField impl = DesktopComponentsHelper.unwrap(textField);

        impl.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
        impl.getActionMap().put("enter", new ValidationAwareAction() {
            @Override
            public void actionPerformedAfterValidation(ActionEvent e) {
                runnable.run();
            }
        });
    }

    public static RootPaneContainer getSwingWindow(java.awt.Component component) {
        java.awt.Component parent = component;
        while (parent != null) {
            if (parent instanceof RootPaneContainer) {
                return (RootPaneContainer) parent;
            }

            parent = parent.getParent();
        }

        return null;
    }
}