# ConstantDynamic performance cliff benchmark

This is a minimal reproducer for an issue where simply changing a single ConstantDynamic 
instruction to an invokeDynamic instruction causes a 74X speedup in a JMH benchmark of some instrumented code.

The JHM results:

```
Benchmark                   (useCondy)   Mode  Cnt      Score     Error  Units
ConstantDynamicCliff.climb        true  thrpt   25    356.166 ±  16.374  ops/s
ConstantDynamicCliff.climb       false  thrpt   25  26922.347 ± 619.850  ops/s
```

By tracing the bytecode we can output the text representation of the produced bytecode with useCondy=true|false and diff them: 

```
69c69,73
<     LDC varHandleConstant : Ljava/lang/invoke/VarHandle; com/eirbjo/cpc/runtime/Bootstraps.varHandleConstant(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle; (6) []
---
>     INVOKEDYNAMIC longArrayVarHandleIndy()Ljava/lang/invoke/VarHandle; [
>       // handle kind 0x6 : INVOKESTATIC
>       com/eirbjo/cpc/runtime/Bootstraps.methodEnterIndy(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
>       // arguments: none
>     ]
```

The full byte code trace when useCondy=true:

```
  public climb(I)I
    INVOKEDYNAMIC methodEnterIndy()Lcom/eirbjo/cpc/runtime/Counter; [
      // handle kind 0x6 : INVOKESTATIC
      com/eirbjo/cpc/runtime/Bootstraps.methodEnterIndy(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments: none
    ]
    ASTORE 3
   L0
   L1
    LINENUMBER 24 L1
    ILOAD 1
    IFNE L2
   L3
    LINENUMBER 25 L3
    IINC 1 1
    ILOAD 1
    IRETURN
   L2
    LINENUMBER 26 L2
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter] []
    ILOAD 1
    ICONST_1
    IF_ICMPLE L4
   L5
    LINENUMBER 27 L5
    IINC 1 -1
    ILOAD 1
    IRETURN
   L4
    LINENUMBER 29 L4
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter] []
    LCONST_0
    LSTORE 4
   L6
    LINENUMBER 30 L6
    ICONST_0
    ISTORE 6
   L7
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter T J I] []
    ILOAD 6
    LDC 100000
    IF_ICMPGE L8
   L9
    LINENUMBER 31 L9
    LLOAD 4
    ALOAD 0
    LLOAD 4
    INVOKEVIRTUAL com/eirbjo/cpc/ConstantDynamicCliff$Cliff.getaLong (J)J
    LADD
    LSTORE 4
   L10
    LINENUMBER 30 L10
    IINC 6 1
    GOTO L7
   L8
    LINENUMBER 33 L8
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter T J] []
    LLOAD 4
    L2I
    IRETURN
   L11
    LOCALVARIABLE i I L7 L8 6
    LOCALVARIABLE l J L6 L11 4
    LOCALVARIABLE this Lcom/eirbjo/cpc/ConstantDynamicCliff$Cliff; L1 L11 0
    LOCALVARIABLE input I L1 L11 1
    TRYCATCHBLOCK L0 L12 L12 null
   L12
    ALOAD 3
    LDC varHandleConstant : Ljava/lang/invoke/VarHandle; com/eirbjo/cpc/runtime/Bootstraps.varHandleConstant(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle; (6) []
    INVOKEVIRTUAL com/eirbjo/cpc/runtime/Counter.count (Ljava/lang/invoke/VarHandle;)V
    ATHROW
    MAXSTACK = 5
    MAXLOCALS = 7
```

And here's the corresponding byte code trace when useCondy=false:

```
  public climb(I)I
    INVOKEDYNAMIC methodEnterIndy()Lcom/eirbjo/cpc/runtime/Counter; [
      // handle kind 0x6 : INVOKESTATIC
      com/eirbjo/cpc/runtime/Bootstraps.methodEnterIndy(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments: none
    ]
    ASTORE 3
   L0
   L1
    LINENUMBER 24 L1
    ILOAD 1
    IFNE L2
   L3
    LINENUMBER 25 L3
    IINC 1 1
    ILOAD 1
    IRETURN
   L2
    LINENUMBER 26 L2
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter] []
    ILOAD 1
    ICONST_1
    IF_ICMPLE L4
   L5
    LINENUMBER 27 L5
    IINC 1 -1
    ILOAD 1
    IRETURN
   L4
    LINENUMBER 29 L4
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter] []
    LCONST_0
    LSTORE 4
   L6
    LINENUMBER 30 L6
    ICONST_0
    ISTORE 6
   L7
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter T J I] []
    ILOAD 6
    LDC 100000
    IF_ICMPGE L8
   L9
    LINENUMBER 31 L9
    LLOAD 4
    ALOAD 0
    LLOAD 4
    INVOKEVIRTUAL com/eirbjo/cpc/ConstantDynamicCliff$Cliff.getaLong (J)J
    LADD
    LSTORE 4
   L10
    LINENUMBER 30 L10
    IINC 6 1
    GOTO L7
   L8
    LINENUMBER 33 L8
   FRAME FULL [com/eirbjo/cpc/ConstantDynamicCliff$Cliff I com/eirbjo/cpc/runtime/Counter T J] []
    LLOAD 4
    L2I
    IRETURN
   L11
    LOCALVARIABLE i I L7 L8 6
    LOCALVARIABLE l J L6 L11 4
    LOCALVARIABLE this Lcom/eirbjo/cpc/ConstantDynamicCliff$Cliff; L1 L11 0
    LOCALVARIABLE input I L1 L11 1
    TRYCATCHBLOCK L0 L12 L12 null
   L12
    ALOAD 3
    INVOKEDYNAMIC longArrayVarHandleIndy()Ljava/lang/invoke/VarHandle; [
      // handle kind 0x6 : INVOKESTATIC
      com/eirbjo/cpc/runtime/Bootstraps.methodEnterIndy(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      // arguments: none
    ]
    INVOKEVIRTUAL com/eirbjo/cpc/runtime/Counter.count (Ljava/lang/invoke/VarHandle;)V
    ATHROW
    MAXSTACK = 5
    MAXLOCALS = 7
```
