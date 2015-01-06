package com.solt.vttconverter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.universalchardet.UniversalDetector;

public class VTTConverter {
	private static final String SRT_PATTERN_STR = "[0-9]+(?:\r\n|\r|\n)([0-9]{2}:[0-9]{2}:[0-9]{2}(?:,|\\.)[0-9]{3}) --> ([0-9]{2}:[0-9]{2}:[0-9]{2}(?:,|\\.)[0-9]{3})(?:\r\n|\r|\n)((?:.*(?:\r\n|\r|\n))*?)(?:\r\n|\r|\n)";	
	private static final String ASS_PATTERN_STR = "Dialogue: [0-9],([^,]*),([^,]*),([^,]*),([^,]*),[^,]*,[^,]*,[^,]*,[^,.]*,(.*)";
	private static final Pattern SRT_PATTERN = Pattern.compile(SRT_PATTERN_STR);
	private static final Pattern ASS_PATTERN = Pattern.compile(ASS_PATTERN_STR);
	
	public static String getContent(Reader reader) throws IOException {
		StringBuilder rs = new StringBuilder();
		try (BufferedReader breader = new BufferedReader(reader)) {
			String line = null;
			while ((line = breader.readLine()) != null) {
				rs.append(line).append('\n');
			}
		};
		if (rs.length() > 0) {
			rs.deleteCharAt(rs.length() - 1);
		}
		return rs.toString();
	}
	
	/**
	 * @experience
	 * @param srtContent
	 * @return
	 */
	public static boolean isValidSRT(String srtContent) {
		return SRT_PATTERN.matcher(srtContent).find();
	}
	
	private static List<SubEntry> parseSRTSubtitles(String srtContent) {
		Matcher matcher = SRT_PATTERN.matcher(srtContent);
		List<SubEntry> subs = new ArrayList<>();
		while (matcher.find()) {
			SubEntry entry = new SubEntry(matcher.group(1), matcher.group(2), matcher.group(3));
			subs.add(entry);
		}
		return subs;
	}
	
	private static List<SubEntry> parseASSSubtitles(String srtContent) {
		Matcher matcher = ASS_PATTERN.matcher(srtContent);
		List<SubEntry> subs = new ArrayList<>();
		while (matcher.find()) {
			SubEntry entry = new SubEntry(matcher.group(1), matcher.group(2), matcher.group(5).replace("\\N", "\n"));
			subs.add(entry);
		}
		return subs;
	}

	public static String fromSRT(Reader reader, boolean stripTags, boolean stripBasic) throws IOException {
		return fromSRT(getContent(reader), stripTags, stripBasic);
	}
	
	public static String fromSRT(String content, boolean stripTags, boolean stripBasic) {
		List<SubEntry> subs = parseSRTSubtitles(content);
		return toVTT(subs, stripBasic, stripBasic);
	}
	
	public static String fromASS(Reader reader, boolean stripTags, boolean stripBasic) throws IOException {
		return fromASS(getContent(reader), stripTags, stripBasic);
	}
	
	public static String fromASS(String content, boolean stripTags, boolean stripBasic) {
		List<SubEntry> subs = parseASSSubtitles(content);
		return toVTT(subs, stripBasic, stripBasic);
	}
	
	public static String tryConvert(Reader reader, boolean stripTags, boolean stripBasic) throws IOException {
		return tryConvert(getContent(reader), stripTags, stripBasic);
	}
	
	public static String tryConvert(String content, boolean stripTags, boolean stripBasic) {
		List<SubEntry> subs = isValidSRT(content) ? parseSRTSubtitles(content) : parseASSSubtitles(content);
		return toVTT(subs, stripBasic, stripBasic);
	}
	
	private static String toVTT(List<SubEntry> subs, boolean stripTags, boolean stripBasic) {
		StringBuilder builder = new StringBuilder("WEBVTT\r\n\r\n");
		int i = 1;
		for (SubEntry entry : subs) {
			builder.append(i).append("\r\n")
					.append(entry.getTimeCodeString(true)).append("\r\n")
					.append(entry.getText(stripTags, stripBasic, null)).append("\r\n")
					.append("\r\n");
			++i;
		}
		return builder.toString();
	}

	public static void main(String[] args) throws IOException {
		URL url = new URL("http://server1.vuiphim.tv/sub/Swelter.2014.1080p.BluRay.DTS.x264-RARBG.srt");
		BufferedInputStream in = new BufferedInputStream(url.openStream());
		in.mark(1 << 32);
		//detect encoding
		byte[] buf = new byte[4096];
	    // (1)
	    UniversalDetector detector = new UniversalDetector(null);

	    // (2)
	    int nread;
	    while ((nread = in.read(buf)) > 0 && !detector.isDone()) {
	      detector.handleData(buf, 0, nread);
	    }
	    // (3)
	    detector.dataEnd();
	    // (4)
	    String encoding = detector.getDetectedCharset();
	    in.reset();
	    Reader reader = (encoding != null) ? new InputStreamReader(in, encoding) : new InputStreamReader(in);	    
		System.out.println(VTTConverter.tryConvert(reader, false, false));
	}
}
