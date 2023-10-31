package ch.usi.gassert.util;

public class InitialAssertionInfo {
	
	private int positiveStateCountEval;
	private int positiveStateCountInit;
	private int numberOfMutationsEval;
	private int negativeStateCountInit;
	
	private int fnInit;
	private int fpEval;
	private double msEval;
	private int complexity = -1;
	
	private int fpInit;
	public int getFpInit() {
		return fpInit;
	}
	public void setFpInit(int fpInit) {
		this.fpInit = fpInit;
	}
	public int getFnInit() {
		return fnInit;
	}
	public void setFnInit(int fnInit) {
		this.fnInit = fnInit;
	}
	public int getFpEval() {
		return fpEval;
	}
	public void setFpEval(int fpEval) {
		this.fpEval = fpEval;
	}
	public double getMsEval() {
		return msEval;
	}
	public void setMsEval(double msEval) {
		this.msEval = msEval;
	}		
	public int getComplexity() {
		return complexity;
	}
	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}
	public int getPositiveStateCountInit() {
		return positiveStateCountInit;
	}
	public void setPositiveStateCountInit(int positiveStateCountInit) {
		this.positiveStateCountInit = positiveStateCountInit;
	}
	public int getNumberOfMutationsEval() {
		return numberOfMutationsEval;
	}
	public void setNumberOfMutationsEval(int numberOfMutationsEval) {
		this.numberOfMutationsEval = numberOfMutationsEval;
	}
	public int getNegativeStateCountInit() {
		return negativeStateCountInit;
	}
	public void setNegativeStateCountInit(int negativeStateCountInit) {
		this.negativeStateCountInit = negativeStateCountInit;
	}	
	
	public int getPositiveStateCountEval() {
		return positiveStateCountEval;
	}
	public void setPositiveStateCountEval(int positiveStateCountEval) {
		this.positiveStateCountEval = positiveStateCountEval;
	}

}
