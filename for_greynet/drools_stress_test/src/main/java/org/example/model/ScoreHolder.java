package org.example.model;

/**
 * A simple class to hold the calculated score.
 * An instance of this class will be set as a "global" in the Drools session
 * so that rules can modify its state.
 */
public class ScoreHolder {
    private double score = 0.0;
    private long rulesFired = 0;

    public void addPenalty(double penalty) {
        this.score += penalty;
    }

    public void incrementRulesFired() {
        this.rulesFired++;
    }

    public double getScore() {
        return score;
    }

    public long getRulesFired() {
        return rulesFired;
    }
}
