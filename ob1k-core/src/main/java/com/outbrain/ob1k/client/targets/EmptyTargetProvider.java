package com.outbrain.ob1k.client.targets;

import java.util.NoSuchElementException;

/**
 * @author eran 6/21/15.
 */
public class EmptyTargetProvider implements TargetProvider {
    @Override
    public String provideTarget() {
        throw new NoSuchElementException("No target was set - nothing to provide");
    }
}
