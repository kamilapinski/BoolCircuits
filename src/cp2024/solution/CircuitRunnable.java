package cp2024.solution;

import cp2024.circuit.*;

import java.util.ArrayList;

public class CircuitRunnable implements Runnable{

    private final CircuitNode circuitNode;
    private boolean value;

    public CircuitRunnable(CircuitNode circuitNode) {
        this.circuitNode = circuitNode;
    }

    public boolean getValue() {
        return value;
    }

    private boolean solveIF(CircuitNode[] args) throws InterruptedException {
        boolean condition = (new ParallelCircuitValue(args[0])).getValue();
        if (condition) {
            return (new ParallelCircuitValue(args[1])).getValue();
        }
        else {
            return (new ParallelCircuitValue(args[2])).getValue();
        }
    }

    private boolean solveAND(CircuitNode[] args) throws InterruptedException {

        ArrayList<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            CircuitRunnable circuitRunnable = new CircuitRunnable(args[i]);
            Thread thread = new Thread(circuitRunnable);
            threadList.add(thread);
            thread.start();
        }

        //TODO
        return false;
    }

    private boolean solveOR(CircuitNode[] args) {
        //TODO
        return false;
    }

    private boolean solveGT(CircuitNode[] args, int threshold) {
        //TODO
        return false;
    }

    private boolean solveLT(CircuitNode[] args, int threshold) {
        //TODO
        return false;
    }

    private boolean solveNOT(CircuitNode[] args) {
        //TODO
        return false;
    }

    private void calculateValue(CircuitNode circuitNode) {
        CircuitNode[] args = circuitNode.getArgs();

        value = switch (circuitNode.getType()) {
            case IF -> solveIF(args);
            case AND -> solveAND(args);
            case OR -> solveOR(args);
            case GT -> solveGT(args, ((ThresholdNode) circuitNode).getThreshold());
            case LT -> solveLT(args, ((ThresholdNode) circuitNode).getThreshold());
            case NOT -> solveNOT(args);
            default -> throw new RuntimeException("Illegal type " + circuitNode.getType());
        };
    }

    @Override
    public void run() {
        try {
            if (circuitNode.getType() == NodeType.LEAF) {
                value = ((LeafNode) circuitNode).getValue();
                return;
            }

            calculateValue(circuitNode);



        } catch(InterruptedException e) {

        }
    }

}