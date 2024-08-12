package com.metrix.architecture.utilities;

/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This file was renamed from IntentResult to MetrixBarcodeScanResult
 * by IFS/Metrix to present a more consistent experience to application 
 * developers.
 *
 */

/**
 * <p>
 * Encapsulates the result of a barcode scan invoked through
 * {@link IntentIntegrator}.
 * </p>
 * 
 * @author Sean Owen
 * 
 * @since 5.6
 */
public final class MetrixBarcodeScanResult {

	private final String contents;
	private final String formatName;
	private final byte[] rawBytes;
	private final Integer orientation;
	private final String errorCorrectionLevel;
	
	public MetrixBarcodeScanResult() {
		this(null, null, null, null, null);
	}

	public MetrixBarcodeScanResult(String contents, String formatName, byte[] rawBytes, Integer orientation, String errorCorrectionLevel) {
		this.contents = contents;
		this.formatName = formatName;
		this.rawBytes = rawBytes;
		this.orientation = orientation;
		this.errorCorrectionLevel = errorCorrectionLevel;
	}

	/**
	 * @return raw content of barcode
	 */
	public String getContents() {
		return contents;
	}

	/**
	 * @return name of format, like "QR_CODE", "UPC_A". See
	 *         {@code BarcodeFormat} for more format names.
	 */
	public String getFormatName() {
		return formatName;
	}

	/**
	 * @return raw bytes of the barcode content, if applicable, or null
	 *         otherwise
	 */
	public byte[] getRawBytes() {
		return rawBytes;
	}

	/**
	 * @return rotation of the image, in degrees, which resulted in a successful
	 *         scan. May be null.
	 */
	public Integer getOrientation() {
		return orientation;
	}

	/**
	 * @return name of the error correction level used in the barcode, if
	 *         applicable
	 */
	public String getErrorCorrectionLevel() {
		return errorCorrectionLevel;
	}

	@Override
	public String toString() {
		StringBuilder dialogText = new StringBuilder(100);
		dialogText.append(AndroidResourceHelper.getMessage("Format1Args", formatName)).append('\n');
		dialogText.append(AndroidResourceHelper.getMessage("Contents1Args", contents)).append('\n');
		int rawBytesLength = rawBytes == null ? 0 : rawBytes.length;
		dialogText.append(AndroidResourceHelper.getMessage("RawBytes1Args", rawBytesLength)).append("\n");
		dialogText.append(AndroidResourceHelper.getMessage("Orientation1Args", orientation)).append('\n');
		dialogText.append(AndroidResourceHelper.getMessage("ECLevel1Args", errorCorrectionLevel)).append('\n');
		return dialogText.toString();
	}

}