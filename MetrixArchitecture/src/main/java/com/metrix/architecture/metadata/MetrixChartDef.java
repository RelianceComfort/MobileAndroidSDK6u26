package com.metrix.architecture.metadata;

import java.util.ArrayList;

import android.graphics.Color;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class MetrixChartDef {

	public String title;
	public String description;
	public String type;
	public int backgroundColor;
	public int labelColor;
	public int axisColor;
	public boolean showAxis;
	public boolean showLabels;
	public int labelTextSize;
	public int titleTextSize;
	public int legendTextSize;
	public ArrayList<MetrixChartValue> values;

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.values = new ArrayList<MetrixChartValue>();
		this.labelTextSize = 20;
		this.titleTextSize = 30;
		this.legendTextSize = 30;
		this.labelColor = Color.BLACK;
	}

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor, ArrayList<MetrixChartValue> values) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.values = values;
		this.labelTextSize = 20;
		this.titleTextSize = 30;
		this.legendTextSize = 30;
		this.labelColor = Color.BLACK;
	}

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor, boolean showLabels, int labelColor) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.labelColor = labelColor;
		this.showLabels = showLabels;
		this.values = new ArrayList<MetrixChartValue>();
		this.labelTextSize = 20;
		this.titleTextSize = 30;
		this.legendTextSize = 30;
	}

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor, boolean showLabels, int labelColor,
			boolean showAxis, int axisColor) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.labelColor = labelColor;
		this.axisColor = axisColor;
		this.showLabels = showLabels;
		this.showAxis = showAxis;
		this.values = new ArrayList<MetrixChartValue>();
		this.labelTextSize = 20;
		this.titleTextSize = 30;
		this.legendTextSize = 30;
	}

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor, boolean showLabels, int labelColor,
			boolean showAxis, int axisColor, ArrayList<MetrixChartValue> values) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.labelColor = labelColor;
		this.axisColor = axisColor;
		this.showLabels = showLabels;
		this.showAxis = showAxis;
		this.values = values;
		this.labelTextSize = 20;
		this.titleTextSize = 30;
		this.legendTextSize = 30;
	}

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor, boolean showLabels, int labelColor,
			boolean showAxis, int axisColor, int labelTextSize, int titleTextSize, int legendTextSize) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.labelColor = labelColor;
		this.axisColor = axisColor;
		this.showLabels = showLabels;
		this.showAxis = showAxis;
		this.titleTextSize = titleTextSize;
		this.labelTextSize = labelTextSize;
		this.legendTextSize = legendTextSize;
		this.values = new ArrayList<MetrixChartValue>();
	}

	public MetrixChartDef(String title, String description, String type,
			int backgroundColor, boolean showLabels, int labelColor,
			boolean showAxis, int axisColor, int labelTextSize, int titleTextSize, int legendTextSize, ArrayList<MetrixChartValue> values) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.backgroundColor = backgroundColor;
		this.labelColor = labelColor;
		this.axisColor = axisColor;
		this.showLabels = showLabels;
		this.showAxis = showAxis;
		this.titleTextSize = titleTextSize;
		this.labelTextSize = labelTextSize;
		this.legendTextSize = legendTextSize;
		this.values = values;
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();

		value.append(AndroidResourceHelper.getMessage("Title1Args", this.title));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Description1Args", this.description));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Type1Args", this.type));

		return value.toString();
	}
}
