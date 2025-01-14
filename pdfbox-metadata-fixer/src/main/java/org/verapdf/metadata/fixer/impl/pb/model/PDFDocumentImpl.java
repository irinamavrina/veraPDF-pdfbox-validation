/**
 * This file is part of veraPDF Metadata Fixer, a module of the veraPDF project.
 * Copyright (c) 2015, veraPDF Consortium <info@verapdf.org>
 * All rights reserved.
 *
 * veraPDF Metadata Fixer is free software: you can redistribute it and/or modify
 * it under the terms of either:
 *
 * The GNU General public license GPLv3+.
 * You should have received a copy of the GNU General Public License
 * along with veraPDF Metadata Fixer as the LICENSE.GPL file in the root of the source
 * tree.  If not, see http://www.gnu.org/licenses/ or
 * https://www.gnu.org/licenses/gpl-3.0.en.html.
 *
 * The Mozilla Public License MPLv2+.
 * You should have received a copy of the Mozilla Public License along with
 * veraPDF Metadata Fixer as the LICENSE.MPL file in the root of the source tree.
 * If a copy of the MPL was not distributed with this file, you can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.verapdf.metadata.fixer.impl.pb.model;

import org.verapdf.xmp.XMPException;
import org.verapdf.xmp.impl.VeraPDFMeta;
import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.verapdf.metadata.fixer.entity.InfoDictionary;
import org.verapdf.metadata.fixer.entity.Metadata;
import org.verapdf.metadata.fixer.entity.PDFDocument;
import org.verapdf.pdfa.results.MetadataFixerResult;
import org.verapdf.pdfa.results.MetadataFixerResultImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.verapdf.pdfa.results.MetadataFixerResult.RepairStatus.*;

/**
 * @author Evgeniy Muravitskiy
 */
public class PDFDocumentImpl implements PDFDocument {

	private static final Logger LOGGER = Logger.getLogger(PDFDocumentImpl.class);

	private final PDDocument document;
	private MetadataImpl metadata;
	private InfoDictionaryImpl info;
	private boolean isUnfiltered = false;

	/**
	 * Create a new PDFDocumentImpl from the passed InputStream
	 * 
	 * @param pdfStream
	 *            an {@link java.io.InputStream} to be parsed as a PDF Document.
	 * @throws IOException
	 *             when there's a problem reading or parsing the file.
	 */
	public PDFDocumentImpl(InputStream pdfStream) throws IOException {
		this(PDDocument.load(pdfStream, false, true));
	}

	/**
	 * @param document
	 */
	public PDFDocumentImpl(PDDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("Document representation can not be null");
		}
		this.document = document;
		this.metadata = parseMetadata();
		this.info = this.getInfo();
	}

	private MetadataImpl parseMetadata() {
		PDDocumentCatalog catalog = this.document.getDocumentCatalog();
		PDMetadata meta = catalog.getMetadata();
		if (meta == null) {
			try (COSStream stream = this.document.getDocument().createCOSStream()) {
				catalog.setMetadata(new PDMetadata(stream));
				catalog.getCOSObject().setNeedToBeUpdated(true);
				VeraPDFMeta xmp = VeraPDFMeta.create();
				return new MetadataImpl(xmp, stream);
			} catch (IOException excep) {
				// TODO Auto-generated catch block
				excep.printStackTrace();
			}
		}
		return parseMetadata(meta);
	}

	private static MetadataImpl parseMetadata(PDMetadata meta) {
		try {
			VeraPDFMeta xmp = VeraPDFMeta.parse(meta.getStream().getUnfilteredStream());
			if (xmp != null) {
				return new MetadataImpl(xmp, meta.getStream());
			}
		} catch (IOException e) {
			LOGGER.debug("Problems with document parsing or structure. " + e.getMessage(), e);
		} catch (XMPException e) {
			LOGGER.debug("Problems with XMP parsing. " + e.getMessage(), e);
		}
		return null;
	}

	private InfoDictionaryImpl getInfo() {
		COSDictionary trailer = this.document.getDocument().getTrailer();
		COSBase infoDict = trailer.getDictionaryObject(COSName.INFO);
		return !(infoDict instanceof COSDictionary) ? null
				: new InfoDictionaryImpl(new PDDocumentInformation((COSDictionary) infoDict));
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public Metadata getMetadata() {
		return this.metadata;
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public InfoDictionary getInfoDictionary() {
		return this.info;
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public boolean isNeedToBeUpdated() {
		boolean metaUpd = this.metadata != null && this.metadata.isNeedToBeUpdated();
		boolean infoUpd = this.info != null && this.info.isNeedToBeUpdated();
		return metaUpd || infoUpd || this.isUnfiltered;
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public MetadataFixerResult saveDocumentIncremental(final MetadataFixerResultImpl.RepairStatus status,
			OutputStream output) {
		MetadataFixerResultImpl.Builder builder = new MetadataFixerResultImpl.Builder();
		try {
			PDMetadata meta = this.document.getDocumentCatalog().getMetadata();
			boolean isMetaPresent = meta != null && this.isNeedToBeUpdated();
			boolean isMetaAdd = meta == null && this.metadata != null;
			if (isMetaPresent || isMetaAdd) {
				this.metadata.updateMetadataStream();
				if (isMetaAdd) {
					this.document.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
				}
				this.document.saveIncremental(output);
				output.close();
				builder.status(getStatus(status));
			} else {
				builder.status(status);
			}
		} catch (Exception e) {
			LOGGER.info(e);
			builder.status(FIX_ERROR).addFix("Problems with document save. " + e.getMessage());
		}
		return builder.build();
	}

	@Override
	public int removeFiltersForAllMetadataObjects() {
		int res = 0;
		try {
			List<COSObject> objects = this.document.getDocument().getObjectsByType(COSName.METADATA);

			List<COSStream> metas = new ArrayList<>();
			for (COSObject obj : objects) {
				COSBase base = obj.getObject();
				if (base instanceof COSStream) {
					metas.add((COSStream) base);
				} else {
					LOGGER.debug("Founded non-stream Metadata dictionary.");
				}
			}
			for (COSStream stream : metas) {
				COSBase filters = stream.getFilters();
				if (filters instanceof COSName || (filters instanceof COSArray && ((COSArray) filters).size() != 0)) {
					try {
						stream.setFilters(null);
						stream.setNeedToBeUpdated(true);
						++res;
					} catch (IOException e) {
						LOGGER.debug("Problems with unfilter stream.", e);
						return -1;
					}
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Can not obtain Metadata objects", e);
			return -1;
		}

		isUnfiltered = res > 0;

		return res;
	}

	private static MetadataFixerResultImpl.RepairStatus getStatus(final MetadataFixerResultImpl.RepairStatus status) {
		return status == NO_ACTION ? SUCCESS : status;
	}

}
