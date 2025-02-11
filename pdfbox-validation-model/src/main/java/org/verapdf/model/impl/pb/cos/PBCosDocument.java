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
package org.verapdf.model.impl.pb.cos;

import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.verapdf.model.baselayer.Object;
import org.verapdf.model.coslayer.CosDocument;
import org.verapdf.model.coslayer.CosIndirect;
import org.verapdf.model.coslayer.CosTrailer;
import org.verapdf.model.coslayer.CosXRef;
import org.verapdf.model.impl.pb.pd.PBoxPDDocument;
import org.verapdf.model.tools.FileSpecificationKeysHelper;
import org.verapdf.model.tools.XMPChecker;
import org.verapdf.pdfa.flavours.PDFAFlavour;

import java.io.IOException;
import java.util.*;

/**
 * Low-level PDF Document object
 *
 * @author Evgeniy Muravitskiy
 */
public class PBCosDocument extends PBCosObject implements CosDocument {

	private static final Logger LOGGER = Logger.getLogger(PBCosDocument.class);

	/** Type name for PBCosDocument */
	public static final String COS_DOCUMENT_TYPE = "CosDocument";

	public static final String TRAILER = "trailer";
	public static final String XREF = "xref";
	public static final String INDIRECT_OBJECTS = "indirectObjects";
	public static final String DOCUMENT = "document";
	public static final String DOC = "doc";
	public static final String EMBEDDED_FILES = "EmbeddedFiles";
	public static final String ID = "ID";
	public static final String REQUIREMENTS = "Requirements";
	public static final COSName PIECE_INFO = COSName.getPDFName("PieceInfo");

	private final PDFAFlavour flavour;

	private PDDocument pdDocument;

	private final long indirectObjectCount;
	private final float headerVersion;
	private final long headerOffset;
	private final String header;
	private final int headerCommentByte1;
	private final int headerCommentByte2;
	private final int headerCommentByte3;
	private final int headerCommentByte4;
	private final boolean isOptionalContentPresent;
	private final boolean isLinearised;
	private final int postEOFDataSize;
	private final Boolean doesInfoMatchXMP;
	private final String firstPageID;
	private final String lastID;
	private final boolean needsRendering;

	private final COSDictionary catalog;

	/**
	 * Default constructor
	 * 
	 * @param pdDocument
	 *            pdfbox PDDocument
	 */
	public PBCosDocument(PDDocument pdDocument, PDFAFlavour flavour) {
		this(pdDocument.getDocument(), flavour);
		this.pdDocument = pdDocument;
		if (flavour.getPart() == PDFAFlavour.Specification.ISO_19005_3) {
			FileSpecificationKeysHelper.registerFileSpecificationKeys(pdDocument);
		}
	}

	/**
	 * Constructor using pdfbox COSDocument
	 * 
	 * @param cosDocument
	 *            pdfbox COSDocument
	 */
	public PBCosDocument(COSDocument cosDocument, PDFAFlavour flavour) {
		super(cosDocument, COS_DOCUMENT_TYPE);
		this.catalog = this.getCatalog();
		this.flavour = flavour;

		this.indirectObjectCount = cosDocument.getObjects().size();
		this.headerVersion = cosDocument.getVersion();
		this.headerOffset = cosDocument.getHeaderOffset();
		this.header = cosDocument.getHeader();
		this.headerCommentByte1 = cosDocument.getHeaderCommentByte1();
		this.headerCommentByte2 = cosDocument.getHeaderCommentByte2();
		this.headerCommentByte3 = cosDocument.getHeaderCommentByte3();
		this.headerCommentByte4 = cosDocument.getHeaderCommentByte4();
		this.isOptionalContentPresent = parseOptionalContentPresent();
		this.postEOFDataSize = cosDocument.getPostEOFDataSize();
		if (cosDocument.getLastTrailer() != null) {
			this.lastID = getTrailerID((COSArray)
					cosDocument.getLastTrailer().getDictionaryObject(ID));
		} else {
			this.lastID = null;
		}
		if (cosDocument.getFirstPageTrailer() != null) {
			this.firstPageID = getTrailerID((COSArray)
					cosDocument.getFirstPageTrailer().getDictionaryObject(ID));
		} else {
			this.firstPageID = null;
		}
		this.isLinearised = cosDocument.getTrailer() != cosDocument.getLastTrailer() && cosDocument.isLinearized();
		this.doesInfoMatchXMP = XMPChecker.doesInfoMatchXMP(cosDocument);
		this.needsRendering = this.getNeedsRenderingValue();
	}

	private boolean parseOptionalContentPresent() {
		return this.catalog != null && this.catalog.getDictionaryObject(COSName.OCPROPERTIES) != null;
	}

	/**
	 * Number of indirect objects in the document
	 */
	@Override
	public Long getnrIndirects() {
		return Long.valueOf(this.indirectObjectCount);
	}

	/**
	 * @return version of pdf document
	 */
	@Override
	public Double getheaderVersion() {
		return Double.valueOf(this.headerVersion);
	}

