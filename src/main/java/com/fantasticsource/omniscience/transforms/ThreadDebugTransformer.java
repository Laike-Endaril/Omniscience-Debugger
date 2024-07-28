package com.fantasticsource.omniscience.transforms;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ThreadDebugTransformer implements ClassFileTransformer
{
    private Class cls;

    ThreadDebugTransformer(Class cls)
    {
        this.cls = cls;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes)
    {
        if (classBeingRedefined == cls)
        {
            System.out.println("Transforming " + className + " (" + bytes.length + " bytes)");
            try
            {
                return ThreadASMTest.threadEditBytes();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return bytes;
    }
}
