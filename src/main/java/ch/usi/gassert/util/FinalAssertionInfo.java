package ch.usi.gassert.util;

public class FinalAssertionInfo {
	
	private int numberOfIterations;
	private int complexity;
	private int time;
	
	private int initFPMedian;
	private int initFNMedian;
	
	private int evalFPMedian;
	private String evalMSMedian;
	
	public int getNumberOfIterations() {
		return numberOfIterations;
	}
	public void setNumberOfIterations(int numberOfIterations) {
		this.numberOfIterations = numberOfIterations;
	}
	public int getComplexity() {
		return complexity;
	}
	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public int getInitFPMedian() {
		return initFPMedian;
	}
	public void setInitFPMedian(int initFPMedian) {
		this.initFPMedian = initFPMedian;
	}
	public int getInitFNMedian() {
		return initFNMedian;
	}
	public void setInitFNMedian(int initFNMedian) {
		this.initFNMedian = initFNMedian;
	}
	public int getEvalFPMedian() {
		return evalFPMedian;
	}
	public void setEvalFPMedian(int evalFPMedian) {
		this.evalFPMedian = evalFPMedian;
	}
	public String getEvalMSMedian() {
		return evalMSMedian;
	}
	public void setEvalMSMedian(String evalMSMedian) {
		this.evalMSMedian = evalMSMedian;
	}
	

}