	@Override
	public Long getheaderOffset() {
		return Long.valueOf(this.headerOffset);
	}

	@Override
	public String getheader() {
		return this.header;
	}

	@Override
	public Long getheaderByte1() {
		return Long.valueOf(this.headerCommentByte1);
	}

	@Override
	public Long getheaderByte2() {
		return Long.valueOf(this.headerCommentByte2);
	}

	@Override
	public Long getheaderByte3() {
		return Long.valueOf(this.headerCommentByte3);
	}

	@Override
	public Long getheaderByte4() {
		return Long.valueOf(this.headerCommentByte4);
	}

	/**
	 * true if catalog contain OCProperties key
	 */
	@Override
	public Boolean getisOptionalContentPresent() {
		return Boolean.valueOf(this.isOptionalContentPresent);
	}

	/**
	 * EOF must complies PDF/A standard
	 */
	@Override
	public Long getpostEOFDataSize() {
		return Long.valueOf(this.postEOFDataSize);
	}

	/**
	 * @return ID of first page trailer
	 */
	@Override
	public String getfirstPageID() {
		return this.firstPageID;
	}

	/**
	 * @return ID of last document trailer
	 */
	@Override
	public String getlastID() {
		if (flavour.getPart().equals(PDFAFlavour.Specification.ISO_19005_1)) {
			return this.lastID;
		} else if (this.isLinearised) {
			return this.firstPageID;
		} else {
			return this.lastID;
		}
	}

	private static String getTrailerID(COSArray ids) {
		if (ids != null) {
			StringBuilder builder = new StringBuilder();
			for (COSBase id : ids) {
				for (byte aByte : ((COSString) id).getBytes()) {
					builder.append((char) (aByte & 0xFF));
				}
			}
			// need to discard last whitespace
			return builder.toString();
		}
		return null;
	}

	/**
	 * @return true if the current document is linearized
	 */
	@Override
	public Boolean getisLinearized() {
		return Boolean.valueOf(this.isLinearised);
	}

	/**
	 * @return true if XMP content matches Info dictionary content
	 */
	@Override
	public Boolean getdoesInfoMatchXMP() {
		return this.doesInfoMatchXMP;
	}

	@Override
	public Boolean getMarked() {
		if (this.catalog != null) {
			COSBase markInfo = this.catalog.getDictionaryObject(COSName.MARK_INFO);
			if (markInfo == null) {
				return null;
			} else if (markInfo instanceof COSDictionary) {
				COSName marked = COSName.getPDFName("Marked");
				boolean value = ((COSDictionary) markInfo).getBoolean(marked, false);
				return Boolean.valueOf(value);
			} else {
				LOGGER.debug("MarkedInfo must be a 'COSDictionary' but got: " + markInfo.getClass().getSimpleName());
				return null;
			}
		}
		return null;
	}

	@Override
	public Boolean getSuspects() {
		if (this.catalog != null) {
			COSBase markInfo = this.catalog.getDictionaryObject(COSName.MARK_INFO);
			if (markInfo == null) {
				return null;
			} else if (markInfo instanceof COSDictionary) {
				COSName suspects = COSName.getPDFName("Suspects");
				return ((COSDictionary) markInfo).getBoolean(suspects, false);
			} else {
				LOGGER.debug("MarkedInfo must be a 'COSDictionary' but got: " + markInfo.getClass().getSimpleName());
				return null;
			}
		}
		return null;
	}

	@Override
	public Boolean getDisplayDocTitle() {
		if (this.catalog != null) {
			COSBase viewerPref = this.catalog.getDictionaryObject(COSName.VIEWER_PREFERENCES);
			if (viewerPref == null) {
				return null;
			} else if (viewerPref instanceof COSDictionary) {
				COSName displayDocTitle = COSName.getPDFName("DisplayDocTitle");
				boolean value = ((COSDictionary) viewerPref).getBoolean(displayDocTitle, false);
				return Boolean.valueOf(value);
			} else {
				LOGGER.debug("viewerPref must be a 'COSDictionary' but got: " + viewerPref.getClass().getSimpleName());
				return null;
			}
		}
		return null;
	}

	@Override
	public Boolean getcontainsInfo() {
		return ((COSDocument)baseObject).getTrailer().getDictionaryObject(COSName.INFO) != null;
	}

	@Override
	public Boolean getcontainsPieceInfo() {
		return this.catalog != null && this.catalog.getDictionaryObject(PIECE_INFO) != null;
	}


	@Override
	public String getMarkInfo() {
		return null;
	}

	@Override
	public String getViewerPreferences() {
		return null;
	}

	@Override
	public String getRequirements() {
		if (this.catalog != null) {

			COSBase reqArray = this.catalog.getDictionaryObject(COSName.getPDFName(REQUIREMENTS));

			if (reqArray instanceof COSArray) {
				return PBCosDocument.getRequirementsString(reqArray);
			}
		}
		return null;
	}

