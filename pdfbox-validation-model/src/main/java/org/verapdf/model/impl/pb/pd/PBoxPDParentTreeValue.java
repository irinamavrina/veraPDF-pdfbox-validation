package org.verapdf.model.impl.pb.pd;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.common.COSObjectable;

/**
 * Described in the PDF specification in the ParentTree segment of the table "Entries in the
 * structure tree root". This is either a dictionary or an array.
 *
 * @author Tilman Hausherr
 */
public class PBoxPDParentTreeValue implements COSObjectable {
	COSObjectable obj;

	public PBoxPDParentTreeValue(COSArray obj) {
		this.obj = obj;
	}

	public PBoxPDParentTreeValue(COSDictionary obj) {
		this.obj = obj;
	}

	@Override
	public COSBase getCOSObject() {
		return obj.getCOSObject();
	}

	@Override
	public String toString() {
		return obj.toString();
	}
}
