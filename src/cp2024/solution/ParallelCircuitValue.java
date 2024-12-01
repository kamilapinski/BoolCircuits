package cp2024.solution;

import cp2024.circuit.*;

import java.util.concurrent.*;

public class ParallelCircuitValue implements CircuitValue {
    private final Future<Boolean> value;

    public ParallelCircuitValue(Future<Boolean> value) {
        this.value = value;
    }

    @Override
    public boolean getValue() throws InterruptedException {
        try {
            return value.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw (InterruptedException) cause;
            }
            throw new RuntimeException("Unexpected exception in task execution", cause);
        }
    }
}