	private static String getRequirementsString(COSBase reqArray) {
		String result = "";
		for (COSBase element : (COSArray) reqArray) {
			if (element instanceof COSDictionary) {
				String sKey = ((COSDictionary) element).getString(COSName.S);
				result += sKey;
				result += " ";
			}
		}
		return result;
	}

	/**
	 * @return true if {@code NeedsRendering} entry contains {@code true} value
	 */
	@Override
	public Boolean getNeedsRendering() {
		return Boolean.valueOf(this.needsRendering);
	}

	@Override
	public Boolean getcontainsEmbeddedFiles() {
		if (catalog != null) {
			COSBase names = this.catalog.getDictionaryObject(COSName.NAMES);
			if (names != null && names instanceof COSDictionary) {
				return ((COSDictionary) names).containsKey(COSName.EMBEDDED_FILES);
			}
		}
		return Boolean.valueOf(false);
	}

	@Override
	public List<? extends Object> getLinkedObjects(String link) {
		switch (link) {
		case TRAILER:
			return this.getTrailer();
		case INDIRECT_OBJECTS:
			return this.getIndirectObjects();
		case DOCUMENT:
			return this.getDocument();
		case XREF:
			return this.getXRefs();
		case EMBEDDED_FILES:
			return this.getEmbeddedFiles();
		case DOC:
			return Collections.emptyList();
		default:
			return super.getLinkedObjects(link);
		}
	}

	/**
	 * @return list of embedded files
	 */
	private List<Object> getEmbeddedFiles() {
		if (this.catalog != null) {
			COSDictionary buffer = (COSDictionary) this.catalog.getDictionaryObject(COSName.NAMES);
			if (buffer != null) {
				COSBase base = buffer.getDictionaryObject(COSName.EMBEDDED_FILES);
				if (base instanceof COSDictionary) {
					List<Object> files = new ArrayList<>();
					this.getNamesEmbeddedFiles(files, new PDEmbeddedFilesNameTreeNode((COSDictionary) base));
					return Collections.unmodifiableList(files);
				}
			}
		}
		return Collections.emptyList();
	}

	private void getNamesEmbeddedFiles(List<Object> files,
									   PDNameTreeNode<PDComplexFileSpecification> node) {
		try {
			final Map<String, PDComplexFileSpecification> names = node.getNames();
			if (names != null) {
				final Set<Map.Entry<String, PDComplexFileSpecification>> entries = names.entrySet();
				for (Map.Entry<String, PDComplexFileSpecification> entry : entries) {
					files.add(
							new PBCosFileSpecification(entry.getValue().getCOSObject(), this.pdDocument, this.flavour));
				}
			}
			if (node.getKids() != null) {
				for (PDNameTreeNode kid : node.getKids()) {
					getNamesEmbeddedFiles(files, kid);
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Something wrong with getting embedded files - return empty list. " + e.getMessage(), e);
		}
	}

	/**
	 * trailer dictionary
	 */
	private List<CosTrailer> getTrailer() {
		COSDocument cosDocument = (COSDocument) this.baseObject;
		List<CosTrailer> list = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
		list.add(new PBCosTrailer(cosDocument.getTrailer(), this.pdDocument, this.flavour));
		return Collections.unmodifiableList(list);
	}

	/**
	 * all indirect objects referred from the xref table
	 */
	private List<CosIndirect> getIndirectObjects() {
		List<COSObject> objects = ((COSDocument) this.baseObject).getObjects();
		List<CosIndirect> list = new ArrayList<>(objects.size());
		for (COSObject object : objects) {
			list.add(new PBCosIndirect(object, this.pdDocument, this.flavour));
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * link to the high-level PDF Document structure
	 */
	private List<org.verapdf.model.pdlayer.PDDocument> getDocument() {
		if (pdDocument != null) {
			List<org.verapdf.model.pdlayer.PDDocument> document = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
			document.add(new PBoxPDDocument(pdDocument, flavour));
			return Collections.unmodifiableList(document);
		}
		return Collections.emptyList();
	}

	/**
	 * link to cross reference table properties
	 */
	private List<CosXRef> getXRefs() {
		COSDocument cosDocument = (COSDocument) this.baseObject;
		List<CosXRef> list = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
		list.add(new PBCosXRef(cosDocument.subSectionHeaderSpaceSeparated(), cosDocument.isXrefEOLMarkersComplyPDFA()));
		return Collections.unmodifiableList(list);
	}

	private boolean getNeedsRenderingValue() {
		COSName needsRenderingLocal = COSName.getPDFName("NeedsRendering");
		return this.catalog != null && this.catalog.getBoolean(needsRenderingLocal, false);
	}

	private COSDictionary getCatalog() {
		COSBase catalogLocal = ((COSDocument) this.baseObject).getTrailer().getDictionaryObject(COSName.ROOT);
		return catalogLocal instanceof COSDictionary ? (COSDictionary) catalogLocal : null;
	}

}
