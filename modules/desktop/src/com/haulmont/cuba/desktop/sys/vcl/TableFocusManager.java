/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.sys.vcl;

import sun.awt.CausedFocusEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

/**
 * <p>Handle focus traversing in table</p>
 * <p>Supports focus forward, backward, up and down navigation</p>
 *
 * @author artamonov
 * @version $Id$
 */
public class TableFocusManager {

    private JTable impl;

    public TableFocusManager(JTable impl) {
        this.impl = impl;
    }

    public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        Set<AWTKeyStroke> forwardKeys = KeyboardFocusManager.getCurrentKeyboardFocusManager().getDefaultFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        Set<AWTKeyStroke> backwardKeys = KeyboardFocusManager.getCurrentKeyboardFocusManager().getDefaultFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        if (forwardKeys.contains(ks)) {
            nextFocusElement();
            return true;
        } else if (backwardKeys.contains(ks)) {
            prevFocusElement();
            return true;
        } else if (e.getModifiers() == 0) {
            return processExtraKeyBinding(ks, e, condition, pressed);
        }

        return false;
    }

    protected boolean processExtraKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (e.getKeyCode() == KeyEvent.VK_UP && pressed) {
            nextUpElement();
            return true;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && pressed) {
            nextDownElement();
            return true;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && pressed) {
            prevFocusElement();
            return true;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && pressed) {
            nextFocusElement();
            return true;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && pressed) {
            impl.requestFocus();
            impl.editingCanceled(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public void processFocusEvent(FocusEvent e) {
        if (e.getID() == FocusEvent.FOCUS_GAINED) {
            if (e instanceof CausedFocusEvent) {
                if (((CausedFocusEvent) e).getCause() == CausedFocusEvent.Cause.TRAVERSAL_FORWARD) {
                    if (impl.getModel().getRowCount() > 0) {
                        moveToStart(0, 0);
                    } else
                        impl.transferFocus();

                } else if (((CausedFocusEvent) e).getCause() == CausedFocusEvent.Cause.TRAVERSAL_BACKWARD) {
                    if (impl.getModel().getRowCount() > 0) {
                        moveToEnd(impl.getRowCount() - 1, impl.getColumnCount() - 1);
                    } else
                        impl.transferFocusBackward();
                }
            }
        }
    }

    /**
     * Navigate down throw Table rows
     */
    protected void nextDownElement() {
        int editingColumn = getActiveColumn();
        int editingRow = getActiveRow();
        int nextRow = editingRow + 1;
        if (editingColumn == -1 || editingRow == -1) {
            return;
        }
        if (nextRow > impl.getRowCount() - 1) {
            nextRow = 0;
        }
        moveToStart(nextRow, editingColumn);
    }

    /**
     * Navigate up throw Table rows
     */
    protected void nextUpElement() {
        int editingColumn = getActiveColumn();
        int editingRow = getActiveRow();
        int nextRow = editingRow - 1;
        if (editingColumn == -1 || editingRow == -1) {
            return;
        }
        if (nextRow == -1) {
            nextRow = impl.getRowCount() - 1;
        }
        moveToStart(nextRow, editingColumn);
    }

    /**
     * Navigate to next active control or cell in table
     */
    public void prevFocusElement() {
        int selectedColumn = getActiveColumn();
        int selectedRow = getActiveRow();
        int prevColumn = selectedColumn - 1;
        int prevRow = selectedRow;

        if (selectedColumn == -1 || selectedRow == -1) {
            if (impl.getModel().getRowCount() > 0) {
                moveToEnd(impl.getRowCount() - 1, impl.getColumnCount() - 1);
            } else
                moveFocusToPrevControl();
            return;
        }

        if (selectedColumn == 0) {
            prevColumn = impl.getColumnCount() - 1;
            prevRow = selectedRow - 1;
        }

        JComponent activeComponent = getActiveComponent();
        boolean wasMoved = false;
        if (activeComponent != null) {
            wasMoved = moveFocusPrevIntoComponent(activeComponent);
        }

        if (!wasMoved) {
            if (prevRow < 0)
                impl.transferFocusBackward();
            else
                moveToEnd(prevRow, prevColumn);
        }
    }

    /**
     * Navigate to previous active control or cell in table
     */
    public void nextFocusElement() {
        int selectedColumn = getActiveColumn();
        int selectedRow = getActiveRow();
        int nextColumn = selectedColumn + 1;
        int nextRow = selectedRow;
        if (selectedColumn == -1 || selectedRow == -1) {
            if (impl.getModel().getRowCount() > 0) {
                moveToStart(0, 0);
            } else
                moveFocusToNextControl();

            return;
        }
        if (selectedColumn == impl.getColumnCount() - 1) {
            nextColumn = 0;
            nextRow = selectedRow + 1;
        }

        JComponent activeComponent = getActiveComponent();
        boolean wasMoved = false;
        if (activeComponent != null) {
            wasMoved = moveFocusNextIntoComponent(activeComponent);
        }

        if (!wasMoved) {
            if (nextRow > impl.getRowCount() - 1)
                impl.transferFocus();
            else
                moveToStart(nextRow, nextColumn);
        }
    }

    /**
     * Focus first cell in specified row
     *
     * @param selectedRow Focused row
     */
    public void focusSelectedRow(int selectedRow) {
        if (impl.getModel().getRowCount() > 0) {
            moveToStart(selectedRow, 0);
        } else
            moveFocusToNextControl();
    }

    protected void moveTo(int row, int col) {
        Component editorComp = impl.getEditorComponent();

        if (editorComp != null) {
            editorComp.dispatchEvent(new FocusEvent(editorComp, FocusEvent.FOCUS_LOST, false, impl));
        }
        impl.scrollRectToVisible(impl.getCellRect(row, col, true));

        if (row >= 0 && col >= 0)
            impl.requestFocus();

        impl.getSelectionModel().setSelectionInterval(row, row);
        impl.getColumnModel().getSelectionModel().setSelectionInterval(col, col);
        impl.editCellAt(
                impl.getSelectedRow(),
                impl.getSelectedColumn()
        );
    }

    protected void moveToStart(int row, int col) {
        moveTo(row, col);
        JComponent newEditorComp = (JComponent) impl.getEditorComponent();

        if (newEditorComp != null) {
            newEditorComp.requestFocusInWindow();
            KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            FocusTraversalPolicy defaultFocusTraversalPolicy = focusManager.getDefaultFocusTraversalPolicy();
            Component component = defaultFocusTraversalPolicy.getFirstComponent(newEditorComp);

            if (component != null)
                component.requestFocus();
        }
    }

    protected void moveToEnd(int row, int col) {
        moveTo(row, col);
        JComponent newEditorComp = (JComponent) impl.getEditorComponent();

        if (newEditorComp != null) {
            newEditorComp.requestFocusInWindow();
            KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            FocusTraversalPolicy defaultFocusTraversalPolicy = focusManager.getDefaultFocusTraversalPolicy();
            Component component = defaultFocusTraversalPolicy.getLastComponent(newEditorComp);

            if (component != null)
                component.requestFocus();
        }
    }

    protected JComponent getActiveComponent() {
        return (JComponent) impl.getEditorComponent();
    }

    protected int getActiveColumn() {
        int editingColumn = impl.getEditingColumn();
        int selectedColumn = impl.getSelectedColumn();
        if (editingColumn < 0)
            return selectedColumn;
        else
            return editingColumn;
    }

    protected int getActiveRow() {
        int editingRow = impl.getEditingColumn() == -1 ? -1 : impl.getEditingRow();
        int selectedRow = impl.getSelectedRow();
        if (editingRow < 0)
            return selectedRow;
        else
            return editingRow;
    }

    protected boolean moveFocusNextIntoComponent(Container activeComponent) {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component focusOwner = focusManager.getFocusOwner();
        FocusTraversalPolicy defaultFocusTraversalPolicy = focusManager.getDefaultFocusTraversalPolicy();
        Component lastComponent = defaultFocusTraversalPolicy.getLastComponent(activeComponent);
        if (focusOwner != null &&
                lastComponent != null &&
                lastComponent != focusOwner) {
            if (focusOwner == impl) {
                Component component = defaultFocusTraversalPolicy.getFirstComponent(activeComponent);
                if (component != null)
                    component.requestFocus();
                else
                    moveFocusToNextControl();
            } else {
                moveFocusToNextControl();
            }

            return true;
        }

        return false;
    }

    protected boolean moveFocusPrevIntoComponent(JComponent activeComponent) {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component focusOwner = focusManager.getFocusOwner();
        FocusTraversalPolicy defaultFocusTraversalPolicy = focusManager.getDefaultFocusTraversalPolicy();
        Component firstComponent = defaultFocusTraversalPolicy.getFirstComponent(activeComponent);
        if (focusOwner != null &&
                firstComponent != null &&
                firstComponent != focusOwner) {
            if (focusOwner == impl) {
                Component component = defaultFocusTraversalPolicy.getLastComponent(activeComponent);
                if (component != null)
                    component.requestFocus();
                else
                    moveFocusToPrevControl();
            } else
                moveFocusToPrevControl();

            return true;
        }

        return false;
    }

    protected void moveFocusToNextControl() {
        FocusHelper.moveFocusToNextControl();
    }

    protected void moveFocusToPrevControl() {
        FocusHelper.moveFocusToPrevControl();
    }
}
