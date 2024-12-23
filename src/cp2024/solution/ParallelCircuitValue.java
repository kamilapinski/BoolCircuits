package cp2024.solution;

import cp2024.circuit.*;

import java.util.concurrent.*;

public class ParallelCircuitValue implements CircuitValue {
    private Future<Boolean> value;
    private final boolean canceledBeforeStarted;

    public ParallelCircuitValue(Future<Boolean> value) {
        this.value = value;
        canceledBeforeStarted = false;
    }

    public ParallelCircuitValue() {
        canceledBeforeStarted = true;
    }

    @Override
    public boolean getValue() throws InterruptedException {
        try {
            if (canceledBeforeStarted) {
                throw new InterruptedException("Circuit Solver stopped. This value can't be calculated.");
            } else {
                return value.get();
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw (InterruptedException) cause;
            }
            throw new InterruptedException();
        }
    }

    public void stop() {
        if (!value.isDone()) {
            value.cancel(true);
        }
    }
}



