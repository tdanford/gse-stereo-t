package edu.mit.csail.cgs.cgstools.oldregression;

public interface Transformation {
	public double transform(double input);
	public double inverseTransform(double output);
	
	public static class LogTransformation implements Transformation {

		public double inverseTransform(double output) {
			return Math.exp(output);
		}

		public double transform(double input) {
			return Math.log(input);
		} 
	}

	public static class ShiftTransformation implements Transformation {
		
		private double shift;
		public ShiftTransformation(double s) { shift = s; }

		public double inverseTransform(double output) {
			return output - shift;
		}

		public double transform(double input) {
			return input + shift;
		} 
	}
	public static class ScaleTransformation implements Transformation {
		
		private double scale;
		public ScaleTransformation(double s) { scale = s; }

		public double inverseTransform(double output) {
			return output / scale;
		}

		public double transform(double input) {
			return input * scale;
		} 
	}
}
