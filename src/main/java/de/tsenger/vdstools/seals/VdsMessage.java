package de.tsenger.vdstools.seals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;

/**
 * @author Tobias Senger
 *
 */
public class VdsMessage {
	
	private ArrayList<MessageTlv> messageTlvList;
	
	public VdsMessage(List<MessageTlv> messageTlvList) {
		this.messageTlvList = (ArrayList<MessageTlv>) messageTlvList;
	}
	
	public VdsMessage() {
		this.messageTlvList = new ArrayList<>(5);	
	}

	public byte[] getRawBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (MessageTlv feature : this.messageTlvList) {
				baos.write(DataEncoder.buildTLVStructure(feature.getTag(), feature.getValue()));
			}
		} catch (IOException e) {
			Logger.error("Can't build raw bytes: "+e.getMessage());
			return new byte[0];
		}
		return baos.toByteArray();
	}

	public void addMessageTlv(MessageTlv tlv) {
		this.messageTlvList.add(tlv);
	}

	public ArrayList<MessageTlv> getMessageTlvList() {
		return this.messageTlvList;
	}
	
	
	
}
