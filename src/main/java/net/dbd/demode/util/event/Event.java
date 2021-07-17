package net.dbd.demode.util.event;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author Nicky Ramone
 */
@RequiredArgsConstructor
@Data
public class Event {

    private final Object type;
    private final Object value;

}
