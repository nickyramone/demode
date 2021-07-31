package net.dbd.demode.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Nicky Ramone
 */
@RequiredArgsConstructor
public class InsufficientDiskSpaceException extends RuntimeException {

    @Getter
    private final long requiredSpaceInBytes;

}
