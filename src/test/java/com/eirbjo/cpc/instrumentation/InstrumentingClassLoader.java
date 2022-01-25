package com.eirbjo.cpc.instrumentation;

import com.eirbjo.cpc.runtime.Bootstraps;
import com.eirbjo.cpc.runtime.Counter;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstrumentingClassLoader extends ClassLoader implements Opcodes {

    private final boolean useCondy;
    private final boolean traceClass;

    public InstrumentingClassLoader(boolean useCondy, boolean traceClass) {
        super(InstrumentingClassLoader.class.getClassLoader());
        this.useCondy = useCondy;
        this.traceClass = traceClass;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            if (shouldLoad(name)) {
                return findClass(name);
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        return getParent().loadClass(name);
    }

    private boolean shouldLoad(String name) {
        return name.endsWith("Cliff");
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class");
        if (stream != null) {

            try {
                final byte[] bytes = stream.readAllBytes();


                ClassReader cr = new ClassReader(bytes);


                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                ClassVisitor visitor = writer;
                if (traceClass) {
                    visitor = new TraceClassVisitor(visitor, new PrintWriter(System.out));
                }

                boolean shouldInstrument = true;
                if (shouldInstrument) {
                    visitor = new ClassVisitor(Opcodes.ASM9, visitor) {


                        final ConstantDynamic LONG_ARRAY_VAR_HANDLE_CONDY = new ConstantDynamic("varHandleConstant",
                                Type.getType(VarHandle.class).getDescriptor(),
                                new Handle(Opcodes.H_INVOKESTATIC,
                                        Type.getInternalName(Bootstraps.class),
                                        "varHandleConstant",
                                        MethodType.methodType(VarHandle.class, MethodHandles.Lookup.class, String.class, Class.class).toMethodDescriptorString(),
                                        false));

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            final MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if (!"climb".equals(name)) {
                                return methodVisitor;
                            } else {
                                return new MethodNode(Opcodes.ASM9, access, name, descriptor, signature, exceptions) {

                                    Set<Integer> lineNumbers = new HashSet<>();

                                    @Override
                                    public void visitLineNumber(int line, Label start) {
                                        lineNumbers.add(line);
                                        super.visitLineNumber(line, start);
                                    }

                                    @Override
                                    public void visitEnd() {
                                        accept(new LocalVariablesSorter(Opcodes.ASM9, access, descriptor, methodVisitor) {
                                            private Label startLabel, endLabel;
                                            Map<Integer, Integer> lineLocals = new HashMap<>();
                                            private int counterLocal;

                                            @Override
                                            public void visitCode() {
                                                super.visitCode();
                                                counterLocal = newLocal(Type.getType(Counter.class));
                                                super.visitInvokeDynamicInsn("methodEnterIndy",
                                                        MethodType.methodType(Counter.class).toMethodDescriptorString(),
                                                        new Handle(Opcodes.H_INVOKESTATIC,
                                                                Type.getInternalName(Bootstraps.class),
                                                                "methodEnterIndy",
                                                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
                                                                false)
                                                );
                                                super.visitVarInsn(ASTORE, counterLocal);


                                                startLabel = new Label();
                                                endLabel = new Label();
                                                visitLabel(startLabel);
                                            }

                                            @Override
                                            public void visitMaxs(int maxStack, int maxLocals) {
                                                visitTryCatchBlock(startLabel, endLabel, endLabel, null);
                                                visitLabel(endLabel);
                                                executeCount();
                                                mv.visitInsn(Opcodes.ATHROW);
                                                super.visitMaxs(maxStack, maxLocals);
                                            }

                                            private void executeCount() {

                                                super.visitVarInsn(ALOAD, counterLocal);
                                                if (useCondy) {
                                                    super.visitLdcInsn(LONG_ARRAY_VAR_HANDLE_CONDY);
                                                } else {
                                                    mv.visitInvokeDynamicInsn("longArrayVarHandleIndy",
                                                            MethodType.methodType(VarHandle.class).toMethodDescriptorString(),
                                                            new Handle(Opcodes.H_INVOKESTATIC,
                                                                    Type.getInternalName(Bootstraps.class),
                                                                    "methodEnterIndy",
                                                                    MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
                                                                    false));
                                                }
                                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(Counter.class),
                                                        "count",
                                                        MethodType.methodType(void.class, VarHandle.class).toMethodDescriptorString(),
                                                        false);


                                            }

                                        });
                                    }
                                };
                            }
                        }
                    };
                }


                cr.accept(visitor, ClassReader.EXPAND_FRAMES);


                final byte[] rewritten = writer.toByteArray();

                return defineClass(name, rewritten, 0, rewritten.length);

            } catch (IOException e) {
                throw new ClassNotFoundException("Not found", e);
            }

        }
        throw new ClassNotFoundException("Not found");
    }
}
