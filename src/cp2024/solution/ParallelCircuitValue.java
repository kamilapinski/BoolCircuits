package cp2024.solution;

import cp2024.circuit.CircuitNode;
import cp2024.circuit.CircuitValue;

public class ParallelCircuitValue implements CircuitValue {
    private final CircuitNode circuitNode;

    public ParallelCircuitValue(CircuitNode circuitNode) {
        this.circuitNode = circuitNode;
    }

    @Override
    public boolean getValue() throws InterruptedException {
        /* FIX ME */
        throw new RuntimeException("Not implemented.");
    }
}
