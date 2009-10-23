package edu.mit.csail.cgs.sigma.blots;

public interface IntensityModel {
	public int getRange();
	public int getExpectedCount(int intensity);
	
	public static class SumModel implements IntensityModel {

		public int getExpectedCount(int intensity) {
			return 0;
		}

		public int getRange() {
			return 0;
		} 
	}
}

