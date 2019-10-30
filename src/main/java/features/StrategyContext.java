package features;

public class StrategyContext {

    private RunStrategy strategy;

    public void setStrategy(RunStrategy strategy) {
        this.strategy = strategy;
    }

    public void runStrategy(){
        strategy.execute();
    }
}
