package gilp.measure;

import java.io.Serializable;

public class ConfusionMatrix implements Serializable {

	private static final long serialVersionUID = 1L;

	private double numberOfTruePositives = 0;// TP

	private double numberOfFalseNegatives = 0;// FN

	private double numberOfFalsePositives = 0;// FP

	private double numberOfTrueNegatives = 0;// TN

	public ConfusionMatrix() {

	}

	public ConfusionMatrix(double numberOfTruePositives, double numberOfFalseNegatives, double numberOfFalsePositives,
			double numberOfTrueNegatives) {
		this.numberOfTruePositives = numberOfTruePositives;
		this.numberOfFalseNegatives = numberOfFalseNegatives;
		this.numberOfFalsePositives = numberOfFalsePositives;
		this.numberOfTrueNegatives = numberOfTrueNegatives;
	}

	@Override
	public Object clone() {
		return new ConfusionMatrix(this.numberOfTruePositives, this.numberOfFalseNegatives, this.numberOfFalsePositives,
				this.numberOfTrueNegatives);
	}

	public void addTruePositives(double numberOfTruePositivesToAdd) {
		this.numberOfTruePositives += numberOfTruePositivesToAdd;
	}

	public void addFalseNegatives(double numberOfFalseNegativesToAdd) {
		this.numberOfFalseNegatives += numberOfFalseNegativesToAdd;
	}

	public void addFalsePositives(double numberOfFalsePositivesToAdd) {
		this.numberOfFalsePositives += numberOfFalsePositivesToAdd;
	}

	public void addTrueNegatives(double numberOfTrueNegativesToAdd) {
		this.numberOfTrueNegatives += numberOfTrueNegativesToAdd;
	}

	public double getNumberOfTruePositives() {
		return this.numberOfTruePositives;
	}

	public double getNumberOfFalseNegatives() {
		return this.numberOfFalseNegatives;
	}

	public double getNumberOfFalsePositives() {
		return this.numberOfFalsePositives;
	}

	public double getNumberOfTrueNegatives() {
		return this.numberOfTrueNegatives;
	}

	public double getNumberOfCorrectlyClassified() {
		return this.numberOfTruePositives + this.numberOfTrueNegatives;
	}

	public double getNumberOfIncorrectClassified() {
		return this.numberOfFalsePositives + this.numberOfFalseNegatives;
	}

	public double getNumberOfPositives() {
		return this.numberOfTruePositives + this.numberOfFalseNegatives;
	}

	public double getNumberOfNegatives() {
		return this.numberOfTrueNegatives + this.numberOfFalsePositives;
	}

	public double getNumberOfPredictedPositive() {
		return this.numberOfTruePositives + this.numberOfFalsePositives;
	}

	public double getNumberOfPredictedNegative() {
		return this.numberOfTrueNegatives + this.numberOfFalseNegatives;
	}

	public double getNumberOfExamples() {
		return this.numberOfTruePositives + this.numberOfFalsePositives + this.numberOfFalseNegatives
				+ this.numberOfTrueNegatives;
	}

	@Override
	public String toString() {
		return "[[" + this.getNumberOfTruePositives() + " " + this.getNumberOfFalsePositives() + "]["
				+ this.getNumberOfFalseNegatives() + " " + this.getNumberOfTrueNegatives() + "]]";
	}

	public void setNumberOfTruePositives(double numberOfTruePositives) {
		this.numberOfTruePositives = numberOfTruePositives;
	}

	public void setNumberOfFalseNegatives(double numberOfFalseNegatives) {
		this.numberOfFalseNegatives = numberOfFalseNegatives;
	}

	public void setNumberOfFalsePositives(double numberOfFalsePositives) {
		this.numberOfFalsePositives = numberOfFalsePositives;
	}

	public void setNumberOfTrueNegatives(double numberOfTrueNegatives) {
		this.numberOfTrueNegatives = numberOfTrueNegatives;
	}

}