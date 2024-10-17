package de.tsenger.vdstools.idb;

public class IcaoBarcodeFormat {
	public static final String BARCODE_IDENTIFIER = "NDB1";

	char barcodeFlag;
	BarcodePayload barcodePayload;

	public IcaoBarcodeFormat(char barcodeFlag, BarcodePayload barcodePayload) {
		this.barcodeFlag = barcodeFlag;
		this.barcodePayload = barcodePayload;
	}

	public boolean isSigned() {
		return ((byte) (((byte) barcodeFlag) - 0x41) & 0x01) == 0x01;
	}

	public boolean isZipped() {
		return ((byte) (((byte) barcodeFlag) - 0x41) & 0x02) == 0x02;
	}
}
