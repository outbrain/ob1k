package com.outbrain.ob1k.client.targets;

import com.google.common.base.Preconditions;

/**
 * A {@link TargetProvider} that provides a fixed target.
 * @author eran 6/21/15.
 */
public class SimpleTargetProvider implements TargetProvider {

    private final String target;

    public SimpleTargetProvider(final String target) {
        this.target = Preconditions.checkNotNull(target, "target must not be null");
    }

    @Override
    public String provideTarget() {
        return target;
    }
}
