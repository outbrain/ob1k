package com.outbrain.ob1k.server.builder;

/**
 * General purpose extension for the builder mechanism if all you want is to add a
 * building method, not replace the whole builder API.
 */
public interface ExtensionBuilder extends BuilderSection<ServerBuilderState> {
}
