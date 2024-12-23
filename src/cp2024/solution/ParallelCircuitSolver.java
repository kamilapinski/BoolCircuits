package cp2024.solution;

import cp2024.circuit.*;
import cp2024.demo.BrokenCircuitValue;

import java.util.ArrayList;
import java.util.concurrent.*;

public class ParallelCircuitSolver implements CircuitSolver {

    private static final int N_THREADS = 1000;

    private final ArrayList<ParallelCircuitValue> circuitValues;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);
    private boolean isStopped;

    public ParallelCircuitSolver() {
        circuitValues = new ArrayList<>();
        isStopped = false;
    }

    @Override
    public CircuitValue solve(Circuit c) {
        try {
            if (isStopped) {
                return new ParallelCircuitValue();
            }
            CircuitNode circuitNode = c.getRoot();
            ParallelCircuitValue circuitValue = new ParallelCircuitValue(threadPool.submit(new CircuitCallable(circuitNode)));
            circuitValues.add(circuitValue);

            return circuitValue;
        } catch (Exception e) {
            return new ParallelCircuitValue();
        }
    }

    @Override
    public void stop() {
        isStopped = true;
        threadPool.shutdownNow();
    }

    private static class CircuitCallable implements Callable<Boolean> {

        private static final int SMALL_POOL_SIZE = 3;
        private final CircuitNode circuitNode;
        private final ExecutorService executor;

        CircuitCallable(CircuitNode circuitNode) {
            this.circuitNode = circuitNode;
            executor = Executors.newFixedThreadPool(N_THREADS);
        }

        Boolean getNOTFutureValue(CircuitNode[] args) throws InterruptedException {
            try {
                return !(new CircuitCallable(args[0])).call();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        Boolean getIFFutureValue(CircuitNode[] args) throws InterruptedException {
            ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
            ExecutorService tempExecutor = Executors.newFixedThreadPool(SMALL_POOL_SIZE);

            try {
                Future<Boolean> cond = tempExecutor.submit(new CircuitCallable(args[0]));
                Future<Boolean> a = tempExecutor.submit(new CircuitCallable(args[1]));
                Future<Boolean> b = tempExecutor.submit(new CircuitCallable(args[2]));

                completionService.submit(() -> {
                    try {
                        if (cond.get()) {
                            b.cancel(true);
                            return a.get();
                        }
                        else {
                            a.cancel(true);
                            return b.get();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("Thread interrupted.");
                    }
                });

                completionService.submit(() -> {
                    try {
                        Boolean aResult = a.get();
                        Boolean bResult = b.get();
                        if (aResult.equals(bResult)) {
                            cond.cancel(true);
                            return aResult;
                        } else {
                            return cond.get() ? aResult : bResult;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("Thread interrupted.");
                    }
                });

                Future<Boolean> result = completionService.take();
                Boolean resultBoolean;

                try {
                    resultBoolean = result.get();
                } catch (ExecutionException e) {
                    result = completionService.take();
                    resultBoolean = result.get();
                }

                return resultBoolean;

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw (InterruptedException) cause;
                }
                throw new InterruptedException("Thread interrupted.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } finally {
                tempExecutor.shutdownNow();
                executor.shutdownNow();
            }
        }

        Boolean getManyChildrenValues(CircuitNode[] args, boolean expectedValue, int expectedAmount) throws InterruptedException, ExecutionException {
            ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);

            try {
                for (CircuitNode childCircuitNode : args) {
                    if (Thread.interrupted()) {
                        executor.shutdownNow();
                        throw new InterruptedException("Thread interrupted.");
                    }
                    completionService.submit(new CircuitCallable(childCircuitNode));
                }

                int amount = 0;
                int i = 0;
                while (amount < expectedAmount && i < args.length) {
                    if (Thread.interrupted()) {
                        executor.shutdownNow();
                        throw new InterruptedException("Thread interrupted.");
                    }
                    Future<Boolean> resultFuture = completionService.take();
                    Boolean result = resultFuture.get();
                    if (result.equals(expectedValue)) {
                        amount++;
                    }
                    i++;
                }

                executor.shutdownNow();

                if (amount >= expectedAmount)
                    return expectedValue;
                else
                    return !expectedValue;
            } catch (Exception e) {
                throw new InterruptedException("Thread interrupted.");
            }
        }

        Boolean getANDFutureValue(CircuitNode[] args) throws InterruptedException {
            try {
                return getManyChildrenValues(args, false, 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                throw new InterruptedException("Thread interrupted.");
            }
        }

        Boolean getORFutureValue(CircuitNode[] args) throws InterruptedException {
            try {
                return getManyChildrenValues(args, true, 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                throw new InterruptedException("Thread interrupted.");
            }
        }

        Boolean getGTFutureValue(CircuitNode[] args, int threshold) throws InterruptedException {
            try {
                return getManyChildrenValues(args, true, threshold + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                throw new InterruptedException("Thread interrupted.");
            }
        }

        Boolean getLTFutureValue(CircuitNode[] args, int threshold) throws InterruptedException {
            try {
                int expectedAmount = args.length - threshold + 1;
                return !getManyChildrenValues(args, false, expectedAmount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                throw new InterruptedException("Thread interrupted.");
            }
        }

        @Override
        public Boolean call() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException("Thread interrupted.");
            }

            try {
                NodeType nodeType = circuitNode.getType();
                CircuitNode[] args = circuitNode.getArgs();

                return switch (nodeType){
                    case LEAF -> ((LeafNode) circuitNode).getValue();
                    case NOT -> getNOTFutureValue(args);
                    case IF -> getIFFutureValue(args);
                    case AND -> getANDFutureValue(args);
                    case OR -> getORFutureValue(args);
                    case GT -> getGTFutureValue(args, ((ThresholdNode) circuitNode).getThreshold());
                    case LT -> getLTFutureValue(args, ((ThresholdNode) circuitNode).getThreshold());
                };
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                throw new InterruptedException("Thread interrupted.");
            }
            finally {
                executor.shutdownNow();
            }
        }
    }

}
