package net.dbd.demode.ui.common;

import net.dbd.demode.util.event.EventSupport;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * Just an event support that dispatches events on the EDT.
 *
 * @author Nicky Ramone
 */
public class SwingEventSupport extends EventSupport {

    public SwingEventSupport() {
        super(new SwingPropertyChangeSupport(NULL_OBJECT));
    }

}
