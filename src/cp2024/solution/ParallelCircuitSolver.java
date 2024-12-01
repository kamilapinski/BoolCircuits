package cp2024.solution;

import cp2024.circuit.*;
import cp2024.demo.BrokenCircuitValue;

import java.util.ArrayList;
import java.util.concurrent.*;

public class ParallelCircuitSolver implements CircuitSolver {

    private static final int N_THREADS = 1000;

    private final ArrayList<ParallelCircuitValue> circuitValues;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);

    public ParallelCircuitSolver() {
        circuitValues = new ArrayList<>();
    }

    @Override
    public CircuitValue solve(Circuit c) {
        try {
            CircuitNode circuitNode = c.getRoot();
            ParallelCircuitValue circuitValue = new ParallelCircuitValue(threadPool.submit(new CircuitCallable(circuitNode)));
            circuitValues.add(circuitValue);

            return circuitValue;
        } catch (Exception e) {
            //TODO
            return new BrokenCircuitValue();
        }
    }

    @Override
    public void stop() {
        threadPool.shutdownNow();
    }

    private static class CircuitCallable implements Callable<Boolean> {

        private final CircuitNode circuitNode;
        private static final ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);

        CircuitCallable(CircuitNode circuitNode) {
            this.circuitNode = circuitNode;
        }

        Future<Boolean> getNOTFutureValue(CircuitNode[] args) throws InterruptedException {
            return threadPool.submit(() -> {
                try {
                    return !(new CircuitCallable(args[0])).call();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            });
        }

        Future<Boolean> getIFFutureValue(CircuitNode[] args) throws InterruptedException {
            try {
                Future<Boolean> conditionFuture = threadPool.submit(new CircuitCallable(args[0]));

                CircuitNode childCircuitNode;
                if (conditionFuture.get() == Boolean.TRUE) {
                    childCircuitNode = args[1];
                }
                else {
                    childCircuitNode = args[2];
                }

                return threadPool.submit(new CircuitCallable(childCircuitNode));

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw (InterruptedException) cause;
                }
                throw new RuntimeException("Unexpected exception in task execution", cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        ArrayList<Future<Boolean>> getManyChildrenFutureValues(CircuitNode[] args) throws InterruptedException {
            ArrayList<Future<Boolean>> childFutures = new ArrayList<>();
            for (CircuitNode childCircuitNode : args) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Thread interrupted.");
                }
                childFutures.add(threadPool.submit(new CircuitCallable(childCircuitNode)));
            }
            return childFutures;
        }

        Boolean getManyChildrenValues(ArrayList<Future<Boolean>> childrenFutureValues, boolean expectedValue, int expectedAmount) throws InterruptedException, ExecutionException {
            int amount = 0;
            for (Future<Boolean> future : childrenFutureValues) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Thread interrupted.");
                }
                if (amount < expectedAmount) {
                    if (future.get().equals(expectedValue)) {
                        amount++;
                    }
                } else {
                    future.cancel(true);
                }
            }
            if (amount >= expectedAmount)
                return expectedValue;
            else
                return !expectedValue;
        }

        Future<Boolean> getANDFutureValue(CircuitNode[] args) throws InterruptedException {
            try {
                ArrayList<Future<Boolean>> childrenFutureValues = getManyChildrenFutureValues(args);
                return threadPool.submit(() -> getManyChildrenValues(childrenFutureValues, false, 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        Future<Boolean> getORFutureValue(CircuitNode[] args) throws InterruptedException {
            try {
                ArrayList<Future<Boolean>> childrenFutureValues = getManyChildrenFutureValues(args);
                return threadPool.submit(() -> getManyChildrenValues(childrenFutureValues, true, 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        Future<Boolean> getGTFutureValue(CircuitNode[] args, int threshold) throws InterruptedException {
            try {
                ArrayList<Future<Boolean>> childrenFutureValues = getManyChildrenFutureValues(args);
                return threadPool.submit(() -> getManyChildrenValues(childrenFutureValues, true, threshold + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        Future<Boolean> getLTFutureValue(CircuitNode[] args, int threshold) throws InterruptedException {
            try {
                ArrayList<Future<Boolean>> childrenFutureValues = getManyChildrenFutureValues(args);
                int expectedAmount = args.length - threshold + 1;
                return threadPool.submit(() -> !getManyChildrenValues(childrenFutureValues, false, expectedAmount));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        @Override
        public Boolean call() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            try {
                NodeType nodeType = circuitNode.getType();
                CircuitNode[] args = circuitNode.getArgs();

                Future<Boolean> value = switch (nodeType){
                    case NodeType.LEAF -> threadPool.submit(() -> { return ((LeafNode) circuitNode).getValue(); });
                    case NodeType.NOT -> getNOTFutureValue(args);
                    case NodeType.IF -> getIFFutureValue(args);
                    case NodeType.AND -> getANDFutureValue(args);
                    case NodeType.OR -> getORFutureValue(args);
                    case NodeType.GT -> getGTFutureValue(args, ((ThresholdNode) circuitNode).getThreshold());
                    case NodeType.LT -> getLTFutureValue(args, ((ThresholdNode) circuitNode).getThreshold());
                };

                return value.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
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

}
