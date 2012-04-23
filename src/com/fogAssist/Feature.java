package com.fogAssist;

public class Feature {
	
	/**
	 * 
	 * @param data
	 * @return mean value
	 */
	public static double getMean ( double[] data ) {
		double sum = 0;

		for (int i = 0; i < data.length; i++)
			sum = sum + data[i];

		return (sum / data.length);
	}
	

	/**
	 * 
	 * @param data
	 * @param mean
	 * @return std value
	 */
	public static double getStd ( double[] data, double mean ) {
		double sum = 0;

		for (int i = 0; i < data.length; i++)
			sum = sum + (data[i] - mean) * (data[i] - mean);

		return Math.sqrt(sum / (data.length - 1));
	}

	
	/**
	 * 
	 * @param data
	 * @return std value
	 */
	public static double getStd ( double[] data ) {
		
		double sum, mean;
		
		// compute first mean
		sum = 0;
		for (int i = 0; i < data.length; i++)
			sum = sum + data[i];
		mean = sum / data.length;
		
		// compute std
		sum = 0;
		for (int i = 0; i < data.length; i++)
			sum = sum + (data[i] - mean) * (data[i] - mean);
		
		return Math.sqrt(sum / (data.length - 1));
	}

}
