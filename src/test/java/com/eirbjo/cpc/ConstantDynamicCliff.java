package com.eirbjo.cpc;

import com.eirbjo.cpc.instrumentation.InstrumentingClassLoader;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ConstantDynamicCliff {

    /**
     * Need an interface since the implementation is loaded using an instrumenting class loader
     */
    public interface Climbable {
        int climb(int i);
    }

    /**
     * The actual method being instrumented
     */
    public static class Cliff implements Climbable {
        public int climb(int input) {
            if(input == 0) {
                return ++input;
            } else if(input > 1) {
                return --input;
            } else {
                long l = 0;
                for(int i = 0; i < 100_000; i++) { // 7
                    l+=getaLong(l); // 8
                }
                return (int) l; // 9
            }
        }

        private long getaLong(long l) {
            return l + 5;
        }

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        if(true) {
            Main.main(new String[]{ConstantDynamicCliff.class.getSimpleName()});
        } else {
            final RunState state = new RunState();
            state.traceClass = true;
            state.useCondy =  true;
            state.setup();
            new ConstantDynamicCliff().climb(state);
        }


    }

    @Benchmark
    public int climb(RunState state) {
        return state.climbable.climb(1);
    }


    @State(Scope.Benchmark)
    public static class RunState {
        private Climbable climbable;

        @Param({"true", "false"})
        public boolean useCondy;

        public boolean traceClass = false;

        @Setup(Level.Trial)
        public void setup() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {
            var classLoader = new InstrumentingClassLoader(useCondy, traceClass);
            climbable = (Climbable) classLoader.loadClass(Cliff.class.getName()).getConstructor().newInstance();
        }

    }

}
