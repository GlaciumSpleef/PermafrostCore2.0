package io.paper.permafrost.utilities;

import java.awt.Color;

public enum PurchasableRank {
	GLACIER("Glacier", "" + gradient("Glacier","#B8F4FF","#1E8FC2") + "§f " , "glacier"),
	POLAR("Polar", "" + gradient("Polar", "#72FAF4", "#03C69B") + "§f ", "polar"), 
	CUSTOM("Custom", "§d§lCUSTOM §r","custom");

	private final String displayName;
	private final String prefix;
	private final String dbKey;

	PurchasableRank(String displayName, String prefix, String dbKey) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.dbKey = dbKey;
    }

	public String getDisplayName() {
		return displayName;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getDbKey() {
		return dbKey;
	}

	public static String gradient(String text, String startHex, String endHex) {
		Color start = Color.decode(startHex);
		Color end = Color.decode(endHex);

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			double ratio = (double) i / (text.length() - 1);

			int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
			int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
			int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

			result.append(toHex(r, g, b)).append(text.charAt(i));
		}

		return result.toString();
	}

	private static String toHex(int r, int g, int b) {
		return String.format("§x§%1$X§%2$X§%3$X§%4$X§%5$X§%6$X", (r >> 4) & 0xF, r & 0xF, (g >> 4) & 0xF, g & 0xF,
				(b >> 4) & 0xF, b & 0xF);
	}

}
