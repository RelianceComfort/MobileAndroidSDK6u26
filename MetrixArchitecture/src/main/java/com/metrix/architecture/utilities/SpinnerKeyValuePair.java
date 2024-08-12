package com.metrix.architecture.utilities;

import java.io.Serializable;

public class SpinnerKeyValuePair implements Serializable {

	public String spinnerKey, spinnerValue;

	public SpinnerKeyValuePair(String key, String value) {
		spinnerKey = key;
		spinnerValue = value;
	}

	public String toString() {
		return spinnerKey;
	}
}
