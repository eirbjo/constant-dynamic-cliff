package com.eirbjo.cpc.runtime;

import java.lang.invoke.*;

public class Bootstraps {
    private static final VarHandle LONG_ARRAY_VAR_HANDLE = MethodHandles.arrayElementVarHandle(long[].class);

    private static CallSite enterCallSite = new MutableCallSite(MethodHandles.constant(Counter.class, new Counter()));

    public static VarHandle varHandleConstant(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return LONG_ARRAY_VAR_HANDLE;
    }

    public static CallSite varHandleIndy(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return new ConstantCallSite(MethodHandles.constant(VarHandle.class, LONG_ARRAY_VAR_HANDLE));
    }

    public static CallSite methodEnterIndy(MethodHandles.Lookup lookup, String name, MethodType type) {
        return enterCallSite;
    }
}
