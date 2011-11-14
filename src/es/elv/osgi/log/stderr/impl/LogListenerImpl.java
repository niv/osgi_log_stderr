package es.elv.osgi.log.stderr.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

public class LogListenerImpl implements LogListener, ManagedService {
	private boolean printStackTraces = true;

	private Integer minLevel = LogService.LOG_INFO;
	private Map<String, Integer> minLevels = new HashMap<String, Integer>();

	void addLRS(LogReaderService lrs) {
		lrs.addLogListener(this);
	}

	void removeLRS(LogReaderService lrs) {
		lrs.removeLogListener(this);
	}

	private int getMinLevelFor(String bundleName) {
		if (minLevels.containsKey(bundleName))
			return minLevels.get(bundleName);
		else
			return minLevel;
	}

	@Override
	public void logged(LogEntry log) {
		if (log.getMessage() == null)
			return;

		if (log.getLevel() > getMinLevelFor(log.getBundle().getSymbolicName()))
			return;

		// Format: bundlesymbolicname [BundleID]: SEVERITY message\nException

		StringBuilder b = new StringBuilder();
		b.append(log.getBundle().getSymbolicName());
		b.append("[" + log.getBundle().getBundleId() + "]");
		b.append(": ");
		switch (log.getLevel()) {
			case LogService.LOG_DEBUG:   b.append("DEBUG"); break;
			case LogService.LOG_INFO:    b.append("INFO"); break;
			case LogService.LOG_WARNING: b.append("WARNING"); break;
			case LogService.LOG_ERROR:   b.append("ERROR"); break;
		}

		b.append(" ");
		b.append(log.getMessage());

		if (printStackTraces  && log.getException() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter w = new PrintWriter(sw);
			log.getException().printStackTrace(w);
			b.append("\n");
			b.append(sw.getBuffer());
			b.append("\n");
		}

		System.err.println(b.toString());
	}
	
	private int resolveLevel(String level) throws ConfigurationException {
		if (level.equals("DEBUG"))
			return LogService.LOG_DEBUG;
		else if (level.equals("INFO"))
			return LogService.LOG_INFO;
		else if (level.equals("WARNING"))
			return LogService.LOG_WARNING;
		else if (level.equals("ERROR"))
			return LogService.LOG_ERROR;
		else
			throw new ConfigurationException("min_level",
				"Invalid value '" + level + "', requires one of: DEBUG INFO WARNING ERROR");
	}
	
	@Override
	public void updated(@SuppressWarnings("rawtypes") Dictionary c) throws ConfigurationException {
		if (c == null)
			return;

		String levelToSet = (String) c.get("min_level");
		minLevel = resolveLevel(levelToSet);

		String bundleMinLevels = (String) c.get("bundle_min_levels");
		if (bundleMinLevels != null && bundleMinLevels.trim().length() > 0) {
			bundleMinLevels = bundleMinLevels.trim();
			StringTokenizer tk = new StringTokenizer(bundleMinLevels, ",");
			while (tk.hasMoreTokens()) {
				String tok = tk.nextToken().trim();
				String[] splt = tok.split("=");
				if (splt.length != 2)
					throw new ConfigurationException("bundle_min_levels", "invalid bundle descriptor/token: " + tok);
				String symname = splt[0].trim();
				int lv = resolveLevel(splt[1].trim());
				minLevels.put(symname, lv);
			}
		}
		
		printStackTraces = (Boolean) c.get("print_stack_traces");
	}

}
