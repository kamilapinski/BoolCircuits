package cp2024.solution;

import cp2024.circuit.CircuitNode;

import java.util.concurrent.CountDownLatch;

public class CountDownCircuitRunnable extends CircuitRunnable {

    private final CountDownLatch countDownLatch;

    public CountDownCircuitRunnable(CircuitNode circuitNode, CountDownLatch countDownLatch) {
        super(circuitNode);
        this.countDownLatch = countDownLatch;
    }

}
