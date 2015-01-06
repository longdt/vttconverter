package com.solt.vttconverter;

import java.util.Map;
import java.util.Map.Entry;

public class SubEntry {
	/**
	 * Start Timecode (in milliseconds)
	 */
	private long start = -1;

	/**
	 * Stop Timecode (in milliseconds)
	 */
	private long stop = -1;
	/**
	 * Brut text (with ends of lines)
	 */
	private String text;
	/**
	 * Pre-formated text (without ends of lines) generated to compute satistics
	 */
	private String strippedText;
	/**
	 * Brut text (with ends of lines) without Advanced SSA tags
	 */
	private String noTagText;
	/**
	 * Entry duration in milliseconds
	 */
	private long durationMS;
	/**
	 * Caracters / second
	 */
	private float CPS;
	/**
	 * Reading Speed (based on VisualSubSync algorithm)
	 */
	private float readingSpeed;
	
	public SubEntry(String start, String stop, String text) {
		this.start = tc2ms(start);
		this.stop = tc2ms(stop);
		this.text = text.trim();
	}

	public static long tc2ms(String timecode) {
		String[] msParts = timecode.contains(".") ? timecode.split("\\.") : timecode.split(",");
		long rs = 0;
		if (msParts.length > 2) {
			return -1;
		} else if (msParts.length == 2) {
			rs = Integer.parseInt(msParts[1]);
		}
		String[] hms = msParts[0].split(":");
		if (hms.length != 3) {
			return -1;
		}
		rs += (Integer.parseInt(hms[0]) * 3600 + Integer.parseInt(hms[1]) * 60 + Integer.parseInt(hms[2])) * 1000;
		return rs;
	}
	
	public static String ms2tc(long ms) {
		long tcMS = ms % 1000;
		ms = ms / 1000;
		long tcS = ms % 60;
		ms = ms / 60;
		long tcM = ms % 60;
		ms = ms / 60;
		long tcH = ms % 24;
		return String.format("%02d:%02d:%02d,%03d", tcH, tcM, tcS, tcMS);
	}
	
	public void prepForStats() {
		genStrippedText();
		calcDuration();
		calcCPS();
		calcRS();
	}
	
	public String getText(boolean stripTags, boolean stripBasic, Map<String, String> replacements) {
		if (stripTags) {
			stripTags(stripBasic, replacements);
			return noTagText;
		}
		return text;
	}
	
	public long getStart() {
		if (start == -1) {
			calcDuration();
		}
		return start;
	}
	
	public long getStop() {
		if (stop == -1) {
			calcDuration();
		}
		return stop;
	}
	
	public String getStrippedText() {
		genStrippedText();
		return strippedText;
	}
	
	public long getDurationMS() {
		return durationMS;
	}
	
	public float getCPS() {
		return CPS;
	}
	
	public float getReadingSpeed() {
		return readingSpeed;
	}
	
	public String getTimeCodeString(boolean vtt) {
		String result = ms2tc(start) + " --> " + ms2tc(stop);
		if (vtt) {
			result = result.replace(',', '.');
		}
		return result;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void setStart(long start) {
		this.start = start;
	}
	
	public void setStop(long stop) {
		this.stop = stop;
	}
	
	public void genStrippedText() {
		stripTags(true, null);
		String pattern = "\r\n|\n|\r";
		strippedText = noTagText.replaceAll(pattern, " ");
	}
	
	public boolean stripTags(boolean stripBasic, Map<String, String> replacements) {
		noTagText = stripBasic ? text.replaceAll("<.*>", "") : text;
		String pattern = "{[^}]+}";
		noTagText = noTagText.replaceAll(pattern, "");
		if (replacements != null) {
			for (Entry<String, String> entry : replacements.entrySet()) {
				noTagText = noTagText.replaceAll(entry.getKey(), entry.getValue());
			}
		}
		return text.equals(noTagText);
	}
	
	public int strlen() {
		if (strippedText == null) {
			genStrippedText();
		}
		return strippedText.length();
	}
	
	public void calcDuration() {
		durationMS = stop - start;
	}
	
	private void calcCPS() {
		CPS = strlen() / (durationMS / 1000.0f);
	}
	
	private void calcRS() {
		if (durationMS <= 500) {
			durationMS = 501;
		}
		readingSpeed = (strlen() * 1000) / (durationMS - 500.0f);
	}
	
	public void scale(long baseTime, int factor) {
		if (factor == 1) {
			return;
		}
		long newStart = baseTime + (getStart() - baseTime) * factor;
		long newStop = baseTime + (getStop() - baseTime) * factor;
		setStart(newStart);
		setStop(newStop);
	}
	
	public void shift(long time) {
		if (time == 0) {
			return;
		}
		
		setStart(getStart() + time);
		setStop(getStop() + time);
	}

}
