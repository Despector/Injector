/*
 * Copyright (c) 2015-2016 VoxelBox <http://engine.thevoxelbox.com>.
 * All Rights Reserved.
 */
package com.voxelgenesis.injector;

public class InjectionPoint {

    public static InjectionPoint compile(String identifier, String code) {
        InjectionPoint point = new InjectionPoint(identifier);

        return point;
    }

    private final String identifier;

    private InjectionPoint(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

}
