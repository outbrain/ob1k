package com.outbrain.ob1k.client.targets;

/**
 * An API for retrieving tagets for the client invocations.
 * An example target may be a static list of hosts + round robin, or a discovery based provider.
 * @author eran 6/21/15.
 */
public interface TargetProvider {
    String provideTarget();
}
