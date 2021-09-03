import java.util.concurrent.RecursiveAction;

public class SimulationProcessor extends RecursiveAction {
    private static final int SPLIT_THRESHOLD = 10;

    private final Country[] toProcess;
    private final int startPoint;
    private final int length;

    public SimulationProcessor(Country[] countryInstances, int start, int length) {
        toProcess = countryInstances;
        startPoint = start;
        this.length = length;
    }

    @Override
    protected void compute() {
        if (length <= SPLIT_THRESHOLD) {
            processDirectly();
        } else {
            int splitPoint = length / 2;
            invokeAll(new SimulationProcessor(toProcess, startPoint, splitPoint), new SimulationProcessor(toProcess, startPoint + splitPoint, length - splitPoint));
        }
    }
    private void processDirectly() {
        for (int i = startPoint; i < startPoint + length; i++) {
            toProcess[i].calculateResults();
        }
    }
}
