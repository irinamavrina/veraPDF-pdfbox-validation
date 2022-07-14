/**
 * This file is part of veraPDF PDF Box PDF/A Validation Model Implementation, a module of the veraPDF project.
 * Copyright (c) 2015, veraPDF Consortium <info@verapdf.org>
 * All rights reserved.
 *
 * veraPDF PDF Box PDF/A Validation Model Implementation is free software: you can redistribute it and/or modify
 * it under the terms of either:
 *
 * The GNU General public license GPLv3+.
 * You should have received a copy of the GNU General Public License
 * along with veraPDF PDF Box PDF/A Validation Model Implementation as the LICENSE.GPL file in the root of the source
 * tree.  If not, see http://www.gnu.org/licenses/ or
 * https://www.gnu.org/licenses/gpl-3.0.en.html.
 *
 * The Mozilla Public License MPLv2+.
 * You should have received a copy of the Mozilla Public License along with
 * veraPDF PDF Box PDF/A Validation Model Implementation as the LICENSE.MPL file in the root of the source tree.
 * If a copy of the MPL was not distributed with this file, you can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.verapdf.model.impl.pb.pd.signatures;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.verapdf.model.impl.pb.pd.PBoxPDObject;
import org.verapdf.model.pdlayer.PDSigRef;

/**
 * @author Sergey Shemyakov
 */
public class PBoxPDSigRef extends PBoxPDObject implements PDSigRef{

	/** Type name for {@code PBoxPDSigRef} */
	public static final String SIGNATURE_REFERENCE_TYPE = "PDSigRef";

	/**
	 * @param dictionary is signature reference dictionary.
	 */
	public PBoxPDSigRef(COSDictionary dictionary, PDDocument document) {
		super(dictionary, SIGNATURE_REFERENCE_TYPE);
		this.document = document;
	}

	/**
	 * @return true if the document permissions dictionary contains DocMDP entry.
	 */
	@Override
	public Boolean getpermsContainDocMDP() {
		COSDictionary documentCatalog =
				this.document.getDocumentCatalog().getCOSObject();
		COSDictionary perms = (COSDictionary)
				documentCatalog.getDictionaryObject(COSName.PERMS);
		if (perms == null) {
			return Boolean.FALSE;
		}
		return Boolean.valueOf(perms.containsKey(COSName.DOC_MDP));
	}
}
